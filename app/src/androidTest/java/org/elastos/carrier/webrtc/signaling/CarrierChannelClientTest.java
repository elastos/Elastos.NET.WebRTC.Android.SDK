package org.elastos.carrier.webrtc.signaling;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.test.core.app.ApplicationProvider;

import org.elastos.carrier.webrtc.CarrierWebrtcClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CarrierChannelClientTest {
    private CarrierChannelClient carrierChannelClient;
    private Context context = ApplicationProvider.getApplicationContext();
    private Handler handler;

    private String SOME_ADDRESS = "YjaM2pSQYWxLSHbhN6dDiQ7iWY9fvWpMZjZCzpwYQfXuS2PkqsRH";

    @Before
    public void setUp() throws Exception {
        //initial carrier client
        final HandlerThread handlerThread = new HandlerThread("TAG");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        carrierChannelClient = new CarrierChannelClient(handler, new CarrierChannelEvents() {
            @Override
            public void onCarrierMessage(String message) {

            }

            @Override
            public void onSdpReceived(String calleeUserId) {

            }

            @Override
            public void onCarrierClose() {

            }

            @Override
            public void onCarrierError(String description) {

            }
        }, context);
    }

    @After
    public void tearDown() throws Exception {
        context = null;
        carrierChannelClient = null;
        handler = null;
    }


    @Test
    public void testGetMyCarrierAddress() {
        String myAddress = carrierChannelClient.getMyCarrierAddress();
        assertNotEquals(myAddress, "");
    }

    @Test
    public void testConnect() throws InterruptedException {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                carrierChannelClient.connect(SOME_ADDRESS, carrierChannelClient.getMyCarrierAddress());
            }
        });

        Thread.sleep(1000);
        assertEquals(carrierChannelClient.getState(), CarrierConnectionState.CONNECTED);

    }

    @Test
    public void testRegister() throws InterruptedException {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                carrierChannelClient.register(SOME_ADDRESS, carrierChannelClient.getMyCarrierAddress());
            }
        });

        Thread.sleep(1000);
        assertEquals(carrierChannelClient.getState(), CarrierConnectionState.REGISTERED);

    }

    @Test
    public void testDisconnect() throws InterruptedException {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                carrierChannelClient.disconnect(false);
            }
        });

        Thread.sleep(1000);
        assertEquals(carrierChannelClient.getState(), CarrierConnectionState.CLOSED);
    }
}