package com.allcom.elastos_webrtc;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.allcom.elastos_webrtc.impl.PeerObserverImpl;
import com.allcom.elastos_webrtc.impl.ProxyVideoSink;
import com.allcom.elastos_webrtc.impl.SdpObserverImpl;
import com.allcom.elastos_webrtc.listener.CallHandler;
import com.allcom.elastos_webrtc.support.FailReason;
import com.allcom.elastos_webrtc.support.RejectReason;
import com.allcom.elastos_webrtc.ui.CallActivity;
import com.allcom.elastos_webrtc.util.CandidateKey;
import com.allcom.elastos_webrtc.util.SignalMessageHeader;
import com.allcom.elastos_webrtc.util.SignalMessageType;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierHandler;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.TurnServer;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

/**
 * <p> this is the main class for video call
 */
public class ElastosWebrtc {
    private static final String TAG = "ElastosWebrtc";
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ElastosWebrtcConfig config;
    private Activity activity;
    private Carrier carrier;
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private SurfaceTextureHelper surfaceTextureHelper;
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    private final ProxyVideoSink remoteProxyVideoSink = new ProxyVideoSink();
    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private VideoTrack remoteVideoTrack;
    private AudioTrack localAudioTrack;
    private SurfaceViewRenderer localVideoRenderer;
    private SurfaceViewRenderer remoteVideoRenderer;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private String talkTo;
    private boolean rendererReverse = false;
    private boolean micEnabled = true;
    private AudioManager audioManager;
    private CallHandler callHandler;


    private ElastosWebrtc() {
    }

    /**
     * <p> get {@link ElastosWebrtc} singleton instance
     *
     * @return {@link ElastosWebrtc} singleton instance
     */
    public static ElastosWebrtc getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p> set necessary object
     *
     * @param c {@link ElastosWebrtcConfig} config
     * @param activity {@link Activity} activity
     * @param carrier {@link Carrier} elastos carrier
     * @param callHandler {@link CallHandler} CallHandler implementation
     */
    public void initialize(@NonNull ElastosWebrtcConfig c, @NonNull Activity activity, @NonNull Carrier carrier, @NonNull CallHandler callHandler) {
        this.config = c;
        this.activity = activity;
        this.carrier = carrier;
        this.eglBase = EglBase.create();
        this.callHandler = callHandler;

        this.createPeerConnectionFactory();
        this.createAudioManager();
    }

    /**
     * <p>send video call invite to userId, the user must be your friend and online.
     *
     * @param userId elastos userId
     */
    public void invite(String userId) {
        try {
            if (!carrier.isFriend(userId)) {
                return;
            }
            talkTo = userId;
            Intent intent = new Intent(activity, CallActivity.class);
            intent.putExtra(ExtraKeys.INTENT_OUTBOUND, true);
            intent.putExtra(ExtraKeys.TALK_TO, userId);
            activity.startActivity(intent);

            sendMessage(userId, SignalMessageType.INVITE, null, null, new FriendInviteResponseHandler() {
                @Override
                public void onReceived(String from, int status, String reason, String data) {
                    Log.d(TAG, String.format("onReceived send invite result: %s %d %s %s", from, status, reason, data));
                    if (status != 0) {
                        callHandler.onFail(FailReason.INVITE_FAIL);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "invite: ", e);
        }
    }

    /**
     * <p> to accept someone's video call invite
     */
    public void acceptInvite() {
        sendMessage(talkTo, SignalMessageType.ACCEPT_INVITE, null, null, new FriendInviteResponseHandler() {
            @Override
            public void onReceived(String from, int status, String reason, String data) {
                Log.d(TAG, String.format("onReceived send accept_invite result: %s %d %s %s", from, status, reason, data));
            }
        });
    }

    /**
     * <p>reject video call invite
     */
    public void reject() {
        sendMessage(talkTo, SignalMessageType.REJECT_INVITE, null, null, new FriendInviteResponseHandler() {
            @Override
            public void onReceived(String from, int status, String reason, String data) {
                Log.d(TAG, String.format("onReceived send reject result: %s %d %s %s", from, status, reason, data));
            }
        });
    }

    /**
     * <p> receive someone's FriendInvite message, this should be called in {@link CarrierHandler#onFriendInviteRequest(Carrier, String, String)}
     *
     * @param from message sender
     * @param data message body
     */
    public void onReceiveFriendInvite(String from, String data) {
        Log.i(TAG, String.format("onReceiveFriendInvite: from = %s", from));
        try {
            // 这里只处理特定格式的json数据
            JSONObject json = new JSONObject(data);
            if (json != null && json.has(SignalMessageHeader.webrtc_message_type.name())) {
                SignalMessageType type = SignalMessageType.valueOf(json.getInt(SignalMessageHeader.webrtc_message_type.name()));
                String sdpType = null;
                if (json.has(SignalMessageHeader.webrtc_sdp_type.name())) {
                    sdpType = json.getString(SignalMessageHeader.webrtc_sdp_type.name());
                }
                String body = null;
                if (json.has(SignalMessageHeader.webrtc_message_body.name())) {
                    body = json.getString(SignalMessageHeader.webrtc_message_body.name());
                }
                switch (type) {
                    case INVITE:
                        Log.d(TAG, "onReceiveFriendInvite: receive invite message");
                        handleInvite(from, body);
                        break;
                    case ACCEPT_INVITE:
                        Log.d(TAG, String.format("onReceiveFriendInvite: %s accept invite", from));
                        handleAcceptInvite(from, body);
                        break;
                    case REJECT_INVITE:
                        Log.d(TAG, "handleMessage: reject invite");
                        handleReject(from, body);
                        break;
                    case OFFER_SDP:
                        Log.d(TAG, "handleMessage: offer sdp");
                        handleOffer(from, sdpType, body);
                        break;
                    case ANSWER_SDP:
                        Log.d(TAG, "handleMessage: acceptInvite sdp");
                        handleAnswer(from, sdpType, body);
                        break;
                    case CANDIDATE:
                        Log.d(TAG, "handleMessage: receive candidate");
                        handleCandidate(from, body);
                        break;
                    case DISCONNECT:
                        Log.d(TAG, "onReceiveFriendInvite: disconnect");
                        handleDisconnect(from, body);
                    default:
                        Log.d(TAG, "handleMessage: other message");
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onReceiveFriendInvite: ", e);
        }
    }

    /**
     * <p> send message to userId
     *
     * @param userId user id
     * @param type {@link SignalMessageType} message type
     * @param sdpType sdp type: answer or offer
     * @param message message body
     * @param handler message send result
     */
    public void sendMessage(String userId, SignalMessageType type, String sdpType, String message, FriendInviteResponseHandler handler) {
        JSONObject inviteJson = new JSONObject();
        try {
            inviteJson.put(SignalMessageHeader.webrtc_message_type.name(), type.getType());
            inviteJson.put(SignalMessageHeader.webrtc_message_from.name(), carrier.getUserId());
            if (!TextUtils.isEmpty(sdpType)) {
                inviteJson.put(SignalMessageHeader.webrtc_sdp_type.name(), sdpType);
            }
            inviteJson.put(SignalMessageHeader.webrtc_message_body.name(), message);

            carrier.inviteFriend(userId, inviteJson.toString(), handler);
        } catch (Exception e) {
            Log.e(TAG, "sendMessage: format json invite message error", e);
        }
    }

    /**
     * <p> set video renderer, this should be called before {@link ElastosWebrtc#acceptInvite()}
     * <code>
     *
     *     <org.webrtc.SurfaceViewRenderer
     *         android:id="@+id/remoteRenderer"
     *         android:layout_width="wrap_content"
     *         android:layout_height="wrap_content"
     *         android:layout_gravity="center" />
     *
     *     <org.webrtc.SurfaceViewRenderer
     *         android:id="@+id/localRenderer"
     *         android:layout_height="144dp"
     *         android:layout_width="wrap_content"
     *         android:layout_gravity="bottom|end"
     *         android:layout_margin="16dp"/>
     *
     * @param localRenderer {@link SurfaceViewRenderer} local video renderer
     * @param remoteRenderer {@link SurfaceViewRenderer} remote video renderer
     */
    public void initVideoRenderer(SurfaceViewRenderer localRenderer, SurfaceViewRenderer remoteRenderer) {
        localRenderer.init(eglBase.getEglBaseContext(), null);
        localRenderer.setZOrderMediaOverlay(true);
        localRenderer.setEnableHardwareScaler(true);
        localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        remoteRenderer.init(eglBase.getEglBaseContext(), null);
        remoteRenderer.setEnableHardwareScaler(false);
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        localVideoRenderer = localRenderer;
        remoteVideoRenderer = remoteRenderer;
        refreshRenderer(false);
    }

    /**
     * <p> toggle camera to front or back, default with front camera
     */
    public void switchCamera() {
        executor.execute(this::switchCameraInternal);
    }

    /**
     * <p> switch local and remote video renderer
     */
    public void switchRenderer() {
        refreshRenderer(!rendererReverse);
    }

    /**
     * <p> change local video resolution
     * @param resolution {@link ElastosWebrtcConfig.Resolution} video resolution
     */
    public void switchResolution(ElastosWebrtcConfig.Resolution resolution) {
        if (resolution == null) {
            Log.d(TAG, "switchResolution: resolution is null");
        }
        Log.d(TAG, "switchResolution: " + resolution.getDescription());
        if (videoSource != null) {
            videoSource.adaptOutputFormat(resolution.getWidth(), resolution.getHeight(), resolution.getFps());
        }
    }

    /**
     * <p> toggle mic on|off
     * @return mic status
     */
    public boolean toggleMic() {
        micEnabled = !micEnabled;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(micEnabled);
        }

        return micEnabled;
    }

    /**
     * <p> set speaker on|off
     *
     * @param on true|false
     */
    public void setSpeaker(boolean on) {
        NotificationManager notificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted()) {
            activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            return;
        }

        audioManager.setSpeakerphoneOn(on);
        if (on) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM), AudioManager.FX_KEY_CLICK);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(AudioManager.MODE_IN_CALL);
            }
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.FX_KEY_CLICK);
        }
    }

    /**
     * <p> disconnect video call
     *
     * @param send send disconnect message to remote user or not
     */
    public void disconnect(boolean send) {
        Log.d(TAG, "disconnect: ");
        if (send) {
            sendMessage(talkTo, SignalMessageType.DISCONNECT, null, null, (String from, int status, String reason, String data) -> {
                Log.d(TAG, String.format("disconnect result: %s %d %s %s", from, status, reason, data));
            });
        }

        try {
            if (localProxyVideoSink != null) {
                localProxyVideoSink.setTarget(null);
            }
            if (remoteProxyVideoSink != null) {
                remoteProxyVideoSink.setTarget(null);
            }

            if (localVideoRenderer != null) {
                try {
                    localVideoRenderer.release();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                localVideoRenderer = null;
            }
            if (remoteVideoRenderer != null) {
                try {
                    remoteVideoRenderer.release();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                remoteVideoRenderer = null;
            }
            if (peerConnection != null) {
                try {
                    peerConnection.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                peerConnection = null;
            }
            if (audioSource != null) {
                try {
                    audioSource.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                audioSource = null;
            }
            if (videoSource != null) {
                try {
                    videoSource.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                videoSource = null;
            }
            if (videoCapturer != null) {
                try {
                    videoCapturer.stopCapture();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                videoCapturer.dispose();
                videoCapturer = null;
            }
            if (surfaceTextureHelper != null) {
                try {
                    surfaceTextureHelper.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                surfaceTextureHelper = null;
            }
            if (remoteVideoTrack != null) {
                try {
                    remoteVideoTrack.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                remoteVideoTrack = null;
            }
            if (localVideoTrack != null) {
                try {
                    localVideoTrack.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                localVideoTrack = null;
            }
            if (peerConnectionFactory != null) {
                try {
                    peerConnectionFactory.dispose();
                } catch (Exception e) {
                    Log.e(TAG, "disconnect: ", e);
                }
                peerConnectionFactory = null;
            }
            if (audioManager != null) {
                setSpeaker(false);
            }
//            if (eglBase != null) {
//                eglBase.release();
//                eglBase = null;
//            }
            PeerConnectionFactory.stopInternalTracingCapture();
            PeerConnectionFactory.shutdownInternalTracer();
            Log.d(TAG, "disconnect: success");
        } catch (Exception e) {
            Log.e(TAG, "disconnect: ", e);
        }
    }

    private void refreshRenderer(boolean reverse) {
        rendererReverse = reverse;
        localProxyVideoSink.setTarget(reverse ? remoteVideoRenderer : localVideoRenderer);
        remoteProxyVideoSink.setTarget(reverse ? localVideoRenderer : remoteVideoRenderer);
    }

    private void switchCameraInternal() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            Log.d(TAG, "switchCamera: ");
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.w(TAG, "switchCamera: is not a camera, can not switch camera");
        }
    }

    private void handleInvite(String from, String body) {
        try {
            talkTo = from;
            Intent intent = new Intent(activity, CallActivity.class);
            intent.putExtra(ExtraKeys.INTENT_OUTBOUND, false);
            intent.putExtra(ExtraKeys.TALK_TO, from);

            callHandler.onReceiveInvite(from);
            if (config.useDefaultUi()) {
                activity.startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "handleInvite : ", e);
        }
    }

    private void handleAcceptInvite(String from, String body) {
        callHandler.onAcceptInvite(from);
        //  1. create peer
        createPeerConnection(from);
        // 2. create video
        createLocalVideo();
        // 3. add media
        addLocalMedia();
        // 4. create offer and send offer
        createOffer(from);
    }

    private void createOffer(String to) {
        // Create SDP constraints.
        final MediaConstraints sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
        executor.execute(() -> {
            Log.d(TAG, "createOffer: ");
            this.peerConnection.createOffer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d(TAG, "createOffer.onCreateSuccess: ");
                    peerConnection.setLocalDescription(new SdpObserverImpl(), sessionDescription);
                    sendMessage(to, SignalMessageType.OFFER_SDP, sessionDescription.type.canonicalForm(), sessionDescription.description, new FriendInviteResponseHandler() {
                        @Override
                        public void onReceived(String from, int status, String reason, String data) {
                            Log.d(TAG, String.format("onReceived FriendInviteResponseHandler.createOffer.onReceived: %d, %s, %s", status, reason, data));
                            if (status != 0) {
                                // send offer fail
                                callHandler.onFail(FailReason.SEND_OFFER_FAIL);
                            }
                        }
                    });
                }

                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "createOffer.onSetSuccess: ");
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d(TAG, "createOffer.onCreateFailure: " + s);
                    callHandler.onFail(FailReason.CREATE_OFFER_FAIL);
                }

                @Override
                public void onSetFailure(String s) {
                    Log.d(TAG, "createOffer.onSetFailure: " + s);
                    callHandler.onFail(FailReason.SET_OFFER_FAIL);
                }
            }, sdpMediaConstraints);
        });
    }

    private void handleOffer(String from, String sdpType, String sdp) {
        //  1. create peer
        createPeerConnection(from);
        this.peerConnection.setRemoteDescription(new SdpObserverImpl(), new SessionDescription(SessionDescription.Type.fromCanonicalForm(sdpType), sdp));
        // 2. create video
        createLocalVideo();
        // 3. add media
        addLocalMedia();
        // 4. create acceptInvite sdp and send acceptInvite sdp
        createAnswer(from);
    }

    private void createAnswer(String to) {
        // Create SDP constraints.
        final MediaConstraints sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
        executor.execute(() -> {
            Log.d(TAG, "createAnswer: ");
            this.peerConnection.createAnswer(new SdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    Log.d(TAG, "createAnswer.onCreateSuccess: ");
                    peerConnection.setLocalDescription(new SdpObserverImpl(), sessionDescription);
                    sendMessage(to, SignalMessageType.ANSWER_SDP, sessionDescription.type.canonicalForm(), sessionDescription.description, new FriendInviteResponseHandler() {
                        @Override
                        public void onReceived(String from, int status, String reason, String data) {
                            Log.d(TAG, String.format("onReceived FriendInviteResponseHandler.createOffer.onReceived: %d, %s, %s", status, reason, data));
                            if (status != 0) {
                                // send acceptInvite fail
                                callHandler.onFail(FailReason.SEND_ANSWER_FAIL);
                            }
                        }
                    });
                }

                @Override
                public void onSetSuccess() {
                    Log.d(TAG, "createAnswer.onSetSuccess: ");
                }

                @Override
                public void onCreateFailure(String s) {
                    Log.d(TAG, "createAnswer.onCreateFailure: " + s);
                    callHandler.onFail(FailReason.CREATE_ANSWER_FAIL);
                }

                @Override
                public void onSetFailure(String s) {
                    Log.d(TAG, "createAnswer.onSetFailure: " + s);
                    callHandler.onFail(FailReason.SET_ANSWER_FAIL);
                }
            }, sdpMediaConstraints);
        });
    }

    private void handleAnswer(String from, String sdpType, String sdp) {
        SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.fromCanonicalForm(sdpType), sdp);
        this.peerConnection.setRemoteDescription(new SdpObserverImpl(), sessionDescription);
    }

    private void handleReject(String from, String body) {
        callHandler.onReject(RejectReason.REJECT_CALL);
    }

    private void handleCandidate(String from, String body) {
        try {
            JSONObject json = new JSONObject(body);
            String sdp = json.getString(CandidateKey.sdp.name());
            int sdpMLineIndex = json.getInt(CandidateKey.sdpMLineIndex.name());
            String sdpMid = json.getString(CandidateKey.sdpMid.name());
            IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
            Log.d(TAG, String.format("handleCandidate: add candidate %s", candidate.toString()));

             this.peerConnection.addIceCandidate(candidate);
        } catch (Exception e) {
            Log.e(TAG, "handleCandidate: ", e);
        }
    }

    private void handleDisconnect(String from, String body) {
        callHandler.onDisconnected();
        disconnect(false);
        if (config.useDefaultUi() && !CallActivity.INSTANCE.isDestroyed() && !CallActivity.INSTANCE.isFinishing()) {
            CallActivity.INSTANCE.finish();
        }
    }

    private void createEglBase() {
        eglBase = EglBase.create();
    }

    private void createAudioManager() {
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        setSpeaker(false);
    }

    private void createPeerConnectionFactory() {
        if (eglBase == null) {
            createEglBase();
        }
        final AudioDeviceModule adm = createJavaAudioDevice();
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(activity).createInitializationOptions());
        this.peerConnectionFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(adm)
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .createPeerConnectionFactory();
        adm.release();
    }

    private void createPeerConnection(String oppositeId) {
        if (this.peerConnectionFactory == null) {
            this.createPeerConnectionFactory();
        }

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        try {
            TurnServer turnServer = carrier.getTurnServer();
            Log.d(TAG, "createPeerConnection: get turn server -> " + turnServer);
            if (turnServer != null) {
                String turn = String.format("turn:%s:%d", turnServer.getServer(), turnServer.getPort());
                String stun = String.format("stun:%s:%d", turnServer.getServer(), turnServer.getPort());
                iceServers.add(PeerConnection.IceServer.builder(turn).setUsername(turnServer.getUsername()).setPassword(turnServer.getPassword()).createIceServer());
                iceServers.add(PeerConnection.IceServer.builder(stun).createIceServer());
            }
        }  catch (Exception e) {
            Log.e(TAG, "createPeerConnection: get turn server -> ", e);
        }
        PeerConnection.RTCConfiguration configuration = new PeerConnection.RTCConfiguration(iceServers);
        configuration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        configuration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        configuration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        configuration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        configuration.keyType = PeerConnection.KeyType.ECDSA;
        configuration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        this.peerConnection = this.peerConnectionFactory.createPeerConnection(configuration, new PeerObserverImpl(oppositeId, callHandler));
    }

    private void addLocalMedia() {
        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        this.peerConnection.addTrack(localVideoTrack, mediaStreamLabels);
        this.peerConnection.addTrack(localAudioTrack, mediaStreamLabels);
    }

    private void createLocalVideo() {
        createCameraCapture();
        createVideoTrack();
        createAudioTrack();


    }

    private void createVideoTrack() {
        Log.d(TAG, "createVideoTrack: resolution(" + this.config.getResolution().getWidth() + " x " + this.config.getResolution().getHeight() + ")");
        if (surfaceTextureHelper == null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        }
        videoSource = this.peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, activity, videoSource.getCapturerObserver());
        videoCapturer.startCapture(this.config.getResolution().getWidth(), this.config.getResolution().getHeight(), this.config.getResolution().getFps());

        localVideoTrack = this.peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        localVideoTrack.setEnabled(true);
        localVideoTrack.addSink(localProxyVideoSink);

        getRemoteVideoTrack();
    }

    public void getRemoteVideoTrack() {
        if (remoteVideoTrack != null) {
            Log.d(TAG, "remoteVideoTrack is not null, return");
            return;
        }

        Log.d(TAG, "getRemoteVideoTrack: ");
        for (RtpTransceiver transceiver : peerConnection.getTransceivers()) {
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (track instanceof VideoTrack) {
                Log.d(TAG, "getRemoteVideoTrack: success");
                remoteVideoTrack = (VideoTrack) track;
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(remoteProxyVideoSink);
            }
        }
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(activity);
    }

    private void createCameraCapture() {
        CameraEnumerator enumerator = null;
        if (useCamera2()) {
            Log.d(TAG, "createCameraCapture: use camera2");
            enumerator = new Camera2Enumerator(activity);
        } else {
            Log.d(TAG, "createCameraCapture: use camera1");
            enumerator = new Camera1Enumerator(false);
        }
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating front facing camera capturer.");
                videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    break;
                }
            }
        }
    }

    private void createAudioTrack() {
        MediaConstraints constraints = new MediaConstraints();
        audioSource = this.peerConnectionFactory.createAudioSource(constraints);
        localAudioTrack = this.peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

    }

    private AudioDeviceModule createJavaAudioDevice() {

        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
            }
        };

        // Set audio record state callbacks.
        JavaAudioDeviceModule.AudioRecordStateCallback audioRecordStateCallback = new JavaAudioDeviceModule.AudioRecordStateCallback() {
            @Override
            public void onWebRtcAudioRecordStart() {
                Log.i(TAG, "Audio recording starts");
            }

            @Override
            public void onWebRtcAudioRecordStop() {
                Log.i(TAG, "Audio recording stops");
            }
        };

        // Set audio track state callbacks.
        JavaAudioDeviceModule.AudioTrackStateCallback audioTrackStateCallback = new JavaAudioDeviceModule.AudioTrackStateCallback() {
            @Override
            public void onWebRtcAudioTrackStart() {
                Log.i(TAG, "Audio playout starts");
            }

            @Override
            public void onWebRtcAudioTrackStop() {
                Log.i(TAG, "Audio playout stops");
            }
        };

        return JavaAudioDeviceModule.builder(activity)
//                .setSamplesReadyCallback(saveRecordedAudioToFile)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .setAudioRecordStateCallback(audioRecordStateCallback)
                .setAudioTrackStateCallback(audioTrackStateCallback)
                .createAudioDeviceModule();
    }

    private static class InstanceHolder {
        public static final ElastosWebrtc INSTANCE = new ElastosWebrtc();
    }

    public class ExtraKeys {
        public static final String INTENT_OUTBOUND = "outbound";
        public static final String TALK_TO = "talk_to";
    }
}

