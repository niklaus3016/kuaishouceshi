package com.baidu.mobads.demo.main;

import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashInteractionListener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 实时开屏，广告实时请求并且立即展现。实时开屏接入请看该类
 */
public class RSplashActivity extends Activity {
    private static final String TAG = "RSplashActivity";

    private TextView mSplashHolder;
    // 推荐使用全局变量，以便统一释放资源
    private SplashAd splashAd;
    private boolean canJumpImmediately = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * 【注意】开屏点睛需要开屏和主页的窗口具有特性 {@linkplain Window.FEATURE_ACTIVITY_TRANSITIONS}
         * Tips: 一般具有Material Design风格的App主题，系统会默认开启该特性。
         *       若没有该特性，可以在{@link #setContentView(int)}之前
         *       调用 {@link #requestWindowFeature(int)} 即可开启特性，如下：
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        }
        /* 设置开屏全屏显示&透明状态栏 */
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        // 把requestWindowFeature放在super前面，要不然部分机型会上报异常
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        // 调用获取版本号的方法,下方的值为版本号
        String SDKVersion= AdSettings.getSDKVersion();
        // 打印出版本号的值
        Log.e(TAG,"广告SDK的版本号为："+SDKVersion);
        mSplashHolder = findViewById(R.id.splash_holder);
        fetchSplashAD();
    }

    /**
     *  竞价结果回传
     */
    private void clientBidding() {
        if (splashAd == null) {
            return;
        }
        Log.e(TAG,"ecpm=" + splashAd.getECPMLevel());
        // 媒体自行设置竞价逻辑，并根据竞价结果上报
        String biddingPrice = splashAd.getECPMLevel();
        boolean biddingResult = false;
        // 模拟竞价
        int firstPrice = 200;
        if (!TextUtils.isEmpty(biddingPrice)) {
            biddingResult = Integer.parseInt(biddingPrice) > firstPrice;
        } else {
            // 取竞胜价格
            biddingPrice = String.valueOf(firstPrice);
        }

        if (biddingResult) {
            // 百青藤竞胜，排名第二的竞价方信息
            LinkedHashMap<String, Object> secondInfo = new LinkedHashMap<>();
            // 百青藤竞胜，各家比价广告中，排名第二的价格，如果百青藤竞胜，则回传百青藤的二价即可
            secondInfo.put("ecpm", biddingPrice);
            // 竞价排名第二的DSP id，参考文档获取
            secondInfo.put("adn", 1);
            // 竞价排名第二的物料类型，参考文档获取
            secondInfo.put("ad_t", 3);
            // 竞价排名第二的广告主名称，物料中获取
            secondInfo.put("ad_n", "广告主名称");
            // 竞价时间，秒级时间戳
            secondInfo.put("ad_time", System.currentTimeMillis() / 1000);
            // 竞价排名第二的DSP的竞价类型，（百度竞价结果参数：1：分层保价；2：价格标签；3：bidding;4:其他)
            secondInfo.put("bid_t", 1);
            // 竞价排名第二的广告主标题，物料中获取
            secondInfo.put("ad_ti", "title");

            // 调用反馈竞价成功及二价
            BiddingListener winBiddingListener = new BiddingListener() {
                @Override
                public void onBiddingResult(boolean result, String message, HashMap<String, Object> ext) {
                    Log.i(TAG, "onBiddingResult-win: " + result + " msg信息：" + message);
                }
            };
            splashAd.biddingSuccess(secondInfo, winBiddingListener);
        } else {
            // 竞胜方信息
            LinkedHashMap<String, Object> winInfo = new LinkedHashMap<>();
            // 竞胜方出价
            winInfo.put("ecpm", biddingPrice);
            // 竞胜方的DSP id，参考文档获取
            winInfo.put("adn", 1);
            // 竞胜方的物料类型，参考文档获取
            winInfo.put("ad_t", 3);
            // 竞胜方的广告主名称，物料中获取
            winInfo.put("ad_n", "广告主名称");
            // 竞价时间，秒级时间戳
            winInfo.put("ad_time", System.currentTimeMillis() / 1000);
            // 竞价类型，（百度竞价结果参数：1：分层保价；2：价格标签；3：bidding;4:其他)
            winInfo.put("bid_t", 3);
            // 竞价失败原因，203：输给其他竞价方，其他见联盟文档平台
            winInfo.put("reason", 203);
            // 指的是此次竞胜pv是否曝光，而非百青藤广告是否曝光
            winInfo.put("is_s", 1);
            // 指的是此次竞胜pv是否点击，而非局限于百青藤广告是否点击
            winInfo.put("is_c", 0);
            // 竞价排名第二的广告主标题，物料中获取
            winInfo.put("ad_ti", "title");

            // 调用反馈竞价失败及原因
            BiddingListener lossBiddingListener = new BiddingListener() {
                @Override
                public void onBiddingResult(boolean result, String message, HashMap<String, Object> ext) {
                    Log.i(TAG, "onBiddingResult-loss: " + result + " msg信息：" + message);
                }
            };
            splashAd.biddingFail(winInfo, lossBiddingListener);
        }
    }

    /**
    * 请求和展现广告
    *
    * @return void
    */
    private void fetchSplashAD() {
        final RelativeLayout adsParent = (RelativeLayout) this.findViewById(R.id.adsRl);

        SplashInteractionListener listener = new SplashInteractionListener() {
            @Override
            public void onLpClosed() {
                Log.i(TAG, "lp页面关闭");
                Toast.makeText(RSplashActivity.this,"lp页面关闭",Toast.LENGTH_SHORT).show();
                jump();
            }

            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed");
                jumpWhenCanClick(); // 跳转至您的应用主界面
            }

            @Override
            public void onAdSkip() {
                Log.i(TAG, "onAdSkip");
            }

            @Override
            public void onADLoaded() {
                Log.i(TAG, "onADLoaded");
                // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
                clientBidding();
            }

            @Override
            public void onAdExposed() {
                Log.i(TAG, "onAdExposed");
            }

            @Override
            public void onAdFailed(String arg0) {
                Log.i(TAG, "" + arg0);
                clientBidding();
                jump();
            }

            @Override
            public void onAdPresent() {
                Log.i(TAG, "onAdPresent");
                mSplashHolder.setVisibility(View.GONE);
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
                jump();
            }
        };

        String adPlaceId = "2058622"; // 重要：请填上您的广告位ID，代码位错误会导致无法请求到广告


//        splashAd = new SplashAd(this, adPlaceId, listener);
//        splashAd.loadAndShow(adsParent);

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
        // 设置开屏图片宽高(单位dp)，建议按照app情况传入正确的宽高比例值
        parameters.setWidth(1080);
        parameters.setHeight(1920);

        splashAd = new SplashAd(this, adPlaceId, parameters.build(), listener);
        // 【可选】【Bidding】设置广告的底价，单位：分
        splashAd.setBidFloor(100);
        // 请求并展示广告
        splashAd.loadAndShow(adsParent);
    }

    private void jumpWhenCanClick() {
        if (canJumpImmediately) {
            if (splashAd != null) {
                Intent intent = new Intent(RSplashActivity.this, BaiduSDKDemo.class);
                /**
                 * 1. 结束当前Activity并启动主页，建议在该方法的回调中执行开屏Activity的结束操作。
                 * 2. 若仅传入Intent而不设置回调，则会在启动主页后自动结束当前开屏Activity，
                 *    例如： {@link SplashAd#finishAndJump(Intent)}
                 * 3. 建议配合主页onCreate阶段中的 {@link SplashAd#registerEnterTransition(
                 *    Activity, SplashAd.SplashFocusAdListener)} 使用
                 */
                splashAd.finishAndJump(intent, new SplashAd.OnFinishListener() {
                    @Override
                    public void onFinishActivity() {
                        Log.i(TAG, "onFinishActivity");
                        finish();
                    }
                });
                splashAd.destroy();
            }
        } else {
            canJumpImmediately = true;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        canJumpImmediately = false;
    }

    /**
     * 不可点击的开屏，使用该jump方法，而不是用jumpWhenCanClick
     */
    private void jump() {
        if (canJumpImmediately) {
            if (splashAd != null) {
                Intent intent = new Intent(RSplashActivity.this, BaiduSDKDemo.class);
                /**
                 * 1. 结束当前Activity并启动主页，建议在该方法的回调中执行开屏Activity的结束操作。
                 * 2. 若仅传入Intent而不设置回调，则会在启动主页后自动结束当前开屏Activity，
                 *    例如： {@link SplashAd#finishAndJump(Intent)}
                 * 3. 建议配合主页onCreate阶段中的 {@link SplashAd#registerEnterTransition(
                 *    Activity, SplashAd.SplashFocusAdListener)} 使用
                 */
                splashAd.finishAndJump(intent, new SplashAd.OnFinishListener() {
                    @Override
                    public void onFinishActivity() {
                        Log.i(TAG, "onFinishActivity");
                        finish();
                    }
                });
                splashAd.destroy();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (canJumpImmediately) {
            jumpWhenCanClick();
        }
        canJumpImmediately = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Activity销毁时，销毁广告对象释放资源，避免潜在的内存泄露
        if (splashAd != null) {
            splashAd.destroy();
            splashAd = null;
        }
        this.finish();
    }
}
