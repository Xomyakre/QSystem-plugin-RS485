/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.xml.bind.JAXBException;
import ru.apertum.cbrs485.core.RS485Property.Operator;
import ru.apertum.qsystem.common.CustomerState;
import ru.evgenic.rxtx.serialPort.ISerialPort;
import ru.evgenic.rxtx.serialPort.RxtxSerialPort;

/**
 * Создает поток, в потоке порт и им шлет.
 *
 * @author Evgeniy Egorov
 */
public class Sender485 {

    public static Sender485 getInstance() {
        return Sender485Holder.INSTANCE;
    }

    private static class Sender485Holder {

        private static final Sender485 INSTANCE = new Sender485();
    }

    /**
     * Одинаковое событие для того же окна в пределах этого времени считается дублем:
     * QSystem может дернуть оба метода change() на одну смену статуса.
     */
    private static final long DUPLICATE_WINDOW_MS = 1000;

    final private LinkedBlockingQueue<Event> stream = new LinkedBlockingQueue<>();
    final private HashMap<String, String> lastKey = new HashMap<>();
    final private HashMap<String, Long> lastTime = new HashMap<>();
    final private Thread sndThread;
    final private RS485Property props;

    private Sender485() {
        try {
            props = RS485Property.unmarshal(new FileInputStream(RS485Property.PROP_FILE));
        } catch (JAXBException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        System.out.println("Sender ready: default port=" + props.getPortName() + " protocol=" + props.getProtocols().getDefaultPtotocol());

        // поток для отсыла стартуем один раз и навсегда
        sndThread = new Thread(this::loop, "ClientboardRS485Plugin-sender");
        sndThread.setDaemon(true);
        sndThread.start();
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            final Event event;
            try {
                event = stream.take();
            } catch (InterruptedException ex) {
                break;
            }
            try {
                process(event);
            } catch (Throwable ex) {
                // одно плохое событие не должно убивать поток отсылки
                System.err.println("!!! ERROR !!! ClientboardRS485Plugin: " + ex);
                ex.printStackTrace();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    private void process(Event event) throws Exception {
        final Operator operator = props.getOperatorByPoint(event.point);
        if (operator == null) {
            System.out.println("!!!!!!!!!!!!!  " + event.point + " NOT FOUND");
            return;
        }
        final byte[] bytes = operator.getNProtocol(props).getData(event, operator, props);
        if (bytes == null) {
            // протоколу нечего показывать для такого состояния
            return;
        }

        final StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(b & 0xFF).append('_');
        }
        final ISerialPort port = operator.getPort(props);
        System.out.println("SEND TO RS485 [" + event.point + " " + event.state + " '" + event.ticket + "'] " + s);
        if (port instanceof RxtxSerialPort) {
            final RxtxSerialPort rp = (RxtxSerialPort) port;
            System.out.println("PORT " + port.getName() + " speed=" + rp.getSpeed() + " bits=" + rp.getDataBits()
                    + " stop=" + rp.getStopBits() + " patity=" + rp.getParity());
        }
        synchronized (this) {
            port.send(bytes);
            if (props.getPause() > 0) {
                Thread.sleep(props.getPause());
            }
        }
    }

    public void send(Event event) {
        if (event == null || event.point == null || event.point.trim().isEmpty()) {
            // у клиента еще нет окна, например он только встал в очередь
            return;
        }
        if (isDuplicate(event)) {
            return;
        }

        // Если это состояние завершения, очистим очередь для этого окна,
        // чтобы номер не появился снова после очистки табло
        if (event.state == CustomerState.STATE_FINISH
                || event.state == CustomerState.STATE_DEAD
                || event.state == CustomerState.STATE_POSTPONED
                || event.state == CustomerState.STATE_REDIRECT) {
            stream.removeIf(e -> e.point.equalsIgnoreCase(event.point));
        }

        stream.offer(event);
    }

    private synchronized boolean isDuplicate(Event event) {
        final String key = event.state + "|" + event.ticket;
        final long now = System.currentTimeMillis();
        final String prevKey = lastKey.put(event.point, key);
        final Long prevTime = lastTime.put(event.point, now);
        return key.equals(prevKey) && prevTime != null && now - prevTime < DUPLICATE_WINDOW_MS;
    }
}
