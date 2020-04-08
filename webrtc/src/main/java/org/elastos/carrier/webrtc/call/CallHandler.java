package org.elastos.carrier.webrtc.call;

public interface CallHandler {

    void onInvite(String friendId);

    void onAnswer();

    void onActive();

    void onEndCall(CallReason reason);

    void onIceConnected();

    void onIceDisConnected();

    void onConnectionError(String description);

    void onConnectionClosed();

}
