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
public class Solt extends RS485Property.SoltProtocol {

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        /*
         byte[] mess = "no".getBytes();
         IProtocol.printBytes(mess);
         return mess;
         */
    }

}
