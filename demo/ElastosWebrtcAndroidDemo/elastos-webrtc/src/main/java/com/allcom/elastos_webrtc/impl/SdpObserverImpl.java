package com.allcom.elastos_webrtc.impl;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class SdpObserverImpl implements SdpObserver {

    private static final String TAG = "SdpObserverImpl";

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(TAG, String.format("onCreateSuccess: %s", sessionDescription.description));
    }

    @Override
    public void onSetSuccess() {
        Log.d(TAG, String.format("onSetSuccess: "));
    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(TAG, String.format("onCreateFailure: %s", s));
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(TAG, String.format("onSetFailure: %s", s));
    }
}
