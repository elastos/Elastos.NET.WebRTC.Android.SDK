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

import org.elastos.carrier.webrtc.signaling.CarrierChannelClient;
import org.elastos.carrier.webrtc.signaling.CarrierChannelEvents;
import org.elastos.carrier.webrtc.signaling.CarrierConnectionState;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * User: cpedia@gmail.com
 *
 * Initial the Carrier Webrtc Client instance for webrtc call using carrier network.
 * <p>To use: create an instance of this object (registering a message handler) and
 * register initialCall().  Once carrier network connection is established
 * onCallInitialized() callback with webrtc parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after Carrier connection is established.
 */
public class CarrierWebrtcClient implements WebrtcClient, CarrierChannelEvents {
  private static final String TAG = "CarrierWebrtcClient";

  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  private final Handler handler;
  private boolean initiator;
  private SignalingEvents events;
  private CarrierChannelClient carrierChannelClient;
  private ConnectionState connectionState;

  private String calleeAddress; //callee's carrier address
  private String callerAddress; //caller's carrier address
  private String calleeUserId; //callee's carrier user id

  private Context context;

  public CarrierWebrtcClient(SignalingEvents events, Context context) {
    this.events = events;
    connectionState = ConnectionState.NEW;
    this.context = context;
    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  // --------------------------------------------------------------------
  // WebrtcClient interface implementation.
  // Asynchronously initial a webrtc call. Once connection is established onCallInitialized()
  // callback is invoked with webrtc parameters.
  @Override
  public void initialCall(String callerAddress, String calleeAddress) {
    this.callerAddress = callerAddress;
    this.calleeAddress = calleeAddress;
    handler.post(new Runnable() {
      @Override
      public void run() {
        initialCallInternal();
      }
    });
  }

  @Override
  public void sendInvite(String calleeId) {
    Log.d(TAG, "sendInvite: " + calleeId);
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "invite");
        // 将当前 address 设为别叫，让对方呼自己
        jsonPut(json, "calleeAddress", callerAddress);
        carrierChannelClient.send(calleeId, json.toString());
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

  @Override
  public void onSdpReceived(String calleeUserId){
    this.calleeUserId = calleeUserId;
  }

  // Connects to webrtc call - function runs on a local looper thread.
  private void initialCallInternal() {
    carrierChannelClient = new CarrierChannelClient(handler, this, context);

    if(calleeAddress.equals(callerAddress)){ //如果是被呼叫者进来，则等待呼叫者的offer.
      Log.d(TAG, "Waiting for connection to carrier address: " + calleeAddress);
    }

    // String connectionUrl = getConnectionUrl(connectionParameters);
    Log.d(TAG, "Connect to carrier address: " + calleeAddress + ", from: " + callerAddress);
    connectionState = ConnectionState.NEW;

    CarrierTurnServerFetcher.CarrierTurnServerFetcherEvents callbacks = new CarrierTurnServerFetcher.CarrierTurnServerFetcherEvents() {
      @Override
      public void onSignalingParametersReady(final SignalingParameters params) {
        CarrierWebrtcClient.this.handler.post(new Runnable() {
          @Override
          public void run() {
            CarrierWebrtcClient.this.signalingParametersReady(params);
          }
        });
      }

      @Override
      public void onSignalingParametersError(String description) {
        CarrierWebrtcClient.this.reportError(description);
      }
    };

    boolean initiator = carrierChannelClient.isInitiator(calleeAddress);
    new CarrierTurnServerFetcher(context, calleeAddress, initiator, callerAddress, callbacks).initializeTurnServer();
  }

  // Disconnect from call and send bye messages - runs on a local looper thread.
  private void disconnectFromCallInternal() {
    Log.d(TAG, "Disconnect. Connection state: " + connectionState);
    if (connectionState == ConnectionState.CONNECTED) {
      Log.d(TAG, "Closing call.");
    }
    connectionState = ConnectionState.CLOSED;
    if (carrierChannelClient != null) {
      carrierChannelClient.disconnect(true);
    }
  }


  // Callback issued when webrtc call parameters are extracted. Runs on local
  // looper thread.
  private void signalingParametersReady(final SignalingParameters signalingParameters) {
    Log.d(TAG, "Carrier address connection completed.");

    if (!signalingParameters.initiator
        && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP from the caller.");
    }
    initiator = signalingParameters.initiator;

    connectionState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    events.onCallInitialized(signalingParameters);

    // Connect and register Carrier client.
    carrierChannelClient.connect(signalingParameters.calleeAddress, signalingParameters.callerAddress);
    carrierChannelClient.register(signalingParameters.calleeAddress, signalingParameters.callerAddress);
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
        carrierChannelClient.send(json.toString());
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

  // --------------------------------------------------------------------
  // CarrierChannelEvents interface implementation.
  // All events are called by CarrierChannelClient on a local looper thread
  // (passed to Carrier client constructor).
  @Override
  public void onCarrierMessage(final String msg) {
    if (carrierChannelClient.getState() != CarrierConnectionState.REGISTERED) {
      Log.e(TAG, "Got Carrier message in non registered state.");
      return;
    }
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

  @Override
  public void onCarrierClose() {
    events.onChannelClose();
  }

  @Override
  public void onCarrierError(String description) {
    reportError("Carrier error: " + description);
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
      carrierChannelClient.send(message.toString());
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

    public final String calleeAddress;
    public final String callerAddress;
    public final SessionDescription offerSdp;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                               String calleeAddress, String callerAddress, SessionDescription offerSdp,
                               List<IceCandidate> iceCandidates) {
      this.iceServers = iceServers;
      this.initiator = initiator;
      this.calleeAddress = calleeAddress;
      this.callerAddress = callerAddress;
      this.offerSdp = offerSdp;
      this.iceCandidates = iceCandidates;
    }
  }
}
