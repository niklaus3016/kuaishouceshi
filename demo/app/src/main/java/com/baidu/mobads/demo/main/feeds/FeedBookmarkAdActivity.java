package com.baidu.mobads.demo.main.feeds;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.adSettings.AdSettingActivity;
import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.NativeBookmarkView;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 优选模版广告接入示例，展示了优选模版在全屏场景下的接入效果和接入方式
 */
public class FeedBookmarkAdActivity extends Activity {
    private static final String TAG = FeedBookmarkAdActivity.class.getSimpleName();
    private static final String FEED_BOOKMARK_AD_PLACE_ID = "2058628"; // 信息流广告位

    List<NativeBookmarkView> bookmarkViews = new ArrayList<NativeBookmarkView>();

    private NativeDrawAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private BaiduNativeManager mBaiduNativeManager;
    private AtomicBoolean mIsLoading = new AtomicBoolean(false);

    // 视频播放器是否静音
    private boolean isVideoMute = false;
    // 视频播放器是否隐藏声音按钮
    private boolean hideMute = false;
    // 是否隐藏广告logo
    private boolean hideAdLogo = false;
    // 是否隐藏负反馈按钮
    private boolean hideDislike = false;
    // 是否限制书签的点击区域
    private boolean regionClick = false;

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
        mBaiduNativeManager = new BaiduNativeManager(this, FEED_BOOKMARK_AD_PLACE_ID);

        loadFeedAd();

        // 读取书签的个性化设置
        AdSettingHelper setting = AdSettingHelper.getInstance();
        isVideoMute = setting.getBooleanFromSetting(AdSettingProperties.FEED_BOOKMARK_MUTE, false);
        hideMute = setting.getBooleanFromSetting(AdSettingProperties.FEED_BOOKMARK_HIDE_MUTE, false);
        hideDislike = setting.getBooleanFromSetting(AdSettingProperties.FEED_BOOKMARK_HIDE_DISLIKE, false);
        hideAdLogo = setting.getBooleanFromSetting(AdSettingProperties.FEED_BOOKMARK_HIDE_AD_LOGO, false);
        regionClick = setting.getBooleanFromSetting(AdSettingProperties.FEED_BOOKMARK_REGION_CLICK, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startColorChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopColorChange();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_menu, menu);
        menu.findItem(R.id.document_menu_copy).setTitle("书签设置");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.document_menu_copy) {
            Intent intent = new Intent(this, AdSettingActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFeedAd() {
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
        mBaiduNativeManager.loadFeedAd(requestParameters, new BaiduNativeManager.FeedAdListener() {
            @Override
            public void onNativeLoad(List<NativeResponse> nativeResponses) {
                Log.i(TAG, "onNativeLoad:" + (nativeResponses != null ? nativeResponses.size() : null));
                mIsLoading.set(false);
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                if (nativeResponses != null && !nativeResponses.isEmpty()) {
                    // 绑定监听事件并渲染模板
                    for (final NativeResponse ad : nativeResponses) {
                        NativeBookmarkView bookmarkView = new NativeBookmarkView(ad,
                                mRecyclerView.getWidth(), mRecyclerView.getHeight());
                        View render = bookmarkView
                                .regionClick(regionClick)
                                .hideAdLogo(hideAdLogo)
                                .hideDislike(hideDislike)
                                .hideMuteButton(hideMute)
                                .mute(isVideoMute)
                                .render();
                        if (render != null) {
                            bookmarkViews.add(bookmarkView);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNoAd(int errorCode, String msg, NativeResponse nativeResponse) {
                Log.i(TAG, "onNoAd reason:" + msg);
                mIsLoading.set(false);
                clientBidding(nativeResponse);
            }

            @Override
            public void onNativeFail(int errorCode, String message, NativeResponse nativeResponse) {
                Log.w(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
                mIsLoading.set(false);
                clientBidding(nativeResponse);
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
        mAdapter = new NativeDrawAdapter();
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
                    if (lastVisibleItemPosition + 3 > 3 * bookmarkViews.size()) {
                        // 展现最后一个广告时，加载新的广告
                        loadFeedAd();
                    }
                }
            }
        });
    }

    class NativeDrawAdapter extends RecyclerView.Adapter<NativeDrawAdapter.DrawViewHolder> {
        NativeBookmarkView currentView;

        @NonNull
        @Override
        public DrawViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
            FrameLayout container = new FrameLayout(FeedBookmarkAdActivity.this);
            container.setBackgroundColor(Color.parseColor("#fce6c9"));
            container.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new DrawViewHolder(container, type);
        }

        @Override
        public void onBindViewHolder(@NonNull DrawViewHolder viewHolder, int position) {
            viewHolder.layout.removeAllViews();
            if (viewHolder.type == 1) {
                // 添加广告视图
                NativeBookmarkView bookmarkView = getAdView(position);
                if (bookmarkView != null) {
                    viewHolder.layout.addView(bookmarkView.getView());
                    registerListeners(bookmarkView);
                    // 启动入场动画
                    bookmarkView.startAnim(new Random().nextBoolean()
                            ? NativeBookmarkView.EnterDirection.RIGHT_TO_LEFT
                            : NativeBookmarkView.EnterDirection.TOP_TO_BOTTOM);
                    currentView = bookmarkView;
                }
            } else {
                // 添加内容视图
                viewHolder.layout.addView(getContentView());
                currentView = null;
            }

        }

        @Override
        public int getItemViewType(int position) {
            return (position % 3) == 0 ? 1 : 0;
        }

        @Override
        public int getItemCount() {
            return bookmarkViews.size() * 3;
        }

        private NativeBookmarkView getAdView(int position) {
            int index = position / 3;
            if (index < bookmarkViews.size()) {
                return bookmarkViews.get(index);
            }
            return null;
        }

        private View getContentView() {
            return LayoutInflater.from(FeedBookmarkAdActivity.this)
                    .inflate(R.layout.item_content_portrait, null, false);
        }

        public void registerListeners(final NativeBookmarkView bookmarkView) {
            final NativeResponse nativeResponse = bookmarkView.getNativeResponse();
            // 绑定下载类广告的隐私协议和权限点击事件
            if (isDownloadAd(nativeResponse)) {
                nativeResponse.setAdPrivacyListener(new NativeResponse.AdDownloadWindowListener() {
                    @Override
                    public void adDownloadWindowShow() {
                        Log.i(TAG, "adDownloadWindowShow: " + nativeResponse.getTitle());
                    }

                    @Override
                    public void adDownloadWindowClose() {
                        Log.i(TAG, "adDownloadWindowClose: " + nativeResponse.getTitle());
                    }

                    @Override
                    public void onADPrivacyClick() {
                        Log.i(TAG, "onADPrivacyClick: " + nativeResponse.getTitle());
                    }

                    @Override
                    public void onADFunctionClick() {
                        Log.i(TAG, "onADFunctionClick: " + nativeResponse.getTitle());
                    }

                    @Override
                    public void onADPermissionShow() {
                        Log.i(TAG, "onADPermissionShow: " + nativeResponse.getTitle());
                    }

                    @Override
                    public void onADPermissionClose() {
                        Log.i(TAG, "onADPermissionClose: " + nativeResponse.getTitle());
                    }
                });
            }
            // 绑定负反馈、关闭的事件监听
            bookmarkView.dislikeListener(new NativeResponse.AdDislikeListener() {
                @Override
                public void onDislikeWindowShow() {
                    Log.i(TAG, "onDislikeWindowShow: " + nativeResponse.getTitle());
                }

                @Override
                public void onDislikeItemClick(String reason) {
                    Log.i(TAG, "onDislikeItemClick: " + reason);
                }

                @Override
                public void onDislikeWindowClose() {
                    Log.i(TAG, "onDislikeWindowClose: " + nativeResponse.getTitle());
                }
            });
            bookmarkView.closeListener(new NativeResponse.AdCloseListener() {
                @Override
                public void onAdClose(NativeResponse response) {
                    Log.i(TAG, "onAdClose: " + response.getTitle());
                }
            });
            // 注册点击事件
            List<View> clickViews = new ArrayList<>();
            List<View> creativeViews = new ArrayList<>();
            nativeResponse.registerViewForInteraction(bookmarkView.getView(), clickViews, creativeViews,
                    new NativeResponse.AdInteractionListener() {
                        @Override
                        public void onAdClick() {
                            Log.i(TAG, "onAdClick: " + nativeResponse.getTitle());
                        }

                        @Override
                        public void onADExposed() {
                            Log.i(TAG, "onADExposed: " + nativeResponse.getTitle());
                        }

                        @Override
                        public void onADExposureFailed(int reason) {
                            Log.w(TAG, "onADExposureFailed: " + nativeResponse.getTitle());
                        }

                        @Override
                        public void onADStatusChanged() {
                            Log.i(TAG, "onADStatusChanged: ");
                        }

                        @Override
                        public void onAdUnionClick() {
                            Log.i(TAG, "onAdUnionClick: " + nativeResponse.getTitle());
                        }
                    });
        }

        private boolean isDownloadAd(NativeResponse nrAd) {
            return nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                    && !TextUtils.isEmpty(nrAd.getAppVersion()) && !TextUtils.isEmpty(nrAd.getPublisher())
                    && !TextUtils.isEmpty(nrAd.getAppPrivacyLink()) && !TextUtils.isEmpty(nrAd.getAppPermissionLink());
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

    // 不断改变颜色
    private ColorChangeHandler mHandler = new ColorChangeHandler(Looper.getMainLooper());
    private void startColorChange() {
        mHandler.isRunning = true;
        mHandler.sendEmptyMessageDelayed(0, 1000);
    }

    private void stopColorChange() {
        mHandler.isRunning = false;
        mHandler.removeMessages(0);
    }

    class ColorChangeHandler extends Handler {
        private boolean isRunning = false;
        private int index = 0;
        public ColorChangeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (isRunning) {
                if (mAdapter.currentView != null) {
                    mAdapter.currentView.setBookmarkColor(getColor());
                }
                sendEmptyMessageDelayed(0, 1000);
            }
        }

        private NativeBookmarkView.BookmarkColor getColor() {
            return NativeBookmarkView.BookmarkColor
                    .values()[index++ % NativeBookmarkView.BookmarkColor.values().length];
        }
    }
}
