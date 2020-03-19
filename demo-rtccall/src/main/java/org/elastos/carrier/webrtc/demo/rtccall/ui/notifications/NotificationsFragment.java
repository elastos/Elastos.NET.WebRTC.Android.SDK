package org.elastos.carrier.webrtc.demo.rtccall.ui.notifications;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import org.elastos.carrier.webrtc.demo.rtccall.R;

import org.elastos.carrier.Carrier;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    private NotificationsViewModel notificationsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel.class);
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        final ImageView imageView = root.findViewById(R.id.qr_code_iv);
        final TextView addressView = root.findViewById(R.id.carrier_address_tv);
        final TextView idView = root.findViewById(R.id.carrier_id_tv);
        notificationsViewModel.getText().observe(this, new Observer<Bitmap>() {
            @Override
            public void onChanged(@Nullable Bitmap s) {
                imageView.setImageBitmap(s);
            }
        });
        notificationsViewModel.getCarrierAddress().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                addressView.setText(s);
            }
        });
        notificationsViewModel.getCarrierId().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                idView.setText(s);
            }
        });
        try {
            notificationsViewModel.resetQrText(Carrier.getInstance().getUserId(), 320, 320);
            notificationsViewModel.setCarrierId(Carrier.getInstance().getUserId());
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: ", e);
        }
        return root;
    }

    @Override
    public void onResume() {
        try {
            notificationsViewModel.resetQrText(Carrier.getInstance().getUserId(), 320, 320);
            notificationsViewModel.setCarrierId(Carrier.getInstance().getUserId());
        } catch (Exception e) {
            Log.e(TAG, "onResume: ", e);
        }
        super.onResume();
    }


}