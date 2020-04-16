package org.elastos.carrier.webrtc.call;

public enum CallState {
    /**
     * init state
     */
    INIT(0, "init"),
    /**
     * you are sending invite message
     */
    INVITING(1, "inviting"),
    /**
     * remote user accepted invite
     */
    CONNECTING(2, "connecting"),
    /**
     * someone is inviting you
     */
    RINGING(3, "ringing"),
    /**
     * call connected
     */
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
