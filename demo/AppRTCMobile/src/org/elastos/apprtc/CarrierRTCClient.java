/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.elastos.apprtc;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import org.elastos.apprtc.RoomParametersFetcher.RoomParametersFetcherEvents;
import org.elastos.apprtc.CarrierChannelClient.CarrierConnectionState;
import org.elastos.carrier.webrtc.signaling.carrier.CarrierClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * User: cpedia@gmail.com
 * Comments: Replace apprtc Carrier signal channel with Carrier network.
 */


/**
 * Negotiates signaling for chatting with https://appr.tc "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after Carrier connection is established.
 */
public class CarrierRTCClient implements AppRTCClient, CarrierChannelClient.CarrierChannelEvents {
  private static final String TAG = "WSRTCClient";
  private static final String ROOM_JOIN = "join";
  private static final String ROOM_MESSAGE = "message";
  private static final String ROOM_LEAVE = "leave";

  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  private enum MessageType { MESSAGE, LEAVE }

  private final Handler handler;
  private boolean initiator;
  private SignalingEvents events;
  private CarrierChannelClient wsClient;
  private ConnectionState roomState;
  private RoomConnectionParameters connectionParameters; //calleeAddress and callerAddress in this object.
  private String calleeUserId; //carrier user id
  private String callerUserId;

  private Context context;

  public CarrierRTCClient(SignalingEvents events, Context context) {
    this.events = events;
    roomState = ConnectionState.NEW;
    this.context = context;
    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  // --------------------------------------------------------------------
  // AppRTCClient interface implementation.
  // Asynchronously connect to an AppRTC room URL using supplied connection
  // parameters, retrieves room parameters and connect to Carrier server.
  @Override
  public void connectToRoom(RoomConnectionParameters connectionParameters) {
    this.connectionParameters = connectionParameters;
    CarrierClient cf = CarrierClient.getInstance(context, null);
    this.calleeUserId = cf.getUserIdFromAddress(connectionParameters.calleeAddress);
    this.callerUserId = cf.getUserIdFromAddress(connectionParameters.callerAddress);
    handler.post(new Runnable() {
      @Override
      public void run() {
        connectToRoomInternal();
      }
    });
  }

  @Override
  public void disconnectFromRoom() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        disconnectFromRoomInternal();
        handler.getLooper().quit();
      }
    });
  }

  @Override
  public void onSdpReceived(String calleeUserId){
    this.callerUserId = calleeUserId;
  }

  // Connects to room - function runs on a local looper thread.
  private void    connectToRoomInternal() {
    wsClient = new CarrierChannelClient(handler, this, context);

    String calleeAddress = connectionParameters.calleeAddress;
    String callerAddress = connectionParameters.callerAddress;

    //if loopback, then set the address to my address.
    if(connectionParameters.loopback){
      calleeAddress  = callerAddress;
    }else if(calleeAddress.equals(callerAddress)){ //如果是被呼叫者进来，则等待呼叫者的offer.
      Log.d(TAG, "Waiting for connection to carrier address: " + calleeAddress);
    }

    // String connectionUrl = getConnectionUrl(connectionParameters);
    Log.d(TAG, "Connect to carrier address: " + calleeAddress + ", from: " + callerAddress);
    roomState = ConnectionState.NEW;

    RoomParametersFetcherEvents callbacks = new RoomParametersFetcherEvents() {
      @Override
      public void onSignalingParametersReady(final SignalingParameters params) {
        CarrierRTCClient.this.handler.post(new Runnable() {
          @Override
          public void run() {
            CarrierRTCClient.this.signalingParametersReady(params);
          }
        });
      }

      @Override
      public void onSignalingParametersError(String description) {
        CarrierRTCClient.this.reportError(description);
      }
    };

    boolean initiator = wsClient.isInitiator(calleeAddress);
    new RoomParametersFetcher(context, calleeAddress, initiator, callerAddress, null, null, callbacks).makeRequest();
  }

  // Disconnect from room and send bye messages - runs on a local looper thread.
  private void disconnectFromRoomInternal() {
    Log.d(TAG, "Disconnect. Room state: " + roomState);
    if (roomState == ConnectionState.CONNECTED) {
      Log.d(TAG, "Closing room.");
      //sendPostMessage(MessageType.LEAVE, callerUserId, null);
    }
    roomState = ConnectionState.CLOSED;
    if (wsClient != null) {
      wsClient.disconnect(true);
    }
  }

/*
  // Helper functions to get connection, post message and leave message URLs
  private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_JOIN + "/" + connectionParameters.calleeAddress
        + getQueryString(connectionParameters);
  }

  private String getMessageUrl(
      RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters.calleeAddress
        + "/" + signalingParameters.clientId + getQueryString(connectionParameters);
  }

  private String getLeaveUrl(
      RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters.calleeAddress + "/"
        + signalingParameters.clientId + getQueryString(connectionParameters);
  }

  private String getQueryString(RoomConnectionParameters connectionParameters) {
    if (connectionParameters.urlParameters != null) {
      return "?" + connectionParameters.urlParameters;
    } else {
      return "";
    }
  }
*/

  // Callback issued when room parameters are extracted. Runs on local
  // looper thread.
  private void signalingParametersReady(final SignalingParameters signalingParameters) {
    Log.d(TAG, "Carrier address connection completed.");
    if (connectionParameters.loopback
        && (!signalingParameters.initiator || signalingParameters.offerSdp != null)) {
      reportError("Loopback address is busy.");
      return;
    }
    if (!connectionParameters.loopback && !signalingParameters.initiator
        && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP from the caller.");
    }
    initiator = signalingParameters.initiator;

    roomState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    events.onConnectedToRoom(signalingParameters);

    // Connect and register Carrier client.
    wsClient.connect(signalingParameters.calleeAddress, signalingParameters.callerAddress);
    wsClient.register(signalingParameters.calleeAddress, signalingParameters.callerAddress, signalingParameters.clientId);
  }


  // Send local offer SDP to the other participant.
  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        sendPostMessage(MessageType.MESSAGE, calleeUserId, json, false);
        if (connectionParameters.loopback) {
          // In loopback mode rename this offer to answer and route it back.
          SessionDescription sdpAnswer = new SessionDescription(
              SessionDescription.Type.fromCanonicalForm("answer"), sdp.description);
          events.onRemoteDescription(sdpAnswer);
        }
      }
    });
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (connectionParameters.loopback) {
          Log.e(TAG, "Sending answer in loopback mode.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        //wsClient.sendJsonMessage(calleeUserId, callerUserId, json,false);
        wsClient.send(json.toString());
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
          if (roomState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate in non connected state.");
            return;
          }
          sendPostMessage(MessageType.MESSAGE, calleeUserId, json, false);
          if (connectionParameters.loopback) {
            events.onRemoteIceCandidate(candidate);
          }
        } else {
          // Call receiver sends ice candidates to Carrier server.
          sendPostMessage(MessageType.MESSAGE, calleeUserId, json, false);
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
          if (roomState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate removals in non connected state.");
            return;
          }
          sendPostMessage(MessageType.MESSAGE, calleeUserId, json, false);
          if (connectionParameters.loopback) {
            events.onRemoteIceCandidatesRemoved(candidates);
          }
        } else {
          // Call receiver sends ice candidates to Carrier server.
          sendPostMessage(MessageType.MESSAGE, calleeUserId, json, false);
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
    if (wsClient.getState() != CarrierConnectionState.REGISTERED) {
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
            reportError("Received answer for call initiator: " + msg);
          }
        } else if (type.equals("offer")) {
          if (!initiator) {
            SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
            events.onRemoteDescription(sdp);
          } else {
            reportError("Received offer for call receiver: " + msg);
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
        if (roomState != ConnectionState.ERROR) {
          roomState = ConnectionState.ERROR;
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

  // Send SDP or ICE candidate to a room server.
  private void sendPostMessage(
      final MessageType messageType, final String calleeUserId, @Nullable final JSONObject message, boolean needSplit) {
    String logInfo = "";
    if (message != null) {
      logInfo += "Send Message: " + message.toString() + " to: " + calleeUserId;
    }
    Log.d(TAG, "C->GAE: " + logInfo);

    //wsClient.sendJsonMessage(calleeUserId, callerUserId, message, needSplit);
    wsClient.send(message.toString());

/*
    AsyncHttpURLConnection httpConnection =
        new AsyncHttpURLConnection("POST", url, message, new AsyncHttpEvents() {
          @Override
          public void onHttpError(String errorMessage) {
            reportError("GAE POST error: " + errorMessage);
          }

          @Override
          public void onHttpComplete(String response) {
            if (messageType == MessageType.MESSAGE) {
              try {
                JSONObject roomJson = new JSONObject(response);
                String result = roomJson.getString("result");
                if (!result.equals("SUCCESS")) {
                  reportError("GAE POST error: " + result);
                }
              } catch (JSONException e) {
                reportError("GAE POST JSON error: " + e.toString());
              }
            }
          }
        });
    httpConnection.send();
*/
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
}
