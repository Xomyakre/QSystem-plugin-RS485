/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core.protocols;

import java.io.UnsupportedEncodingException;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import static ru.apertum.cbrs485.core.protocols.IProtocol.toByte;

/**
 *
 * @author Evgeniy Egorov
 */
public class QSystemPr extends RS485Property.QSystemProtocol {

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        final int pos = operator.getPosition() == null || operator.getPosition() <= 0 ? 5 : operator.getPosition();
        final String dir = operator.getDirection() == null || operator.getDirection().isEmpty() ? "1" : operator.getDirection();

        byte[] bytes = null;
        try {
            //1amA147>127
            // 7й или 8й байт :  0x3C < знак «меньше» Светится стрелка влево | 0x3E > знак «больше» Светится стрелка вправо | 0x2D - знак «минус» Светится черточка без указания направления
            bytes = (("123" + event.ticket + "                                         ").substring(0, pos + 2) + "-" + event.point
                    + "                                     ").subSequence(0, 11).toString().getBytes("cp1251");
        } catch (UnsupportedEncodingException ex) {
            System.err.println("!!! ERROR !!! " + ex);
            System.err.println(ex);
            return new byte[3];
        }
        bytes[0] = 0x01; // начало
        bytes[bytes.length - 1] = 0x07; // конец
        bytes[1] = toByte(operator.getAddressRS()); // адрес
        bytes[2] = 32;//0x20; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
        bytes[pos + 2] = dir.getBytes()[0]; // стрелочка

        switch (event.state) {
            case STATE_INVITED:
                bytes[2] = 33;//0x21; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно;
                break;
            case STATE_INVITED_SECONDARY:
                bytes[2] = 33;//0x21; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно;
                break;
            case STATE_WORK:
                break;
            case STATE_WORK_SECONDARY:
                break;
            case STATE_DEAD:
                //bytes[addr.position + 2] = 'd'; // стрелочка убирается
                bytes[3] = ' ';
                bytes[4] = ' ';
                bytes[5] = ' ';
                bytes[6] = ' ';
                break;
            case STATE_FINISH:
                //bytes[addr.position + 2] = 'd'; // стрелочка убирается
                bytes[3] = ' ';
                bytes[4] = ' ';
                bytes[5] = ' ';
                bytes[6] = ' ';
                break;
            case STATE_POSTPONED:
                //bytes[addr.position + 2] = 'd'; // стрелочка убирается
                bytes[3] = ' ';
                bytes[4] = ' ';
                bytes[5] = ' ';
                bytes[6] = ' ';
                break;
            case STATE_REDIRECT:
                //bytes[addr.position + 2] = 'd'; // стрелочка убирается
                bytes[4] = ' ';
                bytes[5] = ' ';
                bytes[6] = ' ';
                bytes[7] = ' ';
                break;
            default:// нужная вещь. чтобы отсечь состояния, которые не при чем в зональном табло
                return null;

        }
        IProtocol.printBytes(bytes);
        return bytes;
    }

}
