/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.allcom.elastos_webrtc_call.ui.call;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.allcom.elastos_webrtc_call.R;


/**
 * Fragment for invite control.
 */
public class ReceiveCallFragment extends Fragment {
  private ImageButton rejectButton;
  private ImageButton answerButton;
  private OnCallEvents callEvents;

  /**
   * Call control interface for container activity.
   */
  public interface OnCallEvents {
    void onRejectCall();
    void onAnswerCall();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View controlView = inflater.inflate(R.layout.fragment_receive_call, container, false);

    // Create UI controls.
    rejectButton = controlView.findViewById(R.id.button_call_reject);
    answerButton = controlView.findViewById(R.id.button_call_answer);

    rejectButton.setOnClickListener((View view) -> callEvents.onRejectCall());
    answerButton.setOnClickListener((View view) -> callEvents.onAnswerCall());

    return controlView;
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  // TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    callEvents = (OnCallEvents) activity;
  }
}
