/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.wpdquickq;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author SBT-Egorov-EV
 */
public class ReqCmd extends Cmd {

    /*
    В простейшем случае, запрос команды выглядит как JSON-объект с параметрами id (номер команды, уникален для пары
    запрос-ответ), command (название команды) и data (данные команды):    
     */
    //{
//  "id": 1,
//  "command": "read",
//  "data": {
//      "name": "did"
//   }
//}
    public ReqCmd() {
    }

    public ReqCmd(String cmd) {
        this.cmd = cmd;
    }

    public ReqCmd(Long id,String cmd, Data data) {
        super(id, data);
        this.cmd = cmd;
    }

    public ReqCmd(Long id, String cmd) {
        this.id = id;
        this.cmd = cmd;
    }

    @Expose
    @SerializedName("command")
    public String cmd;
}
