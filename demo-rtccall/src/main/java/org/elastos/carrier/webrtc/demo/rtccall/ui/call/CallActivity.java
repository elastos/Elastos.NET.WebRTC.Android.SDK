package org.elastos.carrier.webrtc.demo.rtccall.ui.call;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.elastos.carrier.webrtc.CarrierWebrtcClient;
import org.elastos.carrier.webrtc.demo.rtccall.R;
import org.elastos.carrier.webrtc.demo.rtccall.util.AppRTCAudioManager;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.webrtc.CarrierPeerConnectionClient;
import org.elastos.carrier.webrtc.signaling.CarrierClient;
import org.elastos.carrier.webrtc.ui.BaseCallActivity;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CallActivity extends BaseCallActivity implements CallFragment.OnCallEvents, ReceiveCallFragment.OnCallEvents {
    private static final String TAG = "CallActivity";
    public static CallActivity INSTANCE;
    private CallFragment callFragment;
    private ReceiveCallFragment receiveCallFragment;
    @Nullable
    private SurfaceViewRenderer pipRenderer;
    @Nullable
    private SurfaceViewRenderer fullscreenRenderer;
    boolean callControlFragmentVisible = true;
    public static final String EXTRA_IS_CALLER = "org.elastos.apprtc.IS_CALLER";
    public static final String EXTRA_ROOMID = "org.elastos.apprtc.ROOMID";

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    // Peer connection statistics callback period in ms.
    private static final int STAT_CALLBACK_PERIOD = 1000;

    @Nullable
    private CarrierWebrtcClient.SignalingParameters signalingParameters;
    @Nullable private AppRTCAudioManager audioManager;
    @Nullable
    private VideoFileRenderer videoFileRenderer;
    private final List<VideoSink> remoteSinks = new ArrayList<>();
    private Toast logToast;
    private boolean commandLineRun;
    private boolean activityRunning;
    @Nullable
    private CarrierPeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private boolean connected;
    private boolean isError;
    private long callStartedTimeMs;
    private boolean micEnabled = true;
    private boolean screencaptureEnabled;
    private static Intent mediaProjectionPermissionResultData;
    private static int mediaProjectionPermissionResultCode;
    // True if local view is in the fullscreen renderer.
    private boolean isSwappedFeeds = false;

    private boolean isCaller;
    private String callerAddress;
    private String calleeAddress;
    private String remoteAddress;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INSTANCE = this;

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        // getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_call);
        pipRenderer = findViewById(R.id.pip_video_view);
        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
        // init renderer
        // ElastosWebrtc.getInstance().initVideoRenderer(pipRenderer, fullscreenRenderer);
        callFragment = new CallFragment();
        receiveCallFragment = new ReceiveCallFragment();
        callFragment.setArguments(getIntent().getExtras());
        receiveCallFragment.setArguments(getIntent().getExtras());

        fullscreenRenderer.setOnClickListener((View view) -> toggleCallControlFragmentVisibility());
        pipRenderer.setOnClickListener((View view) -> setSwappedFeeds(!isSwappedFeeds));


        final Intent intent = getIntent();
        isCaller = intent.getBooleanExtra(EXTRA_IS_CALLER, false);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.call_fragment_container, callFragment);
        ft.add(R.id.receive_call_fragment_container, receiveCallFragment);
        // ft.add(R.id.hud_fragment_container, hudFragment);
        if (!isCaller) {
            ft.hide(callFragment);
        } else {
            ft.hide(receiveCallFragment);
        }
        ft.commit();

        // Get Intent parameters.
        callerAddress = CarrierClient.getInstance(this).getMyAddress();
        if (isCaller) {
            remoteAddress = intent.getStringExtra(EXTRA_ROOMID);
            try {
                calleeAddress = Carrier.getInstance().getAddress();
            } catch (Exception e) {
                Log.e(TAG, "onCreate: ", e);
            }
        } else {
            calleeAddress = intent.getStringExtra(EXTRA_ROOMID); //calleeAddress
            remoteAddress = calleeAddress;
        }

        remoteSinks.add(remoteProxyRenderer);

        final EglBase eglBase = EglBase.create();
        // Create video renderers.
        pipRenderer.init(eglBase.getEglBaseContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
        // Start with local feed in fullscreen and swap it to the pip when the register is connected.
        setSwappedFeeds(true /* isSwappedFeeds */);
        Log.d(TAG, "Callee Address: " + calleeAddress);
        if ((calleeAddress == null || calleeAddress.length() == 0)){
            logAndToast("miss callee address");
            Log.e(TAG, "Incorrect Callee Address in intent!");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        int videoWidth = 1920;
        int videoHeight = 1080;
        peerConnectionParameters = peerConnectionParameters =
                new CarrierPeerConnectionClient.PeerConnectionParameters(true,
                        false, videoWidth, videoHeight, 30,
                        20, "h264",
                        true,
                        false,
                        0, "opus",
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false, null);
        // Create peer connection client.
        carrierPeerConnectionClient = new CarrierPeerConnectionClient(
                getApplicationContext(), eglBase, peerConnectionParameters, CallActivity.this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        Log.d(TAG, "onCreate:  carrierPeerConnectionClient = " + carrierPeerConnectionClient);
        carrierPeerConnectionClient.createPeerConnectionFactory(options);

        // 主叫直接呼叫，被叫接听后再呼.
        if (isCaller) {
            startCall();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        INSTANCE = null;
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
        // ElastosWebrtc.getInstance().hangup();
        if (webrtcClient != null) {
            webrtcClient.disconnectFromCall();
        }
        finish();
    }

    @Override
    public void onCameraSwitch() {
        // ElastosWebrtc.getInstance().switchCamera();
        if (carrierPeerConnectionClient != null) {
            carrierPeerConnectionClient.switchCamera();
        }
    }

    @Override
    public void toggleSpeaker(boolean speaker) {
        // ElastosWebrtc.getInstance().setSpeaker(speaker);
        // toggleSpeaker(speaker);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(speaker);
        }
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {

    }

    @Override
    public boolean onToggleMic() {
        // return ElastosWebrtc.getInstance().toggleMic();
        if (carrierPeerConnectionClient != null) {
            micEnabled = !micEnabled;
            carrierPeerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    @Override
    public void onRejectCall() {
        if (webrtcClient != null) {
            webrtcClient.disconnectFromCall();
        }
        finish();
    }

    @Override
    public void onAnswerCall() {
//        ElastosWebrtc.getInstance().acceptInvite();
        startCall();
        hideReceiveButtons();
    }

    @Override
    public void onSelectResolution(String rs) {
//        ElastosWebrtcConfig.Resolution resolution = null;
//        switch (rs) {
//            case "480P":
//                resolution = ElastosWebrtcConfig.Resolution.RS_480P;
//                break;
//            case "720P":
//                resolution = ElastosWebrtcConfig.Resolution.RS_720P;
//                break;
//            case "1080P":
//                resolution = ElastosWebrtcConfig.Resolution.RS_1080P;
//                break;
//            case "2K":
//                resolution = ElastosWebrtcConfig.Resolution.RS_2K;
//                break;
//            case "4K":
//                resolution = ElastosWebrtcConfig.Resolution.RS_4K;
//                break;
//        }
//        ElastosWebrtc.getInstance().switchResolution(resolution);
    }

    private void startCall() {
        if (webrtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a register.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        logAndToast("connect to: " + calleeAddress);
        webrtcClient.initialCall(callerAddress, calleeAddress);
        Log.d(TAG, "startCall: isCaller = " + isCaller + "; caller = " + callerAddress + "; callee = " + calleeAddress + "; remote = " + remoteAddress);
        if (isCaller) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                Log.e(TAG, "startCall: ", e);
            }
            String remoteId = CarrierClient.getInstance(this).getUserIdFromAddress(remoteAddress);
            webrtcClient.sendInvite(remoteId);
        }

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(getApplicationContext());
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (carrierPeerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        carrierPeerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        Log.e(TAG, "disconnect: ");
        activityRunning = false;
        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);
        if (webrtcClient != null) {
            webrtcClient.disconnectFromCall();
            webrtcClient = null;
        }
        if (pipRenderer != null) {
            pipRenderer.release();
            pipRenderer = null;
        }
        if (videoFileRenderer != null) {
            videoFileRenderer.release();
            videoFileRenderer = null;
        }
        if (fullscreenRenderer != null) {
            fullscreenRenderer.release();
            fullscreenRenderer = null;
        }
        if (carrierPeerConnectionClient != null) {
            carrierPeerConnectionClient.close();
            carrierPeerConnectionClient = null;
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (connected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("disconnect error")
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.setGravity(Gravity.TOP, 4, 4);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError) {
                    isError = true;
                    disconnectWithErrorMessage(description);
                }
            }
        });
    }

    private boolean captureToTexture() {
        return true;
    }

    @TargetApi(21)
    private @Nullable VideoCapturer createScreenCapturer() {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError("User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(
                mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                reportError("User revoked permission to capture the screen.");
            }
        });
    }

    private boolean useCamera2() {
        return true;
    }

    private @Nullable VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private @Nullable
    VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;
        String videoFileAsCamera = getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
        if (videoFileAsCamera != null) {
            try {
                videoCapturer = new FileVideoCapturer(videoFileAsCamera);
            } catch (IOException e) {
                reportError("Failed to open video file for emulated camera");
                return null;
            }
        } else if (screencaptureEnabled) {
            return createScreenCapturer();
        } else if (useCamera2()) {
            if (!captureToTexture()) {
                reportError("get camera error");
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);
    }

    private void onConnectedToCallInternal(final CarrierWebrtcClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        Log.d(TAG, "onConnectedToCallInternal: carrierPeerConnectionClient = " + carrierPeerConnectionClient);
        Log.d(TAG, "onConnectedToCallInternal: localProxyVideoSink = " + localProxyVideoSink);
        Log.d(TAG, "onConnectedToCallInternal: videoCapturer" + videoCapturer);
        carrierPeerConnectionClient.createPeerConnection(this,
                localProxyVideoSink, remoteSinks, videoCapturer);

        if (signalingParameters.initiator) {
            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            carrierPeerConnectionClient.createOffer();
        } else {
            if (params.offerSdp != null) {
                carrierPeerConnectionClient.setRemoteDescription(params.offerSdp);
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                carrierPeerConnectionClient.createAnswer();
            }
            if (params.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : params.iceCandidates) {
                    carrierPeerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }
    }


    // -----Implementation of WebrtcClient.SignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    @Override
    public void onCallInitialized(final CarrierWebrtcClient.SignalingParameters params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectedToCallInternal(params);
            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (carrierPeerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                carrierPeerConnectionClient.setRemoteDescription(sdp);
                if (!signalingParameters.initiator) {
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    carrierPeerConnectionClient.createAnswer();
                }
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (carrierPeerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                    return;
                }
                carrierPeerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (carrierPeerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                    return;
                }
                carrierPeerConnectionClient.removeRemoteIceCandidates(candidates);
            }
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                disconnect();
            }
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    @Override
    public void onCreateOffer() {
        carrierPeerConnectionClient.createOffer();
    }

    // -----Implementation of CarrierPeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (webrtcClient != null) {
                    logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                    if (signalingParameters.initiator) {
                        webrtcClient.sendOfferSdp(sdp);
                    } else {
                        webrtcClient.sendAnswerSdp(sdp);
                    }
                }
                if (peerConnectionParameters.videoMaxBitrate > 0) {
                    Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                    carrierPeerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (webrtcClient != null) {
                    webrtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (webrtcClient != null) {
                    webrtcClient.sendLocalIceCandidateRemovals(candidates);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
            }
        });
    }

    @Override
    public void onConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("DTLS connected, delay=" + delta + "ms");
                connected = true;
                callConnected();
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("DTLS disconnected");
                connected = false;
                disconnect();
            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {}

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: ");
            }
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }
}
