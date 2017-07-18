package com.pw.ethan.lib.service;

import android.content.Context;
import android.util.Log;

import com.pw.ethan.lib.util.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Administrator
 */
public class Client {

    private final int STATE_OPEN = 1; // socket打开
    private final int STATE_CLOSE = 1 << 1; // socket关闭
    private final int STATE_CONNECT_START = 1 << 2; // 开始连接server
    private final int STATE_CONNECT_SUCCESS = 1 << 3; // 连接成功
    private final int STATE_CONNECT_FAILED = 1 << 4; // 连接失败
    private final int STATE_CONNECT_WAIT = 1 << 5; // 等待连接
    private final int READ_MAX = 50;
    private String respondStr; // 响应的消息
    private String IP_Cloud = ""; // 云服务器端的IP
    private int PORT_Cloud = 0; // 云服务器端的端口

    private int state = STATE_CONNECT_START;

    private Socket socket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private Thread conn = null;
    private Thread send = null;
    private Thread rec = null;

    private Context context;
    private ISocketResponse respListener;
    private LinkedBlockingQueue<Packet> requestQueen = new LinkedBlockingQueue<Packet>();
    private final Object lock = new Object();
    private final String TAG = "HealthCare";

    public int send(Packet in) {
        requestQueen.add(in);
        synchronized (lock) {
            lock.notifyAll();
        }
        return in.getId();
    }

    public void cancel(int reqId) {
        Iterator<Packet> mIterator = requestQueen.iterator();
        while (mIterator.hasNext()) {
            Packet packet = mIterator.next();
            if (packet.getId() == reqId) {
                mIterator.remove();
            }
        }
    }

    public Client(Context context, ISocketResponse respListener) {
        this.context = context;
        this.respListener = respListener;
    }

    public boolean isNeedConn() {
        return !((state == STATE_CONNECT_SUCCESS) && (null != send && send.isAlive()) && (null != rec && rec.isAlive()));
    }

    public void open(String host, int port) {
        this.IP_Cloud = host;
        this.PORT_Cloud = port;
        reconn();
    }

    private long lastConnTime = 0;

    public synchronized void reconn() {
        if (System.currentTimeMillis() - lastConnTime < 2000) {
            return;
        }
        lastConnTime = System.currentTimeMillis();

        close();
        state = STATE_OPEN;
        conn = new Thread(new Conn());
        conn.start();
    }

    public synchronized void close() {
        try {
            if (state != STATE_CLOSE) {
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

                state = STATE_CLOSE;
            }
            requestQueen.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Conn implements Runnable {
        public void run() {
            Log.i(TAG, "Conn -- Start");
            try {
                while (state != STATE_CLOSE) {
                    try {
                        state = STATE_CONNECT_START;
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(IP_Cloud, PORT_Cloud), 15 * 1000);
                        state = STATE_CONNECT_SUCCESS;
                    } catch (Exception e) {
                        Log.e(TAG, "Conn ; " + e.getMessage());
                        e.printStackTrace();
                        state = STATE_CONNECT_FAILED;
                    }

                    if (state == STATE_CONNECT_SUCCESS) {
                        try {
                            outStream = socket.getOutputStream();
                            inStream = socket.getInputStream();
                        } catch (IOException e) {
                            Log.e(TAG, "Conn ; " + e.getMessage());
                            e.printStackTrace();
                        }

                        send = new Thread(new Send());
                        rec = new Thread(new Rec());
                        send.start();
                        rec.start();
                        break;
                    } else {
                        state = STATE_CONNECT_WAIT;
                        //如果有网络没有连接上，则定时取连接，没有网络则直接退出
                        if (NetworkUtil.isNetworkAvailable(context)) {
                            try {
                                Thread.sleep(15 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Conn ; " + e.getMessage());
                e.printStackTrace();
            }

            Log.i(TAG, "Conn --- End");
        }
    }

    private class Send implements Runnable {
        public void run() {
            Log.i(TAG, "Send --- Start");
            try {
                while (state != STATE_CLOSE && state == STATE_CONNECT_SUCCESS && null != outStream) {
                    Packet item;
                    while (null != (item = requestQueen.poll())) {
                        outStream.write(item.getPacket());
                        outStream.flush();
                        item = null;
                    }
                    synchronized (lock) {
                        lock.wait();
                    }
                }
            } catch (SocketException e1) {
                Log.e(TAG, "Send ; " + e1.getMessage());
                e1.printStackTrace();
                reconn();
            } catch (Exception e) {
                Log.e(TAG, "Send ; " + e.getMessage());
                e.printStackTrace();
            }

            Log.i(TAG, "Send --- End");
        }
    }

    private class Rec implements Runnable {
        public void run() {
            Log.i(TAG, "Rec --- Start");
            try {
                while (state != STATE_CLOSE && state == STATE_CONNECT_SUCCESS && null != inStream) {
//							byte[] bodyBytes=new byte[5];
//							int offset=0;
//							int length=LEN;
//							int read=0;

                    int readIndex = READ_MAX;
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(inStream), readIndex);
                    char[] charArray = new char[readIndex];
                    int read_rst = bufferedReader.read(charArray);
                    respondStr = new String(charArray, 0, read_rst);
                    bufferedReader.close();
                    respListener.onSocketResponse(respondStr);

//							while((read=inStream.read(bodyBytes, offset, length))>0)
//							{
//								System.out.println("+++++++++++++++++=");
//								System.out.println(read);
//								System.out.println("+++++++++++++++++=");
//
//
//								if(length-read==0)
//								{
//									if(null!=respListener)
//									{
//										respListener.onSocketResponse(new String(bodyBytes));
//									}
//
//									offset=0;
//									length=LEN;
//									read=0;
//									continue;
//								}
//								offset += read;
//								length=LEN-offset;
//							}

                    reconn();//走到这一步，说明服务器socket断了
                    break;
                }
            } catch (SocketException e1) {
                Log.e(TAG, "Send ; " + e1.getMessage());
                e1.printStackTrace();
            } catch (Exception e2) {
                Log.e(TAG, "Send ; " + e2.getMessage());
                e2.printStackTrace();
            }

            Log.i(TAG, "Rec --- End");
        }
    }
}
