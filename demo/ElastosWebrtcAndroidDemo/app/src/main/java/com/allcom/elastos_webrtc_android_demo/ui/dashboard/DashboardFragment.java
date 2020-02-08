package com.allcom.elastos_webrtc_android_demo.ui.dashboard;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.allcom.elastos_webrtc.ElastosWebrtc;
import com.allcom.elastos_webrtc_android_demo.MainActivity;
import com.allcom.elastos_webrtc_android_demo.R;
import com.allcom.elastos_webrtc_android_demo.adapter.BaseAdapter;
import com.allcom.elastos_webrtc_android_demo.model.Friend;
import com.allcom.elastos_webrtc_android_demo.ui.RecyclerListAdapter;
import com.allcom.elastos_webrtc_android_demo.util.ACache;
import com.allcom.elastos_webrtc_android_demo.util.Constants;
import com.allcom.elastos_webrtc_android_demo.util.Utils;

import org.json.JSONArray;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    public static DashboardFragment INSTANCE;

    private static final String TAG = "DashboardFragment";

    public static DashboardViewModel dashboardViewModel;
    private TextView addFriendsButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INSTANCE = this;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        initFriends();
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        addFriendsButton = root.findViewById(R.id.add_friends_btn);
        setCarrierOnline(MainActivity.carrierOnline);
        final RecyclerView listView = root.findViewById(R.id.friends_rv);
        dashboardViewModel.getValue().observe(this, new Observer<List<Friend>>() {
            @Override
            public void onChanged(@Nullable List<Friend> s) {
                RecyclerListAdapter listAdapter = new RecyclerListAdapter(s, R.layout.list_item);
                listAdapter.addAll(s);
                listAdapter.setOnItemClickListener(new OnItemClick());
                listView.setLayoutManager(new LinearLayoutManager(getActivity()));
                listView.setAdapter(listAdapter);
            }
        });

        return root;
    }

    public void initFriends() {
        try {
            ACache aCache = ACache.get(MainActivity.INSTANCE);
            JSONArray array = aCache.getAsJSONArray(Constants.CACHE_KEY_FRIENDS);
            if (array != null && array.length() > 0) {
                final List<Friend> friends = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    String friend = array.getString(i);
                    boolean online = false;
                    if (Utils.ONLINE_FRIENDS != null && Utils.ONLINE_FRIENDS.contains(friend)) {
                        online = true;
                    }
                    friends.add(new Friend(online, array.getString(i)));
                }

                if (this.getView() != null) {
                    this.getView().post(new Runnable() {
                        @Override
                        public void run() {
                            dashboardViewModel.updateValue(friends);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "initFriends: ", e);
        }

    }

    @TargetApi(23)
    public void setCarrierOnline(boolean online) {
//        addFriendsButton.setTextAppearance(online ? R.style.MatchWrapTv : R.style.MatchWrapTvDisabled);
        addFriendsButton.setAlpha(online ? 1.0f : 0.3f);
    }

    @Override
    public void onResume() {
        super.onResume();
        initFriends();
    }

    public class OnItemClick implements BaseAdapter.OnItemClickListener<Friend> {
        @Override
        public void onClick(View view, int position, Friend item) {
            Log.e(TAG, "onClick: " + item.getName());
            if (item.isOnline()) {
                ElastosWebrtc.getInstance().invite(item.getName());
            }
        }

        @Override
        public void onLongClick(View view, int position, Friend item) {
            Log.e(TAG, "onLongClick: " + item.getName());
        }
    }
}