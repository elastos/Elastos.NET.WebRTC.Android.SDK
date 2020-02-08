package com.allcom.elastos_webrtc_android_demo;

import android.os.Bundle;
import android.util.Log;

import com.allcom.elastos_webrtc_android_demo.ui.dashboard.DashboardFragment;
import com.allcom.elastos_webrtc_android_demo.util.ACache;
import com.allcom.elastos_webrtc_android_demo.util.Constants;
import com.allcom.elastos_webrtc_android_demo.util.Utils;

import org.elastos.carrier.Carrier;
import org.json.JSONArray;

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
            Carrier.getInstance().addFriend(result, "hi");
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
