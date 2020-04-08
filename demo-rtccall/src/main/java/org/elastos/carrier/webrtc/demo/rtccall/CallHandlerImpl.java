package org.elastos.carrier.webrtc.demo.rtccall;

import android.content.Intent;
import android.util.Log;

import org.elastos.carrier.webrtc.call.CallHandler;
import org.elastos.carrier.webrtc.call.CallReason;
import org.elastos.carrier.webrtc.demo.rtccall.ui.call.CallActivity;

public class CallHandlerImpl implements CallHandler {

    private static final String TAG = "CallHandlerImpl";

    @Override
    public void onInvite(String friendId) {
        Log.d(TAG, "onInvite: " + friendId);
//        ConnectActivity.INSTANCE.connectToRoom(friendId, false, false, 0, false);
        Intent intent = new Intent(MainActivity.INSTANCE, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_IS_CALLER, false);
        intent.putExtra(CallActivity.EXTRA_ROOMID, friendId);
        MainActivity.INSTANCE.startActivity(intent);

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
            if (CallActivity.INSTANCE != null) {
//                CallActivity.INSTANCE.disconnect();
                CallActivity.INSTANCE.onCallHangUp();
            }
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
