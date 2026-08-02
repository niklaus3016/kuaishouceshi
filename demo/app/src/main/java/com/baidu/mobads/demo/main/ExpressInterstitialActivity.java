package com.baidu.mobads.demo.main;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.ExpressInterstitialAd;
import com.baidu.mobads.sdk.api.ExpressInterstitialListener;
import com.baidu.mobads.sdk.api.RequestParameters;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * native插屏的集成类NativeInterstitialActivity
 * */
public class ExpressInterstitialActivity extends Activity {

    private static final String TAG = ExpressInterstitialActivity.class.getSimpleName();
    /** 广告请求所使用的类*/

    ExpressInterstitialAd expressInterstitialAd;
    ExpressInterstitialAd.InterAdDownloadWindowListener adDownloadListener;
    ExpressInterstitialListener mExpressInterstitialListener;
    /** 横版插屏的广告位id，用于横版场景*/
    private String  mHorizontalId = "8262339";
    /** 竖版插屏的广告位id，用于竖版插屏*/
    private String  mVerticalId = "2403633";

    Context context;
    Button showButton;
    EditText apidEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AdSettingHelper.getInstance().
                getBooleanFromSetting(AdSettingProperties.INTERSTITIAL_FULL, false)) {
            fullSetting();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_interstitial);
        Button loadButton = findViewById(R.id.native_interstitial_load);
        showButton = findViewById(R.id.native_interstitial_show);
        apidEditText = findViewById(R.id.native_interstitial_edit_apid_value);
        apidEditText.setText(mVerticalId);
        TextView changeDirection = findViewById(R.id.native_interstitial_change_direction);
        context = this;
        /** 这些为基本的回调，建议进行设置*/
        mExpressInterstitialListener = new ExpressInterstitialListener() {

            @Override
            public void onADLoaded() {
                Log.e(TAG,"onADLoaded");
                if (expressInterstitialAd != null) {
                    Log.e(TAG,"ecpm="+expressInterstitialAd.getECPMLevel());
                    // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
                    clientBidding();
                }
            }

            @Override
            public void onAdClick() {
                Log.e(TAG, "onAdClick" );
            }

            @Override
            public void onAdClose() {
                Log.e(TAG, "onAdClose");
            }

            @Override
            public void onAdFailed(int errorCode, String message) {
                Log.e(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
                clientBidding();
            }

            @Override
            public void onNoAd(int errorCode, String message) {
                Log.e(TAG, "onNoAd reason:" + message + "errorCode:" + errorCode);
                clientBidding();
            }

            @Override
            public void onADExposed() {
                Log.e(TAG, "onADExposed");
            }

            @Override
            public void onADExposureFailed() {
                Log.e(TAG, "onADExposureFailed");
            }

            @Override
            public void onAdCacheSuccess() {
                Log.e(TAG, "onAdCacheSuccess");
                // 物料缓存已完成，可以进行展现了
                showButton.setEnabled(true);
            }

            @Override
            public void onAdCacheFailed() {
                Log.e(TAG, "onAdCacheFailed");
            }

            @Override
            public void onLpClosed() {
                Log.e(TAG, "onLpClosed");
            }
        };
        /** 这些为下载弹窗以及四要素的回调设置，无特别需要可以不进行设置*/
        adDownloadListener =
                new ExpressInterstitialAd.InterAdDownloadWindowListener() {
                    @Override
                    public void adDownloadWindowShow() {
                        Log.e(TAG, "adDownloadWindowShow");
                    }

                    @Override
                    public void adDownloadWindowClose() {
                        Log.e(TAG, "adDownloadWindowClose");
                    }

                    @Override
                    public void onADPrivacyClick() {
                        Log.e(TAG, "onADPrivacyClick");
                    }

                    @Override
                    public void onADPrivacyClose() {
                        Log.e(TAG, "onADPrivacyClose");
                    }

                    @Override
                    public void onADPermissionShow() {
                        Log.e(TAG, "onADPermissionShow");
                    }

                    @Override
                    public void onADPermissionClose() {
                        Log.e(TAG, "onADPermissionClose");
                    }
                };
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final RequestParameters requestParameters = new RequestParameters.Builder()
                        /**
                         * 【插屏传参】传参功能支持的参数见ArticleInfo类，各个参数字段的描述和取值可以参考如下注释
                         */
                        // 通用信息：用户性别，取值：0-unknown，1-male，2-female
                        .addCustExt(ArticleInfo.USER_SEX, "1")
                        // 最近阅读：小说、文章的名称
                        .addCustExt(ArticleInfo.PAGE_TITLE, "测试书名")
                        // 自定义传参，参考如下接入
                        .addCustExt("cust_这是Key", "cust_这是Value" + System.currentTimeMillis())
                        .addCustExt("Key2", "Value2")
                        .build();
                // 在请求前取输入框中的广告位id
                String apid = apidEditText.getText().toString();
                expressInterstitialAd = new ExpressInterstitialAd(ExpressInterstitialActivity.this.getApplicationContext(), apid);
                expressInterstitialAd.setLoadListener(mExpressInterstitialListener);
                expressInterstitialAd.setDownloadListener(adDownloadListener);
                // 设置下载弹窗，默认为false
                expressInterstitialAd.setDialogFrame(AdSettingHelper.getInstance().
                        getBooleanFromSetting(AdSettingProperties.INTERSTITIAL_DOWNLOAD, false));
                // 【可选】【Bidding】设置广告的底价，单位：分
                expressInterstitialAd.setBidFloor(100);
                // 【非必要】设置传参
                expressInterstitialAd.setRequestParameters(requestParameters);
                // 请求的时候说明广告还没回来，无法进行show
                showButton.setEnabled(false);
                // 请求广告
                expressInterstitialAd.load();
            }
        });
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expressInterstitialAd != null) {

                    expressInterstitialAd.show(ExpressInterstitialActivity.this);

                    // 如需缓存成功后再展现，可以判断isReady接口。
                    // isReady：广告缓存成功且未过期才会返回true。
//                    if (expressInterstitialAd.isReady()) {
//                        expressInterstitialAd.show(ExpressInterstitialActivity.this);
//                    } else {
//                        Toast.makeText(ExpressInterstitialActivity.this,"广告未准备好",Toast.LENGTH_LONG).show();
//                    }
                } else {
                    Toast.makeText(ExpressInterstitialActivity.this,"请先请求广告",Toast.LENGTH_LONG).show();
                }
            }
        });

        changeDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如果横屏的话切换到竖屏，如果竖屏切换到横屏
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    apidEditText.setText(mHorizontalId);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else {
                    apidEditText.setText(mVerticalId);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
    }

    private void fullSetting() {
        // 设置全屏
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View
                        .SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    /**
     *  竞价结果回传
     */
    private void clientBidding() {

        Log.e(TAG,"ecpm=" + expressInterstitialAd.getECPMLevel());
        // 媒体自行设置竞价逻辑，并根据竞价结果上报
        String biddingPrice = expressInterstitialAd.getECPMLevel();

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
            expressInterstitialAd.biddingSuccess(secondInfo, winBiddingListener);
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
            expressInterstitialAd.biddingFail(winInfo, lossBiddingListener);
        }
    }

}
