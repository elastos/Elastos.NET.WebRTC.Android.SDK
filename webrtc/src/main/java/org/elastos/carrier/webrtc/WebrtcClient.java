/*
 * Copyright (c) 2018 Elastos Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.elastos.carrier.webrtc;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierExtension;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.exceptions.CarrierException;
import org.elastos.carrier.webrtc.call.CallHandler;
import org.elastos.carrier.webrtc.call.CallReason;
import org.elastos.carrier.webrtc.call.CallState;
import org.elastos.carrier.webrtc.exception.WebrtcException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSink;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Initial the Carrier Webrtc Client instance for webrtc call using carrier network.
 * <p>To use: create an instance of this object (registering a message handler) and
 * then invite the peer to join the call (inviteCall), if the peer accept (acceptCallInvite()), then
 * then peers can prepare the peerConnection by call initialCall(), after that,
 * the peers can exchange offer/answer.
 * <p>
 * Once the call is invited, the onCallInvited() callback with webrtc parameters is invoked,
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after Carrier connection is established.
 */
public class WebrtcClient extends CarrierExtension implements PeerConnectionEvents {
    private static final String TAG = "WebrtcClient";

    private static WebrtcClient INSTANCE;
    private final Handler handler;
    private boolean initiator;
    private ConnectionState connectionState;
    private String remoteUserId;
    private String currentUserId;
    private Carrier carrier;
    private FriendInviteResponseHandler friendInviteResponseHandler;
    private CallHandler callHandler;
    private CallState callState;
    private Context context;
    private EglBase eglBase;
    private CarrierPeerConnectionClient carrierPeerConnectionClient;
    private CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private SignalingParameters signalingParameters;
    private ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    private ProxyVideoSink remoteProxyVideoSink = new ProxyVideoSink();
    private List<VideoSink> remoteSinks = Arrays.asList(new ProxyVideoSink[]{remoteProxyVideoSink});
    private SurfaceViewRenderer localVideoRenderer;
    private SurfaceViewRenderer remoteVideoRenderer;
    private SessionDescription remoteSdp;

    private WebrtcClient(Context context,
                         Carrier carrier,
                         CallHandler callHandler,
                         CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters)
            throws WebrtcException {
        super(carrier);
        try {
            registerExtension();
            this.currentUserId = carrier.getUserId();
        } catch (CarrierException e) {
            throw new WebrtcException("Carrier extension registered error.");
        }
        this.carrier = carrier;
        this.context = context;
        this.callHandler = callHandler;
        this.friendInviteResponseHandler = new CarrierMessageObserver();
        this.callState = CallState.INIT;
        if (peerConnectionParameters != null) {
            this.peerConnectionParameters = peerConnectionParameters;
        } else {
            this.peerConnectionParameters = PeerConnectionParametersBuilder.builder().build();
        }
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
    }

    public static WebrtcClient createInstance(@NonNull Context context,
                                              @NonNull Carrier carrier,
                                              @Nullable CallHandler callHandler,
                                              @Nullable CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters) {
        synchronized (WebrtcClient.class) {
            try {
                if (INSTANCE == null) {
                    INSTANCE = new WebrtcClient(context, carrier, callHandler, peerConnectionParameters);
                    Log.d(TAG, "initialize: success");
                }
            } catch (WebrtcException e) {
                Log.e(TAG, "Create webrtc client error " + e.getMessage());
            }
        }
        return INSTANCE;
    }

    public static WebrtcClient getInstance() {
        return INSTANCE;
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make call to remote peer. And remote peer could choose to accept this
     * call inviation, or hangup (reject) the calll invatation.
     *
     * @param peerAddress The remote peer to which the call is going to make with.
     */
    public void makeCall(String peerAddress) throws WebrtcException {
        if (peerAddress == null) {
            throw new IllegalArgumentException("Invalid remote address");
        }
        if (peerAddress.equals(currentUserId)) {
            throw new IllegalArgumentException("PeerAddress is current node address");
        }

        this.initiator = true;
        this.remoteUserId = peerAddress;

        // make call, just send offer to remote peer
        this.setCallState(CallState.CONNECTING);
        initialCall();
        // create offer
        carrierPeerConnectionClient.createOffer();
    }

    /**
     * Answer the call.
     */
    public void answerCall() {
        this.initiator = false;
        this.setCallState(CallState.CONNECTING);

        initialCall();
        // set remote sdp
        carrierPeerConnectionClient.setRemoteDescription(remoteSdp);
        // create answer
        carrierPeerConnectionClient.createAnswer();
    }

    /**
     * Hangup/reject the call invitation.
     */
    public void rejectCall() throws WebrtcException {
        sendBye(CallReason.REJECT);
    }

    public CallState getCallState() {
        return this.callState;
    }

    private void setCallState(CallState callState) {
        this.callState = callState;
    }

    /**
     * Get remote peer address
     *
     * @return {@link String} remote peer address.
     */
    public String getPeerAddress() {
        return this.remoteUserId;
    }

    public void hangupCall() {
        this.setCallState(CallState.INIT);

        sendBye(CallReason.NORMAL_HANGUP);

        disconnectFromCallInternal();
        handler.getLooper().quit();
        Log.d(TAG, "Disconnect the call with" + remoteUserId);
    }

    public void renderVideo(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        if (localRenderer == null || remoteRenderer == null)
            throw new IllegalArgumentException("Invalid video render");

        this.localVideoRenderer = localRenderer;
        this.remoteVideoRenderer = remoteRenderer;
        if (this.eglBase == null) {
            this.eglBase = EglBase.create();
        }
        this.localVideoRenderer.init(eglBase.getEglBaseContext(), null);
        this.remoteVideoRenderer.init(eglBase.getEglBaseContext(), null);
        swapVideoRenderer(false);
    }

    public void swapVideoRenderer(boolean isSwap) {
        localProxyVideoSink.setTarget(isSwap ? remoteVideoRenderer : localVideoRenderer);
        remoteProxyVideoSink.setTarget(isSwap ? localVideoRenderer : remoteVideoRenderer);
        remoteVideoRenderer.setMirror(isSwap);
        localVideoRenderer.setMirror(!isSwap);
    }

    public void switchCamera() {
        if (carrierPeerConnectionClient != null)
            carrierPeerConnectionClient.switchCamera();
    }

    public void setResolution(int width, int height, int fps) {
        if (carrierPeerConnectionClient != null)
            carrierPeerConnectionClient.changeCaptureFormat(width, height, fps);
    }

    public void setAudioEnable(boolean enable) {
        if (carrierPeerConnectionClient != null)
            carrierPeerConnectionClient.setAudioEnabled(enable);
    }

    public void setVideoEnable(boolean enable) {
        if (carrierPeerConnectionClient != null)
            carrierPeerConnectionClient.setVideoEnabled(enable);
    }

    /**
     * send data
     * @param byteBuffer message content
     * @param binary binary file or plain text
     */
    public void sendMessage(ByteBuffer byteBuffer, boolean binary) throws WebrtcException {
        if (byteBuffer == null) {
            throw new IllegalArgumentException("byteBuffer can not be null");
        }
        if (carrierPeerConnectionClient == null) {
            throw new IllegalArgumentException("carrierPeerConnectionClient is null");
        }
        if (carrierPeerConnectionClient != null) {
            carrierPeerConnectionClient.sendMessage(new DataChannel.Buffer(byteBuffer, binary));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * override CarrierExtension.onFriendInvite
     *
     * @param carrier
     * @param from
     * @param data
     */
    @Override
    protected void onFriendInvite(Carrier carrier, String from, String data) {
        Log.e(TAG, "carrier friend invite onFriendInviteRequest from: " + from);
        Log.e(TAG, "carrier friend invite onFriendInviteRequest data: " + data);
        try {
            replyFriendInvite(from, 0, "", "");
        } catch (Exception e) {
            Log.e(TAG, "onFriendInvite: ", e);
        }
        if (data != null && (data.contains("msg") ||  data.contains("type"))) {
            this.remoteUserId = from;
            onCarrierMessage(data, from);
            Log.d(TAG, "Get the carrier message: " + data);
        }
    }

    // accept the call invite and then send the offer.
    private void initialCall() {
        Log.d(TAG, "Connect to carrier user: " + remoteUserId);
        connectionState = ConnectionState.NEW;
        List<PeerConnection.IceServer> iceServers = getIceServers();
        signalingParameters = new SignalingParameters(
                iceServers, initiator, remoteUserId, null, null);
        Log.d(TAG, "initialCall() from " + (initiator ? "caller" : "callee") + ": " + currentUserId + ", to: " + remoteUserId);
        if (!signalingParameters.initiator
                && signalingParameters.offerSdp == null) {
            Log.w(TAG, "No offer SDP from the caller.");
        }
        connectionState = ConnectionState.CONNECTED;
        // init PeerConnection
        initialWebrtc();
        // Fire connection and signaling parameters events.
        initialCallInternal();
    }

    private List<PeerConnection.IceServer> getIceServers() {
        TurnServerInfo turnServerInfo = null;
        try {
            turnServerInfo = getTurnServerInfo();
        } catch (CarrierException e) {
            Log.e(TAG, "Get Turn server from carrier network error.");
        }
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        if (turnServerInfo != null) {
            iceServers.add(PeerConnection.IceServer.builder("stun:" + turnServerInfo.getServer() + ":" + turnServerInfo.getPort()).setUsername(turnServerInfo.getUsername()).setPassword(turnServerInfo.getPassword()).createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("turn:" + turnServerInfo.getServer() + ":" + turnServerInfo.getPort()).setUsername(turnServerInfo.getUsername()).setPassword(turnServerInfo.getPassword()).createIceServer());
        }
        return iceServers;
    }

    // Disconnect from call and send bye messages - runs on a local looper thread.
    private void disconnectFromCallInternal() {
        Log.d(TAG, "Disconnect. Connection state: " + connectionState);
        if (connectionState == ConnectionState.CONNECTED) {
            Log.d(TAG, "Closing call.");
        }
        this.setCallState(CallState.INIT);
        connectionState = ConnectionState.CLOSED;
        release();
    }

    private void release() {
        try {
            remoteProxyVideoSink.setTarget(null);
            localProxyVideoSink.setTarget(null);
            if (localVideoRenderer != null) {
                localVideoRenderer.release();
                localVideoRenderer = null;
            }
            if (remoteVideoRenderer != null) {
                remoteVideoRenderer.release();
                remoteVideoRenderer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "release video renderer error: ", e);
        }
        try {
            if (carrierPeerConnectionClient != null) {
                carrierPeerConnectionClient.close();
                carrierPeerConnectionClient = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "release carrier peer connection error: ", e);
        }
        try {
            if (eglBase != null) {
                eglBase.release();
                eglBase = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "release: Eglbase error", e);
        }
    }

    // Send local offer SDP to the other participant.
    private void sendOfferSdp(final SessionDescription sdp) {
        if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.NEW) {
            Log.e(TAG, "sendOfferSdp: Sending offer SDP in non connected state.");
            return;
        }

        sendOffer(sdp, true, peerConnectionParameters.videoCallEnabled, peerConnectionParameters.dataChannelParameters != null);
        Log.d(TAG, "sendOfferSdp() from " + currentUserId + ", to: " + remoteUserId);
    }

    // Send local answer SDP to the other participant.
    private void sendAnswerSdp(final SessionDescription sdp) {
        sendAnswer(sdp);
        Log.d(TAG, "sendAnswerSdp() from " + currentUserId + ", to: " + remoteUserId);
    }

    // Send Ice candidate to the other participant.
    private void sendLocalIceCandidate(final IceCandidate candidate) {
        if (initiator) {
            // Call initiator sends ice candidates to peer.
            if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.NEW) {
                Log.e(TAG, "sendLocalIceCandidate: Sending ICE candidate in non connected state.");
                return;
            }
            sendCandidate(candidate);
            Log.d(TAG, "sendLocalIceCandidate() from " + currentUserId + ", to: " + remoteUserId);
        } else {
            // Call receiver sends ice candidates to peer.
            sendCandidate(candidate);
        }
    }

    // Send removed Ice candidates to the other participant.
    private void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        if (initiator) {
            // Call initiator sends ice candidates to peer.
            if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.NEW) {
                Log.e(TAG, "sendLocalIceCandidateRemovals: Sending ICE candidate removals in non connected state.");
                return;
            }
            sendRemovalCandidates(candidates);
        } else {
            // Call receiver sends ice candidates to peer.
            sendRemovalCandidates(candidates);
        }
        Log.d(TAG, "sendLocalIceCandidateRemovals() from " + currentUserId + ", to: " + remoteUserId);
    }

    private void initialWebrtc() {
        if (eglBase == null) {
            eglBase = EglBase.create();
        }
        // Create peer connection client.
        carrierPeerConnectionClient = new CarrierPeerConnectionClient(
                context, getIceServers(), eglBase, peerConnectionParameters, this, callHandler);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        carrierPeerConnectionClient.createPeerConnectionFactory(options);
    }

    private void handleOffer(JSONObject json) {
        Log.d(TAG, "handleOffer: ");
        if (json == null) {
            Log.e(TAG, "handleOffer: error, json message is null");
            return;
        }
        String sdpStr = json.optString(MessageKey.sdp.name(), "");
        SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm("offer"), sdpStr
        );
        // save remote sdp
        remoteSdp = sdp;
        JSONArray options = json.optJSONArray(MessageKey.options.name());
        boolean audio = false;
        boolean video = false;
        if (options != null && options.length() > 0) {
            for (int i = 0; i < options.length(); i++) {
                String o = options.optString(i, "");
                if ("audio".equalsIgnoreCase(o)) {
                    audio = true;
                }
                if ("video".equalsIgnoreCase(o)) {
                    video = true;
                }
            }
        }
        this.setCallState(CallState.RINGING);
        signalingParameters = new SignalingParameters(getIceServers(), false, remoteUserId, null, null);
        // emit user callback
        callHandler.onInvite(remoteUserId, audio, video);
    }

    private void handleAnswer(JSONObject json) {
        Log.d(TAG, "handleAnswer: ");
        if (json == null) {
            Log.e(TAG, "handleAnswer: error, json message is null");
            return;
        }
        String sdpStr = json.optString(MessageKey.sdp.name(), "");
        SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm("answer"), sdpStr
        );
        remoteSdp = sdp;
        // set remote sdp
        carrierPeerConnectionClient.setRemoteDescription(sdp);
        callHandler.onAnswer();
    }

    private void handleCandidate(JSONObject json) {
        if (json == null) {
            Log.e(TAG, "handleCandidate: handle candidate error, json message is null");
            return;
        }

        try {
            // handle candidate from json message
            JSONArray array = json.optJSONArray(MessageKey.candidates.name());
            if (array != null && array.length() > 0) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject cJson = array.optJSONObject(i);
                    carrierPeerConnectionClient.addRemoteIceCandidate(toJavaCandidate(cJson));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "handleCandidate: ", e);
        }
    }

    private void handleCandidateRemoval(JSONObject json) {
        if (json == null) {
            Log.e(TAG, "handleCandidateRemoval: handle candidate-removal error, json message is null");
            return;
        }

        try {
            // handle candidate removal from json message
            JSONArray array = json.optJSONArray(MessageKey.candidates.name());
            if (array != null && array.length() > 0) {
                IceCandidate[] candidates = new IceCandidate[array.length()];
                for (int i = 0; i < array.length(); i++) {
                    JSONObject cJson = array.optJSONObject(i);
                    candidates[i] = toJavaCandidate(cJson);
                }
                carrierPeerConnectionClient.removeRemoteIceCandidates(candidates);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleCandidate: ", e);
        }
    }

    private void handleBye(JSONObject json) {
        CallReason callReason = null;
        if (json != null) {
            int reason = json.optInt(MessageKey.reason.name());
            callReason = CallReason.valueOf(reason);
            Log.d(TAG, "handleBye: reason -> " + callReason);
        }
        if (callReason == null) {
            callReason = CallReason.NORMAL_HANGUP;
        }
        Log.d(TAG, "handleBye: " + callReason);
        this.setCallState(CallState.INIT);
        disconnectFromCallInternal();
        callHandler.onEndCall(callReason);
    }

    private void handleReject() {
        Log.d(TAG, "handleReject: ");
        this.setCallState(CallState.INIT);
        callHandler.onEndCall(CallReason.REJECT);
    }

    // --------------------------------------------------------------------
    // CarrierChannelEvents interface implementation.
    // All events are called by CarrierChannelClient on a local looper thread
    // (passed to Carrier client constructor).
    private void onCarrierMessage(final String msg, String from) {
        try {
            Log.d(TAG, "onCarrierMessage: " + msg);
            JSONObject json = new JSONObject(msg);
            String type = json.optString(MessageKey.type.name(), "");
            MessageType messageType = MessageType.getType(type);
            if (messageType == null) {
                Log.e(TAG, "onCarrierMessage: handle call message error, type is null -> " + msg);
                return;
            }
            Log.d(TAG, "onCarrierMessage: handle message -> " + messageType);
            switch (messageType) {
                case OFFER:
                    handleOffer(json);
                    break;
                case ANSWER:
                    handleAnswer(json);
                    break;
                case CANDIDATE:
                    handleCandidate(json);
                    break;
                case REMOVAL_CANDIDATES:
                    handleCandidateRemoval(json);
                    break;
                case BYE:
                    handleBye(json);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "onCarrierMessage: error -> " + msg);
        }
    }

    private void initialCallInternal() {
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        carrierPeerConnectionClient.createPeerConnection(context,
                localProxyVideoSink, remoteSinks, videoCapturer);
        if (signalingParameters.offerSdp != null) {
            carrierPeerConnectionClient.setRemoteDescription(signalingParameters.offerSdp);
            Log.d(TAG, "initialCallInternal: creating answer");
            // Create answer. Answer SDP will be sent to offering client in
            // PeerConnectionEvents.onLocalDescription event.
            carrierPeerConnectionClient.createAnswer();
        }
        if (signalingParameters.iceCandidates != null) {
            // Add remote ICE candidates from room.
            for (IceCandidate iceCandidate : signalingParameters.iceCandidates) {
                carrierPeerConnectionClient.addRemoteIceCandidate(iceCandidate);
            }
        }
    }

    private @Nullable
    VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;
        if (useCamera2()) {
            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(false));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(context);
    }

    private @Nullable
    VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        return null;
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        if (connectionState != ConnectionState.ERROR) {
            connectionState = ConnectionState.ERROR;
            // events.onChannelError(errorMessage);
            callHandler.onConnectionError(errorMessage);
        }
    }

    // Converts a Java candidate to a JSONObject.
    private JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);
        jsonPut(json, "sdpMid", candidate.sdpMid);
        jsonPut(json, "sdp", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    private IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(
                json.getString("sdpMid"), json.getInt("sdpMLineIndex"), json.getString("sdp"));
    }

    // Helper method for debugging purposes. Ensures that Carrier method is
    // called on a looper thread.
    private void checkIfCalledOnValidThread() {
        if (Thread.currentThread() != handler.getLooper().getThread()) {
            throw new IllegalStateException("Carrier method is not called on valid thread");
        }
    }

    private void send(String message) {
        try {
            String data = new JSONObject()
                    .put("cmd", "send")
                    .put("msg", message)
                    .put("remoteUserId", remoteUserId)
                    .toString();
            // no wrap
            Log.d(TAG, "Calling control command : " + message);
            inviteFriend(remoteUserId, message, friendInviteResponseHandler);

        } catch (JSONException e) {
            Log.e(TAG, "send: Carrier send JSON error: " + e.getMessage());
        } catch (CarrierException e) {
            e.printStackTrace();
            Log.e(TAG, "send: carrier send message error: " + e.getMessage());
        }
    }

    /**
     * send offer sdp to remote peer
     * @param sdp offer sdp
     * @param audio enable audio
     * @param video enable video
     */
    private void sendOffer(SessionDescription sdp, boolean audio, boolean video, boolean data) {
        if (sdp == null) {
            Log.e(TAG, "send offer error, offer sdp is null");
            return;
        }
        JSONObject json = new JSONObject();
        // offer type
        jsonPut(json, MessageKey.type.name(), MessageType.OFFER.getValue());
        // offer sdp
        jsonPut(json, MessageKey.sdp.name(), sdp.description);
        // offer options
        JSONArray options = new JSONArray();
        if (audio) {
            options.put("audio");
        }
        if (video) {
            options.put("video");
        }
        if (data) {
            options.put("data");
        }
        jsonPut(json, MessageKey.options.name(), options);

        // send json message
        send(json.toString());
    }

    /**
     * send answer sdp to remote peer
     * @param sdp answer sdp
     */
    private void sendAnswer(SessionDescription sdp) {
        if (sdp == null) {
            Log.e(TAG, "send answer error, answer sdp is null");
            return;
        }
        JSONObject json = new JSONObject();
        // answer type
        jsonPut(json, MessageKey.type.name(), MessageType.ANSWER.getValue());
        // answer sdp
        jsonPut(json, MessageKey.sdp.name(), sdp.description);

        // send json message
        send(json.toString());
    }

    /**
     * send candidate to remote peer
     * @param candidate ice candidate
     */
    private void sendCandidate(IceCandidate candidate) {
        if (candidate == null) {
            Log.e(TAG, "send IceCandidate error, candidate is null");
            return;
        }
        JSONObject json = new JSONObject();
        // candidate type
        jsonPut(json, MessageKey.type.name(), MessageType.CANDIDATE.getValue());
        // candidate array
        JSONArray array = new JSONArray();
        array.put(toJsonCandidate(candidate));
        jsonPut(json, MessageKey.candidates.name(), array);

        // send json message
        send(json.toString());
    }

    /**
     * send candidates to remote peer
     * @param candidates ice candidates
     */
    private void sendCandidates(IceCandidate[] candidates) {
        if (candidates == null || candidates.length <= 0) {
            Log.e(TAG, "send IceCandidates error, candidates is empty");
            return;
        }
        JSONObject json = new JSONObject();
        // candidate type
        jsonPut(json, MessageKey.type.name(), MessageType.CANDIDATE.getValue());
        // candidate array
        JSONArray array = new JSONArray();
        for (IceCandidate candidate : candidates) {
            array.put(toJsonCandidate(candidate));
        }
        jsonPut(json, MessageKey.candidates.name(), array);

        // send json message
        send(json.toString());
    }

    /**
     * send ice candidates removal message to remote peer
     * @param candidates removal ice candidates
     */
    private void sendRemovalCandidates(IceCandidate[] candidates) {
        if (candidates == null || candidates.length <= 0) {
            Log.e(TAG, "send removal IceCandidates error, candidates is empty");
            return;
        }

        JSONObject json = new JSONObject();
        // candidate type
        jsonPut(json, MessageKey.type.name(), MessageType.REMOVAL_CANDIDATES.getValue());
        // candidate array
        JSONArray array = new JSONArray();
        for (IceCandidate candidate : candidates) {
            array.put(toJsonCandidate(candidate));
        }
        jsonPut(json, MessageKey.candidates.name(), array);

        // send json message
        send(json.toString());
    }

    /**
     * send bye message to remote peer
     * @param reason bye reason
     */
    private void sendBye(CallReason reason) {
        JSONObject json = new JSONObject();
        jsonPut(json, MessageKey.type.name(), MessageType.BYE.getValue());
        jsonPut(json, MessageKey.reason.name(), reason.getValue());

        send(json.toString());
    }

    /**
     * start implements PeerConnectionEvents
     */
    @Override
    public void onLocalDescription(SessionDescription sdp) {
        if (initiator) {
            sendOfferSdp(sdp);
        } else {
            sendAnswerSdp(sdp);
        }
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        sendLocalIceCandidate(candidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        sendLocalIceCandidateRemovals(candidates);
    }

    @Override
    public void onIceConnected() {
        callHandler.onIceConnected();
    }

    @Override
    public void onIceDisconnected() {
        callHandler.onIceDisConnected();
    }

    @Override
    public void onConnected() {
        this.setCallState(CallState.ACTIVE);
        callHandler.onActive();
    }

    @Override
    public void onDisconnected() {
        callHandler.onEndCall(CallReason.NORMAL_HANGUP);
    }

    @Override
    public void onPeerConnectionClosed() {
        callHandler.onConnectionClosed();
    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
    }

    @Override
    public void onPeerConnectionError(String description) {
        callHandler.onConnectionError(description);
    }

    private enum ConnectionState {NEW, CONNECTED, CLOSED, ERROR}

    /**
     * call message keys
     */
    private enum MessageKey {type, sdp, candidates, options, reason}

    /**
     * call message types
     */
    private enum MessageType {
        OFFER("offer"),
        ANSWER("answer"),
        CANDIDATE("candidate"),
        REMOVAL_CANDIDATES("removal-candidates"),
        BYE("bye"),
        ;

        private String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MessageType getType(String value) {
            for (MessageType type : values()) {
                if (type.getValue().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Struct holding the signaling parameters of an webrtc communication.
     */
    private static class SignalingParameters {
        final List<PeerConnection.IceServer> iceServers;
        final String remoteUserId;
        final SessionDescription offerSdp;
        final List<IceCandidate> iceCandidates;
        boolean initiator;

        SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                            String remoteUserId, SessionDescription offerSdp,
                            List<IceCandidate> iceCandidates) {
            this.iceServers = iceServers;
            this.initiator = initiator;
            this.remoteUserId = remoteUserId;
            this.offerSdp = offerSdp;
            this.iceCandidates = iceCandidates;
        }
    }

    private class CarrierMessageObserver implements FriendInviteResponseHandler {

        @Override
        public void onReceived(String from, int status, String reason, String data) {
            Log.e(TAG, "carrier friend invite  onReceived from: " + from);
        }
    }
}
