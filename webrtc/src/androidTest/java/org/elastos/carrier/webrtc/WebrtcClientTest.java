package org.elastos.carrier.webrtc;

import android.content.Context;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.webrtc.call.CallState;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.util.ArrayList;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebrtcClientTest {
    private Context context = ApplicationProvider.getApplicationContext();
    private Carrier carrier;

    @Before
    public void setUp() throws Exception {
        CarrierOptions options = new CarrierOptions(context.getFilesDir().getParent());
        AbstractCarrierHandler carrierHandler = new AbstractCarrierHandler() {
            @Override
            public void onReady(Carrier carrier) {
                super.onReady(carrier);
            }
        };
        Carrier.initializeInstance(options, carrierHandler);
        carrier = Carrier.getInstance();

        assertNotNull(carrier);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void a01initialize() {
        assertNotNull(WebrtcClient.initialize(context, carrier, null, null));
    }

    @Test
    public void a02getInstance() {
        assertNotNull(WebrtcClient.getInstance());
    }

    @Test
    public void a03inviteCall() {
        WebrtcClient.getInstance().inviteCall("BVyDM3qweb7jdGmbySEpve3ETLy9L8sYN9orh3QaT8r9");
        assertEquals("BVyDM3qweb7jdGmbySEpve3ETLy9L8sYN9orh3QaT8r9", WebrtcClient.getInstance().getRemoteUserId());
    }

    @Test
    public void a04acceptCallInvite() {
    }

    @Test
    public void a05rejectCallInvite() {
    }


    @Test
    public void a06renderVideo() {
    }

    @Test
    public void a07switchCamera() {
    }

    @Test
    public void a08setResolution() {
    }

    @Test
    public void a09setAudioEnable() {
    }

    @Test
    public void a10swapVideoRenderer() {
    }

    @Test
    public void a11setVideoEnable() {
    }

    @Test
    public void a12getCallState() {
        System.out.println(9);
        CallState callState = WebrtcClient.getInstance().getCallState();
        assertNotNull(callState);
    }

    @Test
    public void a13getRemoteUserId() {
    }

    @Test
    public void a14disconnect() {
    }

    class CarrierOptions extends Carrier.Options {
        CarrierOptions(String path) {
            super();

            File file = new File(path);
            if (file.exists())
                file.delete();
            file.mkdir();

            try {
                setUdpEnabled(true);
                setPersistentLocation(path);

                ArrayList<BootstrapNode> arrayList = new ArrayList<>();
                BootstrapNode node = new BootstrapNode();
                node.setIpv4("13.58.208.50");
                node.setPort("33445");
                node.setPublicKey("89vny8MrKdDKs7Uta9RdVmspPjnRMdwMmaiEW27pZ7gh");
                arrayList.add(node);

                node = new BootstrapNode();
                node.setIpv4("18.216.102.47");
                node.setPort("33445");
                node.setPublicKey("G5z8MqiNDFTadFUPfMdYsYtkUDbX5mNCMVHMZtsCnFeb");
                arrayList.add(node);

                node = new BootstrapNode();
                node.setIpv4("18.216.6.197");
                node.setPort("33445");
                node.setPublicKey("H8sqhRrQuJZ6iLtP2wanxt4LzdNrN2NNFnpPdq1uJ9n2");
                arrayList.add(node);

                node = new BootstrapNode();
                node.setIpv4("52.83.171.135");
                node.setPort("33445");
                node.setPublicKey("5tuHgK1Q4CYf4K5PutsEPK5E3Z7cbtEBdx7LwmdzqXHL");
                arrayList.add(node);

                node = new BootstrapNode();
                node.setIpv4("52.83.191.228");
                node.setPort("33445");
                node.setPublicKey("3khtxZo89SBScAMaHhTvD68pPHiKxgZT6hTCSZZVgNEm");
                arrayList.add(node);

                setBootstrapNodes(arrayList);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}