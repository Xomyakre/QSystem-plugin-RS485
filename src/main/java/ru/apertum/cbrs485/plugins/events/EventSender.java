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

import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.Sender485;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IChangeCustomerStateEvent;
import ru.apertum.cbrs485.plugins.IClientboardRS485PluginUID;

/**
 * Плагин во время смены статуса клиенту отсылает статистику в сервер
 * отображений зональных табло по сети
 * @author Evgeniy Egorov
 */
public class EventSender implements IChangeCustomerStateEvent, IClientboardRS485PluginUID {

    @Override
    public void change(String userPoint, String customerPrefix, int customerNumber, CustomerState cs) {
        Sender485.getInstance().send(new Event(userPoint, customerPrefix + customerNumber, customerPrefix, customerNumber, cs));
    }

    @Override
    public void change(QCustomer qc, CustomerState cs, Long newServiceId) {
        // Создаем событие
        String nom = qc.getPrefix() + qc.getNumber();
        switch (cs) {
            case STATE_INVITED:
                break;
            case STATE_INVITED_SECONDARY:
                break;
            case STATE_WORK:
                break;
            case STATE_WORK_SECONDARY:
                break;
            case STATE_DEAD:
            case STATE_FINISH:
            case STATE_POSTPONED:
            case STATE_REDIRECT:
                // Очищаем табло (5 пробелов)
                nom = "     ";
                break;
            default:
                // Для других состояний не отправляем на табло
                return;
        }
        
        // Отправляем событие на табло
        if (qc.getUser() != null) {
            Sender485.getInstance().send(new Event(qc.getUser().getPoint(), nom, qc.getPrefix(), qc.getNumber(), cs));
        }
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
