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
interface Webrtc {

  /**
   * Invite a peer to join the webrtc call, the remote peer can choose to accept the invitation or reject.
   * @param peer EOS user id you want to call
   */
  void makeCall(String peer);

  /**
   * To accept a webrtc call invite.
   */
  void answerCall() ;

  /**
   * To reject the webrtc call invite.
   */
  void rejectCall();

  /**
   * get current call state
   * @return {@link CallState} current call state
   */
  CallState getCallState();

  /**
   * get remote user id you are talking to
   * @return {@link String} remote user id
   */
  String getRemoteUserId();

  /**
   * set video renderer, render video to you app
   * @param localRenderer {@link SurfaceViewRenderer} render local video
   * @param remoteRenderer {@link SurfaceViewRenderer} render remote video
   */
  void renderVideo(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer);

  /**
   * swap video renderer or not
   * @param isSwap
   *   swap local <-> remote video - true: swap local and remote video renderer otherwise false
   */
  void swapVideoRenderer(boolean isSwap);

  /**
   * <p>switch camera</p>
   */
  void switchCamera();

    /**
     * <p>reset video resolution, this might not </p>
     * @param width video width
     * @param height video height
     * @param fps fps
     */
  void setResolution(int width, int height, int fps);

    /**
     * enable or disable audio
     * @param enable {@code true} to enable audio; {@code false} to disable audio
     */
  void setAudioEnable(boolean enable);

    /**
     * enable or disable video
     * @param enable {@code true} to enable audio; {@code false} to disable audio
     */
  void setVideoEnable(boolean enable);

  /**
   * Disconnect from call.
   */
  void disconnect();

}
