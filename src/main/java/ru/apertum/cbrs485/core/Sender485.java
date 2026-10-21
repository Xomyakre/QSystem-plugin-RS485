/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import ru.apertum.cbrs485.core.RS485Property.Operator;
import ru.apertum.qsystem.common.CustomerState;
import ru.evgenic.rxtx.serialPort.ISerialPort;
import ru.evgenic.rxtx.serialPort.RxtxSerialPort;

/**
 * Отсылка на табло. Один поток, посылки строго по очереди, порт открываем сами:
 * вызов клиента - порт открыт и занят, номер мигает; прием - номер горит;
 * завершение - табло гаснет и, если больше никого не обслуживают, порт закрывается.
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
     * Шлем в порт только из процесса сервера. Программа оператора на том же компьютере
     * грузит этот же плагин и меняет статусы своим устаревшим копиям клиентов,
     * из-за чего на табло всплывали прошлые номера.
     * Для персонального табло у оператора запускать клиента с -Drs485.client=true
     */
    private static final boolean ENABLED = Boolean.getBoolean("rs485.client")
            || String.valueOf(System.getProperty("sun.java.command")).startsWith("ru.apertum.qsystem.Server");

    /**
     * Одинаковое событие для того же окна в пределах этого времени считается дублем:
     * QSystem может дернуть оба метода change() на одну смену статуса.
     */
    private static final long DUPLICATE_WINDOW_MS = 1000;

    /** окно -> последнее не отосланное событие */
    final private LinkedHashMap<String, Event> pending = new LinkedHashMap<>();
    final private HashMap<String, String> lastKey = new HashMap<>();
    final private HashMap<String, Long> lastTime = new HashMap<>();
    /** имя порта -> окна, где сейчас идет вызов или прием */
    final private HashMap<String, Set<String>> busy = new HashMap<>();
    /** имя порта -> открытый нами порт */
    final private HashMap<String, DirectPort> opened = new HashMap<>();
    final private Thread sndThread;
    final private RS485Property props;

    private Sender485() {
        try {
            props = RS485Property.unmarshal(new FileInputStream(RS485Property.PROP_FILE));
        } catch (JAXBException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        System.out.println("Sender ready (v4, enabled=" + ENABLED + "): default port=" + props.getPortName() + " protocol=" + props.getProtocols().getDefaultPtotocol());
        PortLog.info("START v4 enabled=" + ENABLED + " port=" + props.getPortName() + " protocol=" + props.getProtocols().getDefaultPtotocol()
                + " cmd=" + System.getProperty("sun.java.command"));

        // поток для отсыла стартуем один раз и навсегда
        sndThread = new Thread(this::loop, "ClientboardRS485Plugin-sender");
        sndThread.setDaemon(true);
        sndThread.start();
    }

    public static boolean isEnabled() {
        return ENABLED;
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
                PortLog.error("send failed " + describe(event), ex);
            }
            if (props.getPause() > 0) {
                try {
                    Thread.sleep(props.getPause());
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        for (DirectPort p : opened.values()) {
            p.close();
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

    private static boolean isClear(CustomerState state) {
        return state == CustomerState.STATE_FINISH
                || state == CustomerState.STATE_DEAD
                || state == CustomerState.STATE_POSTPONED
                || state == CustomerState.STATE_REDIRECT;
    }

    private void process(Event event) throws Exception {
        final Operator operator = props.getOperatorByPoint(event.point);
        if (operator == null) {
            System.out.println("!!!!!!!!!!!!!  " + event.point + " NOT FOUND");
            PortLog.info("SKIP window not in settings " + describe(event));
            return;
        }
        final byte[] bytes = operator.getNProtocol(props).getData(event, operator, props);
        if (bytes == null) {
            // протоколу нечего показывать для такого состояния
            PortLog.info("SKIP protocol returned nothing " + describe(event));
            return;
        }

        // настройки порта (имя, скорость, биты) берем из того же объекта, что и раньше, но сам порт открываем сами
        final ISerialPort cfg = operator.getPort(props);
        final String portName = cfg.getName();
        final Set<String> windows = busy.computeIfAbsent(portName, k -> new HashSet<>());
        if (isClear(event.state)) {
            windows.remove(event.point);
        } else {
            windows.add(event.point);
        }

        DirectPort port = opened.get(portName);
        if (port == null) {
            port = new DirectPort(cfg);
            opened.put(portName, port);
        }

        System.out.println("SEND TO RS485 [" + event.point + " " + event.state + " '" + event.ticket + "'] " + PortLog.hex(bytes));
        PortLog.info("SEND " + describe(event) + " addr=" + operator.getAddressRS());
        try {
            port.write(bytes);
        } catch (Exception ex) {
            // порт мог отвалиться (выдернули переходник) - переоткрываем и пробуем еще раз
            PortLog.error("write failed, reopen " + portName, ex);
            port.close();
            port.write(bytes);
        }

        if (windows.isEmpty()) {
            // никого не обслуживают - освобождаем порт до следующего вызова
            port.close();
        }
    }

    private static String describe(Event event) {
        return "[window=" + event.point + " state=" + event.state + " ticket='" + event.ticket + "']";
    }

    public void send(Event event) {
        if (event == null) {
            return;
        }
        if (!ENABLED) {
            PortLog.info("IGNORED not a server process " + describe(event));
            return;
        }
        if (event.point == null || event.point.trim().isEmpty()) {
            // у клиента еще нет окна, например он только встал в очередь
            return;
        }
        if (isDuplicate(event)) {
            PortLog.info("DROP duplicate " + describe(event));
            return;
        }
        synchronized (pending) {
            // старое не отосланное событие этого окна больше не нужно, его заменяет новое
            final Event old = pending.remove(event.point);
            if (old != null) {
                PortLog.info("DROP outdated " + describe(old) + " replaced by " + describe(event));
            }
            pending.put(event.point, event);
            pending.notifyAll();
        }
        PortLog.info("QUEUE " + describe(event));
    }

    private synchronized boolean isDuplicate(Event event) {
        final String key = event.state + "|" + event.ticket;
        final long now = System.currentTimeMillis();
        final String prevKey = lastKey.put(event.point, key);
        final Long prevTime = lastTime.put(event.point, now);
        return key.equals(prevKey) && prevTime != null && now - prevTime < DUPLICATE_WINDOW_MS;
    }

    /**
     * COM порт, который открываем и закрываем сами, без фоновых потоков библиотеки.
     * write() возвращается, когда байты уже ушли в провод.
     */
    private static class DirectPort {

        private final String name;
        private final int speed;
        private final int bits;
        private final int stopBits;
        private final int parity;
        private SerialPort port;
        private OutputStream out;

        DirectPort(ISerialPort cfg) {
            this.name = cfg.getName();
            if (cfg instanceof RxtxSerialPort) {
                final RxtxSerialPort rp = (RxtxSerialPort) cfg;
                speed = rp.getSpeed();
                bits = rp.getDataBits();
                stopBits = rp.getStopBits();
                parity = rp.getParity();
            } else {
                speed = 9600;
                bits = 8;
                stopBits = 1;
                parity = 0;
            }
        }

        void write(byte[] bytes) throws Exception {
            if (port == null) {
                open();
            }
            out.write(bytes);
            out.flush();
            // ждем, пока байты физически уйдут: 10 бит на байт + запас для переходника RS485
            Thread.sleep(bytes.length * 10L * 1000L / Math.max(speed, 1) + 50);
            PortLog.tx(name, bytes);
        }

        private void open() throws Exception {
            PortLog.info("OPEN " + name + " speed=" + speed + " bits=" + bits + " stop=" + stopBits + " parity=" + parity);
            final CommPortIdentifier id = CommPortIdentifier.getPortIdentifier(name);
            // через reflection: в RXTX open() возвращает CommPort, в NRJavaSerial - RXTXPort
            final Object p = id.getClass().getMethod("open", String.class, int.class).invoke(id, "QSystem-RS485", 2000);
            port = (SerialPort) p;
            port.setSerialPortParams(speed, bits, stopBits, parity);
            out = port.getOutputStream();
            PortLog.info("OPENED " + name);
        }

        void close() {
            if (port == null) {
                return;
            }
            try {
                out.close();
            } catch (Exception ex) {
                PortLog.error("close stream " + name, ex);
            }
            try {
                port.close();
            } catch (Exception ex) {
                PortLog.error("close port " + name, ex);
            }
            port = null;
            out = null;
            PortLog.info("CLOSED " + name);
        }
    }
}
