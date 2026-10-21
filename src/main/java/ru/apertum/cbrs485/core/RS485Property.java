/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import gnu.io.SerialPortEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import ru.apertum.cbrs485.core.protocols.Akis;
import ru.apertum.cbrs485.core.protocols.IProtocol;
import ru.apertum.cbrs485.core.protocols.Leddisplay;
import ru.apertum.cbrs485.core.protocols.Pr9800;
import ru.apertum.cbrs485.core.protocols.QSystemPr;
import ru.apertum.cbrs485.core.protocols.SSI;
import ru.apertum.cbrs485.core.protocols.Solt;
import ru.apertum.cbrs485.core.protocols.Svetovod;
import ru.apertum.cbrs485.core.protocols.SvetovodM;
import ru.evgenic.rxtx.serialPort.IReceiveListener;
import ru.evgenic.rxtx.serialPort.ISerialPort;
import ru.evgenic.rxtx.serialPort.RxtxSerialPort;

/**
 *
 * @author egorov
 */
@XmlRootElement(name = "rs485")
public class RS485Property {

    public static void main(String[] args) throws FileNotFoundException, JAXBException {
        RS485Property p = RS485Property.unmarshal(new FileInputStream(PROP_FILE));

        System.out.println(">> -- " + p.portName + " " + p.speed + " " + p.bits + " " + p.stopBits + " " + p.parity + " -- <<");

        p.ports.forEach(port -> {
            System.out.println("}} COM port: " + port.portName + " " + port.speed + " " + port.bits + " " + port.stopBits + " " + port.parity + " -- {{");
        });

        p.operators.forEach(op -> {
            System.out.println(">+0-- Operator: " + op.point + " -> " + op.addressRS + " port=" + op.comPort + " __ protocol=" + op.protocol
                    + " position=" + op.position + " direction=" + op.direction);
        });

        System.out.println("qsys: " + p.protocols.qsystem.getName() + ": " + p.protocols.qsystem.getDef());
        System.out.println("svetovod: " + p.protocols.svetovod.getName() + ": " + p.protocols.svetovod.getDef());
        System.out.println("svetovodM: " + p.protocols.svetovodM.getName() + ": " + p.protocols.svetovodM.getDef() + " " + p.protocols.svetovodM.getFontName() + " " + p.protocols.svetovodM.getFontSize() + " (" + p.protocols.svetovodM.getLenghtBy8() + "*8 X " + p.protocols.svetovodM.getHeight() + ")");
        System.out.println("ssi: " + p.protocols.ssi.getName() + ": " + p.protocols.ssi.getDef());
        System.out.println("leddisplay: " + p.protocols.leddisplay.getName() + ": " + p.protocols.leddisplay.getDef());
        System.out.println("solt: " + p.protocols.solt.getName() + ": " + p.protocols.solt.getDef());
        System.out.println("akis: " + p.protocols.akis.getName() + ": " + p.protocols.akis.getDef() + " bri=" + p.protocols.akis.getBrightness());
        if (p.protocols.pr9800 != null) {
            System.out.println("pr9800: " + p.protocols.pr9800.getName() + ": " + p.protocols.pr9800.getDef()
                    + " disp=" + p.protocols.pr9800.getDispNo() + " type=" + p.protocols.pr9800.getDisplayType());
        }
    }

    public static final File PROP_FILE = new File("config/ClientboardRS485Plugin.xml");

    public void marshal(OutputStream outputStream) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(new Class[]{RS485Property.class});
        final Marshaller m = jc.createMarshaller();
        m.setProperty("jaxb.formatted.output", true);
        m.marshal(this, outputStream);
    }

    public static RS485Property rs485Property;

    public static RS485Property unmarshal(InputStream inputStream) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(new Class[]{RS485Property.class});
        final Unmarshaller u = jc.createUnmarshaller();
        rs485Property = (RS485Property) u.unmarshal(inputStream);
        if (rs485Property.protocols != null && rs485Property.protocols.pr9800 == null) {
            final Pr9800 proto = new Pr9800();
            proto.setDef(false);
            proto.setName("PR9800");
            proto.setDispNo(0);
            proto.setDisplayType(0);
            proto.setFont(0);
            proto.setAlign(0);
            proto.setOpt(0);
            proto.setSpeed(0);
            proto.setColor(1);
            rs485Property.protocols.setPr9800(proto);
        }
        return rs485Property;
    }

    private String portName;

    public String getPortName() {
        return portName;
    }

    @XmlAttribute(name = "name", required = true)
    public void setPortName(String portName) {
        this.portName = portName;
    }

    private Integer speed;

    public Integer getSpeed() {
        return speed;
    }

    @XmlAttribute(name = "speed", required = true)
    public void setSpeed(Integer speed) {
        this.speed = speed;
    }

    private Integer bits;

    public Integer getBits() {
        return bits;
    }

    @XmlAttribute(name = "bits", required = true)
    public void setBits(Integer bits) {
        this.bits = bits;
    }

    private Integer stopBits;

    public Integer getStopBits() {
        return stopBits;
    }

    @XmlAttribute(name = "stopBits", required = true)
    public void setStopBits(Integer stopBits) {
        this.stopBits = stopBits;
    }

    private Boolean parity;

    public Boolean getParity() {
        return parity;
    }

    @XmlAttribute(name = "parity", required = true)
    public void setParity(Boolean parity) {
        this.parity = parity;
    }

    private Integer pause;

    public Integer getPause() {
        return pause == null ? 0 : pause;
    }

    @XmlAttribute(name = "delay", required = false)
    public void setPause(Integer pause) {
        this.pause = pause == null ? 0 : pause;
    }

    private ISerialPort defPort;

    public synchronized ISerialPort getDefaultPort() {
        System.out.println("Get default port " + getPortName());
        if (defPort == null) {
            try {
                defPort = new RxtxSerialPort(getPortName());
            } catch (Exception ex) {
                System.err.println(ex);
                throw new RuntimeException(ex);
            }
            defPort.setSpeed(getSpeed());
            defPort.setDataBits(getBits());
            defPort.setParity(getParity() ? 1 : 0);
            defPort.setStopBits(getStopBits());
            /*
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
             */
        }
        return defPort;
    }

    //*************************************************************************
    private List<PortProperty> ports = new ArrayList<>();

    public List<PortProperty> getPorts() {
        return ports;
    }

    @XmlElementWrapper(name = "ports")
    @XmlElements({
        @XmlElement(name = "port", type = PortProperty.class)
    })
    public void setPorts(List<PortProperty> ports) {
        this.ports = ports;
    }

    @XmlRootElement(name = "port")
    public static class PortProperty {

        public PortProperty() {
        }

        private String portName;

        public String getPortName() {
            return portName;
        }

        @XmlAttribute(name = "name", required = true)
        public void setPortName(String portName) {
            this.portName = portName;
        }

        private Integer speed;

        public Integer getSpeed() {
            return speed;
        }

        @XmlAttribute(name = "speed", required = true)
        public void setSpeed(Integer speed) {
            this.speed = speed;
        }

        private Integer bits;

        public Integer getBits() {
            return bits;
        }

        @XmlAttribute(name = "bits", required = true)
        public void setBits(Integer bits) {
            this.bits = bits;
        }

        private Integer stopBits;

        public Integer getStopBits() {
            return stopBits;
        }

        @XmlAttribute(name = "stopBits", required = true)
        public void setStopBits(Integer stopBits) {
            this.stopBits = stopBits;
        }

        private Boolean parity;

        public Boolean getParity() {
            return parity;
        }

        @XmlAttribute(name = "parity", required = true)
        public void setParity(Boolean parity) {
            this.parity = parity;
        }
    }

    //************************************************************************
    private ArrayList<Operator> operators = new ArrayList<>();

    public ArrayList<Operator> getOperators() {
        return operators;
    }

    @XmlElementWrapper(name = "operators")
    @XmlElements({
        @XmlElement(name = "operator", type = Operator.class)
    })
    public void setOperators(ArrayList<Operator> Operators) {
        this.operators = Operators;
    }

    public Operator getOperatorByPoint(String point) {
        if (point == null) {
            throw new IllegalArgumentException("Point of operator is NULL");
        }
        for (Operator o : operators) {
            if (point.equalsIgnoreCase(o.point)) {
                return o;
            }
        }
        return null;
    }

    public Operator addNewOperator(String point) {
        final Operator p = new Operator();
        p.setPoint(point);
        p.setAddressRS(32);
        p.setComPort(null);
        p.setDirection(null);
        p.setPosition(null);
        p.setProtocol(null);
        getOperators().add(p);
        return p;
    }

    @XmlRootElement(name = "operator")
    public static class Operator {

        public Operator() {
        }

        private String point;

        public String getPoint() {
            return point;
        }

        @XmlAttribute(name = "point", required = true)
        public void setPoint(String point) {
            this.point = point;
        }
        private Integer addressRS;

        public Integer getAddressRS() {
            return addressRS;
        }

        @XmlAttribute(name = "addressRS", required = true)
        public void setAddressRS(Integer addressRS) {
            this.addressRS = addressRS;
        }

        private String comPort;

        public String getComPort() {
            return comPort;
        }

        @XmlAttribute(name = "comPort", required = false)
        public void setComPort(String comPort) {
            this.comPort = comPort;
        }
        private Integer protocol;

        public Integer getProtocol() {
            return protocol;
        }

        @XmlAttribute(name = "protocol", required = false)
        public void setProtocol(Integer protocol) {
            this.protocol = protocol;
        }

        private Integer position;

        public Integer getPosition() {
            return position;
        }

        @XmlAttribute(name = "position", required = false)
        public void setPosition(Integer position) {
            this.position = position;
        }

        private String direction;

        public String getDirection() {
            return direction;
        }

        @XmlAttribute(name = "direction", required = false)
        public void setDirection(String direction) {
            this.direction = direction;
        }

        public ISerialPort getPort(RS485Property rsp) {
            synchronized (PORTS) {
                PortProperty pp = null;
                for (PortProperty port1 : rsp.ports) {
                    if (getComPort() != null && getComPort().equalsIgnoreCase(port1.portName)) {
                        pp = port1;
                        break;
                    }
                }
                if (pp == null) {
                    ISerialPort port = PORTS.get(rsp.getPortName());
                    if (port == null) {
                        port = rsp.getDefaultPort();
                        PORTS.put(rsp.getPortName(), port);
                    }
                    return port;
                }
                ISerialPort port = PORTS.get(pp.portName);
                if (port == null) {
                    System.out.println("Get personal port " + pp.portName);
                    try {
                        port = new RxtxSerialPort(pp.portName);
                        port.setSpeed(pp.speed);
                        port.setDataBits(pp.bits);
                        port.setParity(pp.parity ? 1 : 0);
                        port.setStopBits(pp.stopBits);
                        port.bind(new IReceiveListener() {
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
                    PORTS.put(pp.portName, port);
                }
                return port;
            }
        }

        private Protocol prot;

        public synchronized Protocol getNProtocol(RS485Property rsp) {
            if (prot == null) {
                prot = rsp.getProtocols().getPtotocolByN(protocol);
            }
            return prot;
        }

    }

    private static final ConcurrentHashMap<String, ISerialPort> PORTS = new ConcurrentHashMap<>();

    //***********************************************************************
    //***********************************************************************
    private Protocols protocols;

    public Protocols getProtocols() {
        return protocols;
    }

    @XmlElements({
        @XmlElement(name = "protocols", type = Protocols.class)
    })
    public void setProtocols(Protocols protocols) {
        this.protocols = protocols;
    }

    @XmlRootElement(name = "protocols")
    public static class Protocols {

        private QSystemPr qsystem;

        @XmlElements({
            @XmlElement(name = "qsystem", type = QSystemPr.class)
        })
        public QSystemPr getQsystem() {
            return qsystem;
        }

        public void setQsystem(QSystemPr qsys) {
            this.qsystem = qsys;
        }
        private Svetovod svetovod;

        public Svetovod getSvetovod() {
            return svetovod;
        }

        @XmlElements({
            @XmlElement(name = "svetovod", type = Svetovod.class)
        })
        public void setSvetovod(Svetovod svet) {
            this.svetovod = svet;
        }

        private SvetovodM svetovodM;

        public SvetovodM getSvetovodM() {
            return svetovodM;
        }

        @XmlElements({
            @XmlElement(name = "svetovodM", type = SvetovodM.class)
        })
        public void setSvetovodM(SvetovodM svetovodM) {
            this.svetovodM = svetovodM;
        }

        private SSI ssi;

        public SSI getSsi() {
            return ssi;
        }

        @XmlElements({
            @XmlElement(name = "ssi", type = SSI.class)
        })
        public void setSsi(SSI ssi) {
            this.ssi = ssi;
        }

        private Leddisplay leddisplay;

        public Leddisplay getLeddisplay() {
            return leddisplay;
        }

        @XmlElements({
            @XmlElement(name = "ledDisplayRu", type = Leddisplay.class)
        })
        public void setLeddisplay(Leddisplay leddisplay) {
            this.leddisplay = leddisplay;
        }

        private Solt solt;

        public Solt getSolt() {
            return solt;
        }

        @XmlElements({
            @XmlElement(name = "solt", type = Solt.class)
        })
        public void setSolt(Solt solt) {
            this.solt = solt;
        }

        private Akis akis;

        public Akis getAkis() {
            return akis;
        }

        @XmlElements({
            @XmlElement(name = "akis", type = Akis.class)
        })
        public void setAkis(Akis akis) {
            this.akis = akis;
        }

        private DoCashProtocol docash;

        public DoCashProtocol getDocash() {
            return docash;
        }

        @XmlElements({
            @XmlElement(name = "docash", type = DoCashProtocol.class)
        })
        public void setDocash(DoCashProtocol docash) {
            this.docash = docash;
        }

        private Pr9800 pr9800;

        public Pr9800 getPr9800() {
            return pr9800;
        }

        @XmlElements({
            @XmlElement(name = "pr9800", type = Pr9800.class)
        })
        public void setPr9800(Pr9800 pr9800) {
            this.pr9800 = pr9800;
        }

        private List<Protocol> all() {
            final List<Protocol> list = new ArrayList<>();
            for (Protocol p : new Protocol[]{qsystem, svetovod, svetovodM, ssi, leddisplay, solt, akis, docash, pr9800}) {
                if (p != null) {
                    list.add(p);
                }
            }
            return list;
        }

        public Protocol getDefaultPtotocol() {
            for (Protocol p : all()) {
                if (Boolean.TRUE.equals(p.isDef())) {
                    return p;
                }
            }
            return qsystem;
        }

        public Protocol getPtotocolByN(Integer n) {
            if (n != null) {
                for (Protocol p : all()) {
                    if (n.equals(p.getNProtocol())) {
                        return p;
                    }
                }
            }
            return getDefaultPtotocol();
        }

//        private static final HashSet<Protocol> pset = new HashSet<>();
//
//        public Protocol getDefaultPtotocol() {
//            for (Protocol pset1 : pset) {
//                if (pset1.isDef()) {
//                    return pset1;
//                }
//            }
//            return qsystem;
//        }
//
//        public Protocol getPtotocolByN(Integer n) {
//            for (Protocol pset1 : pset) {
//                if (pset1.getNProtocol().equals(n)) {
//                    return pset1;
//                }
//            }
//            return getDefaultPtotocol();
//        }

    }

    @XmlRootElement(name = "protocol")
    public static abstract class Protocol implements IProtocol {



        private Boolean def;

        public Boolean getDef() {
            return def;
        }

        public Boolean isDef() {
            return def;
        }

        @XmlAttribute(name = "default", required = true)
        public void setDef(Boolean def) {
            this.def = def;
        }

        private String name;

        public String getName() {
            return name;
        }

        @XmlValue()
        public void setName(String name) {
            this.name = name;
        }

        public abstract Integer getNProtocol();

        @Override
        public String toString() {
            return name + "_" + getNProtocol() + "_" + def;
        }

    }

    public abstract static class QSystemProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 0;
        }

    }

    public abstract static class SvetovodProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 1;
        }
    }

    public abstract static class SvetovodMProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 2;
        }

        private Integer lenghtBy8;

        public Integer getLenghtBy8() {
            return lenghtBy8;
        }

        @XmlAttribute(name = "lenghtBy8", required = true)
        public void setLenghtBy8(Integer lenghtBy8) {
            this.lenghtBy8 = lenghtBy8;
        }
        private Integer height;

        public Integer getHeight() {
            return height;
        }

        @XmlAttribute(name = "height", required = true)
        public void setHeight(Integer height) {
            this.height = height;
        }
        private Integer fontSize;

        public Integer getFontSize() {
            return fontSize;
        }

        @XmlAttribute(name = "fontSize", required = true)
        public void setFontSize(Integer fontSize) {
            this.fontSize = fontSize;
        }
        private String fontName;

        public String getFontName() {
            return fontName;
        }

        @XmlAttribute(name = "fontName", required = true)
        public void setFontName(String fontName) {
            this.fontName = fontName;
        }
    }

    public abstract static class SSIProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 3;
        }
    }

    public abstract static class LeddisplayProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 4;
        }
    }

    public abstract static class SoltProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 5;
        }
    }

    public abstract static class AkisProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 6;
        }

        private Integer brightness;

        public Integer getBrightness() {
            return brightness;
        }

        @XmlAttribute(name = "brightness", required = true)
        public void setBrightness(Integer brightness) {
            this.brightness = brightness;
        }
    }

    public static class DoCashProtocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 7;
        }

        @Override
        public byte[] getData(Event event, Operator operator, RS485Property props) {
            return null;
        }
    }

    public abstract static class Pr9800Protocol extends Protocol {

        @Override
        public Integer getNProtocol() {
            return 8;
        }

        private Integer dispNo;

        public Integer getDispNo() {
            return dispNo == null ? 0 : dispNo;
        }

        @XmlAttribute(name = "dispNo", required = false)
        public void setDispNo(Integer dispNo) {
            this.dispNo = dispNo;
        }

        private Integer displayType;

        public Integer getDisplayType() {
            return displayType == null ? 0 : displayType;
        }

        /**
         * Старшие 2 бита номера дисплея: 0 — по умолчанию, 1 — текстовый, 2 — цифровой.
         */
        @XmlAttribute(name = "displayType", required = false)
        public void setDisplayType(Integer displayType) {
            this.displayType = displayType;
        }

        private Integer font;

        public Integer getFont() {
            return font == null ? 0 : font;
        }

        @XmlAttribute(name = "font", required = false)
        public void setFont(Integer font) {
            this.font = font;
        }

        private Integer align;

        public Integer getAlign() {
            return align == null ? 0 : align;
        }

        @XmlAttribute(name = "align", required = false)
        public void setAlign(Integer align) {
            this.align = align;
        }

        private Integer opt;

        public Integer getOpt() {
            return opt == null ? 0 : opt;
        }

        @XmlAttribute(name = "opt", required = false)
        public void setOpt(Integer opt) {
            this.opt = opt;
        }

        private Integer speed;

        public Integer getSpeed() {
            return speed == null ? 0 : speed;
        }

        @XmlAttribute(name = "speed", required = false)
        public void setSpeed(Integer speed) {
            this.speed = speed;
        }

        private Integer color;

        public Integer getColor() {
            return color == null ? 1 : color;
        }

        @XmlAttribute(name = "color", required = false)
        public void setColor(Integer color) {
            this.color = color;
        }
    }
}
