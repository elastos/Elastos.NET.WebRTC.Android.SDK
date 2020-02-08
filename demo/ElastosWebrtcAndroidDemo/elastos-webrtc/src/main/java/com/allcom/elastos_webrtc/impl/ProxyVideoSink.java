package com.allcom.elastos_webrtc.impl;

import android.util.Log;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class ProxyVideoSink implements VideoSink {

    private static final String TAG = "";

    private VideoSink target;

    @Override
    public void onFrame(VideoFrame videoFrame) {
        if (target == null) {
            Log.d(TAG, "onFrame: target is null, dropping frame");
            return;
        }

        target.onFrame(videoFrame);
    }

    synchronized public void setTarget(VideoSink target) {
        this.target = target;
    }


}
