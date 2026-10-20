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
 * @author Egorov-EV
 */
public class Cmd {

    public static String SERVER_SEARCH = "server search";
    public static String TABLO_SEARCH = "tablo search";
    public static String READ = "read";
    public static String WRITE = "write";
    public static String PRINT = "print";
    public static String MARQUEE = "marquee";

    public Cmd() {
    }

    public Cmd(Long id, Data data) {
        this.id = id;
        this.data = data;
    }

    @Expose
    @SerializedName("id")
    public Long id;
    @Expose
    @SerializedName("data")
    public Data data;

    public static class Data {

        public Data() {
        }

        public Data(String name) {
            this.name = name;
        }

        public Data(String text, Integer blinkTime, Integer blinkPeriod, Integer ticketFont, Integer rowCenter) {
            this.blinkTime = blinkTime;
            this.blinkPeriod = blinkPeriod;
            this.ticketFont = ticketFont;
            this.text = text;
            this.rowCenter = rowCenter;
        }

        public Data(String text, Integer marqueeSpeed, Integer marqueeFont) {
            this.marqueeSpeed = marqueeSpeed;
            this.marqueeFont = marqueeFont;
            this.text = text;
        }

        @Expose
        @SerializedName("did")
        public Long did;
        @Expose
        @SerializedName("current_ip")
        public String currentIp;
        @Expose
        @SerializedName("current_netmask")
        public String currentNetmask;
        @Expose
        @SerializedName("current_gateway")
        public String currentGateway;
        @Expose
        @SerializedName("mac")
        public String mac;
        @Expose
        @SerializedName("tcp_port")
        public Integer tcpPort;
        @Expose
        @SerializedName("hardware_version")
        public String hardwareVersion;
        @Expose
        @SerializedName("firmware_version")
        public String firmwareVersion;
        @Expose
        @SerializedName("firmware_date")
        public String firmwareDate;
        /*
        read
Команда для получения параметра[ов] табло.
         */
        @Expose
        @SerializedName("name")
        public String name;// could be = "all"
        /*
        print
Поскольку табло делятся на однострочные и многострочные (главные), протокол предлагает два варианта команды print -
однострочную:
         */
        @Expose
        @SerializedName("row_center")
        public Integer rowCenter;
        @Expose
        @SerializedName("blink_period")
        public Integer blinkPeriod;
        @Expose
        @SerializedName("blink_time")
        public Integer blinkTime;
        @Expose
        @SerializedName("ticket_font")
        public Integer ticketFont;
        @Expose
        @SerializedName("text")
        public String text;
        @Expose
        @SerializedName("center")
        public Integer center;
        /*
        marquee
Как и команда print, команда marquee подразделяется на однострочную: и многострочную:
         */
        @Expose
        @SerializedName("marquee_speed")
        public Integer marqueeSpeed;
        @Expose
        @SerializedName("marquee_font")
        public Integer marqueeFont;
    }

}
