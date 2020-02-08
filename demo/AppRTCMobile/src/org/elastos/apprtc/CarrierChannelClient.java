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
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.elastos.apprtc.util.AsyncHttpURLConnection;
import org.elastos.apprtc.util.AsyncHttpURLConnection.AsyncHttpEvents;
import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.UserInfo;
import org.elastos.carrier.exceptions.CarrierException;
import org.elastos.carrier.webrtc.signaling.carrier.CarrierClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Carrier client implementation.
 *
 * <p>All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */
public class CarrierChannelClient {
  private static final String TAG = "CarrierChannelRTCClient";
  private static final int CLOSE_TIMEOUT = 1000;
  private final CarrierChannelEvents events;
  private final Handler handler;

  private CarrierClient ws;
  private String calleeUserId;
  private String callerUserId;
  @Nullable
  private String calleeAddress;
  private String callerAddress;
  @Nullable
  private String clientID;
  private CarrierConnectionState state;
  // Do not remove this member variable. If this is removed, the observer gets garbage collected and
  // this causes test breakages.
  private CarrierObserver carrierObserver;
  private final Object closeEventLock = new Object();
  private boolean closeEvent;
  // Carrier send queue. Messages are added to the queue when Carrier
  // client is not registered and are consumed in register() call.
  private final List<String> wsSendQueue = new ArrayList<>();

  private Context context;

  private FriendInviteResponseHandler friendInviteResponseHandler;

  public boolean isInitiator(String address) {
    String myAddress = ws.getMyAddress();
    return address!=null && !address.equals(myAddress);
  }

  public String getMyCarrierAddress() {
    return ws.getMyAddress();
  }

  public Carrier getCarrier() {
    return ws.getCarrier();
  }

  /**
   * Possible Carrier connection states.
   */
  public enum CarrierConnectionState { NEW, CONNECTED, REGISTERED, CLOSED, ERROR }

  /**
   * Callback interface for messages delivered on Carrier.
   * All events are dispatched from a looper executor thread.
   */
  public interface CarrierChannelEvents {
    void onCarrierMessage(final String message);
    void onSdpReceived(final String calleeUserId);
    void onCarrierClose();
    void onCarrierError(final String description);
  }

  public CarrierChannelClient(Handler handler, CarrierChannelEvents events, Context context) {
    this.handler = handler;
    this.events = events;
    calleeAddress = null;
    callerAddress = null;
    clientID = null;
    state = CarrierConnectionState.NEW;
    this.context = context;

    carrierObserver = new CarrierObserver();
    friendInviteResponseHandler = new CarrierMessageObserver();
    ws = CarrierClient.getInstance(context,null);
    //ws.setmHandler(handler);
    ws.addCarrierHandler(carrierObserver);
    ws.setFriendInviteResponseHandler(friendInviteResponseHandler);
  }

  public CarrierConnectionState getState() {
    return state;
  }

  //add friend
  public void connect(final String calleeAddress, final String callerAddress) {
    checkIfCalledOnValidThread();
    if (state != CarrierConnectionState.NEW) {
      Log.e(TAG, "Carrier network is already connected.");
      return;
    }
    this.calleeAddress = calleeAddress;
    this.callerAddress = callerAddress;

    CarrierClient cf = CarrierClient.getInstance(context, null);

    calleeUserId = cf.getUserIdFromAddress(calleeAddress);
    callerUserId = cf.getUserIdFromAddress(callerAddress);
    closeEvent = false;

    //Log.d(TAG, "Connecting Carrier to: " + calleeAddress + ". Post URL: " + callerAddress);

    try {
      if (!calleeAddress.equals(callerAddress)) {
        ws.addFriend(calleeAddress);
      }
      state = CarrierConnectionState.CONNECTED;
    } catch (CarrierException e) {
      reportError("addFriend connection error: " + e.getMessage());
    }

  }

  //call
  public void register(final String calleeAddress, final String callerAddress, final String clientID) {
    checkIfCalledOnValidThread();
    this.calleeAddress = calleeAddress;
    this.clientID = clientID;
    if (state != CarrierConnectionState.CONNECTED) {
      Log.w(TAG, "Carrier register() in state " + state);
      return;
    }
    Log.d(TAG, "Registering Carrier for callee " + calleeAddress + ". ClientID: " + clientID);
    JSONObject json = new JSONObject();
    try {
      json.put("cmd", "register");
      json.put("roomid", calleeAddress);
      json.put("clientid", clientID);
      Log.d(TAG, "C->WSS: " + json.toString());
      //sendJsonMessage(calleeUserId, callerUserId, json, false);
      state = CarrierConnectionState.REGISTERED;
      // Send any previously accumulated messages.
      for (String sendMessage : wsSendQueue) {
        send(sendMessage);
      }
      wsSendQueue.clear();
    } catch (JSONException e) {
      reportError("Carrier register JSON error: " + e.getMessage());
    } /*catch (CarrierException e) {
      e.printStackTrace();
      reportError("carrier send message error: " + e.getMessage());
    }*/
  }


  private static final int MAX_LENGTH = 800;


  private void sendJsonMessage(String calleeUserId, String callerUserId, JSONObject messageJson, boolean needSplit) throws CarrierException, JSONException {
      if(calleeUserId.equals(callerUserId)){
        return; //因为carrier不能发送消息给自己的限制，被呼叫方进入无需发送消息给自己，等待offer.
      }
      if(!needSplit || messageJson.toString().length() < 1024){
          ws.sendMessageByInvite(calleeUserId, messageJson.toString());
          Log.w(TAG, "Message length: " + messageJson.toString().length() + ", no need to split it: " + messageJson);
      }else{
        //拆分remove-candidates和sdp.
        if(messageJson.has("candidates") && messageJson.optString("type")!=null && messageJson.optString("type").equals("remove-candidates")){
          JSONArray candidates = messageJson.optJSONArray("candidates");
          if(candidates!=null){
            for (int i = 0; i < candidates.length(); i++) { //send remove-candidates one by one
              JSONObject candidate = candidates.optJSONObject(i);
              if(candidate!=null){
                JSONObject msg = new JSONObject();
                JSONObject jsonObject = new JSONObject(messageJson.toString());
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(candidate);
                jsonObject.put("candidates", jsonArray);
                msg.put("msg", jsonObject);
                ws.sendMessageByInvite(calleeUserId, msg.toString());
                Log.w(TAG, "Split message length: " + msg.toString().length() + ", content: " + messageJson);
              }
            }
          }
        }else if(messageJson.has("sdp")){
           JSONObject sdp = messageJson.optJSONObject("sdp");
           if(sdp!=null) {
             String message = sdp.toString();
             String[] lines = message.split("\r\n");
             int length = 0;
             StringBuilder buffer = new StringBuilder();
             for (String line : lines) {
               if (length + line.length() >= MAX_LENGTH) { //平均每行不超过100个字符，所以一旦sdp累计字符超过800就立即送出
                 JSONObject msg = new JSONObject();

                 JSONObject jsonObject = new JSONObject(messageJson.toString());
                 jsonObject.put("sdp", buffer.toString());
                 msg.put("msg", jsonObject);
                 ws.sendMessageByInvite(calleeUserId, msg.toString());
                 Log.w(TAG, "Split message length: " + msg.toString().length() + ", content: " + messageJson);
                 buffer.setLength(0);
               }
               buffer.append(line).append("\r\n");
               length += line.length() + 4;
             }
           }
        }
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

  //send message
  public void send(String message) {
    checkIfCalledOnValidThread();
    switch (state) {
      case NEW:
      case CONNECTED:
        // Store outgoing messages and send them after Carrier client
        // is registered.
        Log.d(TAG, "WS ACC: " + message);
        wsSendQueue.add(message);
        return;
      case ERROR:
      case CLOSED:
        Log.e(TAG, "Carrier send() in error or closed state : " + message);
        return;
      case REGISTERED:
        JSONObject json = new JSONObject();
        try {
          json.put("cmd", "send");
          json.put("msg", message);
          message = json.toString();
          Log.d(TAG, "C->WSS: " + message);

          sendJsonMessage(calleeUserId, callerUserId, json, false);

        } catch (JSONException e) {
          reportError("Carrier send JSON error: " + e.getMessage());
        } catch (CarrierException e) {
          e.printStackTrace();
          reportError("carrier send message error: " + e.getMessage());
        }
        break;
    }
  }

  // This call can be used to send Carrier messages before Carrier
  // connection is opened.
  public void post(String message) {
    checkIfCalledOnValidThread();
    sendWSSMessage("POST", message);
  }

  public void disconnect(boolean waitForComplete) {
    checkIfCalledOnValidThread();
    Log.d(TAG, "Disconnect Carrier. State: " + state);
    if (state == CarrierConnectionState.REGISTERED) {
      // Send "bye" to Carrier server.
      send("{\"type\": \"bye\"}");
      state = CarrierConnectionState.CONNECTED;
      // Send http DELETE to http Carrier server.
      //sendWSSMessage("DELETE", "");
    }
    // Close Carrier in CONNECTED or ERROR states only.
    if (state == CarrierConnectionState.CONNECTED || state == CarrierConnectionState.ERROR) {
//      ws.getsCarrier().kill();
      state = CarrierConnectionState.CLOSED;

      // Wait for Carrier close event to prevent Carrier library from
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
    }
    Log.d(TAG, "Disconnecting Carrier done.");
  }

  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (state != CarrierConnectionState.ERROR) {
          state = CarrierConnectionState.ERROR;
          events.onCarrierError(errorMessage);
        }
      }
    });
  }

  // Asynchronously send POST/DELETE to Carrier server.
  private void sendWSSMessage(final String method, final String message) {
    String postUrl = callerUserId + "/" + calleeAddress + "/" + clientID;
    Log.d(TAG, "WS " + method + " : " + postUrl + " : " + message);
    AsyncHttpURLConnection httpConnection =
        new AsyncHttpURLConnection(method, postUrl, message, new AsyncHttpEvents() {
          @Override
          public void onHttpError(String errorMessage) {
            reportError("WS " + method + " error: " + errorMessage);
          }

          @Override
          public void onHttpComplete(String response) {}
        });
    httpConnection.send();
  }

  // Helper method for debugging purposes. Ensures that Carrier method is
  // called on a looper thread.
  private void checkIfCalledOnValidThread() {
    if (Thread.currentThread() != handler.getLooper().getThread()) {
      throw new IllegalStateException("Carrier method is not called on valid thread");
    }
  }

  private class CarrierObserver extends AbstractCarrierHandler {
    @Override
    public void onConnection(Carrier carrier, ConnectionStatus status) {
      Log.d(TAG, "Carrier connection opened to: " + calleeUserId);
      handler.post(new Runnable() {
        @Override
        public void run() {
          if (status == ConnectionStatus.Connected) {
            state = CarrierConnectionState.CONNECTED;
            // Check if we have pending register request.
            if (calleeAddress != null && clientID != null) {
              register(calleeAddress, callerAddress,  clientID);
            }
          }
        }
      });
    }


    @Override
    public void onFriendMessage(Carrier carrier, String from, byte[] message, boolean isOffline) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          if (state == CarrierConnectionState.CONNECTED
              || state == CarrierConnectionState.REGISTERED) {
            try {
              String message_= new String(message, "UTF-8");
              events.onCarrierMessage(message_);
              Log.d(TAG, "WSS->C: " + message_);
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
          }
        }
      });
    }

    @Override
    public void onReady(Carrier carrier) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(context, "carrier ready!!!", Toast.LENGTH_LONG).show();
        }
      });

    }

    @Override
    public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String message) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(context, "carrier onFriendRequest : " + userId, Toast.LENGTH_LONG).show();
          Log.e(TAG, "carrier onFriendRequest : " + userId);

          if (message != null && message.contains("msg")) { //通过添加好友的消息回执绕过carrier message 1024字符的限制
            if (state == CarrierConnectionState.CONNECTED
                    || state == CarrierConnectionState.REGISTERED) {
              events.onCarrierMessage(message);
              Log.d(TAG, "WSS->C: " + message);
            }
          }
        }

      });

    }

    @Override
    public void onFriendInviteRequest(Carrier carrier, String from, String data) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(context, "carrier friend invite onFriendInviteRequest from : " + from, Toast.LENGTH_LONG).show();
          Log.e(TAG, "carrier friend invite  onFriendInviteRequest from: " + from);

          if (data != null && data.contains("msg")) { //通过添加好友的消息回执绕过carrier message 1024字符的限制
            if (state == CarrierConnectionState.CONNECTED
                    || state == CarrierConnectionState.REGISTERED) {

              //更新calleeUserId,
              calleeUserId = from; //更新为消息回执者

              if(data.contains("offer")){
                events.onSdpReceived(from);
              }

              events.onCarrierMessage(data);
              Log.d(TAG, "WSS->C: " + data);
            }
          }
        }

      });
    }
  }


  private class CarrierMessageObserver implements FriendInviteResponseHandler {

    @Override
    public void onReceived(String from, int status, String reason, String data) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(context, "carrier friend invite onReceived from : " + from, Toast.LENGTH_LONG).show();
          Log.e(TAG, "carrier friend invite  onReceived from: " + from);
        }

      });

    }
  }
}
