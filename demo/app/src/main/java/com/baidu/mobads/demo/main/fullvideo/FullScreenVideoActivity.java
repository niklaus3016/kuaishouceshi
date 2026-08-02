package com.baidu.mobads.demo.main.fullvideo;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;
import com.baidu.mobads.demo.main.permission.BasePermissionActivity;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.FullScreenVideoAd;
import com.baidu.mobads.sdk.api.RequestParameters;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * 1. 全屏视频集成参考类：FullScreenVideoActivity
 * 2. 全屏视频广告推荐缓存成功后再播放，避免卡顿。
 *    需提前预加载load，缓存成功后，调用show播放。
 *    可以通过isReady接口判断是否缓存成功。
 * 3. 单次请求的广告不支持多次展现。下次展现前需要重新预加载，可以在点击关闭操作后重新预加载新广告。
 * 4. 广告存在有效期，需要一定时间（2小时，该值为非固定值）内展现。可以通过isReady判断是否过期。
 * 5. 监听展现回调请实现接口 -> {@link FullScreenVideoAd.FullScreenVideoAdListener}
 *    展现失败onAdFailed中做异常流程处理。
 * 6. 全屏视频播放5秒后，显示跳过按钮，此时提供跳过回调并返回已播放进度 -> {@link CustomFullScreenListener#onAdSkip(float)}
 * 7. 检查API是否发生更改。
 */
public class FullScreenVideoActivity extends BasePermissionActivity {

    public static final String TAG = "FullScreenVideoActivity";
    // 线上广告位id
    private static final String AD_PLACE_ID = "7339862";
    public FullScreenVideoAd mFullScreenVideoAd;
    private EditText mAdPlaceIdView;
    private TextView mStateTextView;
    private Button mBtnShow;
    private boolean needReload = true;
    // 测试环境的广告位id
    //    private String mAdPlaceId = "2411590";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_video);
        initView();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /** 全屏视频加载监听 */
    class CustomFullScreenListener implements FullScreenVideoAd.FullScreenVideoAdListener {

        @Override
        public void onVideoDownloadSuccess() {
            // 视频缓存成功
            // 建议：可以在收到该回调后，再调用show展示全屏视频
            Log.i(TAG, "onVideoDownloadSuccess, isReady=" + mFullScreenVideoAd.isReady());
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
            // 播放完成回调，媒体可以在这儿给用户奖励
            Log.i(TAG, "playCompletion");
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
            needReload = true;
            if (mBtnShow != null) {
                mBtnShow.setEnabled(false);
            }
        }

        @Override
        public void onAdFailed(String arg0) {
            // 广告失败回调，直到开始下一次load前，将不会再收到任何回调
            // 失败可能原因：广告内容填充为空；网络原因请求广告超时等
            // 建议：收到该回调之后，可以重新load下一条广告，最好限制load次数（4-5次即可）
            Log.i(TAG, "onAdFailed");
            mStateTextView.setText("广告失败，请重新加载");
            needReload = true;
            if (mBtnShow != null) {
                mBtnShow.setEnabled(false);
            }
            clientBidding();
        }

        @Override
        public void onAdSkip(float playScale) {
            // 用户跳过了广告
            // playScale[0.0-1.0],1.0表示播放完成
            Log.i(TAG, "onAdSkip: " + playScale);
        }

        @Override
        public void onAdLoaded() {

            // 请求成功回调
            Log.i(TAG, "onAdLoaded");
            mStateTextView.setText("广告请求成功，等待物料缓存");
            if (mBtnShow != null) {
                mBtnShow.setEnabled(true);
            }
            // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
            clientBidding();
        }
    }

    private void initView() {
        mStateTextView = this.findViewById(R.id.fv_state_view);
        Button btn1 = this.findViewById(R.id.btn_change_orientation);
        btn1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentOrientation = getResources().getConfiguration().orientation;
                if (currentOrientation == ORIENTATION_PORTRAIT) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (currentOrientation == ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });

        Button btn2 = this.findViewById(R.id.btn_load);
        btn2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final RequestParameters requestParameters = new RequestParameters.Builder()
                        /**
                         * 【全屏视频传参】传参功能支持的参数见ArticleInfo类，各个参数字段的描述和取值可以参考如下注释
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
                // 全屏视频产品可以选择是否使用SurfaceView进行渲染视频
                mFullScreenVideoAd = new FullScreenVideoAd(FullScreenVideoActivity.this,
                        mAdPlaceIdView.getText().toString(), new CustomFullScreenListener(),
                        AdSettingHelper.getInstance().getBooleanFromSetting(AdSettingProperties.FULL_SCREEN_VIDEO_USE_SURFACE, false));
                // 【可选】【Bidding】设置广告的底价，单位：分
                mFullScreenVideoAd.setBidFloor(100);
                // 全屏视频传参，按需添加该方法（非必要）
                mFullScreenVideoAd.setRequestParameters(requestParameters);
                // 请求广告
                mFullScreenVideoAd.load();
                needReload = false;
            }

        });

        mBtnShow = this.findViewById(R.id.btn_show);
        mBtnShow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFullScreenVideoAd == null || needReload) {
                    toastText("请在加载成功后进行广告展示！");
                    return;
                }
                // 1. 强烈建议在收到onVideoDownloadSuccess回调、视频物料缓存完成后再展示广告，
                //    提升全屏视频的播放体验，否则有播放卡顿的风险。
                // 2. 在展示前可以调用isReady接口判断广告是否就绪：
                //    此接口会判断本地是否存在【未展示 & 未过期 & 已缓存】的广告
                if (!mFullScreenVideoAd.isReady()) {
                    toastText("视频广告未缓存/已展示/已过期");
                    return;
                }
                mFullScreenVideoAd.show();
            }

        });

        mAdPlaceIdView = findViewById(R.id.edit_apid);
        mAdPlaceIdView.setText(AD_PLACE_ID);
        mAdPlaceIdView.clearFocus();
    }

    private void toastText(String text) {
        Toast.makeText(FullScreenVideoActivity.this, text, Toast.LENGTH_SHORT).show();
    }


    /**
     *  竞价结果回传
     */
    private void clientBidding() {

        Log.e(TAG,"ecpm=" + mFullScreenVideoAd.getECPMLevel());
        // 媒体自行设置竞价逻辑，并根据竞价结果上报
        String biddingPrice = mFullScreenVideoAd.getECPMLevel();
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
            mFullScreenVideoAd.biddingSuccess(secondInfo, winBiddingListener);
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
            mFullScreenVideoAd.biddingFail(winInfo, lossBiddingListener);
        }
    }

}