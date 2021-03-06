package com.hunterdavis.autorobointercom.network;

import java.io.IOException;
import android.os.Looper;

/**
 * Created by hunter on 3/3/14.
 */
public class NetworkAnnounceThread extends Thread{
    private Object mPauseLock;
    private boolean mPaused;
    private boolean mFinished;

    // sleep for 5 minutes
    private long mthreadSleepTime =  1000 * 60 * 5;

    public NetworkAnnounceThread() {
        mPauseLock = new Object();
        mPaused = false;
        mFinished = false;
    }

    public void setFinished() {
        mFinished = true;
    }

    @Override
    public void run() {
        Looper.prepare();
        while (!mFinished) {
            try {
                NetworkTransmissionUtilities.sendTextToAllClients("");
                Thread.sleep(mthreadSleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
}
