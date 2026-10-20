/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import ru.apertum.qsystem.common.CustomerState;

/**
 *
 * @author Evgeniy Egorov
 */
public class Event {

    public final String point;
    public final String ticket;
    public final CustomerState state;
    public final String preffix;
    public final int number;

    public Event(String point, String ticket, String preffix, int number, CustomerState state) {
        this.point = point;
        this.state = state;
        this.ticket = ticket;
        this.preffix = preffix;
        this.number = number;
    }
}
