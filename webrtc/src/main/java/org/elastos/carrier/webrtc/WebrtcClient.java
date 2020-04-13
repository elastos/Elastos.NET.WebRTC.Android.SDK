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
import android.text.TextUtils;
import android.util.Log;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierExtension;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.exceptions.CarrierException;
import org.elastos.carrier.webrtc.call.CallHandler;
import org.elastos.carrier.webrtc.call.CallReason;
import org.elastos.carrier.webrtc.call.CallState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 *
 * Initial the Carrier Webrtc Client instance for webrtc call using carrier network.
 * <p>To use: create an instance of this object (registering a message handler) and
 * then invite the peer to join the call (inviteCall), if the peer accept (acceptCallInvite()), then
 * then peers can prepare the peerConnection by call initialCall(), after that,
 * the peers can exchange offer/answer.
 *
 * Once the call is invited, the onCallInvited() callback with webrtc parameters is invoked,
 * once the call is initialized, onCallInitialized() callback will be fired.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after Carrier connection is established.
 */
public class WebrtcClient extends CarrierExtension implements Webrtc, PeerConnectionEvents {

  private static final String TAG = "WebrtcClient";
  private static final int CLOSE_TIMEOUT = 1000;

  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  private final Handler handler;
  private boolean initiator;
  // private SignalingEvents events;
  private ConnectionState connectionState;

  private String remoteUserId;

  private final Object closeEventLock = new Object();

  private Carrier carrier;

  private FriendInviteResponseHandler friendInviteResponseHandler;

  private static WebrtcClient INSTANCE;
  private CallHandler callHandler;
  private CallState callState;
  private Context context;
  private EglBase eglBase;
  private CarrierPeerConnectionClient carrierPeerConnectionClient;
  private CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
  private SignalingParameters signalingParameters;
  private ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
  private ProxyVideoSink remoteProxyVideoSink = new ProxyVideoSink();
  private List<VideoSink> remoteSinks = Arrays.asList(new ProxyVideoSink[]{ remoteProxyVideoSink });
  private SurfaceViewRenderer localVideoRenderer;
  private SurfaceViewRenderer remoteVideoRenderer;

  public static WebrtcClient initialize(@NonNull Context context,
                                        @NonNull Carrier carrier,
                                        @Nullable CallHandler callHandler,
                                        @Nullable CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters) {
    if (INSTANCE == null) {
      synchronized (WebrtcClient.class) {
        if (INSTANCE == null) {
          WebrtcClient temp = new WebrtcClient(context, carrier, callHandler, peerConnectionParameters);
          INSTANCE = temp;
        }
      }
    }
    return INSTANCE;
  }

  public static Webrtc getInstance() {
    if (INSTANCE == null) {
      throw new RuntimeException("call initialize method before getInstance");
    }
    return INSTANCE;
  }

  private WebrtcClient(Context context, Carrier carrier, CallHandler callHandler, CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters) {
    super(carrier);
    try {
      registerExtension();
    } catch (Exception e) {
      Log.e(TAG, "CarrierPeerConnectionClient: register carrier extension error", e);
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
    handler = new Handler(handlerThread.getLooper());
  }

//  public WebrtcClient(Carrier carrier, SignalingEvents events) {
//    super(carrier);
//
//    try {
//      registerExtension();
//    } catch (Exception e) {
//      Log.e(TAG, "CarrierPeerConnectionClient: register carrier extension error", e);
//    }
//
//    this.carrier = carrier;
//    this.events = events;
//    connectionState = ConnectionState.NEW;
//
//    friendInviteResponseHandler = new CarrierMessageObserver();
//
//    final HandlerThread handlerThread = new HandlerThread(TAG);
//    handlerThread.start();
//    handler = new Handler(handlerThread.getLooper());
//  }

  public void setRemoteUserId(String remoteUserId) {
    this.remoteUserId = remoteUserId;
  }

  // --------------------------------------------------------------------
  // Webrtc interface implementation.
  // AInvite a webrtc call to peer, the peer can choose to accept the invitation or reject.
  @Override
  public void inviteCall(String peer) {
    Log.d(TAG, "inviteCall: " + peer);
    this.initiator = true;
    this.remoteUserId = peer;
    this.setCallState(CallState.INVITING);
     sendInvite();
  }


  // Asynchronously accept a webrtc call. Once accept the offer by CarrierExtension.onFriendInvite()
  // callback is invoked.
  @Override
  public void acceptCallInvite() {
    this.initiator = false;
    this.setCallState(CallState.CONNECTING);
    acceptInvite();
  }

  @Override
  public void rejectCallInvite() {
    try {
      JSONObject json = new JSONObject();
      jsonPut(json, "type", "reject");
      jsonPut(json, "remoteUserId", remoteUserId);
      send(json.toString());

      Log.d(TAG, "rejectCallInvite() from "  + carrier.getUserId() + ", to: "+ remoteUserId);

    } catch (Exception e) {
      Log.e(TAG, "rejectCallInvite: ", e);
    }
  }
//
//  /**
//   * send invite message to callee.
//   */
  private void sendInvite() {
    Log.d(TAG, "sendInvite to : " + remoteUserId);
    if(remoteUserId == null){
      throw new IllegalStateException("Webrtc has not been invited, please call Webrtc.inviteCall(String peer) firstly.");
    }
    try {
      JSONObject json = new JSONObject();
      jsonPut(json, "type", "invite");
      jsonPut(json, "remoteUserId", remoteUserId);
      send(json.toString());

      JSONObject object = new JSONObject();
      jsonPut(object, "cmd", "send");
      jsonPut(object, "msg", json.toString());
      jsonPut(object, "remoteUserId", remoteUserId);
      inviteFriend(remoteUserId, object.toString(), friendInviteResponseHandler);//why we invite twice？ this is used for the activity to handle the message.

      Log.d(TAG, "inviteCall() from caller: " + carrier.getUserId()+" to "+ remoteUserId);
    } catch (Exception e) {
      Log.e(TAG, "sendInvite: ", e);
    }
  }

  private void sendBye() {
    try {
      JSONObject json = new JSONObject();
      jsonPut(json, "type", "bye");
      jsonPut(json, "remoteUserId", remoteUserId);
      send(json.toString());

      Log.d(TAG, "send bye from "  + carrier.getUserId() + ", to: "+ remoteUserId);

    } catch (Exception e) {
      Log.e(TAG, "sendBye: ", e);
    }
  }

  /**
   * accept invite message to caller.
   */
  private void acceptInvite() {
    Log.d(TAG, "acceptInvite from : " + remoteUserId);
    try {
      JSONObject json = new JSONObject();
      jsonPut(json, "type", "acceptInvite");
      jsonPut(json, "remoteUserId", remoteUserId);
      send(json.toString());

      Log.d(TAG, "acceptCallInvite() from callee: " + carrier.getUserId()+" to "+ remoteUserId);

    } catch (Exception e) {
      Log.e(TAG, "acceptInvite: ", e);
    }
  }

  @Override
  public void disconnectFromCall() {
    this.setCallState(CallState.INIT);
    sendBye();
    disconnectFromCallInternal();

    handler.getLooper().quit();
    try {
      Log.d(TAG, "disconnectFromCall() from callee: " + carrier.getUserId()+" to "+ remoteUserId);
    } catch (CarrierException e) {
      Log.e(TAG, "No Carrier instance");
    }
  }

  // accept the call invite and then send the offer.
  private void initialCall() {
    Log.d(TAG, "Connect to carrier user: " + remoteUserId);
    connectionState = ConnectionState.NEW;

    List<PeerConnection.IceServer> iceServers = getIceServers();
    signalingParameters = new SignalingParameters(
            iceServers, initiator, remoteUserId,null, null);

    try {
      Log.d(TAG, "initialCall() from " + (initiator?"caller":"callee") + ": " + carrier.getUserId() + ", to: "+ remoteUserId);
    } catch (CarrierException e) {
      Log.e(TAG, "No Carrier instance.");
    }

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

  @Override
  public void renderVideo(@NonNull SurfaceViewRenderer localRenderer, @NonNull SurfaceViewRenderer remoteRenderer) {
    this.localVideoRenderer = localRenderer;
    this.remoteVideoRenderer = remoteRenderer;
    if (this.eglBase == null) {
      this.eglBase = EglBase.create();
    }
    this.localVideoRenderer.init(eglBase.getEglBaseContext(), null);
    this.remoteVideoRenderer.init(eglBase.getEglBaseContext(), null);
    swapVideoRenderer(false);
  }

  @Override
  public void swapVideoRenderer(boolean isSwap) {
    if (localVideoRenderer == null || remoteVideoRenderer == null) {
      Log.w(TAG, "swapVideoRenderer: video renderer is null");
      return;
    }
    localProxyVideoSink.setTarget(isSwap ? remoteVideoRenderer : localVideoRenderer);
    remoteProxyVideoSink.setTarget(isSwap ? localVideoRenderer : remoteVideoRenderer);
    remoteVideoRenderer.setMirror(isSwap);
    localVideoRenderer.setMirror(!isSwap);
  }

  @Override
  public void switchCamera() {
    if (carrierPeerConnectionClient == null) {
      Log.w(TAG, "switchCamera: carrierPeerConnectionClient is null");
      return;
    }
    carrierPeerConnectionClient.switchCamera();
  }

  @Override
  public void setResolution(int width, int height, int fps) {
    if (carrierPeerConnectionClient == null) {
      Log.w(TAG, "switchCamera: carrierPeerConnectionClient is null");
      return;
    }
    carrierPeerConnectionClient.changeCaptureFormat(width, height, fps);
  }

  @Override
  public void setAudioEnable(boolean enable) {
    if (carrierPeerConnectionClient == null) {
      Log.w(TAG, "switchCamera: carrierPeerConnectionClient is null");
      return;
    }
    carrierPeerConnectionClient.setAudioEnabled(enable);
  }

  @Override
  public void setVideoEnable(boolean enable) {
    if (carrierPeerConnectionClient == null) {
      Log.w(TAG, "switchCamera: carrierPeerConnectionClient is null");
      return;
    }
    carrierPeerConnectionClient.setVideoEnabled(enable);
  }

  /**
   * Initial
   * @return
   */
  private List<PeerConnection.IceServer> getIceServers() {
    TurnServerInfo turnServerInfo = null;
    try {
      turnServerInfo = getTurnServerInfo();
    } catch (CarrierException e) {
      Log.e(TAG, "Get Turn server from carrier network error.");
    }

    List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    if (turnServerInfo !=null) {
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

//    if (audioManager != null) {
//      audioManager.stop();
//      audioManager = null;
//    }
  }

  // Send local offer SDP to the other participant.
  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    if (connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.NEW) {
      reportError("Sending offer SDP in non connected state.");
      return;
    }
    JSONObject json = new JSONObject();
    jsonPut(json, "sdp", sdp.description);
    jsonPut(json, "type", "offer");
    send(json.toString());

    try {
      Log.d(TAG, "sendOfferSdp() from "  + carrier.getUserId() + ", to: "+ remoteUserId);
    } catch (CarrierException e) {
      Log.e(TAG, "No Carrier instance.");
    }
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    JSONObject json = new JSONObject();
    jsonPut(json, "sdp", sdp.description);
    jsonPut(json, "type", "answer");
    send(json.toString());

    try {
      Log.d(TAG, "sendAnswerSdp() from "  + carrier.getUserId() + ", to: "+ remoteUserId);
    } catch (CarrierException e) {
      Log.e(TAG, "No Carrier instance.");
    }
  }

  // Send Ice candidate to the other participant.
  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
    JSONObject json = new JSONObject();
    jsonPut(json, "type", "candidate");
    jsonPut(json, "label", candidate.sdpMLineIndex);
    jsonPut(json, "id", candidate.sdpMid);
    jsonPut(json, "candidate", candidate.sdp);
    if (initiator) {
      // Call initiator sends ice candidates to peer.
      if (connectionState != ConnectionState.CONNECTED && connectionState!=ConnectionState.NEW) {
        reportError("Sending ICE candidate in non connected state.");
        return;
      }
      send(json.toString());

      try {
        Log.d(TAG, "sendLocalIceCandidate() from "  + carrier.getUserId() + ", to: "+ remoteUserId);
      } catch (CarrierException e) {
        Log.e(TAG, "No Carrier instance.");
      }

    } else {
      // Call receiver sends ice candidates to peer.
      send(json.toString());
    }
  }

  // Send removed Ice candidates to the other participant.
  @Override
  public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
    JSONObject json = new JSONObject();
    jsonPut(json, "type", "remove-candidates");
    JSONArray jsonArray = new JSONArray();
    for (final IceCandidate candidate : candidates) {
      jsonArray.put(toJsonCandidate(candidate));
    }
    jsonPut(json, "candidates", jsonArray);
    if (initiator) {
      // Call initiator sends ice candidates to peer.
      if (connectionState != ConnectionState.CONNECTED && connectionState!=ConnectionState.NEW) {
        reportError("Sending ICE candidate removals in non connected state.");
        return;
      }
      send(json.toString());
    } else {
      // Call receiver sends ice candidates to peer.
      send(json.toString());
    }

    try {
      Log.d(TAG, "sendLocalIceCandidateRemovals() from "  + carrier.getUserId() + ", to: "+ remoteUserId);
    } catch (CarrierException e) {
      Log.e(TAG, "No Carrier instance.");
    }
  }

  @Override
  protected void onFriendInvite(Carrier carrier, String from, String data) {
    Log.e(TAG, "carrier friend invite  onFriendInviteRequest from: " + from);

    if (data != null && data.contains("msg")) { //通过添加好友的消息回执绕过carrier message 1024字符的限制
      this.remoteUserId = from;
      onCarrierMessage(data, from);
      Log.d(TAG, "Get the carrier message: " + data);
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
  }

  @Override
  public void destroy() {
    try {
      finalize();
    } catch (Throwable e) {
      Log.e(TAG, "destroy: ", e);
    }
  }

  public CallState getCallState() {
    return this.callState;
  }

  private void setCallState(CallState callState) {
    this.callState = callState;
  }

  private void initialWebrtc() {
    if (eglBase == null) {
      eglBase = EglBase.create();
    }
    // Create peer connection client.
    carrierPeerConnectionClient = new CarrierPeerConnectionClient(
            context, getIceServers(), eglBase, peerConnectionParameters, this);
    PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

    carrierPeerConnectionClient.createPeerConnectionFactory(options);
  }

  private void handleOffer(SessionDescription sdp) {
    Log.d(TAG, "handleOffer: ");
    if (!initiator) {
      initialCall();
      // events.onRemoteDescription(sdp);
      carrierPeerConnectionClient.setRemoteDescription(sdp);
      carrierPeerConnectionClient.createAnswer();
    } else {
      reportError("Received offer for register receiver: ");
    }
  }

  private void handleAnswer(SessionDescription sdp) {
    Log.d(TAG, "handleAnswer: ");
    if (initiator) {
      // events.onRemoteDescription(sdp);
      carrierPeerConnectionClient.setRemoteDescription(sdp);
    } else {
      reportError("Received answer for register initiator: ");
    }
  }

  private void handleCandidate(IceCandidate[] candidates) {
    Log.d(TAG, "handleCandidate: ");
  }

  private void handleInvite(String from) {
    Log.d(TAG, "handleInvite: " + from);
    if (this.callState != null && this.callState.getValue() > CallState.INIT.getValue()) {
      Log.d(TAG, "handleInvite: state is not init, return");
      return;
    }
    this.remoteUserId = from;
    this.setCallState(CallState.RINGING);
    signalingParameters = new SignalingParameters(getIceServers(), false, from, null, null);
    // events.onCallInvited(signalingParameters); //let the activity to handle the call invited event.
    callHandler.onInvite(from);
  }

  private void handleAcceptInvite(String from) {
    Log.d(TAG, "handleAcceptInvite: ");
    this.setCallState(CallState.CONNECTING);
    initialCall();
    // events.onCreateOffer(); //let the activity to handle the call invite accepted event.
    carrierPeerConnectionClient.createOffer();
    callHandler.onAnswer();
  }

  private void handleBye() {
    // events.onChannelClose();
    Log.d(TAG, "handleBye: ");
    this.setCallState(CallState.INIT);
    disconnectFromCallInternal();
    callHandler.onEndCall(CallReason.NORMAL_HANGUP);
  }

  private void handleReject() {
    // events.onChannelClose();
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
      JSONObject json = new JSONObject(msg);
      String msgText = json.optString("msg", "");
      if (TextUtils.isEmpty(msgText)) {
        // events.onCreateOffer();
        return;
      }
      String errorText = json.optString("error");
      if (msgText.length() > 0) {
        json = new JSONObject(msgText);
        String type = json.optString("type");
        if (type.equals("candidate")) {
          // events.onRemoteIceCandidate(toJavaCandidate(json));
          carrierPeerConnectionClient.addRemoteIceCandidate(toJavaCandidate(json));
        } else if (type.equals("remove-candidates")) {
          JSONArray candidateArray = json.getJSONArray("candidates");
          IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
          for (int i = 0; i < candidateArray.length(); ++i) {
            candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
          }
          // events.onRemoteIceCandidatesRemoved(candidates);
          carrierPeerConnectionClient.removeRemoteIceCandidates(candidates);
        } else if (type.equals("answer")) {
          SessionDescription sdp = new SessionDescription(
                  SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
          handleAnswer(sdp);
        } else if (type.equals("offer")) {
          SessionDescription sdp = new SessionDescription(
                  SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
          handleOffer(sdp);
        } else if (type.equals("bye")) {
          handleBye();
        }  else if (type.equals("reject")) {
          handleReject();
        } else if(type.equals("invite")) {
          Log.d(TAG, "onCarrierMessage: invite-message -> " + msg);
          handleInvite(from);
        } else if(type.equals("acceptInvite")) {
          handleAcceptInvite(from);
        }else {
          reportError("Unexpected Carrier message: " + msg);
        }
      } else {
        if (errorText != null && errorText.length() > 0) {
          reportError("Carrier error message: " + errorText);
        } else {
          reportError("Unexpected Carrier message: " + msg);
        }
      }
    } catch (JSONException e) {
      reportError("Carrier message JSON parsing error: " + e.toString());
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

  private @Nullable VideoCapturer createVideoCapturer() {
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

  // Put a |key|->|value| mapping in |json|.
  private static void jsonPut(JSONObject json, String key, Object value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  // Converts a Java candidate to a JSONObject.
  private JSONObject toJsonCandidate(final IceCandidate candidate) {
    JSONObject json = new JSONObject();
    jsonPut(json, "label", candidate.sdpMLineIndex);
    jsonPut(json, "id", candidate.sdpMid);
    jsonPut(json, "candidate", candidate.sdp);
    return json;
  }

  // Converts a JSON candidate to a Java object.
  IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
    return new IceCandidate(
            json.getString("id"), json.getInt("label"), json.getString("candidate"));
  }


  /**
   * Struct holding the signaling parameters of an webrtc communication.
   */
  public static class SignalingParameters {
    public final List<PeerConnection.IceServer> iceServers;
    public boolean initiator;

    public final String remoteUserId;
    public final SessionDescription offerSdp;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                               String remoteUserId,  SessionDescription offerSdp,
                               List<IceCandidate> iceCandidates) {
      this.iceServers = iceServers;
      this.initiator = initiator;
      this.remoteUserId = remoteUserId;
      this.offerSdp = offerSdp;
      this.iceCandidates = iceCandidates;
    }
  }

  // Helper method for debugging purposes. Ensures that Carrier method is
  // called on a looper thread.
  private void checkIfCalledOnValidThread() {
    if (Thread.currentThread() != handler.getLooper().getThread()) {
      throw new IllegalStateException("Carrier method is not called on valid thread");
    }
  }

  //send message
  private void send(String message) {
    // checkIfCalledOnValidThread();

    JSONObject json = new JSONObject();
    try {
      json.put("cmd", "send");
      json.put("msg", message);
      json.put("remoteUserId", remoteUserId);
      message = json.toString();

      Log.d(TAG, "C->Call: " + message);

      if (remoteUserId.equals(carrier.getUserId())) {
        return; //can not send message to self through carrier network.
      }
      sendMessageByInvite(remoteUserId, message);

    } catch (JSONException e) {
      reportError("Carrier send JSON error: " + e.getMessage());
    } catch (CarrierException e) {
      e.printStackTrace();
      reportError("carrier send message error: " + e.getMessage());
    }

  }

  private void sendMessageByInvite(String fid, String message) throws CarrierException {
    if(fid!=null && !fid.equals(carrier.getUserId())){
      inviteFriend(fid, message, friendInviteResponseHandler);
    }
  }

  private class CarrierMessageObserver implements FriendInviteResponseHandler {

    @Override
    public void onReceived(String from, int status, String reason, String data) {
      Log.e(TAG, "carrier friend invite  onReceived from: " + from);
    }
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
  /**
   * end implements PeerConnectionEvents
   */
}
