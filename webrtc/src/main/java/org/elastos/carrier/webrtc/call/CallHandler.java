package org.elastos.carrier.webrtc.call;

import java.nio.ByteBuffer;

/**
 * call events
 */
public interface CallHandler {

    /**
     * fired when receive invite from your friends
     *
     * @param friendId who is calling you
     * @param audio is audio enabled
     * @param video is video enabled
     */
    void onInvite(String friendId, boolean audio, boolean video);

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
     *
     * @param reason {@link CallReason} the reason call ended
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
     *
     * @param description error message
     */
    void onConnectionError(String description);

    /**
     * webrtc connection closed
     */
    void onConnectionClosed();

    /**
     * webrtc message received
     * @param buffer message data
     * @param binary whether binary data or not
     */
    void onMessage(ByteBuffer buffer, boolean binary);

}
