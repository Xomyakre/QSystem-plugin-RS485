/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.bind.JAXBException;
import ru.apertum.cbrs485.core.RS485Property.Operator;
import ru.evgenic.rxtx.serialPort.ISerialPort;

/**
 * Создает поток, в потоке порт и им шлет.
 * Для каждого окна хранится только самое свежее событие: если порт не успевает,
 * устаревшие номера просто выбрасываются и на табло не попадают.
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
     * Минимальная пауза между посылками, мс. Порт открывается и закрывается на каждую посылку,
     * без паузы следующая посылка может уйти раньше предыдущей.
     */
    private static final long MIN_PAUSE_MS = 500;

    /**
     * Одинаковое событие для того же окна в пределах этого времени считается дублем:
     * QSystem может дернуть оба метода change() на одну смену статуса.
     */
    private static final long DUPLICATE_WINDOW_MS = 1000;

    /** окно -> последнее не отосланное событие */
    final private LinkedHashMap<String, Event> pending = new LinkedHashMap<>();
    /** окно -> последнее отосланное событие (state|ticket) */
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
        System.out.println("Sender ready (v3, enabled=" + ENABLED + "): default port=" + props.getPortName() + " protocol=" + props.getProtocols().getDefaultPtotocol());

        // поток для отсыла стартуем один раз и навсегда
        sndThread = new Thread(this::loop, "ClientboardRS485Plugin-sender");
        sndThread.setDaemon(true);
        sndThread.start();
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            final Event event;
            try {
                event = takeNext();
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
                Thread.sleep(Math.max(MIN_PAUSE_MS, props.getPause()));
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    private Event takeNext() throws InterruptedException {
        synchronized (pending) {
            while (pending.isEmpty()) {
                pending.wait();
            }
            final Iterator<Map.Entry<String, Event>> it = pending.entrySet().iterator();
            final Event event = it.next().getValue();
            it.remove();
            return event;
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
        synchronized (this) {
            port.send(bytes);
        }
    }

    /**
     * Шлем в порт только из процесса сервера. Программа оператора на том же компьютере
     * грузит этот же плагин и меняет статусы своим устаревшим копиям клиентов,
     * из-за чего на табло всплывали прошлые номера.
     * Для персонального табло у оператора запускать клиента с -Drs485.client=true
     */
    private static final boolean ENABLED = Boolean.getBoolean("rs485.client")
            || String.valueOf(System.getProperty("sun.java.command")).startsWith("ru.apertum.qsystem.Server");

    public static boolean isEnabled() {
        return ENABLED;
    }

    public void send(Event event) {
        if (!ENABLED) {
            return;
        }
        if (event == null || event.point == null || event.point.trim().isEmpty()) {
            // у клиента еще нет окна, например он только встал в очередь
            return;
        }
        if (isDuplicate(event)) {
            return;
        }
        synchronized (pending) {
            // старое не отосланное событие этого окна больше не нужно, его заменяет новое
            pending.remove(event.point);
            pending.put(event.point, event);
            pending.notifyAll();
        }
    }

    private synchronized boolean isDuplicate(Event event) {
        final String key = event.state + "|" + event.ticket;
        final long now = System.currentTimeMillis();
        final String prevKey = lastKey.put(event.point, key);
        final Long prevTime = lastTime.put(event.point, now);
        return key.equals(prevKey) && prevTime != null && now - prevTime < DUPLICATE_WINDOW_MS;
    }
}
