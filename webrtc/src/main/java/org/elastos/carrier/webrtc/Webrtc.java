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

import org.elastos.carrier.webrtc.call.CallState;
import org.webrtc.SurfaceViewRenderer;

/**
 * Webrtc is the interface representing an Carrier webrtc client.
 */
public interface Webrtc {

  /**
   * Invite a peer to join the webrtc call, the peer can choose to accept the invitation or reject.
   */
  void inviteCall(String peer);

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
   * get current call state
   * @return
   */
  CallState getCallState();

  /**
   * get remote user id where you are talking to
   * @return
   */
  String getRemoteUserId();

  /**
   * set video renderer
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
   * Disconnect from call.
   */
  void disconnect();

}
