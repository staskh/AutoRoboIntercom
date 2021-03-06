package com.hunterdavis.autorobointercom.network;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.hunterdavis.autorobointercom.util.AutoRoboApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.Arrays;

/**
 * Created by hunter on 3/3/14.
 */
public class NetworkReceiverThread  extends Thread {
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;

    // our multicast lock
    private WifiManager.MulticastLock multicastLock;

    public NetworkReceiverThread() {
        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
    }

    @Override
    public void run() {
        while (!mFinished) {
            try {
                receive();
            } catch (IOException e) {
                mFinished = true;
                e.printStackTrace();
            }
            synchronized (mPauseLock) {
                while (mPaused) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /**
     * Call this on pause.
     */
    public void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    public void setFinished() {
        mFinished = true;
    }

    public void receive() throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Acquire multicast lock
        WifiManager wifi = (WifiManager) AutoRoboApplication.getContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("multicastLock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        MulticastSocket socket = new MulticastSocket(NetworkConstants.DEFAULT_PORT);

        InetAddress group = InetAddress.getByName(NetworkConstants.DEFAULT_GROUP);
        socket.joinGroup(group);

        DatagramPacket packet;

        String received= "";
        while(received!=null && !mFinished && !mPaused)
        {
            byte[] buf = new byte[NetworkConstants.DEFAULT_DATAGRAM_SIZE];
            byte val = '\n';
            Arrays.fill(buf,val);

            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            received = new String(packet.getData()).split("\n")[0];

            if(TextUtils.isEmpty(received)) {
                received = "";
            }

            String hostsName = received.split(NetworkConstants.BROADCAST_EXTRA_SPECIAL_CHARACTER_DELIMINATOR)[0];
            String ourHostName = AutoRoboApplication.getName();
            String hostAddress = packet.getAddress().getHostAddress();


            Log.d("hunter", "host name is " + hostsName + ", and our name is: " + ourHostName);

            if(!hostsName.equalsIgnoreCase(ourHostName)) {

                String receivedPlusNetworkInfo = received + NetworkConstants.BROADCAST_EXTRA_SPECIAL_CHARACTER_DELIMINATOR + hostAddress;
                processMessage(receivedPlusNetworkInfo);
            }

        }

        socket.leaveGroup(group);
        socket.close();

        // Once your finish using it, release multicast lock
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
    }

    private void processMessage(String message) {
        Log.d(NetworkReceiverThread.class.getName(),"received: "+message);
        Intent in=new Intent(NetworkConstants.BROADCAST_ACTION);

        // add in our message
        in.putExtra(NetworkConstants.BROADCAST_EXTRA_STRING_UDP_MESSAGE,message);

        Log.d(NetworkReceiverThread.class.getName(),"sending broadcast");
        LocalBroadcastManager.getInstance(AutoRoboApplication.getContext()).sendBroadcast(in);
    }

}
