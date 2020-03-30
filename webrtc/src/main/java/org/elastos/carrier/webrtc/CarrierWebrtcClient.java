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

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierExtension;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.exceptions.CarrierException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Initial the Carrier Webrtc Client instance for webrtc call using carrier network.
 * <p>To use: create an instance of this object (registering a message handler) and
 * register initialCall().  Once carrier network connection is established
 * onCallInitialized() callback with webrtc parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after Carrier connection is established.
 */
public class CarrierWebrtcClient extends CarrierExtension implements WebrtcClient {
  private static final String TAG = "CarrierWebrtcClient";
  private static final int CLOSE_TIMEOUT = 1000;

  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  private final Handler handler;
  private boolean initiator;
  private SignalingEvents events;
  private ConnectionState connectionState;

  private String remoteUserId;

  private final Object closeEventLock = new Object();
  private boolean closeEvent;

  private Carrier carrier;

  private FriendInviteResponseHandler friendInviteResponseHandler;

  public CarrierWebrtcClient(Carrier carrier, SignalingEvents events) {
    super(carrier);

    try {
      registerExtension();
    } catch (Exception e) {
      Log.e(TAG, "CarrierPeerConnectionClient: register carrier extension error", e);
    }

    this.carrier = carrier;
    this.events = events;
    connectionState = ConnectionState.NEW;

    friendInviteResponseHandler = new CarrierMessageObserver();

    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  // --------------------------------------------------------------------
  // WebrtcClient interface implementation.
  // AInvite a webrtc call to peer, the peer can choose to accept the invitation or reject.
  @Override
  public void inviteCall(String peer) {
    this.remoteUserId = peer;
    sendInvite();

    initialCallInternal(false);

  }


  // Asynchronously accept a webrtc call. Once accept the offer by CarrierExtension.onFriendInvite()
  // callback is invoked.
  @Override
  public void acceptCallInvite(String peer) {
    this.remoteUserId = peer;

    initialCallInternal(false);

    acceptInvite();

  }

  @Override
  public void rejectCallInvite(String peer) {
    this.remoteUserId = peer;
    handler.post(new Runnable() {
      @Override
      public void run() {
        rejectCallInternal();
      }
    });
  }

  /**
   * send invite message to callee.
   */
  private void sendInvite() {
    Log.d(TAG, "sendInvite to : " + remoteUserId);
    if(remoteUserId == null){
      throw new IllegalStateException("WebrtcClient has not been invited, please call WebrtcClient.inviteCall(String peer) firstly.");
    }
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          JSONObject json = new JSONObject();
          jsonPut(json, "type", "invite");
          jsonPut(json, "remoteUserId", remoteUserId);
          send(json.toString(), remoteUserId);

          JSONObject object = new JSONObject();
          jsonPut(object, "cmd", "send");
          jsonPut(object, "msg", json.toString());
          jsonPut(object, "remoteUserId", remoteUserId);
          carrier.inviteFriend(remoteUserId, object.toString(), friendInviteResponseHandler);

        } catch (Exception e) {
          Log.e(TAG, "sendInvite: ", e);
        }
      }
    });
  }

  /**
   * accept invite message to caller.
   */
  private void acceptInvite() {
    Log.d(TAG, "acceptInvite to : " + remoteUserId);
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          JSONObject json = new JSONObject();
          jsonPut(json, "type", "acceptInvite");
          jsonPut(json, "remoteUserId", remoteUserId);
          send(json.toString(), remoteUserId);
        } catch (Exception e) {
          Log.e(TAG, "acceptInvite: ", e);
        }
      }
    });
  }

  @Override
  public void disconnectFromCall() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        disconnectFromCallInternal();
        handler.getLooper().quit();
      }
    });
  }


  // accept the call invite and then send the offer.
  public void initialCallInternal(boolean initiator) {

    Log.d(TAG, "Connect to carrier user: " + remoteUserId);
    connectionState = ConnectionState.NEW;

    List<PeerConnection.IceServer> iceServers = getIceServers();

    SignalingParameters params = new SignalingParameters(
            iceServers, initiator, remoteUserId,null, null);

    signalingParametersReady(params);
  }


  // reject the call invite and set the connectionState to
  private void rejectCallInternal() {
    disconnectFromCallInternal();
  }


  /**
   * Initial
   * @return
   */
  public List<PeerConnection.IceServer> getIceServers() {
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
    connectionState = ConnectionState.CLOSED;
    disconnect(true);
  }


  // Callback issued when webrtc call parameters are extracted. Runs on local
  // looper thread.
  private void signalingParametersReady(final SignalingParameters signalingParameters) {
    Log.d(TAG, "Carrier WebrtcClient call initialized completed.");

    if (!signalingParameters.initiator
            && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP from the caller.");
    }

    connectionState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    events.onCallInviteAccepted(signalingParameters);
  }


  // Send local offer SDP to the other participant.
  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (connectionState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        send(json.toString(), remoteUserId);
      }
    });
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        send(json.toString(), remoteUserId);
      }
    });
  }

  // Send Ice candidate to the other participant.
  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        if (initiator) {
          // Call initiator sends ice candidates to peer.
          if (connectionState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate in non connected state.");
            return;
          }
          send(json.toString(), remoteUserId);
        } else {
          // Call receiver sends ice candidates to peer.
          send(json.toString(), remoteUserId);
        }
      }
    });
  }

  // Send removed Ice candidates to the other participant.
  @Override
  public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
          jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);
        if (initiator) {
          // Call initiator sends ice candidates to peer.
          if (connectionState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate removals in non connected state.");
            return;
          }
          send(json.toString(), remoteUserId);
        } else {
          // Call receiver sends ice candidates to peer.
          send(json.toString(), remoteUserId);
        }
      }
    });
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


  // --------------------------------------------------------------------
  // CarrierChannelEvents interface implementation.
  // All events are called by CarrierChannelClient on a local looper thread
  // (passed to Carrier client constructor).
  private void onCarrierMessage(final String msg, String from) {

    try {
      JSONObject json = new JSONObject(msg);
      String msgText = json.optString("msg", "");
      if (TextUtils.isEmpty(msgText)) {
        events.onCreateOffer();
        return;
      }
      String errorText = json.optString("error");
      if (msgText.length() > 0) {
        json = new JSONObject(msgText);
        String type = json.optString("type");
        if (type.equals("candidate")) {
          events.onRemoteIceCandidate(toJavaCandidate(json));
        } else if (type.equals("remove-candidates")) {
          JSONArray candidateArray = json.getJSONArray("candidates");
          IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
          for (int i = 0; i < candidateArray.length(); ++i) {
            candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
          }
          events.onRemoteIceCandidatesRemoved(candidates);
        } else if (type.equals("answer")) {
          if (initiator) {
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
            events.onRemoteDescription(sdp);
          } else {
            reportError("Received answer for register initiator: " + msg);
          }
        } else if (type.equals("offer")) {
          if (!initiator) {
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
            events.onRemoteDescription(sdp);
          } else {
            reportError("Received offer for register receiver: " + msg);
          }
        } else if (type.equals("bye")) {
          events.onChannelClose();
        }else if(type.equals("invite")){
          events.onCallInvited(from); //let the activity to handle the call invited event.
        }else if(type.equals("acceptInvite")){
          SignalingParameters param = new SignalingParameters(getIceServers(), true, from, null, null);
          events.onCallInviteAccepted(param); //let the activity to handle the call invite accepted event.
        } else {
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
    }
  }


  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (connectionState != ConnectionState.ERROR) {
          connectionState = ConnectionState.ERROR;
          events.onChannelError(errorMessage);
        }
      }
    });
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
    public final boolean initiator;

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
  private void send(String message, String remoteUserId) {
    checkIfCalledOnValidThread();

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

  public void disconnect(boolean waitForComplete) {
    checkIfCalledOnValidThread();
    Log.d(TAG, "Disconnect Carrier WebrtcClient.");
    // Close Carrier in CONNECTED or ERROR states only.

    // Wait for Carrier close event to prevent Carrier from
    // sending any pending messages to deleted looper thread.
    if (waitForComplete) {
      synchronized (closeEventLock) {
        while (!closeEvent) {
          try {
            closeEventLock.wait(CLOSE_TIMEOUT);
            break;
          } catch (InterruptedException e) {
            Log.e(TAG, "Wait error: " + e.toString());
          }
        }
      }
    }
    Log.d(TAG, "Disconnecting Carrier WebrtcClient done.");
  }


  private class CarrierMessageObserver implements FriendInviteResponseHandler {

    @Override
    public void onReceived(String from, int status, String reason, String data) {
      Log.e(TAG, "carrier friend invite  onReceived from: " + from);
    }
  }


}
