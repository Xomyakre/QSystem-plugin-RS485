/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core.protocols;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import static ru.apertum.cbrs485.core.protocols.IProtocol.toByte;
import static ru.apertum.cbrs485.core.protocols.IProtocol.toByteArrayBigI;
import ru.apertum.qsystem.common.CustomerState;

/**
 *
 * @author Evgeniy Egorov
 */
public final class SvetovodM extends RS485Property.SvetovodMProtocol {

    private SvetovodM() {
    }

    public SvetovodM(int lenB, int he, int fontSize, String fontName) {
        setLenghtBy8(lenB);
        setHeight(he);
        setFontSize(fontSize);
        setFontName(fontName);
        bi1 = new BufferedImage(getLenghtBy8() * 8 * 2, getHeight(), BufferedImage.TYPE_BYTE_BINARY);
    }

    private BufferedImage bi1;

    private synchronized BufferedImage getBi() {
        if (bi1 == null) {
            bi1 = new BufferedImage(getLenghtBy8() * 8 * 2, getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        }
        return bi1;
    }

    private static class SvetovodMHolder {

        private static final SvetovodM INSTANCE = new SvetovodM();
    }

    public static SvetovodM get() {
        return SvetovodMHolder.INSTANCE;
    }

    public void prepare() {
        /*
         lenB = Integer.parseInt(Sender485.getInstance().props.getProperty("protocol.svetovodm.len", "6"));
         len = lenB * 8;
         he = Integer.parseInt(Sender485.getInstance().props.getProperty("protocol.svetovodm.he", "18"));
         fontSize = Integer.parseInt(Sender485.getInstance().props.getProperty("protocol.svetovodm.fontsize", "25"));
         fontName = Sender485.getInstance().props.getProperty("protocol.svetovodm.fontname", "Arial");
         */
    }

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());
        byte[] mess = new byte[8 + getLenghtBy8() * getHeight() + 1];
        mess[0] = -32;// $E0 – стартовый байт = 224 = -32
        mess[1] = 0;// $00 – адрес отправителя. (Компьютер всегда $00)
        int adr = operator.getAddressRS();
        mess[2] = (byte) (adr >= 0 && adr < 128 ? adr : (adr - 256)); // SysNum – адрес получателя [1...254].

        mess[3] = 0;
        mess[4] = toByte((getLenghtBy8() * getHeight()) / 256);
        mess[5] = toByte((getLenghtBy8() * getHeight()) % 256); //Для табло, на пример, 9 х 24 каждая строка это три байта (24 пикселя). Строки расположены последовательно. Т.е. первый пиксель на второй строке - это старший бит 4-го байта.  

        short s = (short) (224 + getLenghtBy8() * getHeight() + adr);
        byte l = (byte) (s & 0xff);
        byte h = (byte) ((s >> 8) & 0xff);
        int hs = h;
        hs = hs + l;

        mess[6] = (byte) (hs >= 0 && hs < 128 ? hs : (hs - 256)); // HIGH (CRC) + LOW (CRC);
        mess[7] = l; // LOW (CRC);
        System.out.println("--->>");

        /*
         **********************************
         */
        boolean finv = event.state == CustomerState.STATE_INVITED || event.state == CustomerState.STATE_INVITED_SECONDARY;
        boolean ff = event.state == CustomerState.STATE_WORK || event.state == CustomerState.STATE_WORK_SECONDARY;
        String txt = (ff || finv) ? event.ticket : "";
        txt = txt.replaceAll("", " ");

        ProcessDraw(getBi(), txt);

        int len = getLenghtBy8() * 8;
        boolean[][] bb = new boolean[len * 2][getHeight()]; // По сути один бит - один пиксель. 1 - горит светодиод, 0 - не горит. true - не горит

        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < len * 2; x++) {
                bb[x][y] = getBi().getRGB(x, y) < -10;
            }
        }

        // отобразили нарисованное на канче в булевскую матрицу, теперь пожмем одинаковые столбцы диодов
        int p = 0;
        int zaelo = 0;
        int lastP = 0;
        while (p < len * 2 - 1) {
            boolean f = true; // если два рядом одинаковые
            for (int y = 0; y < getHeight(); y++) {
                f = f && (bb[p][y] == bb[p + 1][y]);
            }
            boolean f1 = p == 0; // если первый стотбец диодов не горит совсем
            for (int y = 0; y < getHeight(); y++) {
                //f1 = f1 && bb[p][y]; inv
                f1 = f1 && !bb[p][y];
            }

            if (f || f1) { // схлоповаем
                if (lastP == p) {
                    zaelo++;
                } else {
                    zaelo = 0;
                }
                lastP = p;
                boolean re = true;
                for (int x = p; x < len * 2 - 1; x++) {
                    for (int y = 0; y < getHeight(); y++) {
                        bb[x][y] = bb[x + 1][y];
                        //re = re && bb[x + 1][y]; inv
                        re = re && !bb[x + 1][y];
                    }
                }
                if (re || zaelo > len * 2) {
                    break;
                }

                /* inv
                 for (int y = 0; y < he; y++) {
                 bb[len * 2 - 1][y] = true;
                 }
                 */
            } else {
                p++;
            }
        }

        //Добавим пробельчик между буквами и цыфрами
        txt = txt.replaceAll("\\s", "").replaceAll("\\d", "");
        if (txt.length() > 0) {
            p = 0;
            int cnt = txt.length(); // если два рядом одинаковые
            while (cnt > 0 && p < len) {
                boolean f = true;
                for (int y = 0; y < getHeight(); y++) {
                    f = f && (bb[p][y] == false);
                }
                cnt = cnt - (f ? 1 : 0);
                p++;
            }
            for (int y = 0; y < getHeight(); y++) {
                for (int i = len - 2; i >= p - 1; i--) {
                    bb[i + 1][y] = bb[i][y];
                }
            }

        } // добавили пробельчик

        // центранем
        p = len - 1; //  p - последний зажженый диод
        boolean f = true; // если два рядом одинаковые
        while (f && p > 0) {
            for (int y = 0; y < getHeight(); y++) {
                f = f && (bb[p][y] == false);
            }
            if (f) {
                p--;
            }
        }

        int ae = len - p - 1;
        int ae2 = (ae / 2); // - половина темных диодов, на которую надо сместить все изображение. типа центровка
        for (int y = 0; y < getHeight(); y++) {
            for (int i = p; i >= 0; i--) {
                bb[i + ae2][y] = bb[i][y];
                if (i < ae2) {
                    bb[i][y] = false;
                }
            }
        }

        boolean[] bib = new boolean[len * getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < len; x++) {
                bib[x + (y * len)] = bb[x][y];
            }
        }

        //************
        byte[] data = toByteArrayBigI(bib);
        int k = 8;
        int su = 0;
        for (byte b : data) {
            mess[k++] = b;
            su = su + b;
        }
        su = su % 256;

        mess[8 + getLenghtBy8() * getHeight() + 1 - 1] = (byte) (su >= 0 && su < 128 ? su : (su - 256)); // DATA_CRC = SUM (DATA0...DATAN).

        System.out.println("length=" + mess.length);
        return mess;
    }

    private void ProcessDraw(BufferedImage image, String text) {
        Graphics2D gO = image.createGraphics();
        gO.setColor(Color.white);
        gO.fillRect(0, 0, 1000, 1000); // inv
        gO.setColor(Color.BLACK);  // inv
        gO.setFont(new Font(getFontName(), Font.PLAIN, getFontSize()));
        gO.drawString(text, 0, getHeight());
    }

}
