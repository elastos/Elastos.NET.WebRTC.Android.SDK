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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;


import com.allcom.elastos_webrtc_call.R;

import org.webrtc.RendererCommon.ScalingType;

/**
 * Fragment for invite control.
 */
public class CallFragment extends Fragment {
  private TextView contactView;
  private ImageButton cameraSwitchButton;
  private ImageButton volumeUpButton;
  private ImageButton toggleMuteButton;
  private Spinner resolutionSpinner;
//  private TextView captureFormatText;
//  private SeekBar captureFormatSlider;
  private OnCallEvents callEvents;
  private ScalingType scalingType;
  private boolean videoCallEnabled = true;
  private boolean speaker = false;

  /**
   * Call control interface for container activity.
   */
  public interface OnCallEvents {
    void onCallHangUp();
    void onCameraSwitch();
    void toggleSpeaker(boolean on);
    void onCaptureFormatChange(int width, int height, int framerate);
    boolean onToggleMic();
    void onSelectResolution(String resolution);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View controlView = inflater.inflate(R.layout.fragment_call, container, false);

    // Create UI controls.
    contactView = controlView.findViewById(R.id.contact_name_call);
    ImageButton disconnectButton = controlView.findViewById(R.id.button_call_disconnect);
    cameraSwitchButton = controlView.findViewById(R.id.button_call_switch_camera);
    volumeUpButton = controlView.findViewById(R.id.button_call_scaling_mode);
    volumeUpButton.setAlpha(speaker ? 1.0f : 0.3f);
    toggleMuteButton = controlView.findViewById(R.id.button_call_toggle_mic);
    resolutionSpinner = controlView.findViewById(R.id.resolution_spinner);
    loadResolutionSpinner();
//    captureFormatText = controlView.findViewById(R.id.capture_format_text_call);
//    captureFormatSlider = controlView.findViewById(R.id.capture_format_slider_call);

    // Add buttons click events.
    disconnectButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        callEvents.onCallHangUp();
      }
    });

    cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        callEvents.onCameraSwitch();
      }
    });

    volumeUpButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        speaker = !speaker;
        callEvents.toggleSpeaker(speaker);
        volumeUpButton.setAlpha(speaker ? 1.0f : 0.3f);
      }
    });
    scalingType = ScalingType.SCALE_ASPECT_FILL;

    toggleMuteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        boolean enabled = callEvents.onToggleMic();
        toggleMuteButton.setAlpha(enabled ? 1.0f : 0.3f);
      }
    });

    return controlView;
  }

  @Override
  public void onStart() {
    super.onStart();

//    boolean captureSliderEnabled = false;
    Bundle args = getArguments();
    if (args != null) {
      String contactName = args.getString("talk_to");
      contactView.setText(contactName);
//      videoCallEnabled = args.getBoolean(CallActivity.EXTRA_VIDEO_CALL, true);
//      captureSliderEnabled = videoCallEnabled
//          && args.getBoolean(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, false);
    }
//    if (!videoCallEnabled) {
//      cameraSwitchButton.setVisibility(View.INVISIBLE);
//    }
//    if (captureSliderEnabled) {
//      captureFormatSlider.setOnSeekBarChangeListener(
//          new CaptureQualityController(captureFormatText, callEvents));
//    } else {
//      captureFormatText.setVisibility(View.GONE);
//      captureFormatSlider.setVisibility(View.GONE);
//    }
  }

  // TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    callEvents = (OnCallEvents) activity;
  }

  private void loadResolutionSpinner() {
    final String[] items = {"480P", "720P", "1080P", "2K", "4K"};

    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getActivity(), R.layout.item_select, items);
    spinnerAdapter.setDropDownViewResource(R.layout.item_drop);
    resolutionSpinner.setAdapter(spinnerAdapter);
    resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String rs = spinnerAdapter.getItem(i);
        callEvents.onSelectResolution(rs);
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });
  }
}
