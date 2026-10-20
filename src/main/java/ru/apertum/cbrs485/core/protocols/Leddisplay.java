/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core.protocols;

import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import static ru.apertum.cbrs485.core.protocols.IProtocol.toByte;
import ru.apertum.qsystem.common.CustomerState;

/**
 *
 * @author Evgeniy Egorov
 */
public class Leddisplay extends RS485Property.LeddisplayProtocol {

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        byte[] mess = new byte[125];

        mess[0] = toByte(255);
        mess[1] = 0x02;
        byte[] adr = Integer.toHexString(operator.getAddressRS()).getBytes();
        if (adr.length == 1) {
            mess[2] = 0x30;
            mess[3] = adr[0];
        } else {
            mess[2] = adr[0];
            mess[3] = adr[1];
        }
        adr = Integer.toHexString(255).getBytes();
        if (adr.length == 1) {
            mess[4] = 0x30;
            mess[5] = adr[0];
        } else {
            mess[4] = adr[0];
            mess[5] = adr[1];
        }
        mess[6] = 'g';
        mess[7] = '!';

        boolean f = event.state == CustomerState.STATE_INVITED || event.state == CustomerState.STATE_INVITED_SECONDARY;
        boolean ff = event.state == CustomerState.STATE_WORK || event.state == CustomerState.STATE_WORK_SECONDARY;
        String s = (f ? " " : "") + (((ff || f) ? event.ticket : " ") + "     ").substring(0, 4);
        System.arraycopy(s.getBytes(), 0, mess, 8, s.length());
        if (f) {
            mess[8] = 0x13;
        }

        mess[8 + s.length()] = 0x03;

        //8	Сумма	s s	Контрольная сумма, 2 байта, HEX. Сумма полей  2…6. Суммирование производиться в том виде, в котором поля присутствуют в пакете. Например, если адрес табло 0x10, то в пакете он будет представлен как 0x31 0x30 (символы ‘1’ и ‘0’) и сумма этого поля будет 0x61.
        int sum = 0;
        for (int i = 2; i < 8 + s.length(); i++) {
            sum = sum + mess[i];
        }
        adr = Integer.toHexString(sum).getBytes();

        if (adr.length == 1) {
            mess[9 + s.length()] = 0x00;
            mess[10 + s.length()] = adr[0];
        } else {
            mess[9 + s.length()] = adr[adr.length - 2];
            mess[10 + s.length()] = adr[adr.length - 1];
        }

        mess[11 + s.length()] = 0x0d;

        int len = 11 + s.length() + 1;
        byte[] mess2 = new byte[len];
        System.arraycopy(mess, 0, mess2, 0, len);
        IProtocol.printBytes(mess2);

        return mess2;
    }

}
