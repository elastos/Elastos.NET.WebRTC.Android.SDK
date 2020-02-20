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

package org.elastos.carrier.webrtc.signaling;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.UserInfo;
import org.elastos.carrier.exceptions.CarrierException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrier message channel client implementation.
 *
 * <p>All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */
public class CarrierChannelClient {
  private static final String TAG = "CarrierChannelClient";
  private static final int CLOSE_TIMEOUT = 1000;
  private final CarrierChannelEvents events;
  private final Handler handler;

  private CarrierClient carrierClient;
  private String calleeUserId;
  private String callerUserId;
  @Nullable
  private String calleeAddress;
  private String callerAddress;

  private CarrierConnectionState state;
  // Do not remove this member variable. If this is removed, the observer gets garbage collected and
  // this causes test breakages.
  private CarrierObserver carrierObserver;
  private final Object closeEventLock = new Object();
  private boolean closeEvent;
  // Carrier send queue. Messages are added to the queue when Carrier
  // client is not registered and are consumed in register() register.
  private final List<String> msgSendQueue = new ArrayList<>();

  private Context context;

  private FriendInviteResponseHandler friendInviteResponseHandler;

  public boolean isInitiator(String address) {
    String myAddress = carrierClient.getMyAddress();
    return address!=null && !address.equals(myAddress);
  }

  public String getMyCarrierAddress() {
    return carrierClient.getMyAddress();
  }

  public Carrier getCarrier() {
    return carrierClient.getCarrier();
  }

  public CarrierChannelClient(Handler handler, CarrierChannelEvents events, Context context) {
    this.handler = handler;
    this.events = events;
    calleeAddress = null;
    callerAddress = null;
    state = CarrierConnectionState.NEW;
    this.context = context;

    carrierObserver = new CarrierObserver();
    friendInviteResponseHandler = new CarrierMessageObserver();
    carrierClient = CarrierClient.getInstance(context);
    //carrierClient.setmHandler(handler);
    carrierClient.addCarrierHandler(carrierObserver);
    carrierClient.setFriendInviteResponseHandler(friendInviteResponseHandler);
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

    CarrierClient cf = CarrierClient.getInstance(context);

    calleeUserId = cf.getUserIdFromAddress(calleeAddress);
    callerUserId = cf.getUserIdFromAddress(callerAddress);
    closeEvent = false;

    try {
      if (!calleeAddress.equals(callerAddress)) {
        carrierClient.addFriend(calleeAddress); //if carrier node can add friend, then it's status should be connected.
      }
      state = CarrierConnectionState.CONNECTED;
    } catch (CarrierException e) {
      reportError("addFriend connection error: " + e.getMessage());
    }

  }

  //register
  public void register(final String calleeAddress, final String callerAddress) {
    checkIfCalledOnValidThread();
    this.calleeAddress = calleeAddress;
    if (state != CarrierConnectionState.CONNECTED) {
      Log.w(TAG, "Carrier register() in state " + state);
      return;
    }
    Log.d(TAG, "Registering Carrier for callee " + calleeAddress);

    state = CarrierConnectionState.REGISTERED;
    // Send any previously accumulated messages.
    for (String sendMessage : msgSendQueue) {
      send(sendMessage);
    }
    msgSendQueue.clear();
  }



  //send message
  public void send(String message) {
    checkIfCalledOnValidThread();
    switch (state) {
      case NEW:
      case CONNECTED:
        // Store outgoing messages and send them after Carrier client
        // is registered.
        Log.d(TAG, "C ACC: " + message);
        msgSendQueue.add(message);
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
          json.put("calleeAddress", calleeAddress);
          message = json.toString();

          Log.d(TAG, "C->Call: " + message);

          if (calleeUserId.equals(callerUserId)) {
            return; //因为carrier不能发送消息给自己的限制，被呼叫方进入无需发送消息给自己，等待offer.
          }
          carrierClient.sendMessageByInvite(calleeUserId, message);

        } catch (JSONException e) {
          reportError("Carrier send JSON error: " + e.getMessage());
        } catch (CarrierException e) {
          e.printStackTrace();
          reportError("carrier send message error: " + e.getMessage());
        }
        break;
    }
  }

  //send message
  public void send(String to, String message) {
    checkIfCalledOnValidThread();
    JSONObject json = new JSONObject();
    try {
      json.put("cmd", "send");
      json.put("msg", message);
      message = json.toString();

      Log.d(TAG, "send_to: " + to + "; message: " + message);

      if (to.equals(callerUserId)) {
        return; //因为carrier不能发送消息给自己的限制，被呼叫方进入无需发送消息给自己，等待offer.
      }
      carrierClient.sendMessageByInvite(to, message);
    } catch (JSONException e) {
      Log.e(TAG, "send_to: " + to + "; message: " + message, e);
      reportError("Carrier send JSON error: " + e.getMessage());
    } catch (CarrierException e) {
      e.printStackTrace();
      Log.e(TAG, "send_to: " + to + "; message: " + message, e);
      reportError("carrier send message error: " + e.getMessage());
    }
  }


  public void disconnect(boolean waitForComplete) {
    checkIfCalledOnValidThread();
    Log.d(TAG, "Disconnect Carrier. State: " + state);
    if (state == CarrierConnectionState.REGISTERED) {
      // Send "bye" to Carrier server.
      send("{\"type\": \"bye\"}");
      state = CarrierConnectionState.CONNECTED;
    }
    // Close Carrier in CONNECTED or ERROR states only.
    if (state == CarrierConnectionState.CONNECTED || state == CarrierConnectionState.ERROR) {
      //carrierClient.getsCarrier().kill(); todo: do we need to kill the carrier instance?
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
            if (calleeAddress != null) {
              register(calleeAddress, callerAddress);
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
