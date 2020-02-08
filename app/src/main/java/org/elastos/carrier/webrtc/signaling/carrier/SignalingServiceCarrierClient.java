package org.elastos.carrier.webrtc.signaling.carrier;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import org.elastos.carrier.exceptions.CarrierException;
import org.elastos.carrier.webrtc.signaling.SignalingListener;
import org.elastos.carrier.webrtc.signaling.model.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Signaling service client based on carrier network.
 */

public class SignalingServiceCarrierClient {

    private static final String TAG = "SignalingServiceCarrierClient";

    private final CarrierClient carrierClient;

    private final ExecutorService executorService;

    private final Gson gson = new Gson();

    public SignalingServiceCarrierClient(Context context, final SignalingListener signalingListener,
                                         final ExecutorService executorService) {
        Log.d(TAG, "Connecting to Carrier network ");
        carrierClient = CarrierClient.getInstance(context, signalingListener);
        this.executorService = executorService;
    }

    public boolean isReady() {
        return carrierClient.getCarrier().isReady();
    }

    public void sendSdpOffer(final Message offer) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (offer.getAction().equalsIgnoreCase("SDP_OFFER")) {

                    Log.d(TAG, "Sending Offer");

                    send(offer);
                }
            }
        });
    }

    public void sendSdpAnswer(final Message answer) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (answer.getAction().equalsIgnoreCase("SDP_ANSWER")) {

                    Log.d(TAG, "Answer sent " + new String(Base64.decode(answer.getMessagePayload().getBytes(),
                            Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE)));

                    send(answer);
                }
            }
        });
    }

    public void sendIceCandidate(final Message candidate) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (candidate.getAction().equalsIgnoreCase("ICE_CANDIDATE")) {

                    send(candidate);
                }

                Log.d(TAG, "Sent Ice candidate message");
            }
        });
    }

    public void disconnect() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                //carrierClient.getCarrier().kill();
                //todo: no need to kill carrier node instance.
            }
        });
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error in disconnect");
        }
    }

    private void send(final Message message) {
        String jsonMessage = gson.toJson(message);
        Log.d(TAG, "Sending JSON Message= " + jsonMessage);
        try {
            carrierClient.sendMessageByInvite(message.getRecipientClientId(), jsonMessage);
            Log.d(TAG, "Sent JSON Message= " + jsonMessage);
        } catch (CarrierException e) {
            Log.e(TAG, "Sent JSON Message error: " + e.getMessage());
        }
    }

}
