package com.baidu.mobads.demo.main;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.BDAdConfig;
import com.baidu.mobads.sdk.api.BDDialogParams;
import com.baidu.mobads.sdk.api.MobadsPermissionSettings;
import com.baidu.mobads.sdk.api.NovelSDKConfig;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;

public class MobadsApplication extends Application {

    private static Application sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        // 重要：适配安卓P，如果WebView使用多进程，添加如下代码
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = getProcessName(this);
            // 填入应用自己的包名
            if (!"com.baidu.mobads.demo.main".equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }

        if (getProcessName(this).startsWith("com.baidu.mobads.demo.main")) {
            // 初始化信息，初始化一次即可，（此处用startsWith()，可包括激励/全屏视频的进程）
            final BDAdConfig bdAdConfig = new BDAdConfig.Builder()
                    // 1、设置app名称，可选
                    .setAppName("网盟demo")
                    // 2、应用在mssp平台申请到的appsid，和包名一一对应，此处设置等同于在AndroidManifest.xml里面设置
                    .setAppsid("e866cfb0")
                    .setBDAdInitListener(new BDAdConfig.BDAdInitListener() {
                        @Override
                        public void success() {
                            Log.e("MobadsApplication","SDK初始化成功");
                        }

                        @Override
                        public void fail() {
                            Log.e("MobadsApplication","SDK初始化失败");
                        }
                    })
                    // 3、设置下载弹窗的类型和按钮动效样式，可选
                    .setDialogParams(new BDDialogParams.Builder()
                            .setDlDialogType(BDDialogParams.TYPE_BOTTOM_POPUP)
                            .setDlDialogAnimStyle(BDDialogParams.ANIM_STYLE_NONE)
                            .build())
                    // 4、设置微信openSDK 应用id
                    .setWXAppid("wx123456")
                    // 5.媒体debug日志调试开关 调试阶段打开，上线前需关闭
                    .setDebug(false)
                    // 6.设置在UI线程校验广告可见性，若APP出现View子线程操作相关的异常问题，可以设置此参数为true
                    // .checkAdInUiThread(false)
                    .build(this);
            bdAdConfig.preInit();

            /**
             * 注意：init方法需要在用户"同意隐私协议"后方可调用，但必须要调用才可完全使用SDK功能
             */
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    bdAdConfig.init();
                }
            };
            handler.postDelayed(runnable, 1000);


            // 合规设置，设置APP的ICON资源，系统通知使用
            AdSettings.setNotificationIcon(R.mipmap.ic_launcher);

            // 设置SDK可以使用的权限，包含：设备信息、定位、存储、APP LIST
            // 注意：建议授权SDK读取设备信息，SDK会在应用获得系统权限后自行获取IMEI等设备信息
            // 授权SDK获取设备信息会有助于提升ECPM
            //设置SDK获取设备信息权限（建议）
            MobadsPermissionSettings.setPermissionReadDeviceID(AdSettingHelper.getInstance()
                    .getBooleanFromSetting(AdSettingProperties.COMMON_PERMISSION_PHONE_STATE, true));
            //设置SDK获取应用列表权限（建议）
            MobadsPermissionSettings.setPermissionAppList(AdSettingHelper.getInstance()
                    .getBooleanFromSetting(AdSettingProperties.COMMON_PERMISSION_APP_LIST, false));
            //设置SDK获取定位权限
            MobadsPermissionSettings.setPermissionLocation(AdSettingHelper.getInstance()
                    .getBooleanFromSetting(AdSettingProperties.COMMON_PERMISSION_LOCATION, false));
            //设置SDK获取外部存储权限
            MobadsPermissionSettings.setPermissionStorage(AdSettingHelper.getInstance()
                    .getBooleanFromSetting(AdSettingProperties.COMMON_PERMISSION_STORAGE, false));
        }
        // 媒体需要小说产品的情况下，需要调用该方法进行初始化，该Appsid不需要与实例化BDAdConfig使用同一个appsid。
        // 我们推荐但不强制媒体在Application中调用该方法，媒体保证在使用小说产品之前该方法能够得到执行即可。
        NovelSDKConfig.attachBaseContext(MobadsApplication.getContext(), "c0da1ec4", "Your App Name");
    }

    private String getProcessName(Context context) {
        if (context == null) return null;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName;
            }
        }
        return null;
    }

    public static Application getContext(){
        return sInstance;
    }
}
