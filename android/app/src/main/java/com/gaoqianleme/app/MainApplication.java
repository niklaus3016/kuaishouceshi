package com.mijingxingzuo.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;

import androidx.multidex.MultiDex;

public class MainApplication extends Application {

    private static final String TAG = "MainApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // MultiDex 安装：防止方法数超 65535 导致启动闪退
        try {
            MultiDex.install(this);
        } catch (Throwable t) {
            Log.e(TAG, "MultiDex install 异常: " + t.getMessage(), t);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Application onCreate");

        String deviceId = getMyDeviceId();
        Log.d(TAG, "========================================");
        Log.d(TAG, "设备 ID: " + deviceId);
        Log.d(TAG, "请将此设备 ID 添加到快手联盟后台的测试设备列表中");
        Log.d(TAG, "========================================");

        // Android P+ WebView 多进程数据目录隔离
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                String processName = getCurrentProcessName();
                if (processName != null && !processName.equals(getPackageName())) {
                    WebView.setDataDirectorySuffix(processName);
                }
            } catch (Throwable ignore) {
            }
        }

        // 仅在主进程初始化 SDK，避免多进程重复初始化造成的资源浪费和异常
        if (isMainProcess(this)) {
            // 注意：如果有隐私弹窗流程，应该在用户点击"同意"后再初始化（或调用 onUserAgreePrivacy 补初始化）。
            // 此处为了保证首次加载广告可用，默认用户已同意隐私（若业务存在明确合规流程，
            // 请将 UserDataObtainController.setUserAgree(true) 和 initSDK 移到用户同意之后触发）。
            UserDataObtainController.getInstance().setUserAgree(true);
            KSSdkInitUtil.initSDK(this);
        }
    }

    private String getMyDeviceId() {
        try {
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Log.e(TAG, "获取设备ID失败: " + e.getMessage());
            return "unknown";
        }
    }

    /** 判断当前是否为主进程（与 Application.getProcessName 比较） */
    private static boolean isMainProcess(Context context) {
        try {
            String pkg = context.getPackageName();
            String processName = getCurrentProcessName();
            return processName == null || pkg.equals(processName);
        } catch (Throwable t) {
            // 无法判时默认按主进程处理（宁可 init 也不要广告不可用）
            return true;
        }
    }

    private static String getCurrentProcessName() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return Application.getProcessName();
            }
            // 低版本通过 ActivityManager 反射
            Class<?> at = Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method m = at.getMethod("currentProcessName");
            Object o = m.invoke(null);
            return o instanceof String ? (String) o : null;
        } catch (Throwable ignore) {
            return null;
        }
    }
}
