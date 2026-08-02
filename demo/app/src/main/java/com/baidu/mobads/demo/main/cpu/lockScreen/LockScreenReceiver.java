package com.baidu.mobads.demo.main.cpu.lockScreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * author: Lijinpeng
 * date: 2021/7/30
 * desc: 内容联盟锁屏场景 锁屏监听广播
 */
public class LockScreenReceiver extends BroadcastReceiver {
    //-1关闭 0模板渲染
    public static int LOCK_TYPE_SETTING = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            if (LOCK_TYPE_SETTING == 0) {
                Intent mLockIntent = new Intent(context, LockScreenActivity.class);
                mLockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(mLockIntent);
            }
        }
    }
}
