package com.allcom.elastos_webrtc_call;

import android.text.TextUtils;
import android.util.Log;

import com.allcom.elastos_webrtc_call.ui.dashboard.DashboardFragment;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.json.JSONException;
import org.json.JSONObject;

public class MyCarrierHandler extends AbstractCarrierHandler {

    private static final String TAG = "MyCarrierHandler";

    @Override
    public void onFriendInviteRequest(Carrier carrier, String from, String data) {
        super.onFriendInviteRequest(carrier, from, data);
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
}
