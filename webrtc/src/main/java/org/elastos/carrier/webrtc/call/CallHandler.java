package org.elastos.carrier.webrtc.call;

/**
 * call events
 */
public interface CallHandler {

    /**
     * fired when receive invite from your friends
     * @param friendId who is calling you
     */
    void onInvite(String friendId);

    /**
     * when your friend accept you invite
     */
    void onAnswer();

    /**
     * peer is connected
     */
    void onActive();

    /**
     * when remote hangup or reject invite
     * @param reason
     */
    void onEndCall(CallReason reason);

    /**
     * webrtc ice connected
     */
    void onIceConnected();

    /**
     * webrtc ice disconnected
     */
    void onIceDisConnected();

    /**
     * webrtc connect error
     * @param description
     */
    void onConnectionError(String description);

    /**
     * webrtc connection closed
     */
    void onConnectionClosed();

}
