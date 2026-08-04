package com.mijingxingzuo.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * 快手广告联盟返回页面。
 * 当用户从广告点击跳转到第三方 App（例如应用商店）后，通过 ksad:// scheme
 * 重新调起当前应用时会落到此 Activity。一般只需要透传到启动页即可。
 */
public class KsReturnBackActivity extends Activity {
    private static final String TAG = "KsReturnBackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "跳转主页面失败: " + e.getMessage(), e);
        }
        finish();
    }
}
