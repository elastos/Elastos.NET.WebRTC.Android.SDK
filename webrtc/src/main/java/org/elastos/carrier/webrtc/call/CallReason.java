package org.elastos.carrier.webrtc.call;

public enum CallReason {
    /**
     * remote user reject you invite
     */
    REJECT(-1, "reject call"),
    /**
     * normal disconnected
     */
    NORMAL_HANGUP(0, "normal hangup"),
    /**
     * connect error
     */
    ERROR(1, "error occurred when calling"),
    ;

    private int value;
    private String name;

    CallReason(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static CallReason valueOf(int value) {
        for (CallReason reason : values()) {
            if (reason.getValue() == value)
                return reason;
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
