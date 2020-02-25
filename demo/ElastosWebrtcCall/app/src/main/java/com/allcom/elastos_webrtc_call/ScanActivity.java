package com.allcom.elastos_webrtc_call;

import android.os.Bundle;
import android.util.Log;

import com.allcom.elastos_webrtc_call.R;
import com.allcom.elastos_webrtc_call.util.Utils;

import org.elastos.carrier.Carrier;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class ScanActivity extends AppCompatActivity implements QRCodeView.Delegate {

    private static final String TAG = "scan_activity";

    private ZXingView zXingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        zXingView = findViewById(R.id.zxingview);
        zXingView.setDelegate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        zXingView.startCamera();
        zXingView.startSpotAndShowRect();
    }

    @Override
    protected void onStop() {
        zXingView.stopCamera();
        super.onStop();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Log.i(TAG, "onScanQRCodeSuccess: " + result);
        try {
            Carrier.getInstance().addFriend(result, Carrier.getInstance().getAddress());
            Utils.addCacheFriend(result);
        } catch (Exception e) {
            Log.e(TAG, "onScanQRCodeSuccess: ", e);
        }

        finish();
    }

    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e(TAG, "onScanQRCodeOpenCameraError: ");
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {
        Log.e(TAG, "onCameraAmbientBrightnessChanged: ");
    }
}
