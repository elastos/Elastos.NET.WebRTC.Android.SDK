package org.elastos.carrier.webrtc.call;

public enum CallState {
    INIT(0, "init"),
    INVITING(1, "inviting"),
    CONNECTING(2, "connecting"),
    RINGING(3, "ringing"),
    ACTIVE(4, "active"),
    ;

    private int value;
    private String name;

    CallState(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public static CallState valueOf(int value) {
        for (CallState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
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
