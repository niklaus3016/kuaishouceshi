package com.baidu.mobads.demo.main.feeds;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BDMarketingTextView;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.INativeVideoListener;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.XNativeView;
import com.baidu.mobads.demo.main.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 信息流自渲染广告在RecyclerView中的使用示例
 */
public class FeedAdRecycleActivity extends Activity {
    private static final String TAG = FeedAdRecycleActivity.class.getSimpleName();
    private static final String FEED_VIDEO_AD_PLACE_ID = "2362913";        // 信息流视频

    List<RecycleViewItem> itemList = new ArrayList<>(); // 媒体自渲染广告使用
    private RecycleAdapter mAdapter;
    private BaiduNativeManager mBaiduNativeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_recycle_view);

        initView();

        /**
         * Step 1. 创建BaiduNative对象，参数分别为： 上下文context，广告位ID
         * 注意：请将adPlaceId替换为自己的广告位ID
         * 注意：信息流广告对象，与广告位id一一对应，同一个对象可以多次发起请求
         */
        mBaiduNativeManager = new BaiduNativeManager(this, FEED_VIDEO_AD_PLACE_ID);

        loadFeedAd();

    }

    private void initView() {
        RecyclerView recyclerView = findViewById(R.id.container);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.onDraw(c, parent, state);
            }

            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.set(0, 0, 0, 4);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (RecyclerView.SCROLL_STATE_IDLE == newState) {
                    int lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
                    if (lastVisibleItemPosition + 3 > 3 * itemList.size()) {
                        // 展现最后一个广告时，加载新的广告
                        loadFeedAd();
                    }
                }
            }
        });
        mAdapter = new RecycleAdapter();
        recyclerView.setAdapter(mAdapter);
    }

    private void loadFeedAd() {
        // 若与百度进行相关合作，可使用如下接口上报广告的上下文
        RequestParameters requestParameters = new RequestParameters.Builder()
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

        mBaiduNativeManager.loadFeedAd(requestParameters, new BaiduNativeManager.FeedAdListener() {
            @Override
            public void onNativeLoad(List<NativeResponse> nativeResponses) {
                Log.i(TAG, "onNativeLoad:" +
                        (nativeResponses != null ? nativeResponses.size() : null));
                Context applicationContext = FeedAdRecycleActivity.this.getApplicationContext();
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                if (nativeResponses != null && !nativeResponses.isEmpty()) {
                    int start = itemList.size();
                    for (NativeResponse ad : nativeResponses) {
                        // 获取广告数据, 提前缓存广告，并可以做好预渲染，保证列表滑动流畅
                        if (ad != null) {
                            itemList.add(new NativeAdItem(ad).prepareItemView(applicationContext));
                        }
                        // Mock内容数据
                        itemList.add(new RecycleViewItem(0).prepareItemView(applicationContext));
                        itemList.add(new RecycleViewItem(0).prepareItemView(applicationContext));
                    }
                    mAdapter.notifyItemRangeInserted(start, nativeResponses.size() * 3);
                }
            }

            @Override
            public void onNoAd(int errorCode, String msg, NativeResponse nativeResponse) {
                Log.w(TAG, "onNoAd reason:" + msg);
            }

            @Override
            public void onNativeFail(int errorCode, String msg, NativeResponse nativeResponse) {
                Log.w(TAG, "onNativeFail reason:" + msg);
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

    private void bindCloseButton(final NativeAdItem item) {
        item.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onAdClose:" + item.nativeResponse.getTitle());
                itemList.remove(item);
                mAdapter.notifyDataSetChanged();
            }
        });
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

    private boolean isDownloadAd(NativeResponse nrAd) {
        return nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                && !TextUtils.isEmpty(nrAd.getAppVersion()) && !TextUtils.isEmpty(nrAd.getPublisher())
                && !TextUtils.isEmpty(nrAd.getAppPrivacyLink()) && !TextUtils.isEmpty(nrAd.getAppPermissionLink());
    }

    class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.NativeViewHolder> {

        public RecycleAdapter() {
        }

        @NonNull
        @Override
        public NativeViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            FrameLayout content = new FrameLayout(viewGroup.getContext());
            content.setBackgroundColor(Color.WHITE);
            content.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new NativeViewHolder(content, viewType);
        }

        @Override
        public void onBindViewHolder(final NativeViewHolder viewHolder, int position) {
            viewHolder.layout.removeAllViews();
            // 获取数据
            RecycleViewItem recycleViewItem = itemList.get(position);
            // 添加广告&内容视图
            if (recycleViewItem.getItemView().getParent() != null) {
                ((ViewGroup) recycleViewItem.getItemView().getParent()).removeView(recycleViewItem.getItemView());
            }
            viewHolder.layout.addView(recycleViewItem.getItemView());
            // 广告数据，注册监听器
            if (recycleViewItem instanceof NativeAdItem) {
                ((NativeAdItem) recycleViewItem).registerListeners();
            }
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return itemList.get(position).type;
        }

        class NativeViewHolder extends RecyclerView.ViewHolder {
            ViewGroup layout;
            int type;
            public NativeViewHolder(@NonNull ViewGroup itemView, int type) {
                super(itemView);
                this.type = type;
                this.layout = itemView;
            }
        }
    }

    class RecycleViewItem {
        protected final int type;
        protected View itemView;

        public RecycleViewItem(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public View getItemView() {
            return itemView;
        }

        public RecycleViewItem prepareItemView(Context context) {
            itemView = LayoutInflater.from(context).inflate(R.layout.item_content_horizontal, null, false);
            return this;
        }
    }

    class NativeAdItem extends RecycleViewItem {
        private final NativeResponse nativeResponse;
        ImageView closeBtn;
        XNativeView video;
        TextView privacy;
        TextView permission;
        public NativeAdItem(NativeResponse nativeResponse) {
            super(1);
            this.nativeResponse = nativeResponse;
        }

        /**
         * 准备广告的视图，其中信息流视频播放器支持对图片素材的展示，也可以根据需要自定义图片组件
         * @param context
         * @return
         */
        @Override
        public RecycleViewItem prepareItemView(Context context) {
            itemView = LayoutInflater.from(context).inflate(R.layout.feed_native_video_item, null, false);
            AQuery aq = new AQuery(itemView);

            ImageView icon = itemView.findViewById(R.id.native_icon_image);
            video = itemView.findViewById(R.id.native_main_image);
            TextView text = itemView.findViewById(R.id.native_text);
            BDMarketingTextView titleView = itemView.findViewById(R.id.native_title);
            TextView brandName = itemView.findViewById(R.id.native_brand_name);
            closeBtn = itemView.findViewById(R.id.close);
            ImageView adLogo = itemView.findViewById(R.id.native_adlogo);
            ImageView bdLogo = itemView.findViewById(R.id.native_baidulogo);
            // 下载类广告信息
            TextView version = itemView.findViewById(R.id.native_version);
            TextView publisher = itemView.findViewById(R.id.native_publisher);
            privacy = itemView.findViewById(R.id.native_privacy);
            permission = itemView.findViewById(R.id.native_permission);

            // 设置字体相关配置
            titleView.setTextFontSizeSp(13);
            titleView.setEllipsize(TextUtils.TruncateAt.END); // 不支持跑马灯式省略
            titleView.setTextMaxLines(2);
            titleView.setAdData(nativeResponse, nativeResponse.getTitle());

            text.setText(nativeResponse.getDesc());
            brandName.setText(nativeResponse.getBrandName());
            video.setNativeItem(nativeResponse);
            // 需要启用下载弹窗时可置为true
            video.setUseDownloadFrame(false);
            video.setVideoMute(false);
            aq.id(icon).image(nativeResponse.getIconUrl(), false, true);
            aq.id(adLogo).image(nativeResponse.getAdLogoUrl(), false, true);
            setUnionLogoClick(adLogo, nativeResponse);
            aq.id(bdLogo).image(nativeResponse.getBaiduLogoUrl(), false, true);
            setUnionLogoClick(bdLogo, nativeResponse);
            // 若为下载类广告，则渲染四个信息字段
            if (isDownloadAd(nativeResponse)) {
                version.setVisibility(View.VISIBLE);
                publisher.setVisibility(View.VISIBLE);
                privacy.setVisibility(View.VISIBLE);
                permission.setVisibility(View.VISIBLE);
                version.setText(String.format("版本 %s", nativeResponse.getAppVersion()));
                publisher.setText(nativeResponse.getPublisher());
            } else {
                version.setVisibility(View.GONE);
                publisher.setVisibility(View.GONE);
                privacy.setVisibility(View.GONE);
                permission.setVisibility(View.GONE);
            }
            // 调用render，对于设置自动播放的广告会自动播放
            video.render();
            return this;
        }

        public void registerListeners() {
            // 绑定关闭的点击事件
            bindCloseButton(this);
            // 绑定下载类广告的隐私协议和权限点击事件
            if (isDownloadAd(nativeResponse)) {
                privacy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nativeResponse.privacyClick();
                    }
                });
                permission.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nativeResponse.permissionClick();
                    }
                });
                nativeResponse.setAdPrivacyListener(new NativeResponse.AdDownloadWindowListener() {
                    @Override
                    public void adDownloadWindowShow() {
                        video.pause();
                    }

                    @Override
                    public void adDownloadWindowClose() {
                        video.resume();
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
            // 视频广告的监听
            video.setNativeVideoListener(new INativeVideoListener() {
                @Override
                public void onRenderingStart() {
                    Log.i(TAG, "onRenderingStart: " + nativeResponse.getTitle());
                }

                @Override
                public void onPause() {
                    Log.i(TAG, "onPause: " + nativeResponse.getTitle());
                }

                @Override
                public void onResume() {
                    Log.i(TAG, "onResume: " + nativeResponse.getTitle());
                }

                @Override
                public void onCompletion() {
                    Log.i(TAG, "onCompletion: " + nativeResponse.getTitle());
                }

                @Override
                public void onError() {
                    Log.i(TAG, "onError: " + nativeResponse.getTitle());
                }
            });
            // 注册点击事件
            List<View> clickViews = new ArrayList<>();
            List<View> creativeViews = new ArrayList<>();
            clickViews.add(itemView);
            nativeResponse.registerViewForInteraction(itemView, clickViews, creativeViews,
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
    }
}
