package com.mijingxingzuo.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.provider.Settings;
import android.app.ActivityManager;

import android.view.Display;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RiskDetector {
    public static class RiskResult {
        public boolean hasRisk;
        public String riskDescription;

        public RiskResult(boolean hasRisk, String riskDescription) {
            this.hasRisk = hasRisk;
            this.riskDescription = riskDescription;
        }
    }

    public static RiskResult checkAllRisks(Context context) {
        // Debug版本跳过所有安全检测，便于开发调试
        if (isDebugBuild(context)) {
            return new RiskResult(false, "Debug模式已跳过安全检测");
        }
        
        List<String> risks = new ArrayList<>();

        // 检测1：开发者选项 + USB调试 + 无线调试
        if (isDeveloperOptionsEnabled(context)) {
            risks.add("开发者选项已开启");
        }
        if (isUsbDebuggingEnabled(context)) {
            risks.add("USB调试已开启");
        }
        if (isWirelessDebuggingEnabled(context)) {
            risks.add("无线调试已开启");
        }

        // 检测2：Root + Xposed/LSPosed/Magisk
        if (isRooted()) {
            risks.add("设备已Root");
        }
        if (isXposedInstalled(context)) {
            risks.add("Xposed框架已安装");
        }
        if (isMagiskInstalled(context)) {
            risks.add("Magisk框架已安装");
        }

        // 检测3：投屏/屏幕镜像 + 录屏
        if (isScreenCastingActive(context)) {
            risks.add("检测到投屏/录屏环境");
        }

        // 检测5：悬浮窗权限（辅助检测）
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    risks.add("悬浮窗权限已授予");
                }
            }
        } catch (Exception ignored) {}

        // 检测6：模拟器 + 应用多开/分身
        if (isEmulator()) {
            risks.add("检测到模拟器环境");
        }
        if (isAppCloned(context)) {
            risks.add("检测到应用多开/分身环境");
        }

        if (risks.isEmpty()) {
            return new RiskResult(false, "");
        } else {
            String riskDesc = String.join("、", risks);
            return new RiskResult(true, riskDesc);
        }
    }

    // ========== 检测方法实现 ==========

    private static boolean isDeveloperOptionsEnabled(Context context) {
        try {
            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
            return enabled == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isUsbDebuggingEnabled(Context context) {
        try {
            int enabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
            return enabled == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWirelessDebuggingEnabled(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                int enabled = Settings.Global.getInt(context.getContentResolver(), "adb_wifi_enabled", 0);
                return enabled == 1;
            }
        } catch (Exception e) {
            // 忽略低版本
        }
        return false;
    }

    private static boolean isRooted() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        try {
            Process process = Runtime.getRuntime().exec("which su");
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = in.readLine();
            in.close();
            return line != null && !line.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isXposedInstalled(Context context) {
        try {
            // 检查常见Xposed包名
            String[] packages = {
                "de.robv.android.xposed",
                "de.robv.android.xposed.installer",
                "io.github.libxposed",
                "io.github.libxposed.service"
            };

            PackageManager pm = context.getPackageManager();
            for (String pkg : packages) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static boolean isMagiskInstalled(Context context) {
        try {
            String[] packages = {
                "com.topjohnwu.magisk"
            };

            PackageManager pm = context.getPackageManager();
            for (String pkg : packages) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static boolean isScreenCastingActive(Context context) {
        try {
            // ============================================
            // 检测1：系统无线投屏 - 虚拟显示器/外接显示器
            // ============================================
            DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            if (displayManager != null) {
                Display[] displays = displayManager.getDisplays();
                for (Display display : displays) {
                    int displayId = display.getDisplayId();
                    // 检查是否有虚拟显示器或外接显示器
                    if (displayId != Display.DEFAULT_DISPLAY) {
                        // 检查显示器名称，看是否包含投屏相关关键词
                        String name = display.getName();
                        if (name != null) {
                            String lowerName = name.toLowerCase();
                            if (lowerName.contains("cast") || 
                                lowerName.contains("mirror") || 
                                lowerName.contains("virtual") || 
                                lowerName.contains("display")) {
                                return true;
                            }
                        }
                        return true; // 只要有非默认显示器，就认为在投屏
                    }
                }
            }
            
            // ============================================
            // 检测2：USB调试 + ADB连接 + Scrcpy
            // ============================================
            if (isUsbDebuggingEnabled(context)) {
                // USB调试已开启，进一步检查ADB连接和Scrcpy
                // 检查Scrcpy相关进程
                String[] scrcpyKeywords = {"scrcpy", "adb", "usbdebug"};
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        String processName = process.processName.toLowerCase();
                        for (String keyword : scrcpyKeywords) {
                            if (processName.contains(keyword)) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            // ============================================
            // 检测3：第三方投屏APP黑名单
            // ============================================
            String[] castingPackages = {
                "com.hpplay.sdk.source", // 乐播投屏
                "com.hpplay.app",
                "com.sand.airdroid", // AirDroid
                "com.sand.airdroidcast", // AirDroid Cast
                "com.xiaomi.mirror", // 小米投屏
                "com.huawei.castscreen", // 华为投屏
                "com.samsung.castscreen", // 三星投屏
                "com.letv.castscreen", // 乐视投屏
                "com.airplay.android", // AirPlay
                "tv.danmaku.bili", // 哔哩哔哩投屏
                "com.apowersoft.mirror", // 傲软投屏
                "com.ijiami.screenmirror", // 幕享
                "com.teamviewer.teamviewer", // TeamViewer
                "com.anydesk.anydeskandroid" // AnyDesk
            };
            
            PackageManager pm = context.getPackageManager();
            for (String pkg : castingPackages) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    // 检查应用是否正在运行
                    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    if (am != null) {
                        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                        for (ActivityManager.RunningAppProcessInfo process : processes) {
                            if (process.processName.equals(pkg)) {
                                return true;
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
            
            // ============================================
            // 检测4：服务类名关键词
            // ============================================
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(100);
                for (ActivityManager.RunningServiceInfo service : services) {
                    String serviceName = service.service.getClassName().toLowerCase();
                    if (serviceName.contains("cast") || 
                        serviceName.contains("mirror") || 
                        serviceName.contains("screenrecord") || 
                        serviceName.contains("media_projection")) {
                        return true;
                    }
                }
            }
            
            // ============================================
            // 检测5：悬浮窗/录屏权限
            // ============================================
            if (Settings.canDrawOverlays(context)) {
                // 悬浮窗权限已授予，可能有自动点击器
                // 作为辅助检测项，如果同时有其他风险项，更确定
            }
            
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static boolean isEmulator() {
        try {
            String brand = Build.BRAND.toLowerCase();
            String device = Build.DEVICE.toLowerCase();
            String product = Build.PRODUCT.toLowerCase();
            String hardware = Build.HARDWARE.toLowerCase();
            String manufacturer = Build.MANUFACTURER.toLowerCase();

            String[] emuKeywords = {
                "google_sdk", "sdk_gphone", "emulator", "sdk", 
                "genymotion", "nox", "bluestacks", "memu", 
                "ldplayer", "mumu", "ddmlib"
            };

            for (String keyword : emuKeywords) {
                if (brand.contains(keyword) || 
                    device.contains(keyword) || 
                    product.contains(keyword) ||
                    hardware.contains(keyword) || 
                    manufacturer.contains(keyword)) {
                    return true;
                }
            }

            if (Build.FINGERPRINT.contains("generic")) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private static boolean isAppCloned(Context context) {
        try {
            String[] clonePackages = {
                "com.lbe.parallel", "com.parallel.space", 
                "com.clone.space", "com.bly.parallel",
                "com.excelliance.kxqp", "com.applist.applist"
            };

            PackageManager pm = context.getPackageManager();
            for (String pkg : clonePackages) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (PackageManager.NameNotFoundException ignored) {}
            }

            String processName = context.getApplicationInfo().processName;
            if (processName != null && (processName.contains(":") || processName.contains("clone"))) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * 判断当前是否为Debug构建版本
     */
    private static boolean isDebugBuild(Context context) {
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            return (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            return false;
        }
    }
}
