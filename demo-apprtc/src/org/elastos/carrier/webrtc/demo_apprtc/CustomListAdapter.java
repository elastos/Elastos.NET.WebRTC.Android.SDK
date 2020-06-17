package org.elastos.carrier.webrtc.demo_apprtc;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomListAdapter<T> extends ArrayAdapter {

    private List<String> onlineList = new ArrayList<>();

    public CustomListAdapter(@NonNull Context context, @LayoutRes int resource,
                         @NonNull List<String> userIds) {
        super(context, resource, userIds);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        String id = (String) getItem(position);
        if (this.onlineList.contains(id))
            ((TextView) view).setTextColor(Color.GREEN);
        else
            ((TextView) view).setTextColor(Color.WHITE);
        return super.getView(position, convertView, parent);
    }

    public void addOnline(String id) {
        if (!this.onlineList.contains(id))
            this.onlineList.add(id);
    }

    public void removeOnline(String id) {
        this.onlineList.remove(id);
    }

    public void setOnlineList(List<String> onlineList) {
        this.onlineList = onlineList;
    }
}
