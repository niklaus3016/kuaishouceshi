package com.baidu.mobads.demo.main.feeds.video;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.baidu.mobads.demo.main.tools.CustomProgressButton;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.XAdNativeResponse;
import com.baidu.mobads.sdk.api.FeedPortraitVideoView;
import com.baidu.mobads.sdk.api.IFeedPortraitListener;
import com.baidu.mobads.demo.main.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
/*
1. 竖版视频集成参考类：FeedPortraitVideoActivity
2. 竖版沉浸式视频广告，SDK提供视频播放器组件FeedPortraitVideoView，接入基本同信息流
3. FeedPortraitVideoView提供可自定义性更强，提供更多播放器相关API。
4. 注意：竖版视频需要您手动发送点击事件。漏发则无法计费。
* */
public class FeedPortraitVideoActivity extends Activity {

    private static final String TAG = "FeedPortraitVideo";
    // 代码位id
    private String mAdPlaceId = "7250989";
    /**
     * 提供的视频组件，整个视频区域不可以点击
     */
    private RelativeLayout mVideoRl;
    private RelativeLayout.LayoutParams videoLp;
    private FeedPortraitVideoView mFeedVideoView;
    private View mCreativeView;
    /**
     * 媒体接入的时候，可以把title,icon,desc,adlogol等等设置为可点击的（按自己产品的设计去开发）
     * 这儿只是示例设置点击之后，如何调用相关的api
     */
    // 控制音量
    private ImageView mVolume;
    private ImageView mBaiduLogo;
    private ImageView mAdLogo;
    private SwipeRefreshLayout mRefreshLayout;
    private TextView mAuthorName;
    private TextView mDescView;

    private Button mBtPause, mBtResume, mBtStop, mBtReplay, mExitButton;
    // 提供的视频组件默认是有声音播放，媒体可以调用设置静音与否的api；
    private Boolean mIsMute = false;
    private NativeResponse mNativeAd;

    /** 自定义下载按钮进度条 */
    private CustomProgressButton mCustomProgressButton;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 需要在视频播放过程中保持屏幕常亮，所以需要设置这个
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.feed_portrait_video);
        mVideoRl = findViewById(R.id.feed_portrait_video);

        mBtPause = findViewById(R.id.bt_pause);
        mBtResume = findViewById(R.id.bt_resume);
        mBtStop = findViewById(R.id.bt_stop);
        mBtReplay = findViewById(R.id.bt_replay);
        mExitButton = findViewById(R.id.bt_exit);
        mCustomProgressButton = findViewById(R.id.downloadBtn);
        mAuthorName = findViewById(R.id.authorname);
        mDescView = findViewById(R.id.descview);

        // 初始化logo
        mBaiduLogo = findViewById(R.id.iv_baidulogo);
        mAdLogo = findViewById(R.id.iv_adlogo);
        // 对工信部四个字段进行实例化。
        mFeedVideoView = new FeedPortraitVideoView(this);
        // 使用下载弹框
        mFeedVideoView.setUseDownloadFrame(true);
        // 【可选配置】是否显示视频播放的进度条
        mFeedVideoView.setShowProgress(true);
        mFeedVideoView.setProgressBackgroundColor(Color.BLACK);
        mFeedVideoView.setProgressBarColor(Color.WHITE);
        mFeedVideoView.setProgressHeightInDp(1);
        videoLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout
                .LayoutParams.MATCH_PARENT);
        mVideoRl.addView(mFeedVideoView, videoLp);
        mFeedVideoView.setFeedPortraitListener(new IFeedPortraitListener() {
            @Override
            public void playCompletion() {
                // 视频播放完成
                Log.i(TAG, "playCompletion==");
                // 播放完成后隐藏优惠券（可选）
                View couponContainer = findViewById(R.id.coupon_float_view_container);
                if (couponContainer != null) {
                    couponContainer.setVisibility(View.INVISIBLE);
                }
                // 播放完隐藏翻页组件(可选)
                View flipPageContainer = findViewById(R.id.flip_page_view_container);
                if (flipPageContainer != null) {
                    flipPageContainer.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void playError() {
                // 播放错误
                Log.i(TAG, "playError==");
            }

            @Override
            public void pauseBtnClick() {
                Log.i(TAG,"pauseBtnClick");
            }

            @Override
            public void playRenderingStart() {
                // 视频开始播放第一帧
                Log.i(TAG, "playRenderingStart==");
            }

            @Override
            public void playPause() {
                Log.i(TAG, "playPause==");
            }

            @Override
            public void playResume() {
                Log.i(TAG, "playResume==");
            }
        });
        mVolume = findViewById(R.id.test_volume);
        mVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsMute = !mIsMute;
                // 调用设置静音的api
                mFeedVideoView.setVideoMute(mIsMute);
                mVolume.setImageResource(mIsMute ? R.mipmap.volume_close : R.mipmap.volume_open);
            }
        });



        mBtPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * SDK内部存在视频管理机制，会自动管理视频暂停和续播行为；
                 * 媒体自己无需调用 mFeedVideoView.pause()或mFeedVideoView.resume()来自己管理
                 *
                 * 如果媒体在外部调用mFeedVideoView.pause();方法，则视频的续播
                 * 行为也需要媒体自己调用，调用《续播》或者《重播》行为。
                 */
                if (mFeedVideoView != null) {
                    mFeedVideoView.pause();
                }
            }
        });

        mBtResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeedVideoView != null) {
                    mFeedVideoView.resume();
                }
            }
        });

        mBtStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * 销毁视频，SDK内部管理视频管理机制失效，外部调用pause(),resume()无效
                 * 之后只能进行重播逻辑来恢复
                 */
                if (mFeedVideoView != null) {
                    mFeedVideoView.stop();
                }
            }
        });
        mBtReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFeedVideoView != null) {
                    mFeedVideoView.setAdData(mNativeAd);
                    mFeedVideoView.play();
                }
            }
        });

        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedPortraitVideoActivity.this.finish();
            }
        });

        /**
         * 说明：媒体划走view两种处理方式：
         * （1）划走时候调用mFeedVideoView.stop()，释放播放器资源，切换到前台之后可以重新设置数据进行播放播放，可以这样调用api：
         if (mFeedVideoView != null) {
         // (XAdNativeResponse) mNativeAd这个广告数据可以是已经播放过的，也可以是已经新请求OK的广告数据
         mFeedVideoView.setAdData((XAdNativeResponse) mNativeAd);
         mFeedVideoView.play();
         }
         */
        // 媒体可以复用同一个组件view，这个组件view接收广告数据（(XAdNativeResponse) mNativeAd）进行重新播放，


        mRefreshLayout = findViewById(R.id.refresh_container);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchAd();
            }
        });
        /**
         * 请求广告数据，
         * 备注：如果需要的话，可以一次请求返回多条广告数据（mssp后台可配置）
         */
        fetchAd();


    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停播放的时候调用pause,也可由播放组件自动管理
//        if (mFeedVideoView != null) {
//            mFeedVideoView.pause();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台时调用resume,也可由播放组件自动管理
//        if (mFeedVideoView != null) {
//            mFeedVideoView.resume();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 调用stop,释放播放器资源
        if (mFeedVideoView != null) {
            mFeedVideoView.stop();
        }
    }

    private void fetchAd() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        int winW = dm.widthPixels;
        int winH = dm.heightPixels;
        /**
         * 配置请求广告的参数
         * @param winW 宽度
         * @param winH 高度
         * @param policy  用户点击下载类广告时，是否弹出提示框让用户选择下载与否
         */
        final RequestParameters requestParameters = new RequestParameters.Builder()
                .setWidth(winW)
                .setHeight(winH)
                // 用户维度：用户性别，取值：0-unknown，1-male，2-female
                .addExtra(ArticleInfo.USER_SEX, "1")
                // 用户维度：收藏的小说ID，最多五个ID，且不同ID用'/分隔'
                .addExtra(ArticleInfo.FAVORITE_BOOK, "这是小说的名称1/这是小说的名称2/这是小说的名称3")
                // 内容维度：小说、文章的名称
                .addExtra(ArticleInfo.PAGE_TITLE, "测试书名")
                // 内容维度：小说、文章的ID
                .addExtra(ArticleInfo.PAGE_ID, "10930484090")
                // 内容维度：小说分类，一级分类和二级分类用'/'分隔
                .addExtra(ArticleInfo.CONTENT_CATEGORY, "一级分类/二级分类")
                // 内容维度：小说、文章的标签，最多10个，且不同标签用'/分隔'
                .addExtra(ArticleInfo.CONTENT_LABEL, "标签1/标签2/标签3")
                .addCustExt("cust_Key_这是key", "cust_Value_这是Value" + System.currentTimeMillis())
                .addCustExt("AAAAAAA", "aaaaaa")
                .addCustExt(ArticleInfo.PAGE_TITLE, "真测试书名")
                .build();

        /**
         * @param context 上下文
         * @param mAdPlaceId 广告位id,说明：媒体需要写自己申请好的广告位id
         * @param BaiduNativeNetworkListener 接收广告请求成功和失败的回调
         */
        final BaiduNativeManager baiduNativeManager = new BaiduNativeManager(this, mAdPlaceId);
        final BaiduNativeManager.FeedAdListener listener = new BaiduNativeManager.FeedAdListener() {

            @Override
            public void onNativeLoad(final List<NativeResponse> nativeResponses) {
                Log.i(TAG, "onADLoaded：");
                mRefreshLayout.setRefreshing(false);
                if (nativeResponses != null && nativeResponses.size() > 0) {
                    // 这里每次都取第一条广告来做展示,模拟多条广告;实际开发过程中需要开发者自己处理
                    mNativeAd = nativeResponses.get(0);
                    // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
                    clientBidding(mNativeAd);
                    List<View> clickViews = new ArrayList<>();
                    List<View> creativeViews = new ArrayList<>();
                    clickViews.add(mFeedVideoView);
                    // creativeViews添加自渲染的下载按钮，后面才能给下载按钮注册点击事件
                    creativeViews.add(mCustomProgressButton);
                    /**
                     * 注册可点击的View，点击和曝光会在内部完成
                     * @Param view 广告容器或广告View
                     * 【【 该View只负责发送展现 】】
                     * @Param clickViews 可点击的View，默认展示下载整改弹框。
                     * @Param creativeViews 带有广告文案之类的View，点击不会触发下载整改弹框。
                     * @Param interactionListener 点击、曝光回调
                     */
                    mNativeAd.registerViewForInteraction(mFeedVideoView, clickViews, creativeViews,
                            new NativeResponse.AdInteractionListener() {
                                @Override
                                public void onAdClick() {
                                    // 视频发生点击
                                    Log.i(TAG, "onAdClick：");
                                    // 点击完隐藏翻页组件(可选)
                                    View flipPageContainer = findViewById(R.id.flip_page_view_container);
                                    if (flipPageContainer != null) {
                                        flipPageContainer.setVisibility(View.INVISIBLE);
                                    }
                                    Toast.makeText(FeedPortraitVideoActivity.this, "视频发生点击", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onADExposed() {
                                    Log.d(TAG, "onADExposed: ");
                                }

                                @Override
                                public void onADExposureFailed(int reason) {
                                    Log.i(TAG , "onADExposureFailed: " + reason);
                                }

                                @Override
                                public void onADStatusChanged() {
                                    Log.d(TAG, "onADStatusChanged: ");
                                    if (mCustomProgressButton != null) {
                                        // 根据下载状态改变button文字并显示下载进度
                                        mCustomProgressButton.updateStatus(mNativeAd);
                                    }
                                }

                                @Override
                                public void onAdUnionClick() {
                                    Log.d(TAG, "onAdUnionClick: ");
                                }
                            });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mFeedVideoView != null) {
                                // 添加下载弹框的接口
                                mNativeAd.setAdPrivacyListener(new NativeResponse.AdDownloadWindowListener() {
                                    @Override
                                    public void adDownloadWindowShow() {
                                        Log.i(TAG, "adDownloadWindowShow: ");
                                        // 使弹窗出现时暂停
                                        mFeedVideoView.pause();
                                    }

                                    @Override
                                    public void adDownloadWindowClose() {
                                        Log.i(TAG, "adDownloadWindowClose: ");
                                        // 使弹窗消失时继续
                                        mFeedVideoView.resume();
                                    }

                                    @Override
                                    public void onADPrivacyClick() {
                                        Log.i(TAG, "onADPrivacyClick: ");
                                    }

                                    @Override
                                    public void onADFunctionClick() {
                                        Log.i(TAG, "onADFunctionClick: ");
                                    }

                                    @Override
                                    public void onADPermissionShow() {
                                        Log.i(TAG, "onADPermissionShow: ");
                                    }

                                    @Override
                                    public void onADPermissionClose() {
                                        Log.i(TAG, "onADPermissionClose: ");
                                    }
                                });
                                final int actionType = mNativeAd.getAdActionType();
                                // 如果是下载类广告则显示自渲染下载按钮
                                if (actionType == 2 && mCustomProgressButton != null) {
                                    mCustomProgressButton.setVisibility(View.VISIBLE);
                                    mCustomProgressButton.initWithResponse(mNativeAd);
                                    mCustomProgressButton.setTextColor(Color.parseColor("#FFFFFF"));
                                    // 字体大小适配屏幕
                                    DisplayMetrics metrics =
                                            FeedPortraitVideoActivity.this.getResources().getDisplayMetrics();
                                    int textSize = (int) (12 * metrics.scaledDensity + 0.5f);
                                    mCustomProgressButton.setTextSize(textSize);
                                    mCustomProgressButton.
                                            setTypeFace(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC));
                                    mCustomProgressButton.setForegroundColor(Color.parseColor("#3388FF"));
                                    mCustomProgressButton.setBackgroundColor(Color.parseColor("#D7E6FF"));
                                    // 设置长按取消下载能力
                                    mCustomProgressButton.useLongClick(true);
                                } else if(mCustomProgressButton != null){
                                    mCustomProgressButton.setVisibility(View.INVISIBLE);
                                }

                                String title = mNativeAd.getTitle();
                                if (mNativeAd.getAdMaterialType().equals(NativeResponse.MaterialType.LIVE.getValue())) {
                                    title = mNativeAd.getAuthorName();
                                }
                                mAuthorName.setText(title);
                                mDescView.setText(mNativeAd.getDesc());

                                // 传入当前想播放的广告数据,如果是视频就开始准备播放器
                                mFeedVideoView.setAdData((XAdNativeResponse) mNativeAd);
                                // 调用play方法开始播放视屏，若已经播放过，则恢复播放
                                mFeedVideoView.play();
                                // 重置静音按钮的状态，保持与播放器的状态一致
                                mIsMute = false;
                                mVolume.setImageResource(mIsMute
                                        ? R.mipmap.volume_close : R.mipmap.volume_open);
                                /**
                                 * 竖版视频组件提供展示文字卡片的能力，配置后应用无需自行渲染标题、描述等字段
                                 * 如配置不使用文字卡片功能，则可以继续添加其他组件或字段的渲染逻辑
                                  */
                                // 渲染促转化的创意组件，有助于提升广告的点击率
                                if (mCreativeView != null) {
                                    // 移除旧的组件
                                    mVideoRl.removeView(mCreativeView);
                                    mCreativeView = null;
                                }
                                // 渲染新的转化组件
                                mCreativeView = getCreativeView(mNativeAd);
                                if (mCreativeView != null) {
                                    // 滑一滑组件会适配容器的大小，所以容器最好不要设置WRAP_CONTENT
                                    RelativeLayout.LayoutParams lp = new RelativeLayout
                                            .LayoutParams(560, 360);
                                    lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                                    mVideoRl.addView(mCreativeView, lp);
                                }
                                // 弹幕组件
                                final RelativeLayout bulletContainer = findViewById(R.id.bullet_view_container);
                                if (bulletContainer != null) {
                                    bulletContainer.removeAllViews();
                                    bulletContainer.setVisibility(View.VISIBLE);
                                    // 渲染弹幕组件应尽量在广告素材的上层，弹幕播放完成会自动移除自身
                                    // @param width 弹幕容器宽度，请使用MATCH_PARENT或大等于120dp的数值
                                    // @param height 弹幕容器高度，请使用WRAP_CONTENT或0，让容器自适应弹幕的高度
                                    View bulletView =
                                            mNativeAd.renderBulletView(RelativeLayout.LayoutParams.MATCH_PARENT,
                                                    RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    // 若广告不支持弹幕组件，则返回空
                                    if (bulletView != null) {
                                        bulletContainer.addView(bulletView);
                                    }
                                }
                                // 渲染优惠券悬浮组件
                                final RelativeLayout couponFloatContainer = findViewById(R.id.coupon_float_view_container);
                                if (couponFloatContainer != null) {
                                    couponFloatContainer.removeAllViews();
                                    couponFloatContainer.setVisibility(View.VISIBLE);
                                    // 若广告不支持优惠券悬浮组件，则返回空
                                    View couponFloatView = mNativeAd.renderCouponFloatView(new NativeResponse.AdShakeViewListener() {
                                        @Override
                                        public void onDismiss() {
                                            Log.i(TAG, "CouponFloatView onDismiss()");
                                            couponFloatContainer.setVisibility(View.INVISIBLE);
                                        }
                                    });
                                    // 渲染尺寸请使用wrap_content以保证布局正常；如需调整大小，可使用setScaleX/Y进行缩放
                                    if (couponFloatView != null) {
                                        couponFloatContainer.addView(couponFloatView);
                                    }
                                }
                                // 渲染翻页组件(组件生效需要进行后台配置)
                                RelativeLayout pageContainer = findViewById(R.id.flip_page_view_container);
                                if (pageContainer != null) {
                                    pageContainer.removeAllViews();
                                    pageContainer.setVisibility(View.VISIBLE);
                                    if (mNativeAd.renderFlipPageView() != null) {
                                        pageContainer.addView(mNativeAd.renderFlipPageView());
                                    }

                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onNoAd(int errorCode, String msg, NativeResponse nativeResponse) {
                mRefreshLayout.setRefreshing(false);
                clientBidding(nativeResponse);
                Toast.makeText(FeedPortraitVideoActivity.this, "没有收到视频广告，请检查", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNativeFail(int errorCode, String msg, NativeResponse nativeResponse) {
                mRefreshLayout.setRefreshing(false);
                clientBidding(nativeResponse);
                Toast.makeText(FeedPortraitVideoActivity.this, "没有收到视频广告，请检查", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoDownloadSuccess() {
                // 视频缓存成功
                Log.i(TAG, "onVideoDownloadSuccess：");
                Toast.makeText(FeedPortraitVideoActivity.this, "视频缓存成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoDownloadFailed() {
                // 视频缓存失败
                Log.i(TAG, "onVideoDownloadFailed：");
                Toast.makeText(FeedPortraitVideoActivity.this, "视频缓存失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLpClosed() {
                // 落地页关闭回调
                Log.i(TAG, "onLpClosed");
                Toast.makeText(FeedPortraitVideoActivity.this, "lp页面关闭", Toast.LENGTH_SHORT).show();
            }
        };
        // 【可选】【Bidding】设置广告的底价，单位：分
        baiduNativeManager.setBidFloor(100);
        // 请求广告
        baiduNativeManager.loadPortraitVideoAd(requestParameters, listener);
    }

    // 点击联盟logo打开官网
    private void setUnionLogoClick(ImageView logo, final NativeResponse nrAd) {
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.unionLogoClick();
            }
        });
    }

    private View getCreativeView(final NativeResponse nrAd) {
        // 添加摇一摇组件，摇一摇组件的宽高需大于80dp
        View nativeView = nrAd.renderShakeView(80, 80,
                new NativeResponse.AdShakeViewListener() {
                    @Override
                    public void onDismiss() {
                        Log.i(TAG, "onShakeViewDismiss Ad.title: " + mNativeAd.getTitle());
                    }
                });
        // 若广告不支持摇一摇或组件宽高小于80dp，则shakeView为空
        if (nativeView == null) {
            // 渲染滑一滑组件，滑一滑组件中有文字，为保证文字正常展示，宽度不应该设置太小
            nativeView = nrAd.renderSlideView(120, -2, 3, new
                    NativeResponse.AdShakeViewListener() {
                        @Override
                        public void onDismiss() {
                            Log.i(TAG, "onSlideViewDismiss Ad.title: " + mNativeAd.getTitle());
                        }
                    });
        }
        return nativeView;
    }


    /**
     *  竞价结果回传
     */
    private void clientBidding(NativeResponse response) {

        Log.e(TAG,"ecpm=" + response.getECPMLevel());
        // 媒体自行设置竞价逻辑，并根据竞价结果上报
        String biddingPrice = response.getECPMLevel();
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
            secondInfo.put("adn", 2);
            // 竞价排名第二的物料类型，参考文档获取
            secondInfo.put("ad_t", 1);
            // 竞价排名第二的广告主名称，物料中获取
            secondInfo.put("ad_n", response.getBrandName());
            // 竞价时间，秒级时间戳
            secondInfo.put("ad_time", System.currentTimeMillis() / 1000);
            // 竞价排名第二的DSP的竞价类型，（百度竞价结果参数：1：分层保价；2：价格标签；3：bidding;4:其他)
            secondInfo.put("bid_t", 1);
            // 竞价排名第二的广告主标题，物料中获取
            secondInfo.put("ad_ti", response.getTitle());

            // 调用反馈竞价成功及二价
            BiddingListener winBiddingListener = new BiddingListener() {
                @Override
                public void onBiddingResult(boolean result, String message, HashMap<String, Object> ext) {
                    Log.i(TAG, "onBiddingResult-win: " + result + " msg信息：" + message);
                }
            };
            response.biddingSuccess(secondInfo, winBiddingListener);
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
            winInfo.put("ad_n", response.getBrandName());
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
            winInfo.put("ad_ti", response.getTitle());

            // 调用反馈竞价失败及原因
            BiddingListener lossBiddingListener = new BiddingListener() {
                @Override
                public void onBiddingResult(boolean result, String message, HashMap<String, Object> ext) {
                    Log.i(TAG, "onBiddingResult-loss: " + result + " msg信息：" + message);
                }
            };
            response.biddingFail(winInfo, lossBiddingListener);
        }
    }
}

