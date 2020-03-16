package org.elastos.carrier.webrtc;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import static org.junit.Assert.*;

public class CarrierPeerConnectionClientTest {
    private CarrierPeerConnectionClient carrierPeerConnectionClient;
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        carrierPeerConnectionClient = new CarrierPeerConnectionClient(context, new PeerConnectionEvents() {
            @Override
            public void onLocalDescription(SessionDescription sdp) {

            }

            @Override
            public void onIceCandidate(IceCandidate candidate) {

            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {

            }

            @Override
            public void onIceConnected() {

            }

            @Override
            public void onIceDisconnected() {

            }

            @Override
            public void onConnected() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onPeerConnectionClosed() {

            }

            @Override
            public void onPeerConnectionStatsReady(StatsReport[] reports) {

            }

            @Override
            public void onPeerConnectionError(String description) {

            }
        });
    }

    @After
    public void tearDown() throws Exception {
        context = null;
        carrierPeerConnectionClient = null;
    }


    @Test
    public void testCreateOffer() {
        carrierPeerConnectionClient.createPeerConnectionFactory();
        carrierPeerConnectionClient.createOffer();
    }

    @Test
    public void testCreateAnswer() {
    }

    @Test
    public void testAddRemoteIceCandidate() {
    }
}