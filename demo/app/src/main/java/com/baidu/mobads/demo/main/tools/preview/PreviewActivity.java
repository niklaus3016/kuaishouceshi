package com.baidu.mobads.demo.main.tools.preview;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.baidu.mobads.demo.main.R;

import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.ExpressInterstitialAd;
import com.baidu.mobads.sdk.api.ExpressInterstitialListener;
import com.baidu.mobads.sdk.api.ExpressResponse;
import com.baidu.mobads.sdk.api.FeedPortraitVideoView;
import com.baidu.mobads.sdk.api.FullScreenVideoAd;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.RewardVideoAd;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashInteractionListener;

import java.util.List;
import java.util.Map;

import static com.baidu.mobads.demo.main.permission.BasePermissionActivity.DEFAULT_SCAN_MODE;

public class PreviewActivity extends Activity {

    private String mProd;
    private String mMapString;
    private RelativeLayout mLayout;
    private RelativeLayout mAppLogoLayout;
    private RelativeLayout mRspLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        Intent intent = getIntent();
        mProd = intent.getStringExtra("prod");
        if ("rvideo".equals(mProd)) {
            // 取出之后制空
            SharedPreferences pre = getSharedPreferences("rvideoData",MODE_PRIVATE);
            mMapString = pre.getString("mapString","");
            SharedPreferences.Editor editor = pre.edit();
            editor.putString("mapString","");
            editor.commit();
        } else {
            mMapString = intent.getStringExtra("mapString");
        }
        mLayout = findViewById(R.id.layout);
        mAppLogoLayout = findViewById(R.id.appLogo);
        mRspLayout = findViewById(R.id.adsRl);
        show();
    }

    private void show() {
        if (mProd.toLowerCase().contains("feed")) {

            RequestParameters requestParameters = new RequestParameters.Builder().build();
            BaiduNativeManager nativeManager = new BaiduNativeManager(this, "2058628");
            final BaiduNativeManager.ExpressAdListener expressAdListener = new BaiduNativeManager.ExpressAdListener() {
                @Override
                public void onNativeLoad(List<ExpressResponse> nativeResponses) {
                    nativeResponses.get(0).render();
                    mLayout.addView(nativeResponses.get(0).getExpressAdView(),
                            new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }

                @Override
                public void onNoAd(int code, String msg, ExpressResponse expressResponse) {

                }

                @Override
                public void onNativeFail(int errorCode, String message, ExpressResponse expressResponse) {

                }

                @Override
                public void onVideoDownloadSuccess() {

                }

                @Override
                public void onVideoDownloadFailed() {

                }

                @Override
                public void onLpClosed() {
                }
            };
            // 发起信息流广告请求
            nativeManager.getFeedBiddingToken(requestParameters);
            nativeManager.setExpressFeedBiddingData(requestParameters,mMapString,expressAdListener);
        } else if (mProd.equals("int")) {
            ExpressInterstitialAd expressInterstitialAd = new ExpressInterstitialAd(PreviewActivity.this, "2403633");
            ExpressInterstitialListener mExpressInterstitialListener = new ExpressInterstitialListener() {

                @Override
                public void onADLoaded() {
                }

                @Override
                public void onAdClick() {
                }

                @Override
                public void onAdClose() {
                    finish();
                }

                @Override
                public void onAdFailed(int errorCode, String message) { }

                @Override
                public void onNoAd(int errorCode, String message) { }

                @Override
                public void onADExposed() { }

                @Override
                public void onADExposureFailed() { }

                @Override
                public void onAdCacheSuccess() {
                }

                @Override
                public void onAdCacheFailed() {
                }

                @Override
                public void onLpClosed() { }
            };
            expressInterstitialAd.setLoadListener(mExpressInterstitialListener);
            expressInterstitialAd.getBiddingToken();
            expressInterstitialAd.setBiddingData(mMapString);
            expressInterstitialAd.show();
        } else if (mProd.equals("rsplash")){
            mAppLogoLayout.setVisibility(View.VISIBLE);
            // 1. 设置开屏listener
            final SplashInteractionListener listener = new SplashInteractionListener() {
                @Override
                public void onLpClosed() {

                }

                @Override
                public void onAdDismissed() {
                    finish();
                }

                @Override
                public void onAdSkip() {

                }

                @Override
                public void onADLoaded() {

                }

                @Override
                public void onAdFailed(String reason) {

                }

                @Override
                public void onAdPresent() {

                }

                @Override
                public void onAdExposed() {

                }

                @Override
                public void onAdClick() {

                }

                @Override
                public void onAdCacheSuccess() {
                }

                @Override
                public void onAdCacheFailed() {
                }
            };
            SplashAd  splashAd = new SplashAd(PreviewActivity.this.getApplicationContext(), "2058622", null, listener);
            splashAd.getBiddingToken();
            splashAd.setBiddingData(mMapString);
            splashAd.show(mRspLayout);
        } else if (mProd.equals("pvideo")) {
            RequestParameters requestParameters = new RequestParameters.Builder().build();
            final BaiduNativeManager.PortraitVideoAdListener listener = new BaiduNativeManager.PortraitVideoAdListener() {
                @Override
                public void onAdClick() {

                }

                @Override
                public void onNativeLoad(final List<NativeResponse> nativeResponses) {
                    FeedPortraitVideoView feedVideoView = new FeedPortraitVideoView(PreviewActivity.this);
                    feedVideoView.setAdData(nativeResponses.get(0));
                    mLayout.addView(feedVideoView,new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    feedVideoView.play();
                }

                @Override
                public void onNoAd(int errorCode, String msg, NativeResponse nativeResponse) {

                }

                @Override
                public void onNativeFail(int errorCode, String msg, NativeResponse nativeResponse) {

                }

                @Override
                public void onVideoDownloadSuccess() {

                }

                @Override
                public void onVideoDownloadFailed() {


                }

                @Override
                public void onLpClosed() {
                    // 落地页关闭回调
                }
            };
            final BaiduNativeManager baiduNativeManager = new BaiduNativeManager(this, "7250989");
            // 【可选】【Bidding】设置广告的底价，单位：分
            baiduNativeManager.getFeedBiddingToken(requestParameters);
            // 请求广告
            baiduNativeManager.setFeedBiddingData(requestParameters,mMapString, listener);
        } else if (mProd.equals("rvideo")){
            RewardVideoAd mRewardVideoAd = new RewardVideoAd(PreviewActivity.this, "5989414", new RewardVideoAd.RewardVideoAdListener() {
                @Override
                public void onAdShow() {

                }

                @Override
                public void onAdClick() {

                }

                @Override
                public void onAdClose(float playScale) {
                    Intent intent = new Intent(PreviewActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, DEFAULT_SCAN_MODE);
                    finish();
                }

                @Override
                public void onAdFailed(String reason) {

                }

                @Override
                public void onVideoDownloadSuccess() {

                }

                @Override
                public void onVideoDownloadFailed() {

                }

                @Override
                public void playCompletion() {

                }

                @Override
                public void onAdLoaded() {

                }

                @Override
                public void onAdSkip(float playScale) {

                }

                @Override
                public void onRewardVerify(boolean rewardVerify, Map<String, Object> rewardInfo) {

                }
            });
            // 请求广告
            mRewardVideoAd.getBiddingToken();
            mRewardVideoAd.setBiddingData(mMapString);
            mRewardVideoAd.show();
        } else if (mProd.equals("fvideo")) {
            FullScreenVideoAd fullScreenVideoAd = new FullScreenVideoAd(PreviewActivity.this, "5989414",
                    new FullScreenVideoAd.FullScreenVideoAdListener() {
                        @Override
                        public void onAdShow() {

                        }

                        @Override
                        public void onAdClick() {

                        }

                        @Override
                        public void onAdClose(float playScale) {
                            Intent intent = new Intent(PreviewActivity.this, CaptureActivity.class);
                            startActivityForResult(intent, DEFAULT_SCAN_MODE);
                            finish();
                        }

                        @Override
                        public void onAdFailed(String reason) {

                        }

                        @Override
                        public void onVideoDownloadSuccess() {

                        }

                        @Override
                        public void onVideoDownloadFailed() {

                        }

                        @Override
                        public void playCompletion() {

                        }

                        @Override
                        public void onAdSkip(float playScale) {

                        }

                        @Override
                        public void onAdLoaded() {

                        }
                    }, false);
            // 请求广告
            fullScreenVideoAd.getBiddingToken();
            fullScreenVideoAd.setBiddingData(mMapString);
            fullScreenVideoAd.show();
        }
    }
}