package org.elastos.carrier.webrtc.signaling.carrier;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierHandler;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.FriendInviteResponseHandler;
import org.elastos.carrier.TurnServer;
import org.elastos.carrier.UserInfo;
import org.elastos.carrier.exceptions.CarrierException;
import org.elastos.carrier.webrtc.signaling.Signaling;

import java.util.LinkedList;

/**
 * Initialize Carrier instance and connect to the carrier network, you can also register your customer carrier handle by call addCarrierHandler()
 *
 */
public class CarrierClient {
	private static final String TAG = "CarrierClient";
	private static CarrierClient carrierClient;
	private static Carrier carrier;
	private Handler androidHandler;
	private Signaling signaling;
	private static String basePath;
	private WrapHandler carrierHandler;

	private FriendInviteResponseHandler friendInviteResponseHandler;


	public static CarrierClient getInstance(Context context, Signaling signaling) {
		if (carrierClient == null) {
			carrierClient = new CarrierClient(context, signaling);
		}

		return carrierClient;
	}

	public void addFriend(String address) throws CarrierException {
		String uid = Carrier.getIdFromAddress(address);
		if (!carrier.isFriend(uid)) {
			carrier.addFriend(address, "hello");
		}
	}

	public void sendMessageByInvite(String fid, String message) throws CarrierException {
		if (friendInviteResponseHandler==null){
			throw CarrierException.fromErrorCode(-1, "FriendInviteResponseHandler not initialized.");
		}else if(fid!=null && !fid.equals(carrier.getUserId())){
			carrier.inviteFriend(fid,message, friendInviteResponseHandler);
		}
	}

	/**
	 * You can register carrier handler by addCarrierHandler() to the client, if the event trigger, all the handles will be invoked.
	 * @param carrierHandler
	 */
	public void addCarrierHandler(CarrierHandler carrierHandler) {
		this.carrierHandler.addHandler(carrierHandler);
	}

	public void setFriendInviteResponseHandler(FriendInviteResponseHandler friendInviteResponseHandler) {
		this.friendInviteResponseHandler = friendInviteResponseHandler;
	}

	public void setAndroidHandler(Handler androidHandler) {
		this.androidHandler = androidHandler;
	}

	public String getMyAddress() {
		try {
			return carrier.getAddress();
		}
		catch (CarrierException e) {
			e.printStackTrace();
		}
		return "";
	}

	public TurnServer getTurnServer() {
		try {
			return carrier.getTurnServer();
		}
		catch (CarrierException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getUserIdFromAddress(String address) {
		return Carrier.getIdFromAddress(address);
	}

	public Carrier getCarrier() {
		return carrier;
	}

	private CarrierClient(Context context, Signaling signaling) {
		carrierHandler = new WrapHandler(context);
		basePath = context.getFilesDir().getParent();
		this.signaling = signaling;
		if(signaling!=null){
			carrierHandler.addHandler(signaling.getCarrierHandler());
		}
		CarrierOptions options = new CarrierOptions(basePath);

		try {
			Carrier.initializeInstance(options, carrierHandler);
			carrier = Carrier.getInstance();
			carrier.start(0);
			Log.i(TAG, "Carrier node is ready now");

		}
		catch (CarrierException /*| InterruptedException*/ e) {
			e.printStackTrace();
			Log.e(TAG, "Carrier node start failed, abort this test.");
		}
	}

	public static class WrapHandler extends AbstractCarrierHandler{
		LinkedList<CarrierHandler> carrierHandlers = new LinkedList<>();
		Context context;

		public WrapHandler(Context context) {
			this.context = context;
		}

		public void addHandler(CarrierHandler handler) {
			if (!this.carrierHandlers.contains(handler)) {
				this.carrierHandlers.add(handler);
			}
		}

		@Override
		public void onFriendMessage(Carrier carrier, String from, byte[] message, boolean isOffline) {
			for (CarrierHandler carrierHandler : carrierHandlers) {
				if(carrierHandler!=null) {
					carrierHandler.onFriendMessage(carrier, from, message, isOffline);
					Log.d(TAG, "carrier onFriendMessage(): from: " + from + ", message:" + message + ".");
				}
			}
		}

		@Override
		public void onConnection(Carrier carrier, ConnectionStatus status) {
			for (CarrierHandler carrierHandler : carrierHandlers) {
				if(carrierHandler!=null) {
					carrierHandler.onConnection(carrier, status);
					Log.d(TAG, "carrier onConnection(): status: " + status + ".");
				}
			}
		}

		@Override
		public void onFriendInviteRequest(Carrier carrier, String from, String data) {
			for (CarrierHandler carrierHandler : carrierHandlers) {
				if(carrierHandler!=null) {
					carrierHandler.onFriendInviteRequest(carrier, from, data);
					Log.d(TAG, "carrier onFriendInviteRequest(): from: " + from + ", data:" + data + ".");
				}
			}
		}
{}
		@Override
		public void onReady(Carrier carrier) {
			for (CarrierHandler carrierHandler : carrierHandlers) {
				carrierHandler.onReady(carrier);
				Log.d(TAG, "carrier onReady.");
			}
		}

		@Override
		public void onFriendAdded(Carrier carrier, FriendInfo info) {
			super.onFriendAdded(carrier, info);
		}

		@Override
		public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String hello) {
			for (CarrierHandler carrierHandler : carrierHandlers) {
				if(carrierHandler!=null) {
					carrierHandler.onFriendRequest(carrier, userId, info, hello);
					Log.d(TAG, "carrier onFriendRequest(): userId: " + userId + ", hello:" + hello + ".");
				}
				try {
					carrier.acceptFriend(userId);
				} catch (CarrierException e) {
					e.printStackTrace();
				}
			}
		}
	}


}
