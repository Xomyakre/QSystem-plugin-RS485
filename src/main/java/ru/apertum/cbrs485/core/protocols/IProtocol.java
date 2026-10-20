/*
 * Apertum QSystem
 */
package ru.apertum.cbrs485.core.protocols;

import java.util.BitSet;
import ru.apertum.cbrs485.core.Event;
import ru.apertum.cbrs485.core.RS485Property;

/**
 *
 * @author Evgeniy Egorov
 */
public interface IProtocol {

    public byte[] getData(Event event, RS485Property.Operator operator, RS485Property props);

    public static int toInt(byte b) {
        return (int) b & 0x000000FF;
    }

    public static byte toByte(int i) {
        i = i % 256;
        return (byte) (i >= 0 && i < 128 ? i : (i - 256));
    }

    public static boolean[] toBooleanArray(byte[] bytes) {
        final BitSet bits = BitSet.valueOf(bytes);
        final boolean[] bools = new boolean[bytes.length * 8];
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            bools[i] = true;
        }
        return bools;
    }

    public static byte[] toByteArrayLessI(boolean[] bools) {
        BitSet bits = new BitSet(bools.length);
        for (int i = 0; i < bools.length; i++) {
            if (bools[i]) {
                bits.set(i);
            }
        }
        return bits.toByteArray();
    }

    public static byte[] toByteArrayBigI(boolean[] input) {
        byte[] toReturn = new byte[input.length / 8];
        for (int entry = 0; entry < toReturn.length; entry++) {
            for (int bit = 0; bit < 8; bit++) {
                if (input[entry * 8 + bit]) {
                    toReturn[entry] |= (128 >> bit);
                }
            }
        }
        return toReturn;
    }
    
    public static void printBytes(byte[] bytes){
        String ss = "";
        String ss2 = "";
        for (int i = 0; i < bytes.length; i++) {
            int c = toInt(bytes[i]);
            ss = ss + Integer.toHexString(c).toUpperCase() + "_";
            ss2 = ss2 + c + "_";
        }
        System.out.println("HEX: " + ss);
        System.out.println("DEC: " + ss2);
    }

}
