package com.baidu.mobads.demo.main.patchvideo;

import java.util.Timer;
import java.util.TimerTask;

import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.PatchVideoNative;
import com.baidu.mobads.demo.main.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 视频贴片广告类，贴片广告支持物料类型【视频、图片】
 *
 * @author gaolinhua
 * @since 2019-03-25
 */
public class VideoPatchAdActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "VideoPatchAdActivity";
    // 贴片视频代码位id
    private String mAdPlaceId = "2058634";
    private PatchVideoNative mPrerollView;
    /** 父容器，添加视频view */
    private RelativeLayout mParentVideoRl;
    /** 倒计时view */
    private TextView mCountDownView;
    /** 倒计时的刷新间隔，单位ms */
    private static final int INTERVAL = 500;
    private final Handler mCountdownHandler = new Handler();
    private View mCloseAdView;
    private View mIntervalView;
    private boolean mNeedHandlePlayer = true;
    private String mMaterialType;
    private Timer mTimer = new Timer();
    private static final int PIC_MATERIAL_DURATION = 5 * 1000;
    private static final int PIC_INTERVAL = 1000;
    private View mNoAdView;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 需要在视频播放过程中保持屏幕常亮，所以需要设置这个
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.patch_ad);
        mParentVideoRl = findViewById(R.id.video_parent_view);
        mNoAdView = getLayoutInflater().inflate(R.layout.no_ad_view, null);
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        int winW = dm.widthPixels;
        int height = winW * 9 / 16;
        FrameLayout.LayoutParams rllp = new FrameLayout.LayoutParams(winW, height);
        mParentVideoRl.setLayoutParams(rllp);
        fetchAd();
    }

    private void initView() {
        if (mCloseAdView != null) {
            mParentVideoRl.removeView(mCloseAdView);
            mCloseAdView = null;
        }

        mCloseAdView = getLayoutInflater().inflate(R.layout.close_skip_ad, null);
        mCountDownView = mCloseAdView.findViewById(R.id.timer);
        mIntervalView = mCloseAdView.findViewById(R.id.interval_view);
        TextView skipAd = mCloseAdView.findViewById(R.id.skip_ad);
        skipAd.setOnClickListener(this);
        ImageView closeAd = mCloseAdView.findViewById(R.id.close_ad);
        closeAd.setOnClickListener(this);
        RelativeLayout.LayoutParams closeLp =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        closeLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        closeLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        int top = dp2px(26);
        int right = dp2px(15);
        closeLp.setMargins(0, top, right, 0);
        mParentVideoRl.addView(mCloseAdView, closeLp);
    }

    private void hideCountDownView() {
        if (mCountDownView != null) {
            mCountDownView.setVisibility(View.GONE);
        }
        if (mIntervalView != null) {
            mIntervalView.setVisibility(View.GONE);
        }
    }

    /** 请求广告 */
    private void fetchAd() {
        mPrerollView = new PatchVideoNative(this, mAdPlaceId, mParentVideoRl,
                new PatchVideoNative.IPatchVideoNativeListener() {
                    @Override
                    public void onAdLoad(String type) {
                        // sdk默认播放非静音，如果想默认静音播放，可以这么设置
                        // mPrerollView.setVideoMute(true);
                        // 广告请求成功 type表示物料类型,目前支持图片（"normal"),视频物料（"video"）；
                        Log.i(TAG, "onAdLoad,广告请求成功");
                        mMaterialType = type;
                        mParentVideoRl.removeView(mNoAdView);
                    }

                    @Override
                    public void onAdFailed(int code, String msg) {
                        // 广告请求失败
                        mNeedHandlePlayer = false;
                        Log.i(TAG, "onAdFailed,广告请求失败: " + msg);
                        RelativeLayout.LayoutParams rllp = new RelativeLayout
                                .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rllp.addRule(RelativeLayout.CENTER_IN_PARENT);
                        mParentVideoRl.addView(mNoAdView, rllp);
                    }

                    @Override
                    public void playCompletion() {
                        // 视频播放完成
                        mNeedHandlePlayer = false;
                        hideCountDownView();
                        stopVideoTimer();
                        Log.i(TAG, "playCompletion,视频播放完成");
                    }

                    @Override
                    public void playError() {
                        // 视频播放错误
                        mNeedHandlePlayer = false;
                        stopVideoTimer();
                        hideCountDownView();
                        Log.i(TAG, "playError,视频播放错误");
                    }

                    @Override
                    public void onAdShow() {
                        // 视频播放第一帧或者图片渲染成功
                        Log.i(TAG, "onAdShow,视频第一帧展示，或者图片渲染成功展示");
                        initView();
                        if ("video".equals(mMaterialType)) {
                            // 视频物料
                            startVideoTimer();
                        } else if ("normal".equals(mMaterialType)) {
                            // 图片物料，倒计时时间需要媒体自己定义，demo默认是5秒
                            startPicTimer();
                        } else if ("gif".equals(mMaterialType)) {
                            // 不支持该物料格式
                        }
                    }

                    @Override
                    public void onAdClick() {
                        Log.i(TAG, "onAdClick,被点击");
                    }
                });
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
        RequestParameters requestParameters = new RequestParameters.Builder()
                .setWidth(winW)
                .setHeight(winH)
                .build();
        // 开始请求广告
        mPrerollView.requestAd(requestParameters);
    }

    @Override
    public void onClick(View v) {
        if (mParentVideoRl != null) {
            mParentVideoRl.removeAllViews();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVideoTimer();
        stopPicTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startVideoTimer();
        startPicTimer();
    }

    /** 计时器开始 */
    private void startVideoTimer() {
        if (mCountDownView != null) {
            mCountdownHandler.removeCallbacksAndMessages(null);
            mCountdownHandler.postDelayed(updateVideoTimerRunnable, 0);
        }
    }

    /** 计时器停止 */
    private void stopVideoTimer() {
        if (mCountDownView != null) {
            mCountdownHandler.removeCallbacksAndMessages(null);
        }
    }

    private void startPicTimer() {
        if (mPrerollView == null || mCountDownView == null || !"normal".equals(mMaterialType)) {
            return;
        }
        if (mTimer == null) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask()

        {
            @Override
            public void run() {
                updateProTask();
            }

        }, 0, PIC_INTERVAL);
    }

    int adTime = PIC_MATERIAL_DURATION / PIC_INTERVAL;

    private void updateProTask() {
        if (adTime >= 1 && adTime <= 5) {
            mCountdownHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCountDownView.setText(String.valueOf(adTime) + "s");
                    // 倒计时继续
                    adTime -= 1;
                }
            });
        } else {
            mCountdownHandler.post(new Runnable() {
                @Override
                public void run() {
                    hideCountDownView();
                }
            });
            stopPicTimer();
        }
    }

    public void stopPicTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    /** 更新倒计时进度 */
    private Runnable updateVideoTimerRunnable = new Runnable() {
        public void run() {
            if (mPrerollView == null || mCountDownView == null || !mNeedHandlePlayer) {
                return;
            }
            int position = (int) mPrerollView.getCurrentPosition();
            int mDuration = (int) mPrerollView.getDuration();
            int adTime = 0;
            if (mDuration > 0 && position <= mDuration && position >= 0) {
                adTime = (int) Math.round((mDuration - position) / 1000.0);
            }
            position = Math.min(position + 1000, mDuration);
            mCountDownView.setText(String.valueOf(adTime) + "s");
            if (position < mDuration) {
                mCountdownHandler.postDelayed(updateVideoTimerRunnable, INTERVAL);
            }
        }
    };

    private int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}

