package com.allcom.elastos_webrtc_android_demo.util;

import android.util.Log;

import com.allcom.elastos_webrtc.listener.CallHandler;
import com.allcom.elastos_webrtc.support.FailReason;
import com.allcom.elastos_webrtc.support.RejectReason;

public class CallHandlerImpl extends CallHandler {

    private static final String TAG = "CallHandlerImpl";

    @Override
    public void onReceiveInvite(String from) {
        Log.d(TAG, "onReceiveInvite: " + from);
    }

    @Override
    public void onAcceptInvite(String from) {
        Log.d(TAG, "onAcceptInvite: " + from);
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected: ");
    }

    @Override
    public void onFail(FailReason reason) {
        Log.d(TAG, "onFail: " + reason);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: ");
    }

    @Override
    public void onReject(RejectReason reason) {
        Log.d(TAG, "onReject: " + reason);
    }
}
