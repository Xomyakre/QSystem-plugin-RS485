package ru.apertum.cbrs485.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Отдельный журнал всего, что происходит с табло и COM портом: logs/rs485.log
 * В каждой строке процесс (SERVER/CLIENT + pid) и поток, чтобы было видно, кто именно пишет в порт.
 */
public final class PortLog {

    private static final File FILE = new File("logs", "rs485.log");
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final String PROCESS;

    static {
        final String cmd = String.valueOf(System.getProperty("sun.java.command"));
        final String kind = cmd.startsWith("ru.apertum.qsystem.Server") ? "SERVER" : "CLIENT";
        PROCESS = kind + " pid=" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    private PortLog() {
    }

    /** Событие или действие. */
    public static void info(String msg) {
        write(msg);
    }

    /** Посылка в порт. */
    public static void tx(String port, byte[] bytes) {
        write("TX " + port + " " + bytes.length + "b: " + hex(bytes));
    }

    /** Ответ из порта. */
    public static void rx(String port, byte[] bytes) {
        write("RX " + port + " " + (bytes == null ? 0 : bytes.length) + "b: " + hex(bytes));
    }

    public static void error(String msg, Throwable ex) {
        final StringWriter sw = new StringWriter();
        if (ex != null) {
            ex.printStackTrace(new PrintWriter(sw));
        }
        write("ERROR " + msg + (ex == null ? "" : "\n" + sw));
    }

    public static String hex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    private static synchronized void write(String msg) {
        final String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                + " [" + PROCESS + " " + Thread.currentThread().getName() + "] " + msg + System.lineSeparator();
        try {
            FILE.getParentFile().mkdirs();
            if (FILE.length() > MAX_SIZE) {
                final File old = new File(FILE.getPath() + ".old");
                old.delete();
                FILE.renameTo(old);
            }
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(FILE, true), StandardCharsets.UTF_8)) {
                w.write(line);
            }
        } catch (Exception ex) {
            System.err.println("PortLog: " + ex);
        }
    }
}
