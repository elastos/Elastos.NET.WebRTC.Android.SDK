package com.allcom.elastos_webrtc_call.util;

import android.util.Log;

import com.allcom.elastos_webrtc_call.MainActivity;
import com.allcom.elastos_webrtc_call.ui.dashboard.DashboardFragment;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final String TAG = "";

    public static List<String> ONLINE_FRIENDS = null;

    public static void addCacheFriend(String friend) {
        try {
            ACache aCache = ACache.get(MainActivity.INSTANCE);
            JSONArray friends = aCache.getAsJSONArray(Constants.CACHE_KEY_FRIENDS);
            if (friends == null) {
                friends = new JSONArray();
            }
            boolean exists = false;
            if (friends.length() > 0) {
                for (int i = 0; i < friends.length(); i++) {
                    if (friend.equals(friends.getString(i))) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                friends.put(friend);
            }
            aCache.put(Constants.CACHE_KEY_FRIENDS, friends);
            if (DashboardFragment.INSTANCE != null && DashboardFragment.INSTANCE.isVisible()) {
                DashboardFragment.INSTANCE.initFriends();
            }
        } catch (Exception e) {
            Log.e(TAG, "addCacheFriend: ", e);
        }
    }

    public static void addCacheOnlineFriend(String friend) {
        try {
            if (ONLINE_FRIENDS == null) {
                ONLINE_FRIENDS = new ArrayList<>();
            }
            if (!ONLINE_FRIENDS.contains(friend)) {
                ONLINE_FRIENDS.add(friend);
            }
            if (DashboardFragment.INSTANCE != null && DashboardFragment.INSTANCE.isVisible()) {
                DashboardFragment.INSTANCE.initFriends();
            }
        } catch (Exception e) {
            Log.e(TAG, "addCacheOnlineFriend: ", e);
        }
    }

    public static void removeCacheOnlineFriend(String friend) {
        try {
            if (ONLINE_FRIENDS == null || ONLINE_FRIENDS.isEmpty()) {
                return;
            }
            if (ONLINE_FRIENDS.contains(friend)) {
                ONLINE_FRIENDS.remove(friend);
            }
            if (DashboardFragment.INSTANCE != null && DashboardFragment.INSTANCE.isVisible()) {
                DashboardFragment.INSTANCE.initFriends();
            }
        } catch (Exception e) {
            Log.e(TAG, "addCacheOnlineFriend: ", e);
        }
    }

}
