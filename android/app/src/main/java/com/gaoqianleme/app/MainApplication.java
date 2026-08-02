package com.yuexuxingzuo.app;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.baidu.mobads.sdk.api.BDAdConfig;
import com.baidu.mobads.sdk.api.MobadsPermissionSettings;

public class MainApplication extends Application {

    private static final String TAG = "MainApplication";
    private static final String APP_ID = "d423b637";

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Application onCreate");
        
        String deviceId = getMyDeviceId();
        Log.d(TAG, "========================================");
        Log.d(TAG, "设备 ID: " + deviceId);
        Log.d(TAG, "请将此设备 ID 添加到百度联盟后台的测试设备列表中");
        Log.d(TAG, "========================================");
        
        initBaiduAdSDK();
    }

    private String getMyDeviceId() {
        try {
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Log.e(TAG, "获取设备ID失败: " + e.getMessage());
            return "unknown";
        }
    }

    private void initBaiduAdSDK() {
        try {
            Log.d(TAG, "开始初始化百度广告SDK，App ID: " + APP_ID);
            
            BDAdConfig bdAdConfig = new BDAdConfig.Builder()
                    .setAppName("简序清单")
                    .setAppsid(APP_ID)
                    .setBDAdInitListener(new BDAdConfig.BDAdInitListener() {
                        @Override
                        public void success() {
                            Log.d(TAG, "✅ 百度广告SDK初始化成功");
                        }

                        @Override
                        public void fail() {
                            Log.e(TAG, "❌ 百度广告SDK初始化失败");
                        }
                    })
                    .setDebug(true)
                    .build(this);
            
            bdAdConfig.init();
            
            MobadsPermissionSettings.setPermissionReadDeviceID(true);
            MobadsPermissionSettings.setPermissionAppList(true);
            MobadsPermissionSettings.setPermissionLocation(true);
            MobadsPermissionSettings.setPermissionStorage(true);
            
            Log.d(TAG, "百度广告SDK初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "百度广告SDK初始化异常: " + e.getMessage(), e);
        }
    }
}
