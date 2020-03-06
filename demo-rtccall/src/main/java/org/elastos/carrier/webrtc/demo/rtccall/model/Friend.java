package org.elastos.carrier.webrtc.demo.rtccall.model;

public class Friend {

    private boolean online;

    private String name;

    public Friend() {
    }

    public Friend(boolean online, String name) {
        this.online = online;
        this.name = name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
