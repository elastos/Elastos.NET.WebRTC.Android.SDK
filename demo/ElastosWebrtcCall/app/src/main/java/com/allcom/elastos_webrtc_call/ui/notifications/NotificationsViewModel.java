package com.allcom.elastos_webrtc_call.ui.notifications;

import android.graphics.Bitmap;

import com.allcom.elastos_webrtc_call.util.ZXingUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class NotificationsViewModel extends ViewModel {

    private MutableLiveData<Bitmap> qrCodeIv;

    private MutableLiveData<String> carrierAddressTv;

    private MutableLiveData<String> carrierIdTv;

    public NotificationsViewModel() {
        qrCodeIv = new MutableLiveData<>();
        carrierIdTv = new MutableLiveData<>();
        carrierAddressTv = new MutableLiveData<>();
    }

    public LiveData<Bitmap> getText() {
        return qrCodeIv;
    }

    public LiveData<String> getCarrierAddress() {
        return carrierAddressTv;
    }

    public LiveData<String> getCarrierId() {
        return carrierIdTv;
    }

    public void resetQrText(String s, int w, int h) {
        qrCodeIv.setValue(ZXingUtils.createQRImage(s, w, h));
        carrierAddressTv.setValue(s);
    }

    public void setCarrierId(String id) {
        carrierIdTv.setValue(id);
    }
}