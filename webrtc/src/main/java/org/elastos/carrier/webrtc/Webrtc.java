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

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

/**
 * Webrtc is the interface representing an Carrier webrtc client.
 */
public interface Webrtc {

  /**
   * Invite a peer to join the webrtc call, the peer can choose to accept the invitation or reject.
   */
  void inviteCall(String peer) ;

  /**
   * The peer accept a webrtc call invite. Once accept the invite, the caller will send offer to the peer,
   * and also the SignalingEvents.onCallInviteAccepted() will be fired.
   */
  void acceptCallInvite() ;

  /**
   * The peer reject the webrtc call invite.
   */
  void rejectCallInvite();

  /**
   * set video renderer, this should be called before inviteCall or acceptCallInvite
   * @param localRenderer
   * @param remoteRenderer
   */
  void renderVideo(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer);

  /**
   * swap video renderer or not
   * @param isSwap
   */
  void swapVideoRenderer(boolean isSwap);

  /**
   * <p>switch camera</p>
   */
  void switchCamera();

    /**
     * <p>reset video resolution</p>
     * @param width video width
     * @param height video height
     * @param fps fps
     */
  void setResolution(int width, int height, int fps);

    /**
     * enable or disable audio
     * @param enable true - enable audio; false - disable audio
     */
  void setAudioEnable(boolean enable);

    /**
     * enable or disable video
     * @param enable true - enable video; false - disable video
     */
  void setVideoEnable(boolean enable);

    /**
     * <p>this will release everything</p>
     */
  void destroy();

  /**
   * Send offer SDP to the other participant.
   */
  void sendOfferSdp(final SessionDescription sdp);

  /**
   * Send answer SDP to the other participant.
   */
  void sendAnswerSdp(final SessionDescription sdp);

  /**
   * Send Ice candidate to the other participant.
   */
  void sendLocalIceCandidate(final IceCandidate candidate);

  /**
   * Send removed ICE candidates to the other participant.
   */
  void sendLocalIceCandidateRemovals(final IceCandidate[] candidates);

  /**
   * Disconnect from call.
   */
  void disconnectFromCall();

  /**
   * Callback interface for messages delivered on signaling channel.
   *
   * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
   */
  interface SignalingEvents {
    /**
     * Callback fired once webrtc call invited by remote peer.
     */
    void onCallInvited(final WebrtcClient.SignalingParameters params);

    /**
     * Callback fired once webrtcClient.initialCall() has been invoked.
     */
    void onCallInitialized(final WebrtcClient.SignalingParameters params);

    /**
     * Callback fired once remote SDP is received.
     */
    void onRemoteDescription(final SessionDescription sdp);

    /**
     * Callback fired once remote Ice candidate is received.
     */
    void onRemoteIceCandidate(final IceCandidate candidate);

    /**
     * Callback fired once remote Ice candidate removals are received.
     */
    void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

    /**
     * Callback fired once channel is closed.
     */
    void onChannelClose();

    /**
     * Callback fired once channel error happened.
     */
    void onChannelError(final String description);

    void onCreateOffer();
  }
}
