package com.baidu.mobads.demo.main.mediaExamples.splashHotStart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.utilsDemo.SplashList;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashAdListener;
/**
 * 开屏热启动的实例，仅用于热启动的场景(一个页面请求，另一个页面展示)。
 * 一个页面请求然后展示的话请参考类RSplashManagerActivity。
 */
public class SplashLoadActivity extends Activity {
    private static final String TAG = "SplashLoadActivity";
    Button splashShow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_utils_demo_splash_load);
        final String adPlaceId = "2058622"; // 重要：请填上您的广告位ID，代码位错误会导致无法请求到广告

        Button splashLoad = findViewById(R.id.feed_utils_demo_splash_load_load);
        splashLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果开屏需要load广告和show广告分开，请参考类RSplashManagerActivity的写法
                // 如果需要修改开屏超时时间、隐藏工信部下载整改展示，请设置下面代码;
                final RequestParameters.Builder parameters = new RequestParameters.Builder();
                // sdk内部默认超时时间为4200，单位：毫秒
                parameters.addExtra(SplashAd.KEY_TIMEOUT, "4200");
                // sdk内部默认值为true
                parameters.addExtra(SplashAd.KEY_DISPLAY_DOWNLOADINFO, "true");
                // 用户点击开屏下载类广告时，是否弹出Dialog
                // 此选项设置为true的情况下，会覆盖掉 {SplashAd.KEY_DISPLAY_DOWNLOADINFO} 的设置
                parameters.addExtra(SplashAd.KEY_POPDIALOG_DOWNLOAD, "true");
                SplashAdListener listener = new SplashAdListener() {
                    @Override
                    public void onADLoaded() {
                        Log.i(TAG, "onADLoaded");
                        Toast.makeText(SplashLoadActivity.this,"请求成功",Toast.LENGTH_LONG).show();
                        splashShow.setEnabled(true);
                    }

                    @Override
                    public void onAdFailed(String reason) {
                        Log.i(TAG, "onAdFailed:" + reason);
                        Toast.makeText(SplashLoadActivity.this,"失败:"+reason,Toast.LENGTH_LONG).show();
                    }
                };
                SplashAd splashAd = new SplashAd(SplashLoadActivity.this.getApplicationContext(), adPlaceId, parameters.build(), listener);
                splashAd.load();
                SplashList.getInstance().setSplashAd(splashAd);
            }
        });

        splashShow = findViewById(R.id.feed_utils_demo_splash_load_show);
        splashShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SplashLoadActivity.this, SplashShowActivity.class);
                startActivity(intent);
                splashShow.setEnabled(false);
            }
        });
    }
}
