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
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import org.elastos.apprtc.AppRTCClient.SignalingParameters;
import org.elastos.carrier.TurnServer;
import org.elastos.carrier.webrtc.signaling.carrier.CarrierClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

/**
 * AsyncTask that converts an AppRTC room URL into the set of signaling
 * parameters to use with that room.
 */
public class RoomParametersFetcher {
  private static final String TAG = "RoomRTCClient";
  private static final int TURN_HTTP_TIMEOUT_MS = 5000;
  private final RoomParametersFetcherEvents events;
  private final String roomUrl;
  private final String calleeAddress;
  private final String callerAddress;
  private final String roomMessage;
  private boolean initiator;

  private Context context;

  private final Handler handler;

  /**
   * Room parameters fetcher callbacks.
   */
  public interface RoomParametersFetcherEvents {
    /**
     * Callback fired once the room's signaling parameters
     * SignalingParameters are extracted.
     */
    void onSignalingParametersReady(final SignalingParameters params);

    /**
     * Callback for room parameters extraction error.
     */
    void onSignalingParametersError(final String description);
  }

  public RoomParametersFetcher(Context context, String calleeAddress, boolean initiator, String callerAddress,
                               String roomUrl, String roomMessage, final RoomParametersFetcherEvents events) {
    this.calleeAddress = calleeAddress;
    this.initiator = initiator;
    this.callerAddress  = callerAddress;
    this.roomUrl = roomUrl;
    this.roomMessage = roomMessage;
    this.events = events;
    this.context = context;

    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());

  }

  public void makeRequest() {
    Log.d(TAG, "Connecting to carrier address: " + calleeAddress);
/*
    AsyncHttpURLConnection httpConnection =
        new AsyncHttpURLConnection("POST", roomUrl, roomMessage, new AsyncHttpEvents() {
          @Override
          public void onHttpError(String errorMessage) {
            Log.e(TAG, "Room connection error: " + errorMessage);
            events.onSignalingParametersError(errorMessage);
          }

          @Override
          public void onHttpComplete(String response) {
            roomHttpResponseParse(response);
          }
        });
    httpConnection.send();
*/

    //addFriend to calleeAddress

    handler.post(new Runnable() {
      @Override
      public void run() {
        String response = "{\n" +
                "  \"params\": {\n" +
                //"    \"is_initiator\": \"true\",\n" +
                "    \"room_link\": \"https://appr.tc/r/695357195\",\n" +
                "    \"version_info\": \"{\\\"gitHash\\\": \\\"7341b731567cfcda05079363fb27de88c22059cf\\\", \\\"branch\\\": \\\"master\\\", \\\"time\\\": \\\"Mon Sep 23 10:45:26 2019 +0200\\\"}\",\n" +
                "    \"messages\": [],\n" +
                "    \"error_messages\": [],\n" +
                "    \"offer_options\": \"{}\",\n" +
                "    \"client_id\": \"72907660\",\n" +
                "    \"ice_server_transports\": \"\",\n" +
                "    \"bypass_join_confirmation\": \"false\",\n" +
                "    \"media_constraints\": \"{\\\"audio\\\": true, \\\"video\\\": {\\\"optional\\\": [{\\\"minWidth\\\": \\\"1280\\\"}, {\\\"minHeight\\\": \\\"720\\\"}], \\\"mandatory\\\": {}}}\",\n" +
                "    \"include_loopback_js\": \"\",\n" +
                "    \"is_loopback\": \"false\",\n" +
                "    \"wss_url\": \"wss://apprtc-ws.webrtc.org:443/ws\",\n" +
                "    \"pc_constraints\": \"{\\\"optional\\\": []}\",\n" +
                "    \"pc_config\": \"{\\\"rtcpMuxPolicy\\\": \\\"require\\\", \\\"bundlePolicy\\\": \\\"max-bundle\\\", \\\"iceServers\\\": []}\",\n" +
                "    \"wss_post_url\": \"https://apprtc-ws.webrtc.org:443\",\n" +
                "    \"ice_server_url\": \"https://networktraversal.googleapis.com/v1alpha/iceconfig?key=AIzaSyA2WoxRAjLTwrD7upuk9N2qdlcOch3D2wU\",\n" +
                "    \"warning_messages\": [],\n" +
                "    \"room_id\": \"695357195\"\n" +
                "  },\n" +
                "  \"result\": \"SUCCESS\"\n" +
                "}";
        roomHttpResponseParse(context, response.replaceAll("695357195", calleeAddress));
      }
    });

  }

  private void roomHttpResponseParse(Context context, String response) {
    //Log.d(TAG, "Room response: " + response);
    try {
      List<IceCandidate> iceCandidates = null;
      SessionDescription offerSdp = null;
      JSONObject roomJson = new JSONObject(response);

      String result = roomJson.getString("result");
      if (!result.equals("SUCCESS")) {
        events.onSignalingParametersError("Room response error: " + result);
        return;
      }
      response = roomJson.getString("params");
      roomJson = new JSONObject(response);
      String roomId = roomJson.getString("room_id");
      String clientId = roomJson.getString("client_id");
      String wssUrl = roomJson.getString("wss_url");
      String wssPostUrl = roomJson.getString("wss_post_url");
      //boolean initiator = (roomJson.getBoolean("is_initiator"));
      if (!initiator) {
        iceCandidates = new ArrayList<>();
        String messagesString = roomJson.getString("messages");
        JSONArray messages = new JSONArray(messagesString);
        for (int i = 0; i < messages.length(); ++i) {
          String messageString = messages.getString(i);
          JSONObject message = new JSONObject(messageString);
          String messageType = message.getString("type");
          Log.d(TAG, "GAE->C #" + i + " : " + messageString);
          if (messageType.equals("offer")) {
            offerSdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(messageType), message.getString("sdp"));
          } else if (messageType.equals("candidate")) {
            IceCandidate candidate = new IceCandidate(
                message.getString("id"), message.getInt("label"), message.getString("candidate"));
            iceCandidates.add(candidate);
          } else {
            Log.e(TAG, "Unknown message: " + messageString);
          }
        }
      }
      Log.d(TAG, "CalleeAddress: " + roomId + ". ClientId: " + clientId);
      Log.d(TAG, "Initiator: " + initiator);
      //Log.d(TAG, "WSS url: " + wssUrl);
      //Log.d(TAG, "WSS POST url: " + wssPostUrl);

      List<PeerConnection.IceServer> iceServers =
          iceServersFromPCConfigJSON(roomJson.getString("pc_config"));
      boolean isTurnPresent = false;
      for (PeerConnection.IceServer server : iceServers) {
        Log.d(TAG, "IceServer: " + server);
        for (String uri : server.urls) {
          if (uri.startsWith("turn:")) {
            isTurnPresent = true;
            break;
          }
        }
      }
      // Request TURN servers.
      try {
        if (!isTurnPresent && !roomJson.optString("ice_server_url").isEmpty()) {
          List<PeerConnection.IceServer> turnServers =
              requestTurnServers(roomJson.getString("ice_server_url"));
          for (PeerConnection.IceServer turnServer : turnServers) {
            Log.d(TAG, "TurnServer: " + turnServer);
            iceServers.add(turnServer);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      } catch (JSONException e) {
        e.printStackTrace();
      }

      //iceServers.add(PeerConnection.IceServer.builder("stun:gfax.net:3478").createIceServer());

      TurnServer turnServer = CarrierClient.getInstance(this.context, null).getTurnServer();
      iceServers.add(PeerConnection.IceServer.builder("stun:" + turnServer.getServer()).setUsername(turnServer.getUsername()).setPassword(turnServer.getPassword()).createIceServer());
      iceServers.add(PeerConnection.IceServer.builder("turn:" + turnServer.getServer()).setUsername(turnServer.getUsername()).setPassword(turnServer.getPassword()).createIceServer());

      SignalingParameters params = new SignalingParameters(
          iceServers, initiator, clientId, roomId, callerAddress, offerSdp, iceCandidates);
      events.onSignalingParametersReady(params);
    } catch (JSONException e) {
      events.onSignalingParametersError("Room JSON parsing error: " + e.toString());
    } catch (Exception e) {
      events.onSignalingParametersError("Room IO error: " + e.toString());
    }
  }



  //todo: 需要替换为Elastos的turn ice server.

  // Requests & returns a TURN ICE Server based on a request URL.  Must be run
  // off the main thread!
  private List<PeerConnection.IceServer> requestTurnServers(String url)
      throws IOException, JSONException {
    List<PeerConnection.IceServer> turnServers = new ArrayList<>();
    Log.d(TAG, "Request TURN from: " + url);
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setDoOutput(true);
    connection.setRequestProperty("REFERER", "https://appr.tc");
    connection.setConnectTimeout(TURN_HTTP_TIMEOUT_MS);
    connection.setReadTimeout(TURN_HTTP_TIMEOUT_MS);
    int responseCode = connection.getResponseCode();
    if (responseCode != 200) {
      throw new IOException("Non-200 response when requesting TURN server from " + url + " : "
          + connection.getHeaderField(null));
    }
    InputStream responseStream = connection.getInputStream();
    String response = drainStream(responseStream);
    connection.disconnect();
    Log.d(TAG, "TURN response: " + response);
    JSONObject responseJSON = new JSONObject(response);
    JSONArray iceServers = responseJSON.getJSONArray("iceServers");
    for (int i = 0; i < iceServers.length(); ++i) {
      JSONObject server = iceServers.getJSONObject(i);
      JSONArray turnUrls = server.getJSONArray("urls");
      String username = server.has("username") ? server.getString("username") : "";
      String credential = server.has("credential") ? server.getString("credential") : "";
      for (int j = 0; j < turnUrls.length(); j++) {
        String turnUrl = turnUrls.getString(j);
        PeerConnection.IceServer turnServer =
            PeerConnection.IceServer.builder(turnUrl)
              .setUsername(username)
              .setPassword(credential)
              .createIceServer();
        turnServers.add(turnServer);
      }
    }
    return turnServers;
  }

  // Return the list of ICE servers described by a WebRTCPeerConnection
  // configuration string.
  private List<PeerConnection.IceServer> iceServersFromPCConfigJSON(String pcConfig)
      throws JSONException {
    JSONObject json = new JSONObject(pcConfig);
    JSONArray servers = json.getJSONArray("iceServers");
    List<PeerConnection.IceServer> ret = new ArrayList<>();
    for (int i = 0; i < servers.length(); ++i) {
      JSONObject server = servers.getJSONObject(i);
      String url = server.getString("urls");
      String credential = server.has("credential") ? server.getString("credential") : "";
        PeerConnection.IceServer turnServer =
            PeerConnection.IceServer.builder(url)
              .setPassword(credential)
              .createIceServer();
      ret.add(turnServer);
    }
    return ret;
  }

  // Return the contents of an InputStream as a String.
  private static String drainStream(InputStream in) {
    Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
