package org.elastos.carrier.webrtc.demo_apprtc;

import android.util.Log;

import org.elastos.carrier.webrtc.WebrtcClient;
import org.elastos.carrier.webrtc.call.CallHandler;
import org.elastos.carrier.webrtc.call.CallReason;

public class CallHandlerImpl implements CallHandler {

    private static final String TAG = "CallHandlerImpl";

    @Override
    public void onInvite(String friendId) {
        Log.d(TAG, "onInvite: " + friendId);
        try {
            Thread.sleep(1000);
            WebrtcClient.getInstance().answerCall();
        } catch (Exception e) {
            Log.e(TAG, "onInvite: ", e);
        }
        ConnectActivity.INSTANCE.startCallActivity(friendId);
    }

    @Override
    public void onAnswer() {
        Log.d(TAG, "onAnswer: ");
    }

    @Override
    public void onActive() {
        Log.d(TAG, "onActive: ");
    }

    @Override
    public void onEndCall(CallReason reason) {
        Log.d(TAG, "onEndCall: " + reason);
        try {
            if (CallActivity.INSTANCE != null)
                CallActivity.INSTANCE.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "onEndCall: ", e);
        }
    }

    @Override
    public void onIceConnected() {
        Log.d(TAG, "onIceConnected: ");
    }

    @Override
    public void onIceDisConnected() {
        Log.d(TAG, "onIceDisConnected: ");
    }

    @Override
    public void onConnectionError(String description) {
        Log.d(TAG, "onConnectionError: " + description);
    }

    @Override
    public void onConnectionClosed() {
        Log.d(TAG, "onConnectionClosed: ");
    }
}
