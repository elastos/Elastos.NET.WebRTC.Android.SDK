package com.allcom.elastos_webrtc.ui;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.allcom.elastos_webrtc.ElastosWebrtc;
import com.allcom.elastos_webrtc.ElastosWebrtcConfig;
import com.allcom.elastos_webrtc.R;

import org.webrtc.SurfaceViewRenderer;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CallActivity extends AppCompatActivity implements CallFragment.OnCallEvents, ReceiveCallFragment.OnCallEvents {
    public static CallActivity INSTANCE;
    private CallFragment callFragment;
    private ReceiveCallFragment receiveCallFragment;
    @Nullable
    private SurfaceViewRenderer pipRenderer;
    @Nullable
    private SurfaceViewRenderer fullscreenRenderer;
    boolean callControlFragmentVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INSTANCE = this;

        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_call);
        pipRenderer = findViewById(R.id.pip_video_view);
        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
        // init renderer
        ElastosWebrtc.getInstance().initVideoRenderer(pipRenderer, fullscreenRenderer);
        callFragment = new CallFragment();
        receiveCallFragment = new ReceiveCallFragment();
        callFragment.setArguments(getIntent().getExtras());
        receiveCallFragment.setArguments(getIntent().getExtras());

        fullscreenRenderer.setOnClickListener((View view) -> toggleCallControlFragmentVisibility());
        pipRenderer.setOnClickListener((View view) -> ElastosWebrtc.getInstance().switchRenderer());

        boolean outbound = getIntent().getBooleanExtra(ElastosWebrtc.ExtraKeys.INTENT_OUTBOUND, true);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.add(R.id.receive_call_fragment_container, receiveCallFragment);
        // ft.add(R.id.hud_fragment_container, hudFragment);
        if (!outbound) {
            ft.hide(callFragment);
        } else {
            ft.hide(receiveCallFragment);
        }
        ft.commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void toggleCallControlFragmentVisibility() {
        if (!callFragment.isAdded()) {
            return;
        }
        // Show/hide invite control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
            // ft.show(hudFragment);
        } else {
            ft.hide(callFragment);
            // ft.hide(hudFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    private void hideReceiveButtons() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
        }
        ft.hide(receiveCallFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    @Override
    public void onCallHangUp() {
        ElastosWebrtc.getInstance().hangup();
        finish();
    }

    @Override
    public void onCameraSwitch() {
        ElastosWebrtc.getInstance().switchCamera();
    }

    @Override
    public void toggleSpeaker(boolean speaker) {
        ElastosWebrtc.getInstance().setSpeaker(speaker);
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {

    }

    @Override
    public boolean onToggleMic() {
        return ElastosWebrtc.getInstance().toggleMic();
    }

    @Override
    public void onRejectCall() {
        ElastosWebrtc.getInstance().reject();
        ElastosWebrtc.getInstance().hangup();
        finish();
    }

    @Override
    public void onAnswerCall() {
        ElastosWebrtc.getInstance().acceptInvite();
        hideReceiveButtons();
    }

    @Override
    public void onSelectResolution(String rs) {
        ElastosWebrtcConfig.Resolution resolution = null;
        switch (rs) {
            case "480P":
                resolution = ElastosWebrtcConfig.Resolution.RS_480P;
                break;
            case "720P":
                resolution = ElastosWebrtcConfig.Resolution.RS_720P;
                break;
            case "1080P":
                resolution = ElastosWebrtcConfig.Resolution.RS_1080P;
                break;
            case "2K":
                resolution = ElastosWebrtcConfig.Resolution.RS_2K;
                break;
            case "4K":
                resolution = ElastosWebrtcConfig.Resolution.RS_4K;
                break;
        }
        ElastosWebrtc.getInstance().switchResolution(resolution);
    }
}
