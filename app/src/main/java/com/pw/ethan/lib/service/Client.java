package com.pw.ethan.lib.service;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {

    private final int READ_MAX = 4096;
    private String IP = "";
    private int PORT = 0;

    private Socket socket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private Thread conn = null;
    private Thread send = null;
    private Thread rec = null;

    private Context context;
    private ISocketResponse respListener;
    private LinkedBlockingQueue<Packet> requestQueen = new LinkedBlockingQueue<>();
    private final Object lock = new Object();

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

    public synchronized void close() {
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

            try {
                if (null != outStream) {
                    outStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                outStream = null;
            }

            try {
                if (null != inStream) {
                    inStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                inStream = null;
            }

            try {
                if (null != conn && conn.isAlive()) {
                    conn.interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                conn = null;
            }

            try {
                if (null != send && send.isAlive()) {
                    send.interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                send = null;
            }

            try {
                if (null != rec && rec.isAlive()) {
                    rec.interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rec = null;
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
            } catch (IOException e) {
                Log.e(TAG, "OneRequest - connect: " + e.getMessage());
                e.printStackTrace();
            }
            //Log.i(TAG, "OneRequest - connect OK");

            try {
                outStream = socket.getOutputStream();
                inStream = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "OneRequest - get stream; " + e.getMessage());
                e.printStackTrace();
            }
            //Log.i(TAG, "OneRequest - get stream; OK");

            try {
                while (null != outStream) {
                    synchronized (lock) {
                        Packet item = requestQueen.poll();
                        if (null != item) {
                            outStream.write(item.getPacket());
                            outStream.flush();

                            int readIndex = READ_MAX;
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(inStream), readIndex);
                            char[] charArray = new char[readIndex];
                            int read_rst = bufferedReader.read(charArray);
                            String respond = new String(charArray, 0, read_rst);
                            bufferedReader.close();
                            respListener.onSocketResponse(respond);
                        } else {
                            lock.wait();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "OneRequest - send & recv; " + e.getMessage());
                e.printStackTrace();
            }

            Log.i(TAG, "OneRequest -- End.");
        }
    }

}
