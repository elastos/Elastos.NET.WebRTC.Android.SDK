/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.elastos.carrier.webrtc.demo_apprtc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.elastos.carrier.AbstractCarrierHandler;
import org.elastos.carrier.Carrier;
import org.elastos.carrier.ConnectionStatus;
import org.elastos.carrier.FriendInfo;
import org.elastos.carrier.UserInfo;
import org.elastos.carrier.exceptions.CarrierException;
import org.elastos.carrier.webrtc.demo_apprtc.apprtc.R;
import org.elastos.carrier.webrtc.demo_apprtc.util.QRCodeUtils;

import java.util.ArrayList;

import androidx.core.app.ActivityCompat;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class InfoActivity extends Activity {
  private static final String TAG = "InfoActivity";
  private static final int CONNECTION_REQUEST = 1;
  private static final int PERMISSION_REQUEST = 2;
  private static final int SCAN_REQUEST = 3;
  private static final int REMOVE_FAVORITE_INDEX = 0;
  private static boolean commandLineRun = false;

  private ImageButton addFavoriteButton;
  private Button copyAddressButton;
  private EditText roomEditText;
  private ImageView mQRCodeImage;
  private TextView mAdrress;
  public static InfoActivity INSTANCE;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    INSTANCE = this;

    // Get setting keys.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    setContentView(R.layout.activity_info);

    roomEditText = findViewById(R.id.room_edittext);
    roomEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_DONE) {
          addFavoriteButton.performClick();
          return true;
        }
        return false;
      }
    });
    roomEditText.requestFocus();

    mQRCodeImage = findViewById(R.id.myaddressqrcode);
    mAdrress = findViewById(R.id.myaddress);
    addFavoriteButton = findViewById(R.id.add_favorite_button);
    addFavoriteButton.setOnClickListener(addFavoriteListener);
    copyAddressButton = findViewById(R.id.copy_address);
    copyAddressButton.setOnClickListener(copyAddressListener);

    requestPermissions();

    /*
    CarrierClient.getInstance(this).addCarrierHandler(new AbstractCarrierHandler() {
      @Override
      public void onReady(Carrier carrier) {

      }

      @Override
      public void onFriendConnection(Carrier carrier, String friendId, ConnectionStatus status) {
        super.onFriendConnection(carrier, friendId, status);
        Log.d(TAG, "onFriendConnection: " + friendId);
        switch (status) {
          case Connected:
            ConnectActivity.INSTANCE.addOnlineFriend(friendId);
            break;
          case Disconnected:
            ConnectActivity.INSTANCE.removeOnlineFriend(friendId);
            break;
          default:
            break;
        }
      }

      @Override
      public void onFriendAdded(Carrier carrier, FriendInfo info) {
        super.onFriendAdded(carrier, info);
        Log.w(TAG, "onFriendAdded: " + info.toString());
        // add favorite
        ConnectActivity.INSTANCE.saveFriend(info.getUserId(), (info.getName() == null || "".equals(info.getName().trim())) ? "Unknown" : info.getName());
      }

      @Override
      public void onFriendInviteRequest(Carrier carrier, String from, String data) {
        super.onFriendInviteRequest(carrier, from, data);
      }

      @Override
      public void onFriendRequest(Carrier carrier, String userId, UserInfo info, String hello) {
        super.onFriendRequest(carrier, userId, info, hello);
        Log.w(TAG, "onFriendAdded: " + info.toString());
        // add favorite
        ConnectActivity.INSTANCE.saveFriend(info.getUserId(), (info.getName() == null || "".equals(info.getName().trim())) ? "Unknown" : info.getName());
      }
    });
    */

    String userId = null;
    String address = null;
    try {
      address = CarrierClient.getInstance(this).getCarrier().getAddress();
      userId = CarrierClient.getInstance(this).getCarrier().getUserId();
    } catch (CarrierException e) {
      e.printStackTrace();
    }

    mQRCodeImage.setImageBitmap(QRCodeUtils.createQRCodeBitmap(address));
    mAdrress.setText(userId);

    if (Build.VERSION.SDK_INT >= 23) {
      int REQUEST_CODE_CONTACT = 101;
      String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
      //验证是否许可权限
      for (String str : permissions) {
        if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
          //申请权限
          this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
          return;
        }
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.info_menu, menu);
    return true;
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (item.getItemId() == REMOVE_FAVORITE_INDEX) {
      AdapterView.AdapterContextMenuInfo info =
          (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      return true;
    }

    return super.onContextItemSelected(item);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items.
    if (item.getItemId() == R.id.addfriend) {
      showCamera();
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  public void showCamera() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
      requestCameraPermission();
    } else {
      IntentIntegrator integrator = new IntentIntegrator(InfoActivity.this);
      integrator.setOrientationLocked(true);
      integrator.initiateScan();
    }
  }

  private void requestCameraPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.CAMERA)) {
      Snackbar.make(roomEditText, "获取摄像头权限",
              Snackbar.LENGTH_INDEFINITE)
              .setAction("OK", new OnClickListener() {
                @Override
                public void onClick(View view) {
                  ActivityCompat.requestPermissions(InfoActivity.this,
                          new String[]{Manifest.permission.CAMERA},
                          SCAN_REQUEST);
                }
              })
              .show();
    } else {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
              SCAN_REQUEST);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if (scanResult != null) {
      String result = scanResult.getContents();
      if (result != null && !result.isEmpty()) {
        roomEditText.post(new Runnable() {
          @Override
          public void run() {
            String id = Carrier.getIdFromAddress(result);
            if (id==null) {
              id=result;
            }
            roomEditText.setText(id);
            try {
              CarrierClient.getInstance(InfoActivity.this).addFriend(result);
            } catch (CarrierException e) {
              e.printStackTrace();
            }
          }
        });
      }
      return;
    }

    if (requestCode == CONNECTION_REQUEST && commandLineRun) {
      Log.d(TAG, "Return: " + resultCode);
      setResult(resultCode);
      commandLineRun = false;
      finish();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == PERMISSION_REQUEST) {
      String[] missingPermissions = getMissingPermissions();
      if (missingPermissions.length != 0) {
        // User didn't grant all the permissions. Warn that the application might not work
        // correctly.
        new AlertDialog.Builder(this)
            .setMessage(R.string.missing_permissions_try_again)
            .setPositiveButton(R.string.yes,
                (dialog, id) -> {
                  // User wants to try giving the permissions again.
                  dialog.cancel();
                  requestPermissions();
                })
            .setNegativeButton(R.string.no,
                (dialog, id) -> {
                  // User doesn't want to give the permissions.
                  dialog.cancel();
                  onPermissionsGranted();
                })
            .show();
      } else {
        // All permissions granted.
        onPermissionsGranted();
      }
    }
  }

  private void onPermissionsGranted() {
    // If an implicit VIEW intent is launching the app, go directly to that URL.
    final Intent intent = getIntent();
    if ("android.intent.action.VIEW".equals(intent.getAction()) && !commandLineRun) {
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void requestPermissions() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      // Dynamic permissions are not required before Android M.
      onPermissionsGranted();
      return;
    }

    String[] missingPermissions = getMissingPermissions();
    if (missingPermissions.length != 0) {
      requestPermissions(missingPermissions, PERMISSION_REQUEST);
    } else {
      onPermissionsGranted();
    }
  }

  @TargetApi(Build.VERSION_CODES.M)
  private String[] getMissingPermissions() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return new String[0];
    }

    PackageInfo info;
    try {
      info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, "Failed to retrieve permissions.");
      return new String[0];
    }

    if (info.requestedPermissions == null) {
      Log.w(TAG, "No requested permissions.");
      return new String[0];
    }

    ArrayList<String> missingPermissions = new ArrayList<>();
    for (int i = 0; i < info.requestedPermissions.length; i++) {
      if ((info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
        missingPermissions.add(info.requestedPermissions[i]);
      }
    }
    Log.d(TAG, "Missing permissions: " + missingPermissions);

    return missingPermissions.toArray(new String[missingPermissions.size()]);
  }

  private final OnClickListener addFavoriteListener = new OnClickListener() {
    @Override
    public void onClick(View view) {
      String newRoom = roomEditText.getText().toString();
      // 添加到列表中
      if (newRoom.length() > 0) {
        ConnectActivity.INSTANCE.saveFriend(newRoom, "Unknown");
      }
    }
  };

  private final OnClickListener copyAddressListener = new OnClickListener() {
    @Override
    public void onClick(View view) {
      try {
        String address = CarrierClient.getInstance(INSTANCE).getCarrier().getAddress();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("", address);
        cm.setPrimaryClip(clipData);
        Toast.makeText(INSTANCE, String.format("%s copied", address), Toast.LENGTH_LONG).show();
      } catch (Exception e) {
        Log.e(TAG, "onClick: ", e);
      }
    }
  };
}
