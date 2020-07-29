package org.elastos.carrier.webrtc;

public class PeerConnectionParametersBuilder {
    private boolean videoCallEnabled = true;
    private boolean tracing = false;
    private int videoWidth = 1280;
    private int videoHeight = 720;
    private int videoFps = 30;
    private int videoMaxBitrate = 0;
    private String videoCodec = "vp8";
    private boolean videoCodecHwAcceleration = false;
    private boolean videoFlexfecEnabled = false;
    private int audioStartBitrate = 0;
    private String audioCodec = "opus";
    private boolean noAudioProcessing = false;
    private boolean aecDump = false;
    private boolean saveInputAudioToFile = false;
    private boolean useOpenSLES = false;
    private boolean disableBuiltInAEC = false;
    private boolean disableBuiltInAGC = false;
    private boolean disableBuiltInNS = false;
    private boolean disableWebRtcAGCAndHPF = false;
    private boolean enableRtcEventLog = false;
    private CarrierPeerConnectionClient.DataChannelParameters dataChannelParameters = null;

    private PeerConnectionParametersBuilder() {
    }

    public static PeerConnectionParametersBuilder builder() {
        return new PeerConnectionParametersBuilder();
    }

    public CarrierPeerConnectionClient.PeerConnectionParameters build() {
        return new CarrierPeerConnectionClient.PeerConnectionParameters(
                this.videoCallEnabled, this.tracing, this.videoWidth, this.videoHeight, this.videoFps,
                this.videoMaxBitrate, this.videoCodec, this.videoCodecHwAcceleration, this.videoFlexfecEnabled,
                this.audioStartBitrate, this.audioCodec, this.noAudioProcessing, this.aecDump, this.saveInputAudioToFile,
                this.useOpenSLES, this.disableBuiltInAEC, this.disableBuiltInAGC, this.disableBuiltInNS, this.disableWebRtcAGCAndHPF,
                this.enableRtcEventLog, this.dataChannelParameters
        );
    }

    public PeerConnectionParametersBuilder videoEnabled(boolean enable) {
        this.videoCallEnabled = enable;
        return this;
    }

    public PeerConnectionParametersBuilder tracing(boolean tracing) {
        this.tracing = tracing;
        return this;
    }

    public PeerConnectionParametersBuilder videoWidth(int width) {
        this.videoWidth = width;
        return this;
    }

    public PeerConnectionParametersBuilder videoHeight(int h) {
        this.videoHeight = h;
        return this;
    }

    public PeerConnectionParametersBuilder videoFps(int f) {
        this.videoFps = f;
        return this;
    }

    public PeerConnectionParametersBuilder videoMaxBitrate(int b) {
        this.videoMaxBitrate = b;
        return this;
    }

    public PeerConnectionParametersBuilder VideoCodec(String c) {
        this.videoCodec = c;
        return this;
    }

    public PeerConnectionParametersBuilder videoCodecHwAcceleration(boolean b) {
        this.videoCodecHwAcceleration = b;
        return this;
    }

    public PeerConnectionParametersBuilder videoFlexfecEnabled(boolean b) {
        this.videoFlexfecEnabled = b;
        return this;
    }

    public PeerConnectionParametersBuilder audioStartBitrate(int b) {
        this.audioStartBitrate = b;
        return this;
    }

    public PeerConnectionParametersBuilder audioCodec(String a) {
        this.audioCodec = a;
        return this;
    }

    public PeerConnectionParametersBuilder noAudioProcessing(boolean b) {
        this.noAudioProcessing = b;
        return this;
    }

    public PeerConnectionParametersBuilder aecDump(boolean a) {
        this.aecDump = a;
        return this;
    }

    public PeerConnectionParametersBuilder saveInputAudioToFile(boolean b) {
        this.saveInputAudioToFile = b;
        return this;
    }

    public PeerConnectionParametersBuilder useOpenSLES(boolean b) {
        this.useOpenSLES = b;
        return this;
    }

    public PeerConnectionParametersBuilder disableBuiltInAEC(boolean b) {
        this.disableBuiltInAEC = b;
        return this;
    }

    public PeerConnectionParametersBuilder disableBuiltInAGC(boolean b) {
        this.disableBuiltInAGC = b;
        return this;
    }

    public PeerConnectionParametersBuilder disableBuiltInNS(boolean b) {
        this.disableBuiltInNS = b;
        return this;
    }

    public PeerConnectionParametersBuilder disableWebRtcAGCAndHPF(boolean b) {
        this.disableWebRtcAGCAndHPF = b;
        return this;
    }

    public PeerConnectionParametersBuilder enableRtcEventLog(boolean b) {
        this.enableRtcEventLog = enableRtcEventLog;
        return this;
    }

    public PeerConnectionParametersBuilder dataChannelParameter(CarrierPeerConnectionClient.DataChannelParameters dataChannelParameters) {
        this.dataChannelParameters = dataChannelParameters;
        return this;
    }
}
