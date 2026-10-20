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
public class RespCmd extends Cmd {

    /*
    В ответе (в случае ошибки) добавлен параметр error, который содержит описание ошибки, но отсутствует параметр
    command:
    {
    "id": 1,
    "error": {
    "code": 0,
    "decs": "OK"
    },
    "data": {
    "did": 1
    }
    }
     */
    public RespCmd() {
    }

    public RespCmd(Long id, Responce responce) {
        this.id = id;
        this.responce = responce;
    }

    public RespCmd(Responce responce) {
        this.responce = responce;
    }

    public RespCmd(Long id, Responce responce, Data data) {
        super(id, data);
        this.responce = responce;
    }

    @Expose
    @SerializedName("error")
    public Responce responce;

    static public class Responce {

        public Responce() {
        }

        public Responce(Long code, String comment) {
            this.code = code;
            this.comment = comment;
        }

        @Expose
        @SerializedName("code")
        public Long code;

        @Expose
        @SerializedName("decs")
        public String comment;

    }

}
