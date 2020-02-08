package com.allcom.elastos_webrtc.listener;

import com.allcom.elastos_webrtc.support.FailReason;
import com.allcom.elastos_webrtc.support.RejectReason;

/**
 * <p> callback for video call.
 * implements this class to listen callback
 */
public abstract class CallHandler {

    public abstract void onReceiveInvite(String from);

    public abstract void onAcceptInvite(String from);

    public abstract void onConnected();

    public abstract void onFail(FailReason reason);

    public abstract void onDisconnected();

    public abstract void onReject(RejectReason reason);
}
