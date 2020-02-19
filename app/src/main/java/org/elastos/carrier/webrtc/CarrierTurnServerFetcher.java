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
import android.util.Log;

import org.elastos.carrier.TurnServer;
import org.elastos.carrier.webrtc.model.SignalingParameters;
import org.elastos.carrier.webrtc.signaling.CarrierClient;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * AsyncTask that fetch turn server from carrier network.
 */
public class CarrierTurnServerFetcher {
  private static final String TAG = "CarrierTurnServer";
  private final CarrierTurnServerFetcherEvents events;
  private final String calleeAddress;
  private final String callerAddress;
  private boolean initiator;

  private Context context;

  private final Handler handler;

  /**
   * Carrier turn server fetcher callbacks.
   */
  public interface CarrierTurnServerFetcherEvents {
    /**
     * Callback fired once the signaling parameters
     * SignalingParameters are extracted.
     */
    void onSignalingParametersReady(final SignalingParameters params);

    /**
     * Callback for turn server initialization error.
     */
    void onSignalingParametersError(final String description);

  }


  public CarrierTurnServerFetcher(Context context, String calleeAddress, boolean initiator, String callerAddress,
                                  final CarrierTurnServerFetcherEvents events) {
    this.calleeAddress = calleeAddress;
    this.initiator = initiator;
    this.callerAddress  = callerAddress;
    this.events = events;
    this.context = context;

    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());

  }

  public void initializeTurnServer() {
    Log.d(TAG, "Get turn server from carrier network: ");

    handler.post(new Runnable() {
      @Override
      public void run() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        TurnServer turnServer = CarrierClient.getInstance(context).getTurnServer();
        iceServers.add(PeerConnection.IceServer.builder("stun:" + turnServer.getServer()).setUsername(turnServer.getUsername()).setPassword(turnServer.getPassword()).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:" + turnServer.getServer()).setUsername(turnServer.getUsername()).setPassword(turnServer.getPassword()).createIceServer());

        SignalingParameters params = new SignalingParameters(
                iceServers, initiator, calleeAddress, callerAddress, null, null);
        events.onSignalingParametersReady(params);

      }
    });

  }
}
