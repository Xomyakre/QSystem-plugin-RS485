/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core.protocols;

import java.nio.ByteBuffer;
import javax.xml.bind.annotation.XmlTransient;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import static ru.apertum.cbrs485.core.protocols.IProtocol.toByte;
import ru.apertum.qsystem.common.CustomerState;
import ru.evgenic.rxtx.serialPort.ISerialPort;

/**
 *
 * @author Evgeniy Egorov
 */
public final class Akis extends RS485Property.AkisProtocol {

    public Akis() {
    }

    public Akis(Integer brightness) {
        setBrightness(brightness);
    }
    

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        try {
            Run(operator.getPort(props), props);
        } catch (Exception ex) {
            System.err.println("ERR: " + ex);
        }

        byte[] mess = new byte[9];
        mess[0] = 0x08; //- код команды
        byte[] adr = ByteBuffer.allocate(4).putInt(operator.getAddressRS()).array();
        mess[1] = adr[2];//- старший байт адреса табло
        mess[2] = adr[3];// - младший байт адреса табло
        mess[3] = 0x47;//- Неизвестно что обозначает. всегда шлется 47.

        boolean f = event.state == CustomerState.STATE_INVITED || event.state == CustomerState.STATE_INVITED_SECONDARY;
        boolean ff = event.state == CustomerState.STATE_WORK || event.state == CustomerState.STATE_WORK_SECONDARY;

        String adrS = (ff || f) ? "ffff" + event.number + "1" : "fffff1";
        adrS = adrS.substring(adrS.length() - 4);
        long adrI1 = Long.parseLong(adrS.substring(0, 2), 16);
        byte[] bytesI1 = ByteBuffer.allocate(8).putLong(adrI1).array();
        long adrI2 = Long.parseLong(adrS.substring(2), 16);
        byte[] bytesI2 = ByteBuffer.allocate(8).putLong(adrI2).array();
        mess[4] = bytesI1[7]; // - первые 2 цифры номера талончика очереди. Если F, то это означает отсутствие цифры.
        mess[5] = bytesI2[7];// - последние 2 цифры номера талончика очереди.

        mess[6] = toByte(255);// - номер окошка (стола или пр.).
        mess[7] = f ? 0x07 : (byte) 0;// - не помню точно. Это типа указание как помигать. // мигают позиции бит в байте. Стать 7 для полного мигания.

        mess[8] = toByte(mess[0] ^ mess[1] ^ mess[2] ^ mess[3] ^ mess[4] ^ mess[5] ^ mess[6] ^ mess[7]);// CRC XOR
        IProtocol.printBytes(mess);
        return mess;
    }

    @XmlTransient
    public final byte[] FIRE = {0x03, toByte(0xFF), toByte(0xFF), 0x03};
    private long d = 0;

    public synchronized void Run(ISerialPort port, RS485Property props) throws Exception {
        if (System.currentTimeMillis() - d > 15 * 60 * 1000) {
            d = System.currentTimeMillis();

            port.send(FIRE);

            final byte[] mess = new byte[8];
            mess[0] = 0x07; //- код команды
            mess[3] = 0x48;//- Неизвестно что обозначает. всегда шлется 47.
            mess[4] = toByte(getBrightness());//- EC - уровень яркости
            mess[5] = 0x1f;//-1F - Неизвестно что обозначает. всегда шлется 1F.
            mess[6] = 0x1f;//-1F - Неизвестно что обозначает. всегда шлется 1F.
            for (RS485Property.Operator o :  props.getOperators()) {
                byte[] adr2 = ByteBuffer.allocate(4).putInt(o.getAddressRS()).array();
                mess[1] = adr2[2];
                mess[2] = adr2[3];
                mess[7] = toByte(mess[0] ^ mess[1] ^ mess[2] ^ mess[3] ^ mess[4] ^ mess[5] ^ mess[6]);// CRC XOR
                Thread.sleep(60);
                port.send(mess);
            }
            Thread.sleep(60);
        }
    }

}
