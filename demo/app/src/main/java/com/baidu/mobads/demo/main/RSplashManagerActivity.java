package com.baidu.mobads.demo.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashInteractionListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 开屏完整示例
 * 1. 快捷接入请参考RSplashActivity，完整示例请参考RSplashManagerActivity
 * 2. 开屏广告需嵌入应用启动页Activity中。
 * 3. 开屏广告支持自定义跳过按钮，需在百青藤平台配置广告位设置。
 * 4. 设置开屏广告请求参数，建议传入，非必选。
 * 5. 设置开屏listener
 * 6. 请求开屏广告，可直接loadAndShow（实时请求并展示）；或可拆分load和show，分开延时展示。
 *
 * 根据工信部的规定，不再默认申请权限，而是主动弹框由用户授权使用。
 * 如果是Android6.0以下的机器, 或者targetSDKVersion < 23，默认在安装时获得了所有权限，可以直接调用SDK
 */
public class RSplashManagerActivity extends Activity {
    // 推荐使用全局变量，以便统一释放资源
    private SplashAd splashAd;
    private RelativeLayout adsParent;
    private TextView stateTextView;
    private boolean needAppLogo = true;
    private boolean loadSuccess = false;
    private static final String TAG = RSplashManagerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_manager);
        adsParent = this.findViewById(R.id.adsRl);
        stateTextView = this.findViewById(R.id.stateTextView);

        initView();
    }

    private void initView() {

        Intent intent = getIntent();
        if (intent != null) {
            // 区分全屏半屏
            needAppLogo = intent.getBooleanExtra("need_app_logo", true);
        }
        // 请求参数准备
        final int width = 1080;
        final int height = needAppLogo ? 1920 : 2310;
        // 重要：请核对您的广告位ID，代码位错误会导致无法请求到广告
        final String adPlaceId = "2058622";

        // 设置开屏listener
        final SplashInteractionListener listener = new SplashInteractionListener() {
            /**
            * 落地页页面关闭时执行
            */
            @Override
            public void onLpClosed() {
                Log.i(TAG, "lp页面关闭");
                Toast.makeText(RSplashManagerActivity.this.getApplicationContext(), "lp页面关闭", Toast.LENGTH_SHORT).show();
                // 落地页关闭后关闭广告，并跳转到应用的主页
//                destorySplash();
            }

            /**
            * 广告被销毁
            */
            @Override
            public void onAdDismissed() {
                Log.i(TAG, "onAdDismissed");
                destorySplash();
            }

            @Override
            public void onAdSkip() {
                Log.i(TAG, "onAdSkip");
            }

            /**
            * AD加载完成
            */
            @Override
            public void onADLoaded() {
                loadSuccess = true;
                Log.i(TAG, "onADLoaded");
                stateTextView.setText("请求成功");
                stateTextView.setTextColor(getResources().getColor(R.color.blue));
                // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
                clientBidding();
            }

            /**
            * 广告请求or展示失败
            *
            * @param reason 错误原因
            */
            @Override
            public void onAdFailed(String reason) {
                Log.i(TAG, "onAdFailed:" + reason);

                // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
                clientBidding();
                destorySplash();

                stateTextView.setTextColor(getResources().getColor(R.color.red));
                stateTextView.setText("请求失败");

            }

            /**
            * 广告显示成功后回调函数
            */
            @Override
            public void onAdPresent() {
                adsParent.setVisibility(View.VISIBLE);
                stateTextView.setText("展示成功");
                Log.i(TAG, "onAdPresent");
            }

            /**
             * 广告曝光成功后回调函数
             */
            @Override
            public void onAdExposed() {
                Log.i(TAG, "onAdExposed");
            }

            /**
            * 广告点击
            */
            @Override
            public void onAdClick() {
                Log.i(TAG, "onAdClick");
            }

            /**
            * 广告缓存成功后回调函数
            */
            @Override
            public void onAdCacheSuccess() {
                Log.i(TAG, "onAdCacheSuccess");
            }

            /**
            * 广告缓存失败
            */
            @Override
            public void onAdCacheFailed() {
                Log.i(TAG, "onAdCacheFailed");
                destorySplash();
            }
        };

        Button btn1 = this.findViewById(R.id.loadAndShow);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adsParent.setVisibility(View.VISIBLE);
                // 2. 设置开屏广告请求参数
                RequestParameters.Builder parameters = new RequestParameters.Builder()
                        // 设置开屏图片宽高(单位dp)，建议按照app情况传入正确的宽高比例值
                        .setHeight(height)
                        .setWidth(width)
                        // sdk内部默认值为true，load广告和show广告合起来为true
                        .addExtra(SplashAd.KEY_FETCHAD, "true")
                        .addExtra(SplashAd.KEY_SHAKE_LOGO_SIZE, "80")
                        // sdk内部默认值为true
                        .addExtra(SplashAd.KEY_DISPLAY_DOWNLOADINFO, String.valueOf(AdSettingHelper.getInstance()
                                .getBooleanFromSetting(AdSettingProperties.SPLASH_DISPLAY_DOWNLOAD, true)))
                        // 用户点击开屏下载类广告时，是否弹出Dialog，sdk内部默认值为false
                        // 此选项设置为true的情况下，会覆盖掉 {SplashAd.KEY_DISPLAY_DOWNLOADINFO} 的设置
                        .addExtra(SplashAd.KEY_POPDIALOG_DOWNLOAD, String.valueOf(AdSettingHelper.getInstance()
                                .getBooleanFromSetting(AdSettingProperties.SPLASH_USE_DIALOG_FRAME, false)))
                        .addCustExt(ArticleInfo.PAGE_TITLE, "标题")
                        .addCustExt("AAAAAAA_汉字", "aaaaaaaaaaaa_汉字" + System.currentTimeMillis());
                // 3. 初始化开屏实例，请求开屏广告
                splashAd = new SplashAd(RSplashManagerActivity.this.getApplicationContext(), adPlaceId, parameters.build(),
                        listener);
                if (needAppLogo) {
                    findViewById(R.id.appLogo).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.appLogo).setVisibility(View.GONE);
                }
                // 【可选】【Bidding】设置广告的底价，单位：分
                splashAd.setBidFloor(100);
                // 请求并展示广告
                splashAd.loadAndShow(adsParent);

            }
        });

        Button btn2 = this.findViewById(R.id.load);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 把loadSuccess设为false，说明重新开始load了。
                loadSuccess = false;
                /**
                 * 2. 设置开屏广告请求参数
                 * fetchAd：是否自动请求广告, 设置为true则自动loadAndshow，无需再主动load和show
                 * 设置为false则仅初始化开屏广告对象，需要手动调用load请求广告，并调用show展示广告
                 **/
                RequestParameters.Builder parameters = new RequestParameters.Builder()
                        // 设置开屏图片宽高(单位dp)，建议按照app情况传入正确的宽高比例值
                        .setHeight(height)
                        .setWidth(width)
                        // sdk内部默认超时时间为4200，单位：毫秒
                        .addExtra(SplashAd.KEY_TIMEOUT, "4200")
                        // sdk内部默认值为true，load广告和show广告分开时设置为false
                        .addExtra(SplashAd.KEY_FETCHAD, "false")
                        // sdk内部默认值为true
                        .addExtra(SplashAd.KEY_DISPLAY_DOWNLOADINFO, String.valueOf(AdSettingHelper.getInstance()
                                .getBooleanFromSetting(AdSettingProperties.SPLASH_DISPLAY_DOWNLOAD, true)))
                        // 用户点击开屏下载类广告时，是否弹出Dialog，sdk内部默认值为false
                        // 此选项设置为true的情况下，会覆盖掉 {SplashAd.KEY_DISPLAY_DOWNLOADINFO} 的设置
                        .addExtra(SplashAd.KEY_POPDIALOG_DOWNLOAD, String.valueOf(AdSettingHelper.getInstance()
                                .getBooleanFromSetting(AdSettingProperties.SPLASH_USE_DIALOG_FRAME, false)))
                        .addCustExt(ArticleInfo.PAGE_TITLE, "标题")
                        .addCustExt("AAAAAAA_汉字", "aaaaaaaaaaaa_汉字" + System.currentTimeMillis());

                // 3. 初始化开屏实例，请求开屏广告
                splashAd = new SplashAd(RSplashManagerActivity.this.getApplicationContext(), adPlaceId, parameters.build(), listener);
                splashAd.setDownloadDialogListener(new SplashAd.SplashAdDownloadDialogListener() {
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
                // 【可选】【Bidding】设置广告的底价，单位：分
                splashAd.setBidFloor(100);
                // 请求广告
                splashAd.load();
            }
        });

        Button btn3 = this.findViewById(R.id.show);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 单条广告多次show只计入一次，请注意
                if (loadSuccess) {
                    if (adsParent != null) {
                        if (splashAd != null) {
                            adsParent.setVisibility(View.VISIBLE);
                            if (needAppLogo) {
                                findViewById(R.id.appLogo).setVisibility(View.VISIBLE);
                            } else {
                                findViewById(R.id.appLogo).setVisibility(View.GONE);
                            }
                            splashAd.show(adsParent);
                        } else {
                            stateTextView.setText("请检查开屏对象是否存在异常");
                            Log.i(TAG, "请检查开屏对象是否存在异常");
                            return;
                        }
                    }
                } else {
                    stateTextView.setText("等load成功再进行show");
                }
            }
        });
    }

    private void destorySplash() {
        adsParent.setVisibility(View.INVISIBLE);
        findViewById(R.id.appLogo).setVisibility(View.INVISIBLE);
        if (splashAd != null) {
            // 根据代码位配置，展示开屏卡片
            if (splashAd.hasSplashCardView()) {
                // show方法返回的布尔值代表卡片是否能够展示，也可以onCardShow回调为准判断卡片的展示
                splashAd.showSplashCardView(this, new SplashAd.SplashCardAdListener() {
                    @Override
                    public void onCardShow() {
                        Log.i(TAG, "onCardShow");
                    }

                    @Override
                    public void onCardClick() {
                        Log.i(TAG, "onCardClick");
                    }

                    @Override
                    public void onCardClose() {
                        Log.i(TAG, "onCardClose");
                        if (splashAd != null) {
                            // 释放资源
                            splashAd.destroy();
                            splashAd = null;
                        }
                    }
                });
            } else {
                // 释放资源
                splashAd.destroy();
                splashAd = null;
            }
        }

        adsParent.removeAllViews();
        stateTextView.setText("等待请求");
        stateTextView.setTextColor(getResources().getColor(R.color.black));
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

}
