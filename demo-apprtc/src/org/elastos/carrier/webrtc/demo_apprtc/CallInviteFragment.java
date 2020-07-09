/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.elastos.carrier.webrtc.demo_apprtc;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.elastos.carrier.webrtc.demo_apprtc.apprtc.R;

/**
 * Fragment for register control.
 */
public class CallInviteFragment extends Fragment {
  private TextView contactView;
  private ImageButton acceptButton;
  private ImageButton rejectButton;
  private OnInviteCallEvents callEvents;

  /**
   * Call control interface for container activity.
   */
  public interface OnInviteCallEvents {
    void onRejectCall();
    void onAcceptCall();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View controlView = inflater.inflate(R.layout.fragment_invite, container, false);

    // Create UI controls.
    contactView = controlView.findViewById(R.id.invite_contact_name_call);
    acceptButton = controlView.findViewById(R.id.button_call_accept);
    rejectButton = controlView.findViewById(R.id.button_call_reject);

    // Add buttons click events.
    acceptButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        callEvents.onAcceptCall();
      }
    });

    rejectButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        callEvents.onRejectCall();
      }
    });

    return controlView;
  }

  @Override
  public void onStart() {
    super.onStart();

    Bundle args = getArguments();
    if (args != null) {
      String contactName = args.getString(CallActivity.EXTRA_REMOTE_USER_ID);
      contactView.setText(contactName);
    }
  }

  // TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    callEvents = (OnInviteCallEvents) activity;
  }
}
