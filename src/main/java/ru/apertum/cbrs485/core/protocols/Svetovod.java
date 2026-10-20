/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core.protocols;

import java.util.HashMap;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import static ru.apertum.cbrs485.core.protocols.IProtocol.toByte;

/**
 *
 * @author Evgeniy Egorov
 */
public class Svetovod extends RS485Property.SvetovodProtocol {

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        byte[] mess = new byte[12];
        mess[0] = -32;// $E0 – стартовый байт = 224 = -32
        mess[1] = 0;// $00 – адрес отправителя. (Компьютер всегда $00)
        mess[2] = toByte(operator.getAddressRS()); // SysNum – адрес получателя [1...254].

        mess[3] = 0;
        mess[4] = 0;
        mess[5] = 3;

        short s = (short) (224 + 3 + operator.getAddressRS());
        byte l = (byte) (s & 0xff);
        byte h = (byte) ((s >> 8) & 0xff);
        int hs = h;
        hs = hs + l;

        mess[6] = (byte) (hs >= 0 && hs < 128 ? hs : (hs - 256)); // HIGH (CRC) + LOW (CRC);
        mess[7] = l; // LOW (CRC);

        int data = event.number;// + 100;//xa-xa
        int i = data;
        mess[10] = bbs.get(i % 10);
        i = data / 10;
        mess[9] = bbs.get(i % 10);
        i = data / 100;
        mess[8] = bbs.get(i % 10);
        int su = (mess[8] + mess[9] + mess[10]) % 256;

        mess[11] = (byte) (su >= 0 && su < 128 ? su : (su - 256)); // DATA_CRC = SUM (DATA0...DATAN).
        IProtocol.printBytes(mess);
        return mess;
    }

    private final HashMap<Integer, Byte> bbs = new HashMap<>();

    {
        bbs.put(0, (byte) 63);
        bbs.put(1, (byte) 6);
        bbs.put(2, (byte) 91);
        bbs.put(3, (byte) 79);
        bbs.put(4, (byte) 102);
        bbs.put(5, (byte) 109);
        bbs.put(6, (byte) 125);
        bbs.put(7, (byte) 7);
        bbs.put(8, (byte) 127);
        bbs.put(9, (byte) 111);
    }

}
