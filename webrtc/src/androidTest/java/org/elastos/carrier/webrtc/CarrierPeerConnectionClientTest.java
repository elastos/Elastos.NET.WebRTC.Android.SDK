package org.elastos.carrier.webrtc;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.elastos.carrier.Carrier;
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
    private Carrier carrier;

    @Before
    public void setUp() throws Exception {
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