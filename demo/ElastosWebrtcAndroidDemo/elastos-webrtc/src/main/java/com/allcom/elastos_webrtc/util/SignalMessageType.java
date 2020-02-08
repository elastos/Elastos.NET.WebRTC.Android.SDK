package com.allcom.elastos_webrtc.util;

public enum SignalMessageType {
    INVITE(0, "invite"),
    ACCEPT_INVITE(1, "accept_invite"),
    REJECT_INVITE(2, "reject_invite"),
    OFFER_SDP(3, "offer_sdp"),
    ANSWER_SDP(4, "answer_sdp"),
    CANDIDATE(5, "candidate"),
    DISCONNECT(6, "disconnect"),
    ;

    private int type;
    private String description;

    SignalMessageType(int type, String description) {
        this.type = type;
        this.description = description;
    }

    public static SignalMessageType valueOf(int type) {
        for (SignalMessageType signalMessageType : values()) {
            if (type == signalMessageType.type) {
                return signalMessageType;
            }
        }

        return null;
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "SignalMessageType{" +
                "type=" + type +
                ", description='" + description + '\'' +
                '}';
    }
}
