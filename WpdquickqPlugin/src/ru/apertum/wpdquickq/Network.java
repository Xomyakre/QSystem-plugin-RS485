/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.wpdquickq;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.apertum.qsystem.common.AUDPServer;
import ru.apertum.qsystem.common.GsonPool;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.model.INetProperty;

/**
 *
 * @author Egorov-EV
 */
public class Network {

    private static Thread t;
    private static FakeUDPServer udpfakeServer;
    private static UDPServer udpServer;

    private static void stopFakeServer(Thread t) {
        if (t != null && t.isAlive()) {
            System.out.println("[FAKE] Stop...");
            t.interrupt();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
    }
    
    public static void stopFakeTCPServer() {
        stopFakeServer(t);
    }

    public static void startFakeServer(int port) {
        stopFakeServer(t);
        t = new Thread(() -> {
            // привинтить сокет на локалхост, порт 3128
            final ServerSocket server;
            try {
                System.out.println("[FAKE] Сервер системы захватывает порт \"" + port + "\".");
                server = new ServerSocket(port);
                server.setSoTimeout(500);
            } catch (IOException e) {
                throw new RuntimeException("[FAKE] Network error. Creating net socket is not possible: " + e);
            } catch (Exception e) {
                throw new RuntimeException("[FAKE] Network error: " + e);
            }

            System.out.println("[FAKE] Fake Server Wpdquick started.\n");
            // слушаем порт
            while (!Thread.interrupted()) {

                try {
                    Socket socket = server.accept();

                    // из сокета клиента берём поток входящих данных
                    InputStream is;
                    try {
                        is = socket.getInputStream();
                    } catch (IOException e) {
                        throw new RuntimeException("[FAKE] Input Stream broken: " + Arrays.toString(e.getStackTrace()));
                    }

                    final String data;
                    try {
                        // подождать пока хоть что-то приползет из сети, но не более 10 сек.
                        int i = 0;
                        while (is.available() == 0 && i < 100) {
                            Thread.sleep(100);//бля
                            i++;
                        }

                        StringBuilder sb = new StringBuilder(new String(readInputStream(is)));
                        while (is.available() != 0) {
                            sb = sb.append(new String(readInputStream(is)));
                            Thread.sleep(150);//бля
                        }
                        data = sb.toString();
                        sb.setLength(0);
                    } catch (IOException ex) {
                        throw new RuntimeException("[FAKE] Ошибка при чтении из входного потока: " + ex);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException("[FAKE] Проблема со сном: " + ex);
                    } catch (IllegalArgumentException ex) {
                        throw new RuntimeException("[FAKE] Ошибка декодирования сетевого сообщения: " + ex);
                    }
                    System.out.println("[FAKE] Task:\n" + (data.length() > 200 ? (data.substring(0, 200) + "...") : data));

                    final String answer;
                    final ReqCmd rpc;
                    final Gson gson = GsonPool.getInstance().borrowGson();
                    try {
                        rpc = gson.fromJson(data, ReqCmd.class);
                        // полученное задание передаем в пул
                        final Object result = new RespCmd(rpc.id, new RespCmd.Responce(0L, "OK"));
                        answer = gson.toJson(result);
                    } catch (JsonSyntaxException ex) {
                        System.out.println("[FAKE] Received data \"" + data + "\" has not correct JSOM format. " + ex);
                        throw new RuntimeException("[FAKE] Received data \"" + data + "\" has not correct JSOM format. " + Arrays.toString(ex.getStackTrace()));
                    } catch (Exception ex) {
                        System.out.println("[FAKE] Late caught the error when running the command. " + ex);
                        throw new RuntimeException("[FAKE] Поздно пойманная ошибка при выполнении команды: " + Arrays.toString(ex.getStackTrace()));
                    } finally {
                        GsonPool.getInstance().returnGson(gson);
                    }

                    // выводим данные:
                    System.out.println("[FAKE] Response:\n" + (answer.length() > 200 ? (answer.substring(0, 200) + "...") : answer));
                    try {
                        // Передача данных ответа
                        socket.getOutputStream().write(answer.getBytes());
                        socket.getOutputStream().flush();
                        socket.getOutputStream().close();
                    } catch (IOException e) {
                        throw new RuntimeException("[FAKE] Ошибка при записи в поток: " + Arrays.toString(e.getStackTrace()));
                    }

                } catch (SocketTimeoutException e) {
                    // ничего страшного, гасим исключение стобы дать возможность отработать входному/выходному потоку
                } catch (IOException e) {
                    throw new RuntimeException("Network error: " + e);
                }

            }
            try {
                server.close();
            } catch (IOException ex) {
            }
            System.out.println("[FAKE] Stoped.");
        }
        );
        t.setDaemon(true);
        t.start();
    }

    protected final static class FakeUDPServer extends AUDPServer {

        int portRq;

        public FakeUDPServer(int port) {
            super(port);
        }

        @Override
        protected void getData(String string, InetAddress ia, int i) {
            System.out.println("[UDP fake] receive from " + ia + " : " + i);
            System.out.println(string);
            System.out.println();
            System.out.println();
            for (long j = 0; j < 3; j++) {

                final Cmd.Data data = new Cmd.Data();
                data.did = j;
                final ReqCmd cmd = new ReqCmd(j, Cmd.SERVER_SEARCH, data);
                sendUDPmsg(new INetProperty() {
                    @Override
                    public Integer getPort() {
                        return portRq;
                    }

                    @Override
                    public InetAddress getAddress() {
                        try {
                            return InetAddress.getLocalHost();
                        } catch (UnknownHostException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }, cmd);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                }
            }
        }

    }

    public static interface IUdpCatcher {

        public void process(String data, InetAddress ia, int i);

    }

    protected final static class UDPServer extends AUDPServer {

        IUdpCatcher catcher;

        public UDPServer(int port) {
            super(port);
        }

        @Override
        protected void getData(String string, InetAddress ia, int i) {
            System.out.println("[UDP] receive from " + ia + " : " + i + " \n");
            System.out.println(">>>   " + string + "\n");
            catcher.process(string, ia, i);
        }

    }

    public static void startFakeUDPserver(int port, int portRq) {
        stopUDPserver(udpfakeServer);
        udpfakeServer = new FakeUDPServer(port);
        udpfakeServer.portRq = portRq;
        udpfakeServer.start();
        System.out.println("[UDP fake] Started.");
    }

    public static void stopFakeUDPserver() {
        stopUDPserver(udpfakeServer);
    }

    public static void startUDPserver(int port, IUdpCatcher catcher) {
        stopUDPserver(udpServer);
        System.out.println("[UDP] Starting...");
        udpServer = new UDPServer(port);
        udpServer.catcher = catcher;
        udpServer.start();
        System.out.println("[UDP] Started.");
    }

    public static void stopUDPserver() {
        stopUDPserver(udpServer);
    }

    private static void stopUDPserver(AUDPServer udp) {
        if (udp != null && udp.isActivate()) {
            System.out.println("[UDP] Stop...");
            udp.stop();
        }
    }

    synchronized public static void sendUDPbroadcast(int port, ReqCmd cmd) {
        final String message;
        Gson gson = GsonPool.getInstance().borrowGson();
        try {
            message = gson.toJson(cmd);
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        Uses.sendUDPBroadcast(message, port);
        System.out.println("[UDP] Broadcast sended.");
    }

    synchronized public static void sendUDPmsg(INetProperty netProperty, ReqCmd cmd) {
        final String message;
        Gson gson = GsonPool.getInstance().borrowGson();
        try {
            message = gson.toJson(cmd);
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        Uses.sendUDPMessage(message, netProperty.getAddress(), netProperty.getPort());
        System.out.println("[UDP] sended on " + netProperty.getAddress());
    }

    synchronized public static void sendUDPmsg(INetProperty netProperty, RespCmd cmd) {
        Uses.sendUDPMessage(cmd.toString(), netProperty.getAddress(), netProperty.getPort());
        System.out.println("[UDP] sended on " + netProperty.getAddress());
    }

    public static byte[] readInputStream(InputStream stream) throws IOException {
        final byte[] result;
        final DataInputStream dis = new DataInputStream(stream);
        result = new byte[stream.available()];
        dis.readFully(result);
        return result;
    }

    synchronized public static RespCmd sendRpc(INetProperty netProperty, ReqCmd cmd) {
        final String message;
        Gson gson = GsonPool.getInstance().borrowGson();
        try {
            message = gson.toJson(cmd);
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        final String data;
        try {

            System.out.println("Task \"" + cmd.cmd + "\" on " + netProperty.getAddress().getHostAddress() + ":" + netProperty.getPort() + "#\n" + message);
            final Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(netProperty.getAddress(), netProperty.getPort()), 15000);
            } catch (IOException ex) {
                throw new Exception("no_connect_to_server", ex);
            }
            System.out.println("Socket was created.");
            final PrintWriter writer;
            final Scanner in;
            try {
                writer = new PrintWriter(socket.getOutputStream());
                writer.print(message);
                System.out.println("Sending...");
                writer.flush();
                System.out.println("Reading...");
                StringBuilder sb = new StringBuilder();
                in = new Scanner(socket.getInputStream());
                while (in.hasNextLine()) {

                    sb = sb.append(in.nextLine()).append("\n");
                }
                data = sb.toString();
                sb.setLength(0);
                writer.close();
                in.close();
            } finally {
                socket.close();
            }
            System.out.println("Response:\n" + data);

        } catch (Exception ex) {
            throw new RuntimeException("no_response_from_server", ex);
        }
        gson = GsonPool.getInstance().borrowGson();
        try {
            final RespCmd rpc = gson.fromJson(data, RespCmd.class);
            if (rpc == null) {
                throw new RuntimeException("error_on_server_no_get_response");
            }
            if (rpc.responce.code == null || rpc.responce.code != 0) {
                throw new RuntimeException("tack_failed" + " " + rpc.responce.code + ":" + rpc.responce.comment);
            }
            return rpc;
        } catch (JsonSyntaxException ex) {
            throw new RuntimeException("bad_response" + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }

    }

}
