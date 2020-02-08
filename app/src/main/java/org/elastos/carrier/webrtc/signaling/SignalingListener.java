package org.elastos.carrier.webrtc.signaling;


import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.webrtc.signaling.model.Event;

/**
 * abstract class of signaling listener, you need the
 */
public abstract class SignalingListener implements Signaling {

    private final static String TAG = "CustomMessageHandler";

    private final Gson gson = new Gson();

    private class CarrierHandler extends AbstractCarrierHandler {

        @Override
        public void onFriendInviteRequest(Carrier carrier, String from, String message) {

            Log.d(TAG, "carrier friend invite  onFriendInviteRequest from: " + from);

            if (!message.isEmpty() && message.contains("messagePayload")) {

                Event evt = gson.fromJson(message, Event.class);

                if(evt != null && evt.getMessageType() != null && !evt.getMessagePayload().isEmpty()){

                    if (evt.getMessageType().equalsIgnoreCase("SDP_OFFER")) {

                        Log.d(TAG, "Offer received: SenderClientId="  + evt.getSenderClientId());

                        byte[] decode = Base64.decode(evt.getMessagePayload(), 0);

                        Log.d(TAG, new String(decode));

                        onSdpOffer(evt);
                    }

                    if (evt.getMessageType().equalsIgnoreCase("SDP_ANSWER")) {

                        Log.d(TAG, "Answer received: SenderClientId="  + evt.getSenderClientId());

                        onSdpAnswer(evt);
                    }

                    if (evt.getMessageType().equalsIgnoreCase("ICE_CANDIDATE")) {

                        Log.d(TAG, "Ice Candidate received: SenderClientId="  + evt.getSenderClientId());

                        byte[] decode = Base64.decode(evt.getMessagePayload(), 0);

                        Log.d(TAG, new String(decode));

                        onIceCandidate(evt);
                    }
                }
            }
        }
    }

    private final CarrierHandler carrierHandler =  new CarrierHandler();

    public CarrierHandler getCarrierHandler() {
        return carrierHandler;
    }

}
