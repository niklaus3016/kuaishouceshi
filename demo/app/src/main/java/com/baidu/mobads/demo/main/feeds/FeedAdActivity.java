package com.baidu.mobads.demo.main.feeds;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.CommonUtils;
import com.baidu.mobads.demo.main.tools.CustomProgressButton;
import com.baidu.mobads.demo.main.tools.RefreshAndLoadMoreView;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BDMarketingTextView;
import com.baidu.mobads.sdk.api.BDRefinedActButton;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.ShakeViewContainer;
import com.baidu.mobads.sdk.api.XAdNativeResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.widget.RelativeLayout.BELOW;

/*
1. 信息流集成参考类：FeedAdActivity
2. 推荐使用BaiduNativeManager集成
    1. 构造请求参数：RequestParameters，非必须。可根据您的需求来创建Parameter
    2. 执行loadFeedAd
    3. 监听FeedAdListener，请求成功会回传List<NativeResponse> ，您可根据NativeResponse中包含的物料内容来创建对应的view
3. 参数回传请联系商务同学，进行相关合作。
4. 下载类广告推荐根据getDownloadStatus获取的下载状态来进行样式渲染。
5. 注意：信息流广告需要您手动发送曝光和点击事件。漏发则无法计费。
    1. 广告数据渲染完毕，View展示的时候使用NativeResponse调用registerViewForInteraction来发送曝光
    2. 绑定view点击事件，发生点击使用NativeResponse调用handleClick来发送点击
6. 广告有时间有效期限制，从拉取到使用超过30分钟，将被视为无效广告。可以利用isReady方法验证。
7. 广告位的返回物料组合配置/返回模板，当APP广告上线后，切勿随意变更，确保前端不会发生崩溃。且前端开发需要对物料进行判空校验，以确定物料是否满足渲染条件，若有不符需要抛弃广告，防止crash。
*/
public class FeedAdActivity extends Activity {
    private static final String TAG = FeedAdActivity.class.getSimpleName();
    private static final String BIG_PIC_AD_PLACE_ID = "2058628";        // 大图+ICON+描述
    private static final String SANTU_AD_PLACE_ID = "5887568";          // 三图

    private static final int NATIVE_BIG_PIC = 1;
    private static final int NATIVE_SANTU = 2;

    private static final int ACTION_ID = 111111111;

    private final List<NativeResponse> nrAdList = new ArrayList<NativeResponse>(); // 媒体自渲染广告使用
    private final Map<String, String> mParameterExtras = new HashMap<>();
    private NativeAdAdapter mAdapter;
    private RefreshAndLoadMoreView mRefreshLoadView;
    private int mAdPatternType = NATIVE_BIG_PIC;
    private int mFunctionState = 0;
    private BaiduNativeManager mBaiduNativeManager;
    /** 下载按钮进度条 */
    private CustomProgressButton customProgressButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_main);
        initView();

        // 默认请求大图广+ICON样式
        /**
         * Step 1. 创建BaiduNative对象，参数分别为： 上下文context，广告位ID
         * 注意：如果你配置了{@link com.baidu.mobads.sdk.api.BDAdConfig.Builder#useActivityDialog(Boolean)}为 false
         *      则这里的上下文context不要使用ApplicationContext，会使弹框无法弹出
         * 注意：请将adPlaceId替换为自己的广告位ID
         * 注意：信息流广告对象，与广告位id一一对应，同一个对象可以多次发起请求
         */
        mBaiduNativeManager = new BaiduNativeManager(this, BIG_PIC_AD_PLACE_ID);
        mRefreshLoadView.setRefreshing(true);
        loadFeedAd();

    }

    private void initView() {

        mRefreshLoadView = findViewById(R.id.refresh_container);
        mRefreshLoadView.setLoadAndRefreshListener(new RefreshAndLoadMoreView.LoadAndRefreshListener() {
            @Override
            public void onRefresh() {
                loadFeedAd();
            }

            public void onLoadMore() {
                loadFeedAd();
            }
        });

        ListView list = mRefreshLoadView.getListView();
        mAdapter = new NativeAdAdapter(this);
        list.setAdapter(mAdapter);

    }

    private void loadFeedAd() {
        // 构建请求参数
        final RequestParameters.Builder parametersBuilder = new RequestParameters.Builder()
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
                .addCustExt(ArticleInfo.PAGE_TITLE, "真测试书名");
        for (String key : mParameterExtras.keySet()) {
            parametersBuilder.addExtra(key, mParameterExtras.get(key));
        }

        final BaiduNativeManager.FeedAdListener feedAdListener = new BaiduNativeManager.FeedAdListener() {
            @Override
            public void onNativeLoad(List<NativeResponse> nativeResponses) {
                Log.i(TAG, "onNativeLoad:" +
                        (nativeResponses != null ? nativeResponses.size() : null));
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                if (nativeResponses != null && nativeResponses.size() > 0) {
                    // 刷新时重制数据
                    if (mRefreshLoadView.isRefreshing()) {
                        nrAdList.clear();
                    }
                    nrAdList.addAll(nativeResponses);
                    mAdapter.notifyDataSetChanged();
                }
                mRefreshLoadView.onLoadFinish();
            }

            @Override
            public void onNoAd(int code, String msg, NativeResponse nativeResponse) {
                Log.i(TAG, "onNoAd reason:" + msg);
                mRefreshLoadView.onLoadFinish();
                // 竞败回传
                clientBidding(nativeResponse);
            }

            @Override
            public void onNativeFail(int errorCode, String message, NativeResponse nativeResponse) {
                Log.w(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
                mRefreshLoadView.onLoadFinish();
                // 竞败回传
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
        };
        // 【可选】【Bidding】设置广告的底价，单位：分
        mBaiduNativeManager.setBidFloor(100);
        // 发起信息流广告请求
        mBaiduNativeManager.loadFeedAd(parametersBuilder.build(), feedAdListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feed_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.feed_patterns) {
            return true;
        }
        switch (id) {
            case R.id.big_pic:  // 大图+ICON+描述
                mAdPatternType = NATIVE_BIG_PIC;
                mRefreshLoadView.setRefreshing(true);
                mBaiduNativeManager = new BaiduNativeManager(this, BIG_PIC_AD_PLACE_ID);
                loadFeedAd();
                break;
            case R.id.santu:    // 信息流三图
                mAdPatternType = NATIVE_SANTU;
                mRefreshLoadView.setRefreshing(true);
                mBaiduNativeManager = new BaiduNativeManager(this, SANTU_AD_PLACE_ID);
                loadFeedAd();
                break;
            case R.id.big_pic_reward:  // 大图+ICON+描述+激励
                mAdPatternType = NATIVE_BIG_PIC;
                mFunctionState |= 0b1;
                mRefreshLoadView.setRefreshing(true);
                mBaiduNativeManager = new BaiduNativeManager(this, BIG_PIC_AD_PLACE_ID);
                AlertDialog.Builder builder = new AlertDialog.Builder(FeedAdActivity.this);
                builder.setTitle("选择电商激励类型");
                String[] options = {"仅绑定奖励", "仅下单奖励", "绑定+下单奖励"};
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mParameterExtras.clear();
                        switch (which) {
                            case 0: // 仅绑定
                                mParameterExtras.put("ecafd_bind_bonus", String.valueOf(200));
                                mParameterExtras.put("ecafd_uid", "AABBCCDD");
                                break;
                            case 1: // 仅下单
                                mParameterExtras.put("ecafd_order_bonus", String.valueOf(800));
                                mParameterExtras.put("ecafd_uid", "AABBCCDD");
                                break;
                            case 2: // 绑定+下单
                                mParameterExtras.put("ecafd_bind_bonus", String.valueOf(200));
                                mParameterExtras.put("ecafd_order_bonus", String.valueOf(800));
                                mParameterExtras.put("ecafd_uid", "AABBCCDD");
                                break;
                        }
                        loadFeedAd();
                    }
                });
                builder.setNegativeButton("取消", null);
                builder.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class NativeAdAdapter extends BaseAdapter {
        LayoutInflater inflater;
        Map<NativeResponse, List<View>> adComponents = new HashMap<>();

        public NativeAdAdapter(Context context) {
            super();
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void releaseComponents() {
            for (NativeResponse nrAd : adComponents.keySet()) {
                List<View> components = adComponents.get(nrAd);
                if (components != null && nrAd != null) {
                    for (View view : components) {
                        nrAd.stopNativeView(view);
                    }
                }
            }
        }

        @Override
        public int getCount() {
            return nrAdList.size();
        }

        @Override
        public NativeResponse getItem(int position) {
            return nrAdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final XAdNativeResponse nrAd = (XAdNativeResponse) getItem(position);
            List<View> components = new ArrayList<>();
            adComponents.put(nrAd, components);
            View mFlipPageView = null;
            // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
            clientBidding(nrAd);
            switch (mAdPatternType) {
                case NATIVE_BIG_PIC:
                    // 大图广告样式
                    if (convertView == null || ((Integer) convertView.getTag()) != NATIVE_BIG_PIC) {
                        convertView = inflater.inflate(R.layout.feed_native_listview_ad_row, null);
                        convertView.setTag(NATIVE_BIG_PIC);
                    }
                    AQuery aq = new AQuery(convertView);
                    aq.id(R.id.native_icon_image).image(nrAd.getIconUrl(), false, true);
                    aq.id(R.id.native_main_image).image(nrAd.getImageUrl(), false, true);
                    aq.id(R.id.native_text).text(nrAd.getDesc());
                    // 营销挂件 宽高务必相等
                    aq.id(R.id.native_marketing_pendant).image(nrAd.getMarketingPendant(), false, true);
                    // 营销TextView
                    BDMarketingTextView titleView = convertView.findViewById(R.id.native_title);
                    // 设置字体相关配置
                    titleView.setTextFontSizeSp(16);
                    titleView.setTextMaxLines(2);
                    titleView.setEllipsize(TextUtils.TruncateAt.END); // 不支持跑马灯式省略
                    String title = nrAd.getTitle();
                    if (nrAd.getAdMaterialType().equals(NativeResponse.MaterialType.LIVE.getValue())) {
                        title = nrAd.getAuthorName();
                    }
                    titleView.setAdData(nrAd, title);
                    // 如果有营销挂件需要的字段才展示营销挂件
                    if (TextUtils.isEmpty(nrAd.getMarketingICONUrl()) || TextUtils.isEmpty(nrAd.getMarketingDesc())) {
                        titleView.setLabelVisibility(View.GONE);
                    } else {
                        titleView.setLabelVisibility(View.VISIBLE);
                    }
                    RelativeLayout layout = convertView.findViewById(R.id.native_outer_view);
                    // 先删除之前添加的act按钮
                    View view =convertView.findViewById(ACTION_ID);
                    layout.removeView(view);
                    // 若为下载类广告，则渲染四个信息字段
                    if (isDownloadAd(nrAd)) {
                        aq.id(R.id.app_name).text(nrAd.getBrandName());
                        showDownloadInfo(convertView, aq, nrAd);
                        convertView.findViewById(R.id.native_brand_name).setVisibility(View.GONE);
                    } else {
                        aq.id(R.id.native_brand_name).text(nrAd.getBrandName());
                        hideDownloadInfo(convertView);
                        BDRefinedActButton actButton = new BDRefinedActButton(FeedAdActivity.this);
                        actButton.setId(ACTION_ID);
                        RelativeLayout.LayoutParams actButtonParams =
                                new RelativeLayout.LayoutParams(dip2px(FeedAdActivity.this, 64),
                                        dip2px(FeedAdActivity.this, 24));
                        actButtonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        actButtonParams.addRule(BELOW, R.id.app_download_container);
                        layout.addView(actButton , actButtonParams);
                        actButton.setAdData(nrAd);
                        convertView.findViewById(R.id.native_brand_name).setVisibility(View.VISIBLE);
                    }
                    aq.id(R.id.native_adlogo).image(nrAd.getAdLogoUrl(), false, true);
                    setUnionLogoClick(convertView, R.id.native_adlogo, nrAd);
                    aq.id(R.id.native_baidulogo).image(nrAd.getBaiduLogoUrl(), false, true);
                    setUnionLogoClick(convertView, R.id.native_baidulogo, nrAd);
                    // 查找促转化组件的容器
                    View creativeViewContainer = convertView.findViewById(R.id.shake_view_container);
                    if (creativeViewContainer instanceof RelativeLayout) {
                        // 渲染促转化的创意组件，有助于提升广告的点击率
                        View creativeView = bindCreativeView((RelativeLayout) creativeViewContainer, nrAd);
                        components.add(creativeView);
                    }
                    // 弹幕组件
                    final RelativeLayout bulletContainer = convertView.findViewById(R.id.bullet_view_container);
                    if (bulletContainer != null) {
                        bulletContainer.removeAllViews();
                        bulletContainer.setVisibility(View.VISIBLE);
                        // 渲染弹幕组件应尽量在广告素材的上层，弹幕播放完成会自动移除自身
                        // @param width 弹幕容器宽度，请使用MATCH_PARENT或大等于120dp的数值
                        // @param height 弹幕容器高度，请使用WRAP_CONTENT或0，让容器自适应弹幕的高度
                        View bulletView = nrAd.renderBulletView(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        components.add(bulletView);
                        // 若广告不支持弹幕组件，则返回空
                        if (bulletView != null) {
                            bulletContainer.addView(bulletView);
                        }
                    }
                    // 渲染优惠券悬浮组件
                    final RelativeLayout couponFloatContainer = convertView.findViewById(R.id.coupon_float_view_container);
                    if (couponFloatContainer != null) {
                        couponFloatContainer.removeAllViews();
                        couponFloatContainer.setVisibility(View.VISIBLE);
                        // 若广告不支持优惠券悬浮组件，则返回空
                        View couponFloatView = nrAd.renderCouponFloatView(new NativeResponse.AdShakeViewListener() {
                            @Override
                            public void onDismiss() {
                                Log.i(TAG, "CouponFloatView onDismiss()");
                                couponFloatContainer.setVisibility(View.INVISIBLE);
                            }
                        });
                        components.add(couponFloatView);
                        // 渲染尺寸请使用wrap_content以保证布局正常；如需调整大小，可使用setScaleX/Y进行缩放
                        if (couponFloatView != null) {
                            couponFloatContainer.addView(couponFloatView);
                        }
                    }
                    // 渲染优惠券翻页组件
                    RelativeLayout flipPageContainer = convertView.findViewById(R.id.flip_page_view_container);
                    if (flipPageContainer != null) {
                        // 添加之前进行删除
                        flipPageContainer.removeAllViews();
                        flipPageContainer.setVisibility(View.VISIBLE);
                        // 若广告不支持优惠券翻页组件，则返回空
                        mFlipPageView = nrAd.renderFlipPageView();
                        components.add(mFlipPageView);
                        // 放在物料的右下角即可
                        if (mFlipPageView != null) {
                            // 翻页组件的大小可以自行进行调整，但请保证宽高比为1:1，才能正确渲染
                            RelativeLayout.LayoutParams params =
                                    new RelativeLayout.LayoutParams(CommonUtils.dp2px(FeedAdActivity.this, 150),
                                            CommonUtils.dp2px(FeedAdActivity.this, 150));
                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                            flipPageContainer.addView(mFlipPageView, params);
                        }
                    }
                    // 电商卡片组件
                    RelativeLayout eCommerceCardContainer = convertView.findViewById(R.id.e_commerce_card_container);
                    if (eCommerceCardContainer != null) {
                        eCommerceCardContainer.removeAllViews();
                        eCommerceCardContainer.setVisibility(View.VISIBLE);
                        // 若广告不支持电商卡片组件，则返回空
                        // 传入目标尺寸单位dp，推荐组件的宽高比 1:1.04
                        View eCommerceCardView = nrAd.renderECommerceView(96, 100, new NativeResponse.AdShakeViewListener() {
                            @Override
                            public void onDismiss() {
                                Log.i(TAG, "eCommerceCardView onDismiss()");
                            }
                        });
                        if (eCommerceCardView != null) {
                            eCommerceCardContainer.addView(eCommerceCardView);
                        }
                    }
                    // 小店-优惠磁贴组件
                    RelativeLayout shopCouponMagneticStickerContainer = convertView.findViewById(R.id.shop_coupon_magnetic_sticker_container);
                    if (shopCouponMagneticStickerContainer != null) {
                        shopCouponMagneticStickerContainer.removeAllViews();
                        shopCouponMagneticStickerContainer.setVisibility(View.VISIBLE);
                        // 若广告不支持小店-优惠磁贴组件，则返回空
                        // 传入目标尺寸单位dp，推荐组件的宽高比 4:1
                        View shopCouponMagneticStickerView = nrAd.renderShopCouponMagneticStickerView(244, 60, new NativeResponse.AdShakeViewListener() {
                            @Override
                            public void onDismiss() {
                                Log.i(TAG, "shopCouponMagneticStickerView onDismiss()");
                            }
                        });
                        if (shopCouponMagneticStickerView != null) {
                            shopCouponMagneticStickerContainer.addView(shopCouponMagneticStickerView);
                        }
                    }
                    // 绑定关闭的点击事件
                    bindCloseButton(convertView.findViewById(R.id.close), nrAd);
                    // nrAd.isDownloadApp() ? "下载" : "查看";
                    Log.i(TAG, "AD button [" + getBtnText(nrAd) + "]: " + nrAd.getTitle());
                    break;
                case NATIVE_SANTU:
                    // 三图广告样式
                    if (convertView == null || ((Integer) convertView.getTag()) != NATIVE_SANTU) {
                        convertView = inflater.inflate(R.layout.feed_native_santu_item, null);
                        convertView.setTag(NATIVE_SANTU);
                    }
                    AQuery aq1 = new AQuery(convertView);
                    aq1.id(R.id.iv_title).text(nrAd.getTitle());
                    aq1.id(R.id.iv_icon).image(nrAd.getIconUrl());
                    List<String> picUrls = nrAd.getMultiPicUrls();
                    if (picUrls != null && picUrls.size() > 2) {
                        aq1.id(R.id.iv_main1).image(picUrls.get(0));
                        aq1.id(R.id.iv_main2).image(picUrls.get(1));
                        aq1.id(R.id.iv_main3).image(picUrls.get(2));
                    }
                    aq1.id(R.id.iv_desc).text(nrAd.getDesc());
                    aq1.id(R.id.iv_baidulogo).image(nrAd.getBaiduLogoUrl());
                    setUnionLogoClick(convertView, R.id.iv_baidulogo, nrAd);
                    aq1.id(R.id.iv_adlogo).image(nrAd.getAdLogoUrl());
                    setUnionLogoClick(convertView, R.id.iv_adlogo, nrAd);
                    RelativeLayout moreLayout = convertView.findViewById(R.id.rlTemplate1);
                    // 先删除之前添加的act按钮
                    View moreView =convertView.findViewById(ACTION_ID);
                    moreLayout.removeView(moreView);
                    if (isDownloadAd(nrAd)) {
                        aq1.id(R.id.app_name).text(nrAd.getBrandName());
                        showDownloadInfo(convertView, aq1, nrAd);
                        convertView.findViewById(R.id.iv_brandname).setVisibility(View.GONE);

                    } else {
                        aq1.id(R.id.iv_brandname).text(nrAd.getBrandName());
                        hideDownloadInfo(convertView);
                        BDRefinedActButton actButton = new BDRefinedActButton(FeedAdActivity.this);
                        actButton.setId(ACTION_ID);
                        RelativeLayout.LayoutParams santuButtonParams =
                                new RelativeLayout.LayoutParams(dip2px(FeedAdActivity.this, 64),
                                        dip2px(FeedAdActivity.this, 24));
                        santuButtonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        santuButtonParams.addRule(BELOW, R.id.app_download_container);
                        moreLayout.addView(actButton , santuButtonParams);
                        actButton.setAdData(nrAd);
                        convertView.findViewById(R.id.iv_brandname).setVisibility(View.VISIBLE);
                    }
                    // 绑定关闭的点击事件
                    bindCloseButton(convertView.findViewById(R.id.close), nrAd);
                    break;
                default:
                    // nop
            }

            // clickViews: 可点击的View，默认展示下载整改弹框
            List<View> clickViews = new ArrayList<>();
            clickViews.add(convertView);
            // creativeViews: 带有下载引导文案的View，默认不会触发下载整改弹框
            List<View> creativeViews = new ArrayList<>();
            creativeViews.add(customProgressButton);

            // 其他额外业务view，请根据业务合作需要进行添加和配置
            Map<String, List<View>> extraViews = new HashMap<>();
            if ((mFunctionState & 0b1) == 0b1) {
                RelativeLayout aboveContainer = convertView.findViewById(R.id.container_above_image);
                RelativeLayout belowContainer = convertView.findViewById(R.id.container_below_image);
                Object ecafdBind = nrAd.getAdDataForKey("ecafd_bind");
                boolean supportBind = (ecafdBind instanceof Integer && ((int) ecafdBind) == 1);
                Object ecafdOrder = nrAd.getAdDataForKey("ecafd_order");
                boolean supportOrder = (ecafdOrder instanceof Integer && ((int) ecafdOrder) == 1);
                int pd = dip2px(FeedAdActivity.this, 5);
                TextView bindRewardBtn = new TextView(FeedAdActivity.this);
                bindRewardBtn.setBackgroundResource(R.drawable.bt_bg_novel);
                bindRewardBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                bindRewardBtn.setPadding(pd, pd, pd, pd);
                bindRewardBtn.setTextColor(Color.parseColor("#FFFFFF"));
                bindRewardBtn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                TextView orderRewardBtn = new TextView(FeedAdActivity.this);
                orderRewardBtn.setBackgroundResource(R.drawable.bt_bg_novel);
                orderRewardBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                orderRewardBtn.setPadding(pd, pd, pd, pd);
                orderRewardBtn.setTextColor(Color.parseColor("#FFFFFF"));
                orderRewardBtn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                RelativeLayout.LayoutParams btnParams =
                        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                String bindBonusStr = mParameterExtras.get("ecafd_bind_bonus");
                int bindBonus = Integer.parseInt(bindBonusStr != null ? bindBonusStr : "0");
                String orderBonusStr = mParameterExtras.get("ecafd_order_bonus");
                int orderBonus = Integer.parseInt(orderBonusStr != null ? orderBonusStr : "0");
                if (supportBind && supportOrder) {
                    btnParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    bindRewardBtn.setText("首次授权百度账号返" + bindBonus + "金币");
                    aboveContainer.addView(bindRewardBtn, btnParams);
                    orderRewardBtn.setText("授权百度账号并下单返" + (bindBonus + orderBonus) + "金币");
                    belowContainer.addView(orderRewardBtn, btnParams);
                    extraViews.put("ecafd_bind_views", Arrays.asList(bindRewardBtn, orderRewardBtn));
                    extraViews.put("ecafd_order_views", Collections.singletonList(orderRewardBtn));
                } else if (supportBind) {
                    bindRewardBtn.setText("首次授权百度账号返" + bindBonus + "金币");
                    belowContainer.addView(bindRewardBtn, btnParams);
                    extraViews.put("ecafd_bind_views", Collections.singletonList(bindRewardBtn));
                } else if (supportOrder) {
                    orderRewardBtn.setText("点击广告后下单返" + orderBonus + "金币");
                    aboveContainer.addView(orderRewardBtn, btnParams);
                    extraViews.put("ecafd_order_views", Collections.singletonList(orderRewardBtn));
                }
                if (supportBind) {
                    nrAd.setAdEventListener(new NativeResponse.AdEventListener() {
                        @Override
                        public void onAdEvent(int code, Map<String, Object> data) {
                            if (code == NativeResponse.EVENT_CODE_ECAFD_BIND_SUCCESS) {
                                if (data != null) {
                                    Log.i(TAG, "onAdEvent: 电商激励绑定成功，uid=" + data.get("ecafd_uid"));
                                }
                            }
                        }
                    });
                }
            }

            /**
             * registerViewForInteraction()与BaiduNativeManager配套使用，发送展现&注册广告的点击事件
             * 警告：调用该函数来发送展现，勿漏！
             */
            final View finalMFlipPageView = mFlipPageView;
            nrAd.registerViewForInteraction(convertView, clickViews, creativeViews, extraViews,
                    new NativeResponse.AdInteractionListener() {
                @Override
                public void onAdClick() {
                    Log.i(TAG, "onAdClick:" + nrAd.getTitle());
                    // 下载类广告点击提示，可在此处提示用户如何取消下载，可长按取消下载或下拉通知栏取消下载
                    if (nrAd.isNeedDownloadApp()) {
                        Toast.makeText(FeedAdActivity.this, "开始下载，可长按下载按钮取消下载",
                                Toast.LENGTH_LONG).show();
                    }
                    // 如果有翻页组件，可以选择在广告触发后将其移除
                    if (finalMFlipPageView != null && finalMFlipPageView.getVisibility() == View.VISIBLE) {
                        finalMFlipPageView.setVisibility(View.GONE);
                    }
                    // 电商订单激励
                    Object ecafdOrder = nrAd.getAdDataForKey("ecafd_order");
                    boolean supportOrder = (ecafdOrder instanceof Integer && ((int) ecafdOrder) == 1);
                    if (supportOrder) {
                        Log.i(TAG, "电商订单激励广告触发点击，request_id=" + nrAd.getAdDataForKey("request_id"));
                    }
                }

                @Override
                public void onADExposed() {
                    Log.i(TAG, "onADExposed:" + nrAd.getTitle() + ", actionType = " + nrAd.getAdActionType());
                }

                @Override
                public void onADExposureFailed(int reason) {
                    Log.i(TAG, "onADExposureFailed: " + reason);
                }

                @Override
                public void onADStatusChanged() {
                    Log.i(TAG, "onADStatusChanged:" + getBtnText(nrAd));
                    if (nrAd != null) {
                        customProgressButton.setProgress();
                    }
                }

                @Override
                public void onAdUnionClick() {
                    Log.i(TAG, "onADUnionClick");
                }
            });

//            nrAd.setAdPrivacyListener(new NativeResponse.AdPrivacyListener() {
//                @Override
//                public void onADPermissionShow() {
//                    Log.i(TAG, "onADPermissionShow");
//                }
//
//                @Override
//                public void onADPermissionClose() {
//                    Log.i(TAG, "onADPermissionClose");
//                }
//                @Override
//                public void onADFunctionClick() {
//                    Log.i(TAG, "onADFunctionClick");
//                }
//                @Override
//                public void onADPrivacyClick() {
//                    Log.i(TAG, "onADPrivacyClick");
//                }
//            });
            nrAd.setAdPrivacyListener(new NativeResponse.AdDownloadWindowListener() {
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
                    Log.e(TAG, "AdDownloadWindowShow");
                }

                @Override
                public void adDownloadWindowClose() {
                    Log.e(TAG, "adDownloadWindowClose");
                }
            });
            return convertView;
        }

        private View bindCreativeView(final RelativeLayout container, NativeResponse nrAd) {
            container.removeAllViews();
            View nativeView = renderCustomShakeView(nrAd, 560, 360);
            if (nativeView == null) {
                // 添加摇一摇组件，摇一摇组件的宽高需大于80dp
                nativeView = nrAd.renderShakeView(80, 80,
                        new NativeResponse.AdShakeViewListener() {
                            @Override
                            public void onDismiss() {
                                Log.i(TAG, "ShakeView onDismiss()");
                                container.setVisibility(View.INVISIBLE);
                            }
                        });
            }
            // 若广告不支持摇一摇或组件宽高小于80dp，则shakeView为空
            if (nativeView == null) {
                // 渲染滑一滑组件，滑一滑组件中有文字，为保证文字正常展示，宽度不应该设置太小
                nativeView = nrAd.renderSlideView(120, -2, 3, new
                        NativeResponse.AdShakeViewListener() {
                            @Override
                            public void onDismiss() {
                                Log.i(TAG, "SlideView onDismiss()");
                                container.setVisibility(View.INVISIBLE);
                            }
                        });
            }
            if (nativeView != null) {
                // 滑一滑组件会适配容器的大小，所以容器最好不要设置WRAP_CONTENT
                RelativeLayout.LayoutParams lp = new RelativeLayout
                        .LayoutParams(560, 360);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                container.addView(nativeView, lp);
                container.setVisibility(View.VISIBLE);
            } else {
                container.setVisibility(View.INVISIBLE);
            }
            return nativeView;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.releaseComponents();
        }
    }

    private void bindCloseButton(View closeView, final NativeResponse nrAd) {
        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onAdClose:" + nrAd.getTitle());
                nrAdList.remove(nrAd);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private View renderCustomShakeView(NativeResponse nrAd, int width, int height) {
        final ShakeViewContainer shakeViewContainer = nrAd.renderShakeViewContainer();
        if (shakeViewContainer != null) {
            final RelativeLayout shakeContainer = shakeViewContainer.getContainer();
            if (shakeContainer != null) {
                shakeContainer.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
                final TextView textView = new TextView(FeedAdActivity.this);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                shakeContainer.addView(textView, lp);
                shakeContainer.setBackgroundColor(Color.parseColor("#aaaaaa00"));
                shakeContainer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

                    @Override
                    public void onViewAttachedToWindow(@NonNull View v) {
                        shakeViewContainer.resume();
                        textView.setText("摇一摇:启动");
                        Log.i(TAG, "Blank SlideView show()");
                    }

                    @Override
                    public void onViewDetachedFromWindow(@NonNull View v) {
                        shakeViewContainer.destroy();
                        textView.setText("摇一摇:停止");
                        Log.i(TAG, "Blank SlideView dismiss()");
                    }
                });
                shakeContainer.getViewTreeObserver().addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {

                    @Override
                    public void onWindowFocusChanged(boolean hasFocus) {
                        if (hasFocus) {
                            shakeViewContainer.resume();
                            textView.setText("摇一摇:启动");
                            Log.i(TAG, "Blank SlideView resume()");
                        } else {
                            shakeViewContainer.pause();
                            textView.setText("摇一摇:暂停");
                            Log.i(TAG, "Blank SlideView pause()");
                        }
                    }
                });
                return shakeContainer;
            }
        }
        return null;
    }

    private void showDownloadInfo(final View convertView, AQuery aq, final NativeResponse nrAd) {
        RelativeLayout downloadInfo = convertView.findViewById(R.id.app_download_container);
        downloadInfo.setVisibility(View.VISIBLE);
        aq.id(R.id.native_version).text("版本 " + nrAd.getAppVersion());
        aq.id(R.id.native_publisher).text(nrAd.getPublisher());

        View function = convertView.findViewById(R.id.native_function);
        function.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.functionClick();
            }
        });

        View privacy = convertView.findViewById(R.id.native_privacy);
        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.privacyClick();
            }
        });

        View permission = convertView.findViewById(R.id.native_permission);
        permission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.permissionClick();
            }
        });
        customProgressButton = new CustomProgressButton(FeedAdActivity.this);
        customProgressButton.useLongClick(true);
        // 字体大小适配屏幕
        if (getResources() != null) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int textSize = (int) (12 * metrics.scaledDensity + 0.5f);
            customProgressButton.setTextSize(textSize);
        }
        customProgressButton.setTypeFace(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC));
        customProgressButton.setForegroundColor(Color.parseColor("#3388FF"));
        customProgressButton.setBackgroundColor(Color.parseColor("#D7E6FF"));
        customProgressButton.initWithResponse(nrAd);

        RelativeLayout.LayoutParams actButtonParams =
                new RelativeLayout.LayoutParams(dip2px(FeedAdActivity.this, 64),
                        dip2px(FeedAdActivity.this, 24));
        actButtonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        actButtonParams.addRule(RelativeLayout.CENTER_VERTICAL);
        actButtonParams.rightMargin = dip2px(FeedAdActivity.this, 8);
        downloadInfo.addView(customProgressButton, actButtonParams);
    }

    // 点击联盟logo打开官网
    private void setUnionLogoClick(final View convertView, int logoId, final NativeResponse nrAd) {
        View logo = convertView.findViewById(logoId);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.unionLogoClick();
            }
        });
    }

    private void hideDownloadInfo(final View convertView) {
        convertView.findViewById(R.id.app_download_container).setVisibility(View.GONE);
    }

    private boolean isDownloadAd(NativeResponse nrAd) {
        return nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                && !TextUtils.isEmpty(nrAd.getAppVersion()) && !TextUtils.isEmpty(nrAd.getPublisher())
                && !TextUtils.isEmpty(nrAd.getAppPrivacyLink()) && !TextUtils.isEmpty(nrAd.getAppPermissionLink());
    }

    // 获取安装状态、下载进度所对应的按钮文案
    private String getBtnText(NativeResponse nrAd) {
        if (nrAd == null) {
            return "";
        }
        String actButtonString = nrAd.getActButtonString();
        if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                || nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_DEEP_LINK) {
            int status = nrAd.getDownloadStatus();
            if (status >= 0 && status <= 100) {
                return "下载中：" + status + "%";
            } else if (status == 101) {
                return "点击安装";
            } else if (status == 102) {
                return "继续下载";
            } else if (status == 103) {
                return "点击启动";
            } else if (status == 104) {
                return "重新下载";
            } else {
                if (!TextUtils.isEmpty(actButtonString)) {
                    return actButtonString;
                }
                return "点击下载";
            }
        }
        if (!TextUtils.isEmpty(actButtonString)) {
            return actButtonString;
        }
        return "查看详情";
    }

    private int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
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
            winInfo.put("ad_time", System.currentTimeMillis());
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
