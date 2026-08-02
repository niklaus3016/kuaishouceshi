package com.baidu.mobads.demo.main.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.adSettings.AdSettingActivity;
import com.baidu.mobads.demo.main.tools.preview.CaptureActivity;

public class BasePermissionActivity extends Activity {


    public static final int DEFAULT_SCAN_MODE = 1001;
    public static final int SCAN_PERMISSION_CODE = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting_menu_enter) {
            Intent intent = new Intent(BasePermissionActivity.this, AdSettingActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.scan_menu_item) {
            // DEFAULT_VIEW为用户自定义用于接收权限校验结果。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.requestPermissions(
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        SCAN_PERMISSION_CODE);
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null || grantResults.length < 2 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1]
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (requestCode == SCAN_PERMISSION_CODE) {
            Intent intent = new Intent(this, CaptureActivity.class);
            startActivityForResult(intent, DEFAULT_SCAN_MODE);
        }
    }
}
