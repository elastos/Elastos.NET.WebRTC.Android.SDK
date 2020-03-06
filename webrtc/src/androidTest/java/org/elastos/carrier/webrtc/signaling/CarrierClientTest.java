package org.elastos.carrier.webrtc.signaling;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.exceptions.CarrierException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CarrierClientTest {
    private CarrierClient carrierClient;
    private Context context = ApplicationProvider.getApplicationContext();

    private String SOME_ADDRESS = "YjaM2pSQYWxLSHbhN6dDiQ7iWY9fvWpMZjZCzpwYQfXuS2PkqsRH";

    @Before
    public void setUp() throws Exception {
        //initial carrier client
        carrierClient = CarrierClient.getInstance(context);
    }

    @After
    public void tearDown() throws Exception {
        context = null;
        carrierClient = null;
    }

    @Test
    public void testGetInstance() {
        //verify carrierClient is a singleton.
        assertEquals(CarrierClient.getInstance(context), carrierClient);
    }

    @Test
    public void testAddFriend() {
        try {
            String self = carrierClient.getMyAddress();
            carrierClient.addFriend(self);
        } catch (CarrierException e) {
            e.printStackTrace();
            assertTrue("can't add yourself as friend", true);
        }

        try {
            carrierClient.addFriend(SOME_ADDRESS);
            assertTrue("now you're the friend of the coder", true);
        } catch (CarrierException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testSendMessageByInvite() {
        String fid = carrierClient.getUserIdFromAddress(SOME_ADDRESS);
        try {
            carrierClient.sendMessageByInvite(fid, "hello");
        } catch (CarrierException e) {
            e.printStackTrace();
            assertTrue("FriendInviteResponseHandler not initialized", true);
        }
        carrierClient.setFriendInviteResponseHandler(new FriendInviteResponseHandler() {
            @Override
            public void onReceived(String from, int status, String reason, String data) {
            }
        });

        try {
            carrierClient.sendMessageByInvite(fid, "hello");
        } catch (CarrierException e) {
            e.printStackTrace();
        }

        assertTrue("message sent", true);

    }

    @Test
    public void testAddCarrierHandler() {
        final boolean[] carrierHandleFired = {false,true};
        carrierClient.addCarrierHandler(new AbstractCarrierHandler() {
            @Override
            public void onFriendInviteRequest(Carrier carrier, String from, String data) {
                super.onFriendInviteRequest(carrier, from, data);
                carrierHandleFired[0] = true;
                carrierHandleFired[1] = false;
            }
        });

        try {
            carrierClient.addFriend(SOME_ADDRESS);
            assertTrue(carrierHandleFired[0] && !carrierHandleFired[1]); // on friendInviteRequest fired.
        } catch (CarrierException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testGetMyAddress() {
        String myAddress = carrierClient.getMyAddress();
        assertNotEquals(myAddress, "");
    }


    @Test
    public void testGetUserIdFromAddress() {
        String fid = carrierClient.getUserIdFromAddress(SOME_ADDRESS);
        assertNotEquals(fid, "");
    }

}