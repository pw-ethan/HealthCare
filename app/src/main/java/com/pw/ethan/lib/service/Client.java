package com.pw.ethan.lib.service;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {


//    private OutputStream outStream = null;
//    private InputStream inStream = null;


//    private Thread send = null;
//    private Thread rec = null;


    private final int READ_MAX = 4096;
    private String IP = "";
    private int PORT = 0;
    private Socket socket = null;

    private Thread conn = null;

    private Context context;
    private ISocketResponse respListener;
    private LinkedBlockingQueue<Packet> requestQueen = new LinkedBlockingQueue<>();
    private final Object lock = new Object();

    private final int OPEN = 0;
    private final int CLOSED = 1;

    private int SOCKET_STATUS = CLOSED;

    private final String TAG = "HealthCare";//"no";

    public Client(Context context, ISocketResponse respListener) {
        this.context = context;
        this.respListener = respListener;
    }

    public void open(String host, int port) {
        this.IP = host;
        this.PORT = port;
        Connect();
    }

    private void Connect() {
        conn = new Thread(new OneRequest());
        conn.start();
    }

    public int send(Packet in) {
        Log.i(TAG, "send: " + new String(in.getPacket()));
        synchronized (lock) {
            requestQueen.add(in);
            lock.notifyAll();
        }
        return in.getId();
    }

    public void close() {
        try {
            if (null != conn && conn.isAlive()) {
                synchronized (lock) {
                    SOCKET_STATUS = CLOSED;
                    lock.notifyAll();
                }
                conn.join();
                //conn.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            conn = null;
        }

        try {
            try {
                if (null != socket) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                socket = null;
            }

            requestQueen.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class OneRequest implements Runnable {
        public void run() {
            Log.i(TAG, "OneRequest -- Start.");

            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(IP, PORT), 15 * 1000);
                synchronized (lock) {
                    SOCKET_STATUS = OPEN;
                }
            } catch (IOException e) {
                Log.e(TAG, "OneRequest - connect: " + e.getMessage());
                e.printStackTrace();
            }
            //Log.i(TAG, "OneRequest - connect OK");
            synchronized (lock) {
                while (socket != null && SOCKET_STATUS == OPEN) {
                    try {
                        Packet item;

                        item = requestQueen.poll();

                        if (null != item) {
                            if (socket.isInputShutdown() || socket.isOutputShutdown()) {
                                Log.e(TAG, "shutdown");
                            }
                            OutputStream os = socket.getOutputStream();
                            os.write(item.getPacket());
                            os.flush();

                            int readIndex = READ_MAX;
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()), readIndex);
                            char[] charArray = new char[readIndex];
                            int read_rst = bufferedReader.read(charArray);
                            String respond = new String(charArray, 0, read_rst);
                            respListener.onSocketResponse(respond);
                        } else {
                            lock.wait();
                        }
                    } catch (InterruptedException ie) {
                        Log.i(TAG, "OneRequest - send & recv; InterruptedException");
                    } catch (Exception e) {
                        Log.e(TAG, "OneRequest - send & recv; " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
