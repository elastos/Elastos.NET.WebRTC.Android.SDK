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

package org.elastos.carrier.webrtc.model;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Struct holding the signaling parameters of an webrtc communication.
 */
public class SignalingParameters {
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
