/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.plugins.events;

import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.Sender485;
import ru.apertum.qsystem.client.forms.FClient;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.model.INetProperty;
import ru.apertum.qsystem.extra.IStartClient;
import ru.apertum.qsystem.server.model.QUser;

import java.awt.event.ActionEvent;

import static ru.apertum.cbrs485.plugins.IClientboardRS485PluginUID.UID;

/**
 * @author Evgeniy Egorov
 */
public class ClientEventSender implements IStartClient {

    @Override
    public void start(FClient form) {
    }

    @Override
    public void beforePressButton(QUser user, INetProperty netProperty, RpcGetSelfSituation.SelfSituation situation, ActionEvent evt, int keyId) {

    }

    @Override
    public void pressedButton(QUser user, INetProperty netProperty, RpcGetSelfSituation.SelfSituation situation, ActionEvent evt, int keyId) {
        if (situation.getCustomer() == null) {
            Sender485.getInstance().send(new Event(user.getPoint(), "", "", 0, CustomerState.STATE_FINISH));
        } else {
            Sender485.getInstance().send(new Event(user.getPoint(), situation.getCustomer().getFullNumber(), situation.getCustomer().getPrefix(), situation.getCustomer().getNumber(), situation.getCustomer().getState()));
        }
    }

    @Override
    public void beforePressButtonParallel(QUser user, INetProperty netProperty, RpcGetSelfSituation.SelfSituation situation, ActionEvent evt, int keyId) {

    }

    @Override
    public void pressedButtonParallel(QUser user, INetProperty netProperty, RpcGetSelfSituation.SelfSituation situation, ActionEvent evt, int keyId) {

    }

    @Override
    public String getDescription() {
        return "Плагин \"ClientboardRS485Plugin\" во время смены статуса клиенту выводит инфу в гирлянду RS ДЛЯ ОПЕРАТОРА";
    }

    @Override
    public long getUID() {
        return UID;
    }
}
