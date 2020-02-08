package com.allcom.elastos_webrtc;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

/**
 *  config for {@link ElastosWebrtc}
 */
public class ElastosWebrtcConfig {

    private static final String TAG = "ElastosWebrtcConfig";

    private Resolution resolution = new Resolution();

//    private List<Ice> ices = Arrays.asList(new Ice[]{new Ice("stun:gfax.net:3478"), new Ice("turn:gfax.net:3478", "allcom", "allcompass")});
    private List<Ice> ices = new ArrayList<>();

    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private boolean defaultUi = true;

    private ElastosWebrtcConfig() {

    }

    /**
     * <p>builder of {@link ElastosWebrtcConfig}
     *
     * @return instance of {@link ElastosWebrtcConfig}
     */
    public static ElastosWebrtcConfig builder() {
        return new ElastosWebrtcConfig();
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
