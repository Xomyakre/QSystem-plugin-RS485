/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import gnu.io.SerialPortEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import javax.xml.bind.JAXBException;
import ru.apertum.cbrs485.core.RS485Property.Protocol;
import ru.evgenic.rxtx.serialPort.IReceiveListener;
import ru.evgenic.rxtx.serialPort.ISerialPort;
import ru.evgenic.rxtx.serialPort.RxtxSerialPort;

/**
 *
 * @author Evgeniy Egorov
 */
public class AddrProp {

    public final RS485Property propsXml;
    private ISerialPort defPort;

    public ISerialPort getDefaultPort() {
        return defPort;
    }
    private Protocol defProtocol;

    public Protocol getdefaultProtocol() {
        return defProtocol;
    }

    final private HashMap<String, Addr> addrs = new HashMap<>();

    public HashMap<String, Addr> getAddrs() {
        return addrs;
    }

    public class Addr {

        public final String point;
        public final int addres;
        public final int position;
        public final byte arrow;

        public Addr(String point, int addres, int position, byte arrow) {
            this.point = point;
            this.addres = addres;
            this.position = position;
            this.arrow = arrow;
            port = null;
            protocol = null;
        }

        private final ISerialPort port;
        private final Protocol protocol;

        public Addr(String point, int addres, int position, byte arrow, ISerialPort port, Protocol protocol) {
            this.point = point;
            this.addres = addres;
            this.position = position;
            this.arrow = arrow;
            this.port = port;
            this.protocol = protocol;
        }

        public ISerialPort getPort() {
            return port == null ? getDefaultPort() : port;
        }

        public Protocol getProtocol() {
            return protocol==null ? getdefaultProtocol() : getProtocol(); //TODO
        }

    }

    private AddrProp() {
        if (RS485Property.PROP_FILE.exists()) {
            try {
                propsXml = RS485Property.unmarshal(new FileInputStream(RS485Property.PROP_FILE));
            } catch (FileNotFoundException | JAXBException ex) {
                throw new RuntimeException(ex);
            }
            try {
                defPort = new RxtxSerialPort(propsXml.getPortName());
            } catch (Exception ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }
            defProtocol = propsXml.getProtocols().getDefaultPtotocol();
            defPort.setSpeed(propsXml.getSpeed());
            defPort.setDataBits(propsXml.getBits());
            defPort.setParity(propsXml.getParity() ? 1 : 0);
            defPort.setStopBits(propsXml.getStopBits());
            try {
                defPort.bind(new IReceiveListener() {

                    @Override
                    public void actionPerformed(SerialPortEvent spe, byte[] bytes) {
                        System.out.println("!!!!!!!!!!!!!!!!!!!! " + Arrays.toString(bytes));
                    }

                    @Override
                    public void actionPerformed(SerialPortEvent spe) {
                    }
                });
            } catch (Exception ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }
        } else {
            propsXml = null;
        }
/*
        if (PROP_FILE.exists() && defPort == null) {
            // подготовим порт для отсыла
            // параметры порта
            try (FileInputStream fis = new FileInputStream(PROP_FILE)) {
                props.load(fis);
            } catch (IOException ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }

            try {
                defPort = new RxtxSerialPort(props.getProperty("port.name", "COM1"));
            } catch (Exception ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }
            defProtocol = Integer.parseInt(props.getProperty("protocol", "0"));
            defPort.setSpeed(Integer.parseInt(props.getProperty("port.speed", "6900")));
            defPort.setDataBits(Integer.parseInt(props.getProperty("port.bits", "8")));
            defPort.setParity(props.getProperty("port.parity", "0").equals("1") ? 1 : 0);
            defPort.setStopBits(Integer.parseInt(props.getProperty("port.stopbits", "1")));
            try {
                defPort.bind(new IReceiveListener() {

                    @Override
                    public void actionPerformed(SerialPortEvent spe, byte[] bytes) {
                        System.out.println("!!!!!!!!!!!!!!!!!!!! " + Arrays.toString(bytes));
                    }

                    @Override
                    public void actionPerformed(SerialPortEvent spe) {
                    }
                });
            } catch (Exception ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }
        }

        if (ADDR_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(ADDR_FILE); Scanner s = new Scanner(fis)) {
                while (s.hasNextLine()) {
                    final String line = s.nextLine().trim();
                    if (!line.startsWith("#")) {
                        final String[] ss = line.split("=");
                        final String[] ssl = ss[1].split(" ");
                        final int adr = Integer.parseInt(ssl[0]);
                        addrs.put(ss[0], new Addr(ss[0], adr, ssl.length > 1 ? Integer.parseInt(ssl[1]) : 4, ssl.length > 2 ? ssl[2].getBytes()[0] : 1));
                    }
                }
            } catch (IOException ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }
        }
        */
    }

    public static AddrProp getInstanceOld() {
        return AddrPropHolder.INSTANCE;
    }

    private static class AddrPropHolder {

        private static final AddrProp INSTANCE = new AddrProp();
    }

    public Addr getAddr1(String point) {
        return addrs.get(point);
    }

    public static void main(String[] ss) throws UnsupportedEncodingException {
        String str = "Часто используя какую-либо систему, мы привыкаем к некоторым шаблонам работы. Но не всегда эти шаблоны оказываются оптимальными. Иногда мы даже приобретаем плохие привычки, которые приводят к беспорядку и неуклюжести в работе. Один из наилучших путей исправления таких недостатков – выработка привычки использовать хорошие приемы, которые препятствуют беспорядку.";
        String str1 = URLEncoder.encode(str, "utf-8");
        String str2 = URLDecoder.decode(str1, "utf-8");
        System.out.println(str);
        System.out.println(str1);
        System.out.println(str2);
        if (str.equals(str2)) {
            System.out.println("OK!");
        } else {
            System.out.println("BAD :((");
        }

        System.out.println("addrs:");
        getInstanceOld().addrs.keySet().stream().forEach((string) -> {
            byte[] bb = new byte[1];
            bb[0] = getInstanceOld().getAddr1(string).arrow;
            System.out.println("- " + getInstanceOld().getAddr1(string).point
                    + "=" + getInstanceOld().getAddr1(string).addres
                    + " " + getInstanceOld().getAddr1(string).position
                    + " " + new String(bb));
        });
    }
}
