package org.elastos.carrier.webrtc.demo.rtccall;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.elastos.carrier.webrtc.WebrtcClient;
import org.elastos.carrier.webrtc.demo.rtccall.eos.CarrierHandlerImpl;
import org.elastos.carrier.webrtc.demo.rtccall.ui.dashboard.DashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.elastos.carrier.Carrier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static MainActivity INSTANCE;

    public static boolean carrierOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        INSTANCE = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        initialCarrier();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();

        // initWebrtc();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_friends_btn:
                if (carrierOnline) {
                    startActivity(new Intent(this, ScanActivity.class));
                }
                break;
        }
    }

    private void initialCarrier() {
        try {
            // CarrierClient.getInstance(this).addCarrierHandler(new MyCarrierHandler());
            CarrierClient.getInstance(this).addCarrierHandler(new CarrierHandlerImpl());
            // Carrier.initializeInstance(new CarrierOption(this.getFilesDir().getParent()), new CarrierHandlerImpl());
            Carrier.getInstance().start(0);
            Log.i(TAG, "Carrier node is ready now");
            initWebrtc();
        } catch (Exception e) {
            Log.e(TAG, "initialCarrier: ", e);
        }
    }

    private void initWebrtc() {
        try {
            WebrtcClient.initialize(this, Carrier.getInstance(), new CallHandlerImpl(),  null);
            //
        }  catch (Exception e) {
            Log.e(TAG, "initWebrtc: error", e);
        }
    }

    private void requestPermissions () {
        String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_NETWORK_STATE};
        if (!EasyPermissions.hasPermissions(this, permissions)) {
            EasyPermissions.requestPermissions(this, "need permissions to continue", 1, permissions);
        }
    }

    public void setCarrierOnline(boolean online) {
        carrierOnline = online;
        if (DashboardFragment.INSTANCE != null && DashboardFragment.INSTANCE.isVisible()) {
            DashboardFragment.INSTANCE.setCarrierOnline(online);
        }
    }
}
