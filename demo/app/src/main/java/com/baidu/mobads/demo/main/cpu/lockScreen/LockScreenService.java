package com.baidu.mobads.demo.main.cpu.lockScreen;


import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.baidu.mobads.demo.main.R;

/**
 * author: Lijinpeng
 * date: 2021/7/30
 * desc: 内容联盟锁屏场景 锁屏自定义服务类
 */
public class LockScreenService extends Service {

    private LockScreenReceiver mLockScreenReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);//获取NotificationManager实例

        // 从Android 8.0开始，前台服务需要注册通知通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(String.valueOf(1),
                    "Service notification channel", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, String.valueOf(1))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Demo锁屏服务运行中");
        startForeground(666, builder.build());

        registerLockScreenReceiver();
    }


    private void registerLockScreenReceiver() {
        if (mLockScreenReceiver == null) {
            mLockScreenReceiver = new LockScreenReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mLockScreenReceiver, intentFilter);
    }

    public static void startLockScreenService(Context context) {
        try {
            Intent intent = new Intent(context, LockScreenService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopLockScreenService(Context context) {
        try {
            Intent intent = new Intent(context, LockScreenService.class);
            context.stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLockScreenReceiver != null) {
            LockScreenReceiver.LOCK_TYPE_SETTING = -1;
            stopForeground(true);
            unregisterReceiver(mLockScreenReceiver);
        }
    }


    public static boolean canBackgroundStart(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                //若未授权则请求悬浮窗权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                ((Activity) context).startActivityForResult(intent, 0);
                return false;
            }

        }
        return true;
    }

}
