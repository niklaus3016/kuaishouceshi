package com.baidu.mobads.demo.main.express;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.ExpressResponse;
import com.baidu.mobads.sdk.api.RequestParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 优选模版广告接入示例，展示了优选模版在全屏场景下的接入效果和接入方式
 */
public class ExpressPortraitFeedActivity extends Activity {
    private static final String TAG = ExpressPortraitFeedActivity.class.getSimpleName();
    private static final String FEED_SMART_OPT_AD_PLACE_ID = "15487839"; // 信息流智能优选(竖版模板)广告位

    List<ExpressResponse> mExpressAds = new ArrayList<ExpressResponse>();

    private ExpressDrawAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private BaiduNativeManager mBaiduNativeManager;
    private AtomicBoolean mIsLoading = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_recycle_view);
        initViewPager();

        /**
         * Step 1. 创建BaiduNative对象，参数分别为： 上下文context，广告位ID
         * 注意：请将adPlaceId替换为自己的广告位ID
         * 注意：信息流广告对象，与广告位id一一对应，同一个对象可以多次发起请求
         */
        mBaiduNativeManager = new BaiduNativeManager(this, FEED_SMART_OPT_AD_PLACE_ID);

        loadExpressAd();
    }

    private void loadExpressAd() {
        if (mIsLoading.getAndSet(true)) {
            // 避免重复加载广告
            return;
        }
        // 构建请求参数
        final RequestParameters requestParameters = new RequestParameters.Builder()
                /**
                 * 【信息流传参】传参功能支持的参数见ArticleInfo类，各个参数字段的描述和取值可以参考如下注释
                 * 注意：所有参数的总长度(不包含key值)建议控制在150字符内，避免因超长发生截断，影响信息的上报
                 * 注意：【高】【中】【低】代表参数的优先级，请尽量提供更多高优先级参数
                 */
                // 【高】通用信息：用户性别，取值：0-unknown，1-male，2-female
                .addExtra(ArticleInfo.USER_SEX, "1")
                // 【高】最近阅读：小说、文章的名称
                .addExtra(ArticleInfo.PAGE_TITLE, "测试书名")
                // 【高】最近阅读：小说、文章的ID
                .addExtra(ArticleInfo.PAGE_ID, "10930484090")
                // 【高】书籍信息：小说分类，取值：一级分类和二级分类用'/'分隔
                .addExtra(ArticleInfo.CONTENT_CATEGORY, "一级分类/二级分类")
                // 【高】书籍信息：小说、文章的标签，取值：最多10个，且不同标签用'/分隔'
                .addExtra(ArticleInfo.CONTENT_LABEL, "标签1/标签2/标签3")
                // 【中】通用信息：收藏的小说ID，取值：最多五个ID，且不同ID用'/分隔'
                .addExtra(ArticleInfo.FAVORITE_BOOK, "这是小说的名称1/这是小说的名称2/这是小说的名称3")
                // 【中】最近阅读：一级目录，格式：章节名，章节编号
                .addExtra(ArticleInfo.FIRST_LEVEL_CONTENTS, "测试一级目录，001")
                // 【低】书籍信息：章节数，取值：32位整数，默认值0
                .addExtra(ArticleInfo.CHAPTER_NUM, "12345")
                // 【低】书籍信息：连载状态，取值：0 表示连载，1 表示完结，默认值0
                .addExtra(ArticleInfo.PAGE_SERIAL_STATUS, "0")
                // 【低】书籍信息：作者ID/名称
                .addExtra(ArticleInfo.PAGE_AUTHOR_ID, "123456")
                // 【低】最近阅读：二级目录，格式：章节名，章节编号
                .addExtra(ArticleInfo.SECOND_LEVEL_CONTENTS, "测试二级目录，2000")
                .addCustExt("cust_Key_这是key", "cust_Value_这是Value" + System.currentTimeMillis())
                .addCustExt("AAAAAAA", "aaaaaa")
                .addCustExt(ArticleInfo.PAGE_TITLE, "真测试书名")
                .build();
        // 【可选】【Bidding】设置广告的底价，单位：分
        mBaiduNativeManager.setBidFloor(100);
        // 发起信息流广告请求
        mBaiduNativeManager.loadExpressAd(requestParameters, new BaiduNativeManager.ExpressAdListener() {
            @Override
            public void onNativeLoad(List<ExpressResponse> expressResponses) {
                Log.i(TAG, "onNativeLoad:" + (expressResponses != null ? expressResponses.size() : null));
                mIsLoading.set(false);
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                if (expressResponses != null && !expressResponses.isEmpty()) {
                    // 绑定监听事件并渲染模板
                    for (final ExpressResponse ad : expressResponses) {
                        // 绑定监听
                        bindExpressListeners(ad);
                        // 设定预期的宽高
                        ad.setExpectedSizePixel(mRecyclerView.getWidth(), mRecyclerView.getHeight());
                        // 渲染模板广告
                        ad.render();
                    }
                }
            }

            @Override
            public void onNoAd(int code, String msg, ExpressResponse expressResponse) {
                Log.i(TAG, "onNoAd reason:" + msg);
                mIsLoading.set(false);
                clientBidding(expressResponse);
            }

            @Override
            public void onNativeFail(int errorCode, String message, ExpressResponse expressResponse) {
                Log.w(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
                mIsLoading.set(false);
                clientBidding(expressResponse);
            }

            @Override
            public void onVideoDownloadSuccess() {

            }

            @Override
            public void onVideoDownloadFailed() {

            }

            @Override
            public void onLpClosed() {
                Log.i(TAG, "onLpClosed.");
            }
        });
    }

    private void bindExpressListeners(final ExpressResponse expressAd) {
        // 注册监听
        expressAd.setInteractionListener(new ExpressResponse.ExpressInteractionListener() {
            @Override
            public void onAdClick() {
                Log.i(TAG, "onAdClick");
            }

            @Override
            public void onAdExposed() {
                Log.i(TAG, "onADExposed");
            }

            @Override
            public void onAdRenderFail(View adView, String reason, int code) {
                Log.i(TAG, "onAdRenderFail: " + reason + code);
            }

            @Override
            public void onAdRenderSuccess(View adView, float width, float height) {
                Log.i(TAG, "onAdRenderSuccess: " + width + ", " + height);
                mExpressAds.add(expressAd);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAdUnionClick() {
                Log.i(TAG, "onAdUnionClick");
            }
        });
        expressAd.setAdPrivacyListener(new ExpressResponse.ExpressAdDownloadWindowListener() {
            @Override
            public void onADPrivacyClick() {
                Log.i(TAG, "onADPrivacyClick");
            }

            @Override
            public void onADFunctionClick() {
                Log.i(TAG, "onADFunctionClick");
            }

            @Override
            public void onADPermissionShow() {
                Log.i(TAG, "onADPermissionShow");
            }

            @Override
            public void onADPermissionClose() {
                Log.i(TAG, "onADPermissionClose");
            }

            @Override
            public void adDownloadWindowShow() {
                Log.i(TAG, "AdDownloadWindowShow");
            }

            @Override
            public void adDownloadWindowClose() {
                Log.i(TAG, "adDownloadWindowClose");
            }
        });
        // 负反馈按钮&负反馈弹框的点击事件
        expressAd.setAdDislikeListener(new ExpressResponse.ExpressDislikeListener() {
            @Override
            public void onDislikeWindowShow() {
                Log.i(TAG, "onDislikeWindowShow");
            }

            @Override
            public void onDislikeItemClick(String reason) {
                Log.i(TAG, "onDislikeItemClick: " + reason);
                Log.i(TAG, "Dislike AD title: " + expressAd.getAdData().getTitle());
                // 移除被关闭的广告
                mExpressAds.remove(expressAd);
                mAdapter.notifyDataSetChanged();

            }

            @Override
            public void onDislikeWindowClose() {
                Log.i(TAG, "onDislikeWindowClose");
            }
        });
        // 广告位开启一键关闭功能后，需要注册此监听，处理点击负反馈后的关闭广告事件
        expressAd.setAdCloseListener(new ExpressResponse.ExpressCloseListener() {
            @Override
            public void onAdClose(ExpressResponse expressResponse) {
                // 移除被关闭的广告
                mExpressAds.remove(expressResponse);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     *  竞价结果回传
     */
    private void clientBidding(ExpressResponse response) {

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
            secondInfo.put("ad_n", "广告主名称");
            // 竞价时间，秒级时间戳
            secondInfo.put("ad_time", System.currentTimeMillis() / 1000);
            // 竞价排名第二的DSP的竞价类型，（百度竞价结果参数：1：分层保价；2：价格标签；3：bidding;4:其他)
            secondInfo.put("bid_t", 1);
            // 竞价排名第二的广告主标题，物料中获取
            secondInfo.put("ad_ti", "广告主标题");

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
            winInfo.put("ad_n", "brandName");
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
            response.biddingFail(winInfo, lossBiddingListener);
        }
    }

    private void initViewPager() {
        mRecyclerView = findViewById(R.id.container);
        mAdapter = new ExpressDrawAdapter();
        mRecyclerView.setAdapter(mAdapter);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (RecyclerView.SCROLL_STATE_IDLE == newState) {
                    int lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
                    if (lastVisibleItemPosition + 3 > 3 * mExpressAds.size()) {
                        // 展现最后一个广告时，加载新的广告
                        loadExpressAd();
                    }
                }
            }
        });
    }

    class ExpressDrawAdapter extends RecyclerView.Adapter<ExpressDrawAdapter.DrawViewHolder> {

        @NonNull
        @Override
        public DrawViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
            FrameLayout container = new FrameLayout(ExpressPortraitFeedActivity.this);
            container.setBackgroundColor(Color.LTGRAY);
            container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new DrawViewHolder(container, type);
        }

        @Override
        public void onBindViewHolder(@NonNull DrawViewHolder viewHolder, int position) {
            viewHolder.layout.removeAllViews();
            if (viewHolder.type == 1) {
                // 添加广告视图
                viewHolder.layout.addView(getAdView(position));
            } else {
                // 添加内容视图
                viewHolder.layout.addView(getContentView());
            }

        }

        @Override
        public int getItemViewType(int position) {
            return (position % 3) == 0 ? 1 : 0;
        }

        @Override
        public int getItemCount() {
            return mExpressAds.size() * 3;
        }

        private View getAdView(int position) {
            int index = position / 3;
            if (index < mExpressAds.size()) {
                return mExpressAds.get(index).getExpressAdView();
            }
            return null;
        }

        private View getContentView() {
            return LayoutInflater.from(ExpressPortraitFeedActivity.this)
                    .inflate(R.layout.item_content_portrait, null, false);
        }

        class DrawViewHolder extends RecyclerView.ViewHolder {
            FrameLayout layout;
            int type;
            public DrawViewHolder(@NonNull FrameLayout itemView, int type) {
                super(itemView);
                this.type = type;
                this.layout = (FrameLayout) itemView;
            }
        }
    }
}
