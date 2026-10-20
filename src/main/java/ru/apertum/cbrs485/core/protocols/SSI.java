/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core.protocols;

import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;

/**
 *
 * @author Evgeniy Egorov
 */
public class SSI extends RS485Property.SSIProtocol {

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        byte[] mess = new byte[25];

        mess[0] = 58;//:
        byte[] bb = Integer.toHexString(operator.getAddressRS()).toUpperCase().getBytes();

        if (bb.length == 1) {
            System.out.println("To adr: " + Integer.toHexString(operator.getAddressRS()).toUpperCase() + " / " + bb[0]);
            mess[1] = 48;
            mess[2] = bb[0];
        } else {
            System.out.println("To adr: " + Integer.toHexString(operator.getAddressRS()).toUpperCase() + " / " + bb[0] + "_" + bb[1]);
            mess[1] = bb[0];
            mess[2] = bb[1];
        }

        //FF010001050100000000F9
        mess[3] = 48;
        mess[4] = 49;
        mess[5] = 48;
        mess[6] = 48;

        mess[7] = 48;
        mess[8] = 49;
        mess[9] = 48;
        mess[10] = 53;

        int num = (int) event.number;
        num = num / 1000;
        bb = Integer.toHexString(num).toUpperCase().getBytes();
        if (bb.length == 1) {
            mess[11] = 48;
            mess[12] = bb[0];
        } else {
            mess[11] = bb[0];
            mess[12] = bb[1];
        }

        num = (int) event.number;
        num = num % 1000 / 100;
        bb = Integer.toHexString(num).toUpperCase().getBytes();
        if (bb.length == 1) {
            mess[13] = 48;
            mess[14] = bb[0];
        } else {
            mess[13] = bb[0];
            mess[14] = bb[1];
        }

        num = (int) event.number;
        num = num % 100 / 10;
        bb = Integer.toHexString(num).toUpperCase().getBytes();
        if (bb.length == 1) {
            mess[15] = 48;
            mess[16] = bb[0];
        } else {
            mess[15] = bb[0];
            mess[16] = bb[1];
        }

        num = (int) event.number;
        num = num % 10 / 1;
        bb = Integer.toHexString(num).toUpperCase().getBytes();
        if (bb.length == 1) {
            mess[17] = 48;
            mess[18] = bb[0];
        } else {
            mess[17] = bb[0];
            mess[18] = bb[1];
        }

        mess[19] = 48;
        mess[20] = 48;

        //FF010001050100000000F9//249
        int x = 0;
        String ss = "";
        for (int i = 0 + 1; i < mess.length - 2 - 2; i++) {
            byte c = mess[i];
            if (ss.length() < 2) {
                ss = ss + (char) c;
            } else {
                //System.out.print(ss + "(" + Integer.parseInt(ss, 16) + ")-");
                x = (x - Integer.parseInt(ss, 16));
                ss = "" + (char) c;
            }
        }
        x = (x - Integer.parseInt(ss, 16));
        String hex = Integer.toHexString(x).toUpperCase();
        String hex2 = hex.substring(hex.length() - 2);

        //System.out.println("X=" + x + " - " + hex + " _ " + hex2);
        bb = hex2.getBytes();
        if (bb.length == 1) {
            mess[21] = 48;
            mess[22] = bb[0];
        } else {
            mess[21] = bb[0];
            mess[22] = bb[1];
        }

        mess[23] = 13;
        mess[24] = 10;

        ss = "";
        for (int i = 0; i < mess.length; i++) {
            byte c = mess[i];
            ss = ss + (char) c;
            System.out.print((char) c);
        }
        IProtocol.printBytes(mess);

        return mess;
    }

}
