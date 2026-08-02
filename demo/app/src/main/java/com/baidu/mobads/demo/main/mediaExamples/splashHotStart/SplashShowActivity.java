package com.baidu.mobads.demo.main.mediaExamples.splashHotStart;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.utilsDemo.SplashList;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashInteractionListener;

public class SplashShowActivity extends Activity {
    private static final String TAG = "SplashShowActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_utils_demo_splash_show);
        SplashInteractionListener listener = new SplashInteractionListener() {
            @Override
            public void onLpClosed() {
                Log.i(TAG, "lp页面关闭");
                Toast.makeText(SplashShowActivity.this,"lp页面关闭",Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed");
                if (SplashList.getInstance().getSplashAd() != null) {
                    SplashList.getInstance().getSplashAd().destroy();
                    SplashList.getInstance().setSplashAd(null);
                }
                finish();
            }

            @Override
            public void onAdSkip() {
                Log.i(TAG, "onAdSkip");
            }

            @Override
            public void onADLoaded() {
                Log.i(TAG, "onADLoaded");
            }

            @Override
            public void onAdFailed(String arg0) {
                Log.i(TAG, "" + arg0);
                finish();
            }

            @Override
            public void onAdPresent() {
                Log.i(TAG, "onAdPresent");
            }

            @Override
            public void onAdExposed() {
                Log.i(TAG, "onAdExposed");
            }

            @Override
            public void onAdClick() {
                Log.i(TAG, "onAdClick");
                // 设置开屏可接受点击时，该回调可用
            }

            @Override
            public void onAdCacheSuccess() {
                Log.i(TAG, "onAdCacheSuccess");
            }

            @Override
            public void onAdCacheFailed() {
                Log.i(TAG, "onAdCacheFailed");
            }
        };
        RelativeLayout layout = findViewById(R.id.feed_utils_demo_splash_show_container);
        SplashList.getInstance().getSplashAd().setListener(listener);
        SplashList.getInstance().getSplashAd().setDownloadDialogListener(new SplashAd.SplashAdDownloadDialogListener() {
            @Override
            public void adDownloadWindowShow() {
                Log.i(TAG, "adDownloadWindowShow");
            }

            @Override
            public void adDownloadWindowClose() {
                Log.i(TAG, "adDownloadWindowClose");
            }

            @Override
            public void onADPrivacyLpShow() {
                Log.i(TAG, "onADPrivacyLpShow");
            }

            @Override
            public void onADPrivacyLpClose() {
                Log.i(TAG, "onADPrivacyLpClose");
            }

            @Override
            public void onADFunctionLpShow() {
                Log.i(TAG, "onADFunctionLpShow");
            }

            @Override
            public void onADFunctionLpClose() {
                Log.i(TAG, "onADFunctionLpClose");
            }

            @Override
            public void onADPermissionShow() {
                Log.i(TAG, "onADPermissionShow");
            }

            @Override
            public void onADPermissionClose() {
                Log.i(TAG, "onADPermissionClose");
            }
        });
        SplashList.getInstance().getSplashAd().show(layout);
    }
}
