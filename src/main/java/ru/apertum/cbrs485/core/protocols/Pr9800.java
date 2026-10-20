/*
 * Apertum QSystem — универсальный протокол PR9800 (табло).
 */
package ru.apertum.cbrs485.core.protocols;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicInteger;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;
import ru.apertum.qsystem.common.CustomerState;

/**
 * Универсальный протокол табло PR9800: пакет L1 (STX/ETX) + пакет L2 (TComPacket),
 * команда вывода текста (Cmd=5), кодировка Win-1251.
 *
 * @author Evgeniy Egorov
 */
public class Pr9800 extends RS485Property.Pr9800Protocol {

    public static final int CMD_OUT_TEXT = 5;
    public static final int PACK_FLAG_NOANSWER = 0x01;
    public static final int PACK_FLAG_NOCHECKID = 0x02;
    public static final int HOST_ADDR = 0;
    public static final int STATUS_COMMAND = 0x80;
    public static final int STX = 0x02;
    public static final int ETX = 0x03;
    public static final int L2_HEAD_SIZE = 10;

    private static final AtomicInteger PACKET_ID = new AtomicInteger(0);

    private static final int[] CRC_TABLE = {
        0, 94, 188, 226, 97, 63, 221, 131, 194, 156, 126, 32, 163, 253, 31, 65,
        157, 195, 33, 127, 252, 162, 64, 30, 95, 1, 227, 189, 62, 96, 130, 220,
        35, 125, 159, 193, 66, 28, 254, 160, 225, 191, 93, 3, 128, 222, 60, 98,
        190, 224, 2, 92, 223, 129, 99, 61, 124, 34, 192, 158, 29, 67, 161, 255,
        70, 24, 250, 164, 39, 121, 155, 197, 132, 218, 56, 102, 229, 187, 89, 7,
        219, 133, 103, 57, 186, 228, 6, 88, 25, 71, 165, 251, 120, 38, 196, 154,
        101, 59, 217, 135, 4, 90, 184, 230, 167, 249, 27, 69, 198, 152, 122, 36,
        248, 166, 68, 26, 153, 199, 37, 123, 58, 100, 134, 216, 91, 5, 231, 185,
        140, 210, 48, 110, 237, 179, 81, 15, 78, 16, 242, 172, 47, 113, 147, 205,
        17, 79, 173, 243, 112, 46, 204, 146, 211, 141, 111, 49, 178, 236, 14, 80,
        175, 241, 19, 77, 206, 144, 114, 44, 109, 51, 209, 143, 12, 82, 176, 238,
        50, 108, 142, 208, 83, 13, 239, 177, 240, 174, 76, 18, 145, 207, 45, 115,
        202, 148, 118, 40, 171, 245, 23, 73, 8, 86, 180, 234, 105, 55, 213, 139,
        87, 9, 235, 181, 54, 104, 138, 212, 149, 203, 41, 119, 244, 170, 72, 22,
        233, 183, 85, 11, 136, 214, 52, 106, 43, 117, 151, 201, 74, 20, 246, 168,
        116, 42, 200, 150, 21, 75, 169, 247, 182, 232, 10, 84, 215, 137, 107, 53
    };

    @Override
    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props) {
        System.out.println("ClientboardRS485Plugin Protocol: " + this.getName());

        final boolean invited = event.state == CustomerState.STATE_INVITED
                || event.state == CustomerState.STATE_INVITED_SECONDARY;
        final boolean work = event.state == CustomerState.STATE_WORK
                || event.state == CustomerState.STATE_WORK_SECONDARY;

        final String text;
        switch (event.state) {
            case STATE_INVITED:
            case STATE_INVITED_SECONDARY:
            case STATE_WORK:
            case STATE_WORK_SECONDARY:
                text = (event.ticket == null || event.ticket.trim().isEmpty()) ? " " : event.ticket.trim();
                break;
            case STATE_DEAD:
            case STATE_FINISH:
            case STATE_POSTPONED:
            case STATE_REDIRECT:
                text = " ";
                break;
            default:
                return null;
        }

        final int dest = operator.getAddressRS() == null ? 1 : operator.getAddressRS();
        final byte[] packet = buildOutTextPacket(dest, text, invited && !work);
        IProtocol.printBytes(packet);
        return packet;
    }

    public byte[] buildOutTextPacket(int destAddr, String text, boolean blink) {
        final byte[] payload = buildTextPayload(text, blink);
        return wrapCommand(destAddr, CMD_OUT_TEXT, PACK_FLAG_NOANSWER | PACK_FLAG_NOCHECKID, payload);
    }

    byte[] buildTextPayload(String text, boolean blink) {
        byte[] txt;
        try {
            txt = (text == null || text.isEmpty() ? " " : text).getBytes("windows-1251");
        } catch (UnsupportedEncodingException ex) {
            txt = (text == null || text.isEmpty() ? " " : text).getBytes();
        }
        final ByteBuffer buf = ByteBuffer.allocate(9 + txt.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(IProtocol.toByte((nz(getDispNo()) & 0x3F) | displayTypeBits()));
        buf.put(IProtocol.toByte(nz(getFont())));
        buf.put(IProtocol.toByte(nz(getAlign())));
        buf.put(IProtocol.toByte(nz(getOpt())));
        buf.put(IProtocol.toByte(nz(getSpeed())));
        final int color = nz(getColor());
        buf.put(IProtocol.toByte(color <= 0 ? 1 : color));
        if (blink) {
            buf.put((byte) 2); // мигание
            buf.put(IProtocol.toByte(255));
            buf.put(IProtocol.toByte(250));
        } else {
            buf.put((byte) 0);
            buf.put((byte) 0);
            buf.put((byte) 0);
        }
        buf.put(txt);
        return buf.array();
    }

    public static byte[] wrapCommand(int destAddr, int cmd, int flags, byte[] data) {
        final byte[] l2 = buildLevel2(HOST_ADDR, destAddr, nextPacketId(), cmd, flags, STATUS_COMMAND, data);
        return wrapLevel1(l2);
    }

    static int nextPacketId() {
        return PACKET_ID.incrementAndGet() & 0x7F;
    }

    static byte[] buildLevel2(int src, int dst, int pid, int cmd, int flags, int status, byte[] data) {
        final int dataLen = data == null ? 0 : data.length;
        final ByteBuffer buf = ByteBuffer.allocate(L2_HEAD_SIZE + dataLen);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) (src & 0xFFFF));
        buf.putShort((short) (dst & 0xFFFF));
        buf.put((byte) (pid & 0xFF));
        buf.put((byte) (cmd & 0xFF));
        buf.put((byte) (flags & 0xFF));
        buf.put((byte) (status & 0xFF));
        buf.putShort((short) (dataLen & 0xFFFF));
        if (dataLen > 0) {
            buf.put(data);
        }
        return buf.array();
    }

    static byte[] wrapLevel1(byte[] l2) {
        final int cs = countCs(l2);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(l2.length + 16);
        out.write(STX);
        writeEncodedWord(out, l2.length);
        encodeData(l2, out);
        writeEncodedWord(out, cs);
        out.write(ETX);
        return out.toByteArray();
    }

    static int countCs(byte[] data) {
        int cs1 = 0;
        int cs2 = 0;
        for (byte value : data) {
            final int b = value & 0xFF;
            cs1 = CRC_TABLE[cs1 ^ b];
            cs2 = (cs2 + (~b)) & 0xFF;
        }
        return ((cs2 & 0xFF) << 8) | (cs1 & 0xFF);
    }

    static int encodeWord(int v) {
        return ((v & 0x3F80) << 1) | (v & 0x7F) | 0x8080;
    }

    private static void writeEncodedWord(ByteArrayOutputStream out, int v) {
        final int encoded = encodeWord(v);
        out.write(encoded & 0xFF);
        out.write((encoded >> 8) & 0xFF);
    }

    static void encodeData(byte[] src, ByteArrayOutputStream dst) {
        for (byte value : src) {
            int b = (value & 0xFF) ^ 0x80;
            if (b < 0x20 || b == 0x7F) {
                dst.write(0x7F);
                dst.write(b | 0x80);
            } else {
                dst.write(b);
            }
        }
    }

    private int displayTypeBits() {
        final int type = nz(getDisplayType());
        return (type & 0x03) << 6;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
