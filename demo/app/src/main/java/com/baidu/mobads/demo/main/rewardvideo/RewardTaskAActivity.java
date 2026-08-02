package com.baidu.mobads.demo.main.rewardvideo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.permission.BasePermissionActivity;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.RewardVideoAd;

import java.util.Map;

public class RewardTaskAActivity extends BasePermissionActivity {

    public static final String TAG = "RewardVideoActivity";
    // 线上广告位id
    private static final String AD_PLACE_ID = "5989414";
    private TextView mStateTextView;
    private Button mBtnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_task_a_activity);
        initView();
    }

    /** 激励视频加载监听 */
    class CustomRewardListener implements RewardVideoAd.RewardVideoAdListener {

        @Override
        public void onVideoDownloadSuccess() {
            // 视频缓存成功
            // 建议：可以在收到该回调后，再调用show展示激励视频
            Log.i(TAG, "onVideoDownloadSuccess,isReady=" + GlobalRewardAd.getInstance().mRewardVideoAd.isReady());
            mStateTextView.setText("视频缓存成功，可以进行展示");
        }

        @Override
        public void onVideoDownloadFailed() {
            // 视频缓存失败，可以在这儿重新load下一条广告，最好限制load次数（4-5次即可）。
            Log.i(TAG, "onVideoDownloadFailed");
            mStateTextView.setText("视频缓存失败，请重新加载");
        }

        @Override
        public void playCompletion() {
            Log.i(TAG, "playCompletion");
        }

        @Override
        public void onRewardVerify(boolean rewardVerify, Map<String, Object> rewardInfo) {
            // 激励视频奖励回调
            Log.i(TAG, "onRewardVerify: " + rewardVerify + ", type: " + rewardInfo.get("reward_type"));
        }

        @Override
        public void onAdSkip(float playScale) {
            // 用户点击跳过, 展示尾帧
            Log.i(TAG, "onSkip: " + playScale);
        }

        @Override
        public void onAdLoaded() {

            // 请求成功回调
            Log.i(TAG, "onAdLoaded");
            mStateTextView.setText("广告请求成功，等待物料缓存");
            if (mBtnNext != null) {
                mBtnNext.setEnabled(true);
            }
        }

        @Override
        public void onAdShow() {
            // 视频开始播放时候的回调
            Log.i(TAG, "onAdShow");
        }

        @Override
        public void onAdClick() {
            // 广告被点击的回调
            Log.i(TAG, "onAdClick");
        }

        @Override
        public void onAdClose(float playScale) {
            // 用户关闭了广告，直到开始下一次load前，将不会再收到任何回调
            // 说明：关闭按钮在mssp上可以动态配置，媒体通过mssp配置，可以选择广告一开始就展示关闭按钮，还是播放结束展示关闭按钮
            // 建议：收到该回调之后，可以重新load下一条广告,最好限制load次数（4-5次即可）
            // playScale[0.0-1.0],1.0表示播放完成，媒体可以按照自己的设计给予奖励
            Log.i(TAG, "onAdClose" + playScale);
            mStateTextView.setText("用户已关闭广告，请重新加载");
            GlobalRewardAd.getInstance().needReload = true;
            if (mBtnNext != null) {
                mBtnNext.setEnabled(false);
            }
        }

        @Override
        public void onAdFailed(String arg0) {
            // 广告失败回调，直到开始下一次load前，将不会再收到任何回调
            // 失败可能原因：广告内容填充为空；网络原因请求广告超时等
            // 建议：收到该回调之后，可以重新load下一条广告，最好限制load次数（4-5次即可）
            Log.i(TAG, "onAdFailed" + arg0);
            mStateTextView.setText("广告失败：" + arg0 + "，请重新加载");
            GlobalRewardAd.getInstance().needReload = true;
            if (mBtnNext != null) {
                mBtnNext.setEnabled(false);
            }
        }
    }

    /**
     * 请求加载下一条激励视频广告
     */
    private void loadNextVideo() {
        mStateTextView.setText("正在请求广告...");
        // 激励视屏产品可以选择是否使用SurfaceView进行渲染视频
        GlobalRewardAd.getInstance().mRewardVideoAd = new RewardVideoAd(RewardTaskAActivity.this,
                AD_PLACE_ID, new CustomRewardListener());
        RewardVideoAd mRewardVideoAd = GlobalRewardAd.getInstance().mRewardVideoAd;
        mRewardVideoAd.setUserId("user123456");
        mRewardVideoAd.setExtraInfo("aa?=bb&cc?=dd");
        // 【可选】【Bidding】设置广告的底价，单位：分
        mRewardVideoAd.setBidFloor(100);
        // 自定义传参
        final RequestParameters requestParameters = new RequestParameters.Builder()
                /**
                 * 【激励视频传参】传参功能支持的参数见ArticleInfo类，各个参数字段的描述和取值可以参考如下注释
                 * 注意：所有参数的总长度(不包含key值)建议控制在150字符内，避免因超长发生截断，影响信息的上报
                 */
                // 通用信息：用户性别，取值：0-unknown，1-male，2-female
                .addCustExt(ArticleInfo.USER_SEX, "1")
                // 最近阅读：小说、文章的名称
                .addCustExt(ArticleInfo.PAGE_TITLE, "测试书名")
                // 自定义传参，参考如下接入
                .addCustExt("cust_这是Key", "cust_这是Value" + System.currentTimeMillis())
                .addCustExt("Key2", "Value2")
                .build();
        // 若传参，如下设置
        mRewardVideoAd.setRequestParameters(requestParameters);
        // 请求广告，展示前必须调用
        mRewardVideoAd.load();
        GlobalRewardAd.getInstance().needReload = false;
    }
    private void initView() {
        mStateTextView = this.findViewById(R.id.rv_state_view);

        Button btn1 = this.findViewById(R.id.btn_load_reward);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 加载下一条激励视频广告
                loadNextVideo();
            }
        });
        mBtnNext = this.findViewById(R.id.btn_launch_taskB);
        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转下个页面
                Intent intent = new Intent(RewardTaskAActivity.this, RewardTaskBActivity.class);
                startActivity(intent);
            }
        });
    }

    static class GlobalRewardAd {
        private static volatile GlobalRewardAd sInstance = null;
        public RewardVideoAd mRewardVideoAd;
        public boolean needReload = true;

        public static GlobalRewardAd getInstance() {
            if (sInstance == null) {
                synchronized (GlobalRewardAd.class) {
                    if (sInstance == null) {
                        sInstance = new GlobalRewardAd();
                    }
                }
            }
            return sInstance;
        }
    }
}