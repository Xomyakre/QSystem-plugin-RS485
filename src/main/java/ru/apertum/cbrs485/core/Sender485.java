/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.LinkedBlockingQueue;
import javax.xml.bind.JAXBException;
import ru.apertum.cbrs485.core.RS485Property.Operator;
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

    final private LinkedBlockingQueue<Event> stream = new LinkedBlockingQueue<>();
    final private Thread sndThread;
    final private RS485Property props;

    private Sender485() {
        try {
            props = RS485Property.unmarshal(new FileInputStream(RS485Property.PROP_FILE));
        } catch (JAXBException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        System.out.println("Sender ready: default port=" + props.getPortName() + " protocol=" + props.getProtocols().getDefaultPtotocol());

        // подготовим поток для отсыла
        sndThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                final Event event;
                try {
                    event = stream.take();
                } catch (InterruptedException ex) {
                    break;
                }
                if (event == null) {
                    continue;
                }
                final Operator operator = props.getOperatorByPoint(event.point);
                if (operator == null) {
                    System.out.println("!!!!!!!!!!!!!  " + event.point + " NOT FOUND");
                    continue;
                }
                final byte[] bytes = operator.getNProtocol(props).getData(event, operator, props);

                if (bytes == null) {
                    return;
                }

                try {
                    String s = "";
                    for (byte b : bytes) {
                        s = s + (b & 0xFF) + "_";
                    }
                    final ISerialPort port = operator.getPort(props);
                    System.out.println("SEND TO RS485 " + s);
                    System.out.println("PORT " + port.getName() + " speed=" + ((RxtxSerialPort) port).getSpeed()
                            + " bits=" + ((RxtxSerialPort) port).getDataBits() + " stop=" + ((RxtxSerialPort) port).getStopBits() + " patity="
                            + ((RxtxSerialPort) port).getParity());
                    synchronized (Sender485Holder.INSTANCE) {
                        port.send(bytes);
                        if (props.getPause() > 0) {
                            Thread.sleep(props.getPause());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("!!! ERROR !!! " + ex);
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }
            }
        });
        sndThread.setDaemon(true);
    }

    public void send(Event event) {
        if (sndThread.getState() == Thread.State.NEW || sndThread.getState() == Thread.State.TERMINATED) {
            sndThread.start();
        }
        try {
            stream.put(event);
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }
}
