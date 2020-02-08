package org.elastos.carrier.webrtc.signaling;

import org.elastos.carrier.CarrierHandler;
import org.elastos.carrier.webrtc.signaling.model.Event;

public interface Signaling {

    void onSdpOffer(Event event);

    void onSdpAnswer(Event event);

    void onIceCandidate(Event event);

    void onError(Event event);

    void onException(Exception e);

    CarrierHandler getCarrierHandler();
}
