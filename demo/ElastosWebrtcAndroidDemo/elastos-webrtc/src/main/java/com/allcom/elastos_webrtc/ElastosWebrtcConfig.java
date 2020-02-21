package com.allcom.elastos_webrtc;

import android.text.TextUtils;
import android.util.Log;

import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 *  config for {@link ElastosWebrtc}
 */
public class ElastosWebrtcConfig {

    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String VIDEO_TRACK_TYPE = "video";
    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    public static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "ISAC";
    public static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
    public static final String VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    public static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    public static final String DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    public static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    public static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    public static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    public static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    public static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

    private static final String TAG = "ElastosWebrtcConfig";
    private Resolution resolution;
    private List<Ice> ices;
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private boolean defaultUi = true;
    private boolean videoCallEnabled;
    private boolean tracing;
    private int videoMaxBitrate;
    private String videoCodec;
    private boolean videoCodecHwAcceleration;
    private boolean videoFlexfecEnabled;
    private int audioStartBitrate;
    private String audioCodec;
    private boolean noAudioProcessing;
    private boolean aecDump;
    private boolean saveInputAudioToFile;
    private boolean useOpenSLES;
    private boolean disableBuiltInAEC;
    private boolean disableBuiltInAGC;
    private boolean disableBuiltInNS;
    private boolean disableWebRtcAGCAndHPF;
    private boolean enableRtcEventLog;

    private ElastosWebrtcConfig() {
        this.resolution = new Resolution();
        this.ices = new ArrayList<>();
        this.defaultUi = true;
        this.videoCallEnabled = true;
        this.tracing = false;
        this.videoMaxBitrate = 1700;
        this.videoCodec = VIDEO_CODEC_VP8;
        this.videoCodecHwAcceleration = true;
        this.videoFlexfecEnabled = false;
        this.audioStartBitrate = 32;
        this.audioCodec = AUDIO_CODEC_OPUS;
        this.noAudioProcessing = false;
        this.aecDump = false;
        this.saveInputAudioToFile = false;
        this.useOpenSLES = false;
        this.disableBuiltInAEC = false;
        this.disableBuiltInAGC = false;
        this.disableBuiltInNS = false;
        this.disableWebRtcAGCAndHPF = false;
        this.enableRtcEventLog = false;
    }

    private ElastosWebrtcConfig(Resolution resolution, List<Ice> ices, boolean defaultUi, boolean videoCallEnabled, boolean tracing,
                               int videoMaxBitrate, String videoCodec, boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled,
                               int audioStartBitrate, String audioCodec, boolean noAudioProcessing, boolean aecDump,
                               boolean saveInputAudioToFile, boolean useOpenSLES, boolean disableBuiltInAEC, boolean disableBuiltInAGC,
                               boolean disableBuiltInNS, boolean disableWebRtcAGCAndHPF, boolean enableRtcEventLog) {
        this.resolution = resolution;
        this.ices = ices;
        this.defaultUi = defaultUi;
        this.videoCallEnabled = videoCallEnabled;
        this.tracing = tracing;
        this.videoMaxBitrate = videoMaxBitrate;
        this.videoCodec = videoCodec;
        this.videoCodecHwAcceleration = videoCodecHwAcceleration;
        this.videoFlexfecEnabled = videoFlexfecEnabled;
        this.audioStartBitrate = audioStartBitrate;
        this.audioCodec = audioCodec;
        this.noAudioProcessing = noAudioProcessing;
        this.aecDump = aecDump;
        this.saveInputAudioToFile = saveInputAudioToFile;
        this.useOpenSLES = useOpenSLES;
        this.disableBuiltInAEC = disableBuiltInAEC;
        this.disableBuiltInAGC = disableBuiltInAGC;
        this.disableBuiltInNS = disableBuiltInNS;
        this.disableWebRtcAGCAndHPF = disableWebRtcAGCAndHPF;
        this.enableRtcEventLog = enableRtcEventLog;
    }

    /**
     * <p>builder of {@link ElastosWebrtcConfig}
     *
     * @return instance of {@link ElastosWebrtcConfig}
     */
    public static ElastosWebrtcConfig builder() {
        return new ElastosWebrtcConfig();
    }

    public static ElastosWebrtcConfig builder(Resolution resolution, List<Ice> ices, boolean defaultUi, boolean videoCallEnabled, boolean tracing,
                                              int videoMaxBitrate, String videoCodec, boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled,
                                              int audioStartBitrate, String audioCodec, boolean noAudioProcessing, boolean aecDump,
                                              boolean saveInputAudioToFile, boolean useOpenSLES, boolean disableBuiltInAEC, boolean disableBuiltInAGC,
                                              boolean disableBuiltInNS, boolean disableWebRtcAGCAndHPF, boolean enableRtcEventLog) {
        return new ElastosWebrtcConfig(resolution, ices, defaultUi, videoCallEnabled, tracing,
                videoMaxBitrate, videoCodec, videoCodecHwAcceleration, videoFlexfecEnabled,
                audioStartBitrate, audioCodec, noAudioProcessing, aecDump,
                saveInputAudioToFile, useOpenSLES, disableBuiltInAEC, disableBuiltInAGC,
                disableBuiltInNS, disableWebRtcAGCAndHPF, enableRtcEventLog);
    }

    /**
     * <p> set video resolution
     *
     * @param width video width
     * @param height video height
     * @return the original instance
     */
    public ElastosWebrtcConfig resolution(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height should be positive");
        }

        this.resolution = new Resolution(width, height);
        return this;
    }

    /**
     * <p> set video resolution
     *
     * @see Resolution
     * @param resolution video resolution
     * @return the original instance
     */
    public ElastosWebrtcConfig resolution(@NonNull Resolution resolution) {
        this.resolution = resolution;
        return this;
    }

    /**
     * <p> reset webrtc stun and turn servers.
     *
     * @see Ice
     * @param iceList stun or turn servers
     * @return the original instance
     */
    public ElastosWebrtcConfig ice(@NonNull List<Ice> iceList) {
        this.ices = iceList;
        return this;
    }

    /**
     * <p> add a stun or turn server.
     *
     * @see Ice
     * @param ice a stun or turn server
     * @return the original instance
     */
    public ElastosWebrtcConfig ice(@NonNull Ice ice) {
        this.ices.add(ice);
        return this;
    }

    /**
     * <p> add stun or turn server list.
     *
     * @see Ice
     * @param iceList stun or turn server list
     * @return the original instance
     */
    public ElastosWebrtcConfig addAllIce(@NonNull List<Ice> iceList) {
        this.ices.addAll(iceList);
        return this;
    }

    /**
     * <p> config if use default call ui or not
     *
     * @param use true to use default ui false not
     * @return the original instance
     */
    public ElastosWebrtcConfig useDefaultUi(boolean use) {
        this.defaultUi = use;
        return this;
    }

    public ElastosWebrtcConfig videoCallEnabled(boolean videoCallEnabled) {
        this.videoCallEnabled = videoCallEnabled;
        return this;
    }

    public ElastosWebrtcConfig tracing(boolean tracing) {
        this.tracing = tracing;
        return this;
    }

    public ElastosWebrtcConfig videoMaxBitrate(int videoMaxBitrate) {
        this.videoMaxBitrate = videoMaxBitrate;
        return this;
    }

    public ElastosWebrtcConfig videoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
        return this;
    }

    public ElastosWebrtcConfig videoCodecHwAcceleration(boolean videoCodecHwAcceleration) {
        this.videoCodecHwAcceleration = videoCodecHwAcceleration;
        return this;
    }

    public ElastosWebrtcConfig videoFlexfecEnabled(boolean videoFlexfecEnabled) {
        this.videoFlexfecEnabled = videoFlexfecEnabled;
        return this;
    }

    public ElastosWebrtcConfig audioStartBitrate(int audioStartBitrate) {
        this.audioStartBitrate = audioStartBitrate;
        return this;
    }

    public ElastosWebrtcConfig audioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
        return this;
    }

    public ElastosWebrtcConfig noAudioProcessing(boolean noAudioProcessing) {
        this.noAudioProcessing = noAudioProcessing;
        return this;
    }

    public ElastosWebrtcConfig aecDump(boolean aecDump) {
        this.aecDump = aecDump;
        return this;
    }

    public ElastosWebrtcConfig saveInputAudioToFile(boolean saveInputAudioToFile) {
        this.saveInputAudioToFile = saveInputAudioToFile;
        return this;
    }

    public ElastosWebrtcConfig useOpenSLES(boolean useOpenSLES) {
        this.useOpenSLES = useOpenSLES;
        return this;
    }

    public ElastosWebrtcConfig disableBuiltInAEC(boolean disableBuiltInAEC) {
        this.disableBuiltInAEC = disableBuiltInAEC;
        return this;
    }

    public ElastosWebrtcConfig disableBuiltInAGC(boolean disableBuiltInAGC) {
        this.disableBuiltInAGC = disableBuiltInAGC;
        return this;
    }

    public ElastosWebrtcConfig disableBuiltInNS(boolean disableBuiltInNS) {
        this.disableBuiltInNS = disableBuiltInNS;
        return this;
    }

    public ElastosWebrtcConfig disableWebRtcAGCAndHPF(boolean disableWebRtcAGCAndHPF) {
        this.disableWebRtcAGCAndHPF = disableWebRtcAGCAndHPF;
        return this;
    }

    public ElastosWebrtcConfig enableRtcEventLog(boolean enableRtcEventLog) {
        this.enableRtcEventLog = enableRtcEventLog;
        return this;
    }

    /**
     * <p> get {@link PeerConnection.IceServer} lists from stun or turn servers.
     *
     * @return list of {@link PeerConnection.IceServer}
     */
    public List<PeerConnection.IceServer> getIceServers() {
        try {
            for (Ice ice : ices) {
                Log.d(TAG, "getIceServers: " + ice);
            }
        } catch (Exception e) {
            Log.e(TAG, "getIceServers: ", e);
        }
        iceServers.clear();
        for (Ice ice : ices) {
            PeerConnection.IceServer.Builder builder = PeerConnection.IceServer.builder(ice.getUri());
            if (!TextUtils.isEmpty(ice.getUsername()) && !TextUtils.isEmpty(ice.getPassword())) {
                builder.setUsername(ice.getUsername()).setPassword(ice.getPassword());
            }
            iceServers.add(builder.createIceServer());
        }

        return iceServers;
    }

    /**
     * <p> get video resolution
     *
     * @return video resolution {@link Resolution}
     */
    public Resolution getResolution() {
        return resolution;
    }

    /**
     * <p> get use default ui or not
     *
     * @return use default ui or not
     */
    public boolean useDefaultUi() {
        return this.defaultUi;
    }

    public boolean isVideoCallEnabled() {
        return videoCallEnabled;
    }

    public boolean isTracing() {
        return tracing;
    }

    public int getVideoMaxBitrate() {
        return videoMaxBitrate;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public boolean isVideoCodecHwAcceleration() {
        return videoCodecHwAcceleration;
    }

    public boolean isVideoFlexfecEnabled() {
        return videoFlexfecEnabled;
    }

    public int getAudioStartBitrate() {
        return audioStartBitrate;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public boolean isNoAudioProcessing() {
        return noAudioProcessing;
    }

    public boolean isAecDump() {
        return aecDump;
    }

    public boolean isSaveInputAudioToFile() {
        return saveInputAudioToFile;
    }

    public boolean isUseOpenSLES() {
        return useOpenSLES;
    }

    public boolean isDisableBuiltInAEC() {
        return disableBuiltInAEC;
    }

    public boolean isDisableBuiltInAGC() {
        return disableBuiltInAGC;
    }

    public boolean isDisableBuiltInNS() {
        return disableBuiltInNS;
    }

    public boolean isDisableWebRtcAGCAndHPF() {
        return disableWebRtcAGCAndHPF;
    }

    public boolean isEnableRtcEventLog() {
        return enableRtcEventLog;
    }

    /**
     * <p> video resolution width x height fps fps
     *
     */
    public static class Resolution {
        public static final Resolution RS_480P = new Resolution(720, 480, 30, "480P");
        public static final Resolution RS_720P = new Resolution(1280, 720, 30, "720P");
        public static final Resolution RS_1080P = new Resolution(1920, 1080, 30, "1080P");
        public static final Resolution RS_2K = new Resolution(2560, 1440, 30, "2K");
        public static final Resolution RS_4K = new Resolution(4096, 2160, 60, "4K");
        public static final int DEFAULT_WIDTH = 1280;
        public static final int DEFAULT_HEIGHT = 720;
        public static final int DEFAULT_FPS = 30;
        public static final String DEFAULT_DESCRIPTION = "720P";

        private int width;
        private int height;
        private int fps;
        private String description;

        public Resolution() {
            this.width = DEFAULT_WIDTH;
            this.height = DEFAULT_HEIGHT;
            this.fps = DEFAULT_FPS;
        }

        public Resolution(int width, int height) {
            this.width = width;
            this.height = height;
            this.fps = DEFAULT_FPS;
            this.description = DEFAULT_DESCRIPTION;
        }

        public Resolution(int width, int height, int fps) {
            this.width = width;
            this.height = height;
            this.fps = fps;
            this.description = DEFAULT_DESCRIPTION;
        }

        public Resolution(int width, int height, int fps, String description) {
            this.width = width;
            this.height = height;
            this.fps = fps;
            this.description = description;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getFps() {
            return fps;
        }

        public void setFps(int fps) {
            this.fps = fps;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * <p> stun or turn server
     */
    public class Ice {
        private String uri;
        private String username;
        private String password;

        public Ice(String uri) {
            this.uri = uri;
        }

        public Ice(String uri, String username, String password) {
            this.uri = uri;
            this.username = username;
            this.password = password;
        }

        public String getUri() {
            return uri;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public String toString() {
            return "Ice{" +
                    "uri='" + uri + '\'' +
                    ", username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }

}
