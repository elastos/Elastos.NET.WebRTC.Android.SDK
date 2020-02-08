package com.allcom.elastos_webrtc.impl;

import android.util.Log;

import com.allcom.elastos_webrtc.ElastosWebrtc;
import com.allcom.elastos_webrtc.listener.CallHandler;
import com.allcom.elastos_webrtc.support.FailReason;
import com.allcom.elastos_webrtc.util.CandidateKey;
import com.allcom.elastos_webrtc.util.SignalMessageType;

import org.elastos.carrier.FriendInviteResponseHandler;
import org.json.JSONObject;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class PeerObserverImpl implements PeerConnection.Observer, SdpObserver {

    private static final String TAG = "PeerObserverImpl";

    private String to;

    private CallHandler callHandler;

    public PeerObserverImpl(String to, CallHandler handler) {
        this.to = to;
        this.callHandler = handler;
    }

    @Override
    public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.d(TAG, String.format("onStandardizedIceConnectionChange: %s", newState.name()));
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
        Log.d(TAG, String.format("onConnectionChange: %s", newState.name()));
    }

    @Override
    public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {
        Log.d(TAG, String.format("onSelectedCandidatePairChanged: %s", event.reason));
    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
        Log.d(TAG, String.format("onTrack: %s", transceiver.getMid()));
        ElastosWebrtc.getInstance().getRemoteVideoTrack();
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, String.format("onSignalingChange: %s", signalingState.name()));
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, String.format("onIceConnectionChange: %s", iceConnectionState.name()));
        switch (iceConnectionState) {
            case FAILED:
                callHandler.onFail(FailReason.CONNECT_FAIL);
                break;
            case CONNECTED:
                callHandler.onConnected();
                break;
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, "onIceConnectionReceivingChange: ");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, String.format("onIceGatheringChange: %s", iceGatheringState.name()));
        switch (iceGatheringState) {
            case NEW:
                Log.d(TAG, "onIceGatheringChange: new ice candidate");
                break;
            case GATHERING:
                Log.d(TAG, "onIceGatheringChange: gathering ice candidate");
                break;
            case COMPLETE:
                Log.d(TAG, "onIceGatheringChange: complete ice candidate");
                break;
            default:
                break;
        }
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, String.format("onIceCandidate: %s", iceCandidate.toString()));
        try {
            JSONObject json = new JSONObject();
            json.put(CandidateKey.sdpMLineIndex.name(), iceCandidate.sdpMLineIndex);
            json.put(CandidateKey.sdpMid.name(), iceCandidate.sdpMid);
            json.put(CandidateKey.sdp.name(), iceCandidate.sdp);
            ElastosWebrtc.getInstance().sendMessage(to, SignalMessageType.CANDIDATE, null, json.toString(), new FriendInviteResponseHandler() {
                @Override
                public void onReceived(String from, int status, String reason, String data) {
                    Log.d(TAG, String.format("onReceived.onIceCandidate: %s, %d, %s, %s", from, status, reason, data));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "onIceCandidate: ", e);
        }
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG, "onIceCandidatesRemoved: ");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream: ");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream: ");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel: ");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded: ");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack: ");
    }


    /*SdpObserver start*/
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {

    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String s) {

    }

    @Override
    public void onSetFailure(String s) {

    }
}
