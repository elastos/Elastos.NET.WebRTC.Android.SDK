package org.elastos.carrier.webrtc.demo_apprtc;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.elastos.carrier.webrtc.demo_apprtc.model.User;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomListAdapter<T> extends ArrayAdapter {

    private List<String> onlineList = new ArrayList<>();

    public CustomListAdapter(@NonNull Context context, @LayoutRes int resource,
                         @NonNull List<User> userIds) {
        super(context, resource, userIds);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        User user = (User) getItem(position);
        ((TextView) view).setText("[" + user.getName() + "]\n" + user.getId());
        if (this.onlineList.contains(user.getId())) {
            ((TextView) view).setTextColor(Color.GREEN);
        } else {
            ((TextView) view).setTextColor(Color.WHITE);
        }
        return view;
    }

    public void addOnline(String id) {
        if (!this.onlineList.contains(id)) {
            this.onlineList.add(id);
        }
    }

    public void removeOnline(String id) {
        this.onlineList.remove(id);
    }

    public void setOnlineList(List<String> onlineList) {
        this.onlineList = onlineList;
    }
}
