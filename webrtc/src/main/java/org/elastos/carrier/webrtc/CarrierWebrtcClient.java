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

import androidx.annotation.Nullable;

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

  private String calleeUserId; //callee's carrier user id
  private String callerUserId; //caller's carrier user id
  private String remoteUserId;

  private final Object closeEventLock = new Object();
  private boolean closeEvent;

  private Carrier carrier;

  private FriendInviteResponseHandler friendInviteResponseHandler;

  public CarrierWebrtcClient(Carrier carrier, SignalingEvents events) {
    super(carrier);
    this.carrier = carrier;
    this.events = events;
    connectionState = ConnectionState.NEW;

    friendInviteResponseHandler = new CarrierMessageObserver();

    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    try {
      registerExtension();
    } catch (Exception e) {
      Log.e(TAG, "CarrierPeerConnectionClient: register carrier extension error", e);
    }
  }

  // --------------------------------------------------------------------
  // WebrtcClient interface implementation.
  // Asynchronously initial a webrtc call. Once connection is established onCallInitialized()
  // callback is invoked with webrtc parameters.
  @Override
  public void initialCall(String calleeUserId, String remoteUserId) {
    try {
      this.callerUserId = carrier.getUserId();
      this.calleeUserId = calleeUserId;
      this.remoteUserId = remoteUserId;
      handler.post(new Runnable() {
        @Override
        public void run() {
          initialCallInternal();
        }
      });
    } catch (CarrierException e) {
      Log.e(TAG, "Get user id from carrier network error.");
    }
  }

  /**
   * send invite message to callee.
   */
  @Override
  public void sendInvite() {
    Log.d(TAG, "sendInvite to : " + remoteUserId);
    if(calleeUserId == null){
      throw new IllegalStateException("WebrtcClient has not been initialized, please call WebrtcClient.initialCall(String calleeUserId) firstly.");
    }
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          JSONObject json = new JSONObject();
          jsonPut(json, "type", "invite");
          // let the counter party call me.
          jsonPut(json, "calleeUserId", callerUserId);
          send(json.toString());

          JSONObject object = new JSONObject();
          jsonPut(object, "cmd", "send");
          jsonPut(object, "msg", json.toString());
          jsonPut(object, "calleeUserId", calleeUserId);
          carrier.inviteFriend(remoteUserId, object.toString(), friendInviteResponseHandler);
        } catch (Exception e) {
          Log.e(TAG, "sendInvite: ", e);
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


  // Connects to webrtc call - function runs on a local looper thread.
  private void initialCallInternal() {

    if(calleeUserId.equals(callerUserId)){ //wait for the connection from callee.
      Log.d(TAG, "Waiting for connection to carrier user: " + calleeUserId);
    }

    Log.d(TAG, "Connect to carrier user: " + calleeUserId + ", from: " + callerUserId);
    connectionState = ConnectionState.NEW;

    boolean initiator = isInitiator(calleeUserId);

    CarrierWebrtcClient.this.handler.post(new Runnable() {
      @Override
      public void run() {
        CarrierExtension.TurnServerInfo turnServerInfo = null;
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

        SignalingParameters params = new SignalingParameters(
                iceServers, initiator, calleeUserId, callerUserId, null, null);

        CarrierWebrtcClient.this.signalingParametersReady(params);
      }
    });
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
    initiator = signalingParameters.initiator;

    connectionState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    events.onCallInitialized(signalingParameters);
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
        sendMessageByCarrierChannel(json);
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
        //carrierChannelClient.sendJsonMessage(calleeUserId, callerUserId, json,false);
        send(json.toString());
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
          // Call initiator sends ice candidates to GAE server.
          if (connectionState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate in non connected state.");
            return;
          }
          sendMessageByCarrierChannel(json);
        } else {
          // Call receiver sends ice candidates to Carrier server.
          sendMessageByCarrierChannel(json);
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
          // Call initiator sends ice candidates to GAE server.
          if (connectionState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate removals in non connected state.");
            return;
          }
          sendMessageByCarrierChannel(json);
        } else {
          // Call receiver sends ice candidates to Carrier server.
          sendMessageByCarrierChannel(json);
        }
      }
    });
  }

  @Override
  protected void onFriendInvite(Carrier carrier, String from, String data) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        Log.e(TAG, "carrier friend invite  onFriendInviteRequest from: " + from);

        if (data != null && data.contains("msg")) { //通过添加好友的消息回执绕过carrier message 1024字符的限制

          //更新calleeUserId,
          calleeUserId = from; //更新为消息回执者

          if(data.contains("offer")){
            calleeUserId = from ;
          }

          onCarrierMessage(data);
          Log.d(TAG, "Get the carrier message: " + data);
        }
      }
    });

  }


  // --------------------------------------------------------------------
  // CarrierChannelEvents interface implementation.
  // All events are called by CarrierChannelClient on a local looper thread
  // (passed to Carrier client constructor).
  public void onCarrierMessage(final String msg) {

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

  // Send SDP or ICE candidate to the callee.
  private void sendMessageByCarrierChannel(@Nullable final JSONObject message) {
    String logInfo = "";
    if (message != null) {
      logInfo += "Send Message: " + message.toString() + " to: " + calleeUserId;
    }
    Log.d(TAG, "C->GAE: " + logInfo);

    if (message != null) {
      send(message.toString());
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

    public final String calleeUserId;
    public final String callerUserId;
    public final SessionDescription offerSdp;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                               String calleeUserId, String callerUserId, SessionDescription offerSdp,
                               List<IceCandidate> iceCandidates) {
      this.iceServers = iceServers;
      this.initiator = initiator;
      this.calleeUserId = calleeUserId;
      this.callerUserId = callerUserId;
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

  public boolean isInitiator(String userId) {
    String myUserId = "";
    try {
      myUserId = carrier.getUserId();
    } catch (CarrierException e) {
      Log.e(TAG, "Get user id from carrier error.");
    }
    return userId!=null && !userId.equals(myUserId);
  }


  //send message
  private void send(String message) {
    checkIfCalledOnValidThread();

    JSONObject json = new JSONObject();
    try {
      json.put("cmd", "send");
      json.put("msg", message);
      json.put("calleeUserId", calleeUserId);
      message = json.toString();

      Log.d(TAG, "C->Call: " + message);

      if (remoteUserId.equals(callerUserId)) {
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
      handler.post(new Runnable() {
        @Override
        public void run() {
          //Toast.makeText(context, "carrier friend invite onReceived from : " + from, Toast.LENGTH_LONG).show();
          Log.e(TAG, "carrier friend invite  onReceived from: " + from);
        }

      });

    }
  }


}