package com.allcom.elastos_webrtc_android_demo.ui.dashboard;

import com.allcom.elastos_webrtc_android_demo.model.Friend;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {

    private MutableLiveData<List<Friend>> friends;

    public DashboardViewModel() {
        friends = new MutableLiveData<>();
//        List<String> list = new ArrayList<>();
//        list.add("Jack");
//        list.add("Michale");
//        list.add("Jenny");
//        list.add("Jenny");
//        list.add("Json");
//        list.add("Anne");
//        list.add("Master");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        list.add("Joy");
//        friends.setValue(list);
    }

    public LiveData<List<Friend>> getValue() {
        return friends;
    }

    public void updateValue(List<Friend> f) {
        this.friends.setValue(f);
    }

    public void add(Friend f) {
        if (this.friends.getValue() == null) {
            updateValue(new ArrayList<Friend>());
        }
        this.friends.getValue().add(f);
    }

    public boolean contains(String f) {
        return this.friends.getValue() != null && !this.friends.getValue().isEmpty() && this.friends.getValue().contains(f);
    }
}