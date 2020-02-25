package com.allcom.elastos_webrtc_call.eos;

import android.text.TextUtils;
import android.util.Log;

import com.allcom.elastos_webrtc_call.MainActivity;
import com.allcom.elastos_webrtc_call.ui.dashboard.DashboardFragment;
import com.allcom.elastos_webrtc_call.util.Utils;

import org.elastos.carrier.Carrier;
import org.elastos.carrier.CarrierHandler;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.PresenceStatus;
import org.elastos.carrier.UserInfo;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CarrierHandlerImpl implements CarrierHandler {

    private static final String TAG = "CarrierHandlerImpl";

    @Override
    public void onIdle(Carrier carrier) {
        // Log.d(TAG, "onIdle: ");
    }

    @Override
    public void onConnection(Carrier carrier, ConnectionStatus status) {
        Log.d(TAG, "onConnection: " + status);
        switch (status) {
            case Disconnected:
                MainActivity.INSTANCE.setCarrierOnline(false);
                break;
            case Connected:
                MainActivity.INSTANCE.setCarrierOnline(true);
                break;
        }
    }

    @Override
    public void onReady(Carrier carrier) {
        Log.d(TAG, "onReady: ");
    }

    @Override
    public void onSelfInfoChanged(Carrier carrier, UserInfo userInfo) {
        Log.d(TAG, "onSelfInfoChanged: " + userInfo);
    }

    @Override
    public void onFriends(Carrier carrier, List<FriendInfo> friends) {
        Log.d(TAG, "onFriends: " + friends.size());
    }

    @Override
    public void onFriendConnection(Carrier carrier, String friendId, ConnectionStatus status) {
        Log.d(TAG, "onFriendConnection: " + friendId + " status: " + status);
        // Utils.addCacheFriend(friendId);
        switch (status) {
            case Connected:
                Utils.addCacheOnlineFriend(friendId);
                break;
            case Disconnected:
                Utils.removeCacheOnlineFriend(friendId);
                break;
        }
    }

    @Override
    public void onFriendInfoChanged(Carrier carrier, String friendId, FriendInfo info) {
        Log.d(TAG, "onFriendInfoChanged: " + info);
    }

    @Override
    public void onFriendPresence(Carrier carrier, String friendId, PresenceStatus presence) {
        Log.d(TAG, "onFriendPresence: " + friendId + " presence: " + presence);
    }

    @Override
    public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String fromAddress) {
        Log.d(TAG, "onFriendRequest: " + info + "; hello: " + fromAddress);
        try {
            Carrier.getInstance().acceptFriend(userId);
            Utils.addCacheFriend(fromAddress);
        } catch (Exception e) {
            Log.e(TAG, "onFriendRequest: ", e);
        }
    }

    @Override
    public void onFriendAdded(Carrier carrier, FriendInfo friendInfo) {
        Log.d(TAG, "onFriendAdded: " + friendInfo);
        try {
            // Utils.addCacheFriend(friendInfo.getUserId());
        } catch (Exception e) {
            Log.e(TAG, "onFriendAdded: ", e);
        }
    }

    @Override
    public void onFriendRemoved(Carrier carrier, String friendId) {

    }

    @Override
    public void onFriendMessage(Carrier carrier, String from, byte[] message, boolean isOffline) {

    }

    @Override
    public void onFriendInviteRequest(Carrier carrier, String from, String data) {
        // .send friend invite messages
        if (data != null && data.contains("invite") && data.contains("calleeAddress")) { //通过添加好友的消息回执绕过carrier message 1024字符的限制

            //启动进去CallActivity
            JSONObject json = null;
            JSONObject msg = null;
            try {
                json = new JSONObject(data);
                String message = json.optString("msg");
                msg = new JSONObject(message);
                String type = msg.optString("type");
                String callee = msg.optString("calleeAddress");

                if ("invite".equalsIgnoreCase(type) && !TextUtils.isEmpty(callee)) {
                    Log.d(TAG, "onFriendInviteRequest: ");
                    DashboardFragment.INSTANCE.receiveCall(callee);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGroupInvite(Carrier carrier, String from, byte[] cookie) {

    }
}
