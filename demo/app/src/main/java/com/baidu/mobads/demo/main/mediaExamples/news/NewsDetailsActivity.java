package com.baidu.mobads.demo.main.mediaExamples.news;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter;
import com.baidu.mobads.demo.main.mediaExamples.news.bean.FeedItem;
import com.baidu.mobads.demo.main.mediaExamples.news.loader.AbstractFeedLoader;
import com.baidu.mobads.demo.main.mediaExamples.news.loader.FeedAdLoader;
import com.baidu.mobads.demo.main.mediaExamples.news.loader.FeedContentLoader;
import com.baidu.mobads.demo.main.mediaExamples.news.utils.IdIterator;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.CONTENT_BIG_PIC;

/**
 * 资讯类接入示例：资讯详情页
 */
public class NewsDetailsActivity extends AppCompatActivity {
    // 广告位ID
    private static final String FEED_VIDEO_AD_PLACE_ID = "2362913";        // 信息流视频
    private static final String BIG_PIC_AD_PLACE_ID = "2058628";        // 大图+ICON+描述
    private static final String SANTU_AD_PLACE_ID = "5887568";          // 三图
    // 应用ID
    private static final String YOUR_APP_ID = "c0da1ec4"; // 双引号中填写自己的APPSID

    private Toolbar topBar;
    private AppBarLayout newsBar;
    private RelativeLayout authorBar;
    private WebView contentWeb;
    private FrameLayout adContainer;
    private LinearLayout recommendContainer;
    private View adView;

    private FeedAdLoader adLoader;
    private FeedContentLoader contentLoader;
    private NewsListAdapter contentAdapter;
    private AQuery aq;
    private List<FeedItem> adItemList = new ArrayList<FeedItem>();
    private List<FeedItem> contentItemList = new ArrayList<FeedItem>();
    private boolean isShowingAuthorBar = false;
    private NestedScrollView nested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_details);
        initView();
        initListener();
        loadNewsData();
        adLoader = FeedAdLoader.newInstance(this, BIG_PIC_AD_PLACE_ID, 3, new IdIterator(), new AbstractFeedLoader.LoadListener() {
            @Override
            public void onLoadComplete() {
                // 仅当adItemList内容为空时，加载一次
                if (null != adItemList && adItemList.size() == 0) {
                    adItemList.addAll(adLoader.getDataLoaded(1));
                    // 载入大图广告
                    if (adItemList.size() > 0) {
                        FeedItem adItem = adItemList.get(0);
                        loadAdData(adItem);
                    }
                }
            }

            @Override
            public void onLoadException(String msg, int errorCode) {

            }
        });
        contentLoader = FeedContentLoader.newInstance(this, NewsListAdapter.CONTENT_TRI_PIC, 3,
                new IdIterator(), new AbstractFeedLoader.LoadListener() {
                    @Override
                    public void onLoadComplete() {
                        // 仅当contentItemList内容为空时，加载一次
                        if (null != contentItemList && contentItemList.size() == 0) {
                            contentItemList.addAll(contentLoader.getDataLoaded(3));
                            // 载入大图广告
                            if (contentItemList.size() > 0) {
                                for (int i = 0; i < contentItemList.size(); i++) {
                                    loadRecommendData(i);
                                }
                            }
                        }
                    }

                    @Override
                    public void onLoadException(String msg, int errorCode) {

                    }
                });
        contentAdapter = new NewsListAdapter(getApplicationContext(), contentItemList);
        contentAdapter.setOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                startActivity(new Intent(NewsDetailsActivity.this, NewsDetailsActivity.class));
            }
        });
    }

    private void initView() {
        aq = new AQuery(this);
        topBar = findViewById(R.id.news_details_top_bar);
        newsBar = findViewById(R.id.news_details_news_bar);
        authorBar = findViewById(R.id.news_details_author_bar);
        contentWeb = findViewById(R.id.news_details_content_web);
        setSupportActionBar(topBar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(false);
        }
        // 初始化广告视图
        adContainer = findViewById(R.id.news_details_ad_container);
        adView = LayoutInflater.from(this).inflate(R.layout.feed_native_listview_ad_row, adContainer);
        // 初始化内容容器
        recommendContainer = findViewById(R.id.news_details_content_container);
        nested = findViewById(R.id.nested);
    }

    private void initListener() {


        adView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adItemList.size() > 0) {
                    NativeResponse nrAd = adItemList.get(0).getNrAd();
                    if (null != nrAd) {
//                        nrAd.handleClick(adView);
                    }
                }
            }
        });
        newsBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
                float height = appBarLayout.getMeasuredHeight() - topBar.getMeasuredHeight();
                float offset = Math.abs(i);
                if (offset <= height) {
                    // 滑动中，调整topBar背景透明度
                    float alpha = Math.min(offset, height) / height;
                    ColorDrawable background = new ColorDrawable(Color.WHITE);
                    background.setAlpha((int) (alpha * 255));
                    topBar.setBackground(background);
                    // 消失authorBar
                    if (isShowingAuthorBar) {
                        isShowingAuthorBar = false;
                        AnimationSet animationSet = new AnimationSet(true);
                        animationSet.addAnimation(new TranslateAnimation(0, 0, 0, 5));
                        animationSet.addAnimation(new AlphaAnimation(1, 0));
                        animationSet.setDuration(150);
                        animationSet.setInterpolator(new LinearInterpolator());
                        authorBar.startAnimation(animationSet);
                        authorBar.setVisibility(View.GONE);
                    }
                } else {
                    // 滑动结束，浮现authorBar
                    if (!isShowingAuthorBar) {
                        topBar.setBackground(new ColorDrawable(Color.WHITE));
                        isShowingAuthorBar = true;
                        AnimationSet animationSet = new AnimationSet(true);
                        animationSet.addAnimation(new TranslateAnimation(0, 0, 5, 0));
                        animationSet.addAnimation(new AlphaAnimation(0, 1));
                        animationSet.setDuration(150);
                        animationSet.setInterpolator(new LinearInterpolator());
                        authorBar.startAnimation(animationSet);
                        authorBar.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void loadNewsData() {
        String title = "2732亿！孙正义终于卖了，史上最大芯片收购案，新芯片霸主诞生！";
        String authorName = "猪猪ZZ1";
        String img = "https://pic.rmb.bdstatic.com/e48deea890b4b1ff59c93f594d4a29c7.jpeg";
        aq.id(R.id.news_details_author_name).text(authorName);
        aq.id(R.id.news_details_author_bar_name).text(authorName);
        ImageView imageAuthor = findViewById(R.id.news_details_author_avatar);
        Glide.with(this)
                .load(img)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(imageAuthor);
        ImageView imageAuthorBar = findViewById(R.id.news_details_author_bar_avatar);
        Glide.with(this)
                .load(img)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(imageAuthorBar);
        aq.id(R.id.news_details_title).text(title);
        contentWeb.loadUrl("file:///android_asset/mock_news_details.html");
    }

    private void loadAdData(FeedItem adItem) {
        final NativeResponse nrAd = adItem.getNrAd();
        if (nrAd != null) {
            aq.id(R.id.native_title).text(nrAd.getTitle());
            aq.id(R.id.native_text).text(nrAd.getDesc());
            aq.id(R.id.native_brand_name).text(nrAd.getBrandName());
            aq.id(R.id.native_icon_image).image(nrAd.getIconUrl(), false, true);
            aq.id(R.id.native_adlogo).image(nrAd.getAdLogoUrl(), false, true);
            aq.id(R.id.native_baidulogo).image(nrAd.getBaiduLogoUrl(), false, true);
            aq.id(R.id.native_main_image).image(nrAd.getImageUrl(), false, true);
            View.OnClickListener unionLogoClicked = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nrAd.unionLogoClick();
                }
            };
            aq.id(R.id.native_adlogo).clicked(unionLogoClicked);
            aq.id(R.id.native_baidulogo).clicked(unionLogoClicked);
            // 展现广告后请手动发送展现
            List<View> clickViews = new ArrayList<>();
            List<View> creativeViews = new ArrayList<>();
            clickViews.add(adView);
            nrAd.registerViewForInteraction(adView,clickViews, creativeViews,
                    new NativeResponse.AdInteractionListener() {
                @Override
                public void onAdClick() {

                }

                @Override
                public void onADExposed() {

                }

                @Override
                public void onADExposureFailed(int reason) {
                    Log.i("NewsDetailsActivity" , "onADExposureFailed: " + reason);
                }

                @Override
                public void onADStatusChanged() {

                }

                @Override
                public void onAdUnionClick() {

                }
            });
        }
    }

    private void loadRecommendData(int position) {
        Holders.FeedItemViewHolder feedItemViewHolder = contentAdapter.onCreateViewHolder(recommendContainer, CONTENT_BIG_PIC);
        recommendContainer.addView(feedItemViewHolder.itemView);
        contentAdapter.onBindViewHolder(feedItemViewHolder, position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 初始化弹出菜单
        getMenuInflater().inflate(R.menu.news_details_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.news_menu_copy_link:
            case R.id.news_menu_favorite:
            case R.id.news_menu_report:
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adLoader.release();
        contentLoader.release();
    }
}