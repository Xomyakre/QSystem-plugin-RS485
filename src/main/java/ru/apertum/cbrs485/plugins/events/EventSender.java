/*
 *
 * Copyright (C) 2011 Evgeniy Egorov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.cbrs485.plugins.events;

import java.util.concurrent.ConcurrentHashMap;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.PortLog;
import ru.apertum.cbrs485.core.Sender485;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IChangeCustomerStateEvent;
import ru.apertum.cbrs485.plugins.IClientboardRS485PluginUID;

/**
 * Плагин во время смены статуса клиенту выводит его номер на табло RS485 над окном оператора:
 * вызов - номер мигает, начало приема - номер горит, завершение - табло гаснет.
 *
 * @author Evgeniy Egorov
 */
public class EventSender implements IChangeCustomerStateEvent, IClientboardRS485PluginUID {

    /**
     * Окно, куда вызвали талон. При завершении у клиента оператора уже может не быть,
     * а гасить надо то же табло, где номер горел.
     */
    private static final ConcurrentHashMap<String, String> POINT_BY_TICKET = new ConcurrentHashMap<>();

    public EventSender() {
        // сервер создает плагин при загрузке - сразу готовим порт в фоне, чтобы первый вызов не ждал
        if (Sender485.isEnabled()) {
            final Thread t = new Thread(Sender485::getInstance, "ClientboardRS485Plugin-init");
            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    public void change(String userPoint, String customerPrefix, int customerNumber, CustomerState cs) {
        PortLog.info("QSYSTEM change(point) window=" + userPoint + " ticket=" + customerPrefix + customerNumber + " state=" + cs);
        send(userPoint, customerPrefix, customerNumber, cs);
    }

    @Override
    public void change(QCustomer qc, CustomerState cs, Long newServiceId) {
        final String point = qc.getUser() == null ? null : qc.getUser().getPoint();
        PortLog.info("QSYSTEM change(customer) window=" + point + " ticket=" + qc.getPrefix() + qc.getNumber() + " state=" + cs);
        send(point, qc.getPrefix(), qc.getNumber(), cs);
    }

    private static void send(String userPoint, String prefix, int number, CustomerState cs) {
        final String ticket = (prefix == null ? "" : prefix) + number;
        String point = userPoint;
        final String nom;
        switch (cs) {
            case STATE_INVITED:
            case STATE_INVITED_SECONDARY:
            case STATE_WORK:
            case STATE_WORK_SECONDARY:
                if (point == null) {
                    return;
                }
                POINT_BY_TICKET.put(ticket, point);
                nom = ticket;
                break;
            case STATE_DEAD:
            case STATE_FINISH:
            case STATE_POSTPONED:
            case STATE_REDIRECT:
                final String last = POINT_BY_TICKET.remove(ticket);
                if (point == null) {
                    point = last;
                }
                if (point == null) {
                    return;
                }
                // Очищаем табло (5 пробелов)
                nom = "     ";
                break;
            default:
                // Для других состояний не отправляем на табло
                return;
        }
        Sender485.getInstance().send(new Event(point, nom, prefix, number, cs));
    }

    @Override
    public String getDescription() {
        return "Плагин \"ClientboardRS485Plugin\" во время смены статуса клиенту выводит инфу в гирлянду RS";
    }

    @Override
    public long getUID() {
        return UID;
    }
}
