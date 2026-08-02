package com.baidu.mobads.demo.main.express;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.RefreshAndLoadMoreView;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.BiddingListener;
import com.baidu.mobads.sdk.api.ExpressResponse;
import com.baidu.mobads.sdk.api.RequestParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/*
1. 信息流模板智选集成参考类：ExpressFeedAdActivity
2. 使用BaiduNativeManager集成
    a. 构造请求参数：RequestParameters，非必须。可根据您的需求来创建Parameter
    b. 执行loadExpressFeedAd
    c. 监听ExpressAdListener，请求成功会回传List<ExpressResponse> ，
       调用ExpressResponse的render接口，通过onRenderSuccess回调或getExpressAdView方法获取渲染后的view
    d. 【注意】展现前请调用bindInteractionActivity方法绑定展示的activity，否则负反馈弹窗无法弹出（无响应）
3. 参数回传请联系商务同学，进行相关合作。
4. 信息流模板智选广告会自动检测曝光和处理点击事件。
5. 广告有时间有效期限制，从拉取到使用超过30分钟，将被视为无效广告。可以利用isReady方法验证。
6. 广告位的返回模板，当APP广告上线后，切勿随意变更，确保前端不会发生崩溃。
   且前端开发需要对渲染回调结果进行校验，以确定view可以正常展示，若有不符需要抛弃广告，防止crash。
*/
public class ExpressFeedAdActivity extends Activity {
    private static final String TAG = ExpressFeedAdActivity.class.getSimpleName();
    private static final String FEED_SMART_OPT_AD_PLACE_ID = "8035132"; // 信息流智能优选
    public static final int VERTICAL_VIDEO_STYLE = 41;

    List<ExpressResponse> nrAdList = new ArrayList<ExpressResponse>(); // 媒体自渲染广告使用
    private NativeAdAdapter mAdapter;
    private RefreshAndLoadMoreView mRefreshLoadView;
    private BaiduNativeManager mBaiduNativeManager;
    private boolean mDarkTheme = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_main);
        initView();

        /**
         * Step 1. 创建BaiduNativeManager对象，参数分别为： 上下文context，广告位ID
         * 注意：请将adPlaceId替换为自己的广告位ID
         * 注意：信息流广告对象，与广告位id一一对应，同一个对象可以多次发起请求
         */
        mBaiduNativeManager = new BaiduNativeManager(this.getApplicationContext(), FEED_SMART_OPT_AD_PLACE_ID);
        mRefreshLoadView.setRefreshing(true);
        loadExpressFeedAd();

    }

    private void initView() {

        mRefreshLoadView = findViewById(R.id.refresh_container);
        mRefreshLoadView.setLoadAndRefreshListener(new RefreshAndLoadMoreView.LoadAndRefreshListener() {
            @Override
            public void onRefresh() {
                loadExpressFeedAd();
            }

            public void onLoadMore() {
                loadExpressFeedAd();
            }
        });

        ListView list = mRefreshLoadView.getListView();
        mAdapter = new NativeAdAdapter(this);
        list.setAdapter(mAdapter);

        //
        final Button button = findViewById(R.id.switch_theme);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDarkTheme = !mDarkTheme;
                button.setText(mDarkTheme ? "暗黑模式: 开" : "暗黑模式: 关");
                button.setTextColor(mDarkTheme ? Color.WHITE : Color.BLACK);
                mRefreshLoadView.setBackgroundColor(mDarkTheme ? Color.BLACK : Color.WHITE);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadExpressFeedAd() {
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
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                if (expressResponses != null && expressResponses.size() > 0) {
                    // 刷新成功时，重制数据
                    if (mRefreshLoadView.isRefreshing()) {
                        nrAdList.clear();
                    }
                    // 绑定监听事件并渲染模板
                    for (final ExpressResponse ad : expressResponses) {
                        // 绑定监听
                        bindExpressListeners(ad);
                        // 渲染模板广告
                        ad.render();
                    }
                }
                // 停止刷新动画
                mRefreshLoadView.onLoadFinish();
            }

            @Override
            public void onNoAd(int code, String msg, ExpressResponse expressResponse) {
                Log.i(TAG, "onNoAd reason:" + msg);
                mRefreshLoadView.onLoadFinish();
                clientBidding(expressResponse);
            }

            @Override
            public void onNativeFail(int errorCode, String message, ExpressResponse expressResponse) {
                Log.w(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
                mRefreshLoadView.onLoadFinish();
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
                nrAdList.add(expressAd);
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
                expressAd.destroy();
                nrAdList.remove(expressAd);
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
                expressResponse.destroy();
                nrAdList.remove(expressResponse);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面退出，释放资源
        for (ExpressResponse response : nrAdList) {
            response.destroy();
        }
        nrAdList.clear();
    }

    class NativeAdAdapter extends BaseAdapter {
        LayoutInflater inflater;

        public NativeAdAdapter(Context context) {
            super();
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return nrAdList.size();
        }

        @Override
        public ExpressResponse getItem(int position) {
            return nrAdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ExpressResponse response = getItem(position);
            ViewHolder viewHolder = new ViewHolder(response, mDarkTheme);
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.feed_native_listview_item, null);
            } else {
                Object tag = convertView.getTag();
                if (tag instanceof ViewHolder) {
                    ViewHolder previous = (ViewHolder) tag;
                    // 复用时，判断是否为同一个广告位
                    if (viewHolder.equalTo(previous)) {
                        return convertView;
                    }
                }
                // 复用的View需要清空容器
                if (convertView instanceof ViewGroup) {
                    ((ViewGroup) convertView).removeAllViews();
                }
            }
            convertView.setTag(viewHolder);
            final ViewGroup adContainer = (ViewGroup) convertView;
            // 【可选】模拟竞价，广告返回后，媒体可以自行进行客户端竞价，反馈竞价结果
            clientBidding(response);
            // 切换主题
            response.switchTheme(mDarkTheme ? ExpressResponse.THEME_DARK : ExpressResponse.THEME_DEFAULT);
            // 添加view
            View adView = response.getExpressAdView();
            Log.i(TAG, "getExpressAdView styleType: " + response.getStyleType());
            if (adView != null) {
                // 注意：复用广告View时，要避免广告View已添加在容器中，需要从旧容器中移除
                ViewParent preContainer = adView.getParent();
                if (preContainer instanceof ViewGroup) {
                    ((ViewGroup) preContainer).removeView(adView);
                }

                // 竖版视频模版时，调整容器宽度，方便展示
                ViewGroup.LayoutParams params;
                if (response.getStyleType() == VERTICAL_VIDEO_STYLE) {
                    params = new ViewGroup.LayoutParams(getScreenWidth(parent.getContext()) * 3 / 4,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                } else {
                    params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                }
                adContainer.addView(adView, params);
            }
            return convertView;
        }

        class ViewHolder {
            ExpressResponse response;
            int theme;

            ViewHolder(ExpressResponse ad, boolean darkTheme) {
                this.response = ad;
                this.theme = darkTheme ? ExpressResponse.THEME_DARK : ExpressResponse.THEME_DEFAULT;
            }

            boolean equalTo(ViewHolder other) {
                if (other != null) {
                    return other.response == this.response
                            && other.theme == this.theme;
                }
                return false;
            }
        }
    }

    private int getScreenWidth(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= 17) {
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getRealMetrics(dm);
        } else {
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getMetrics(dm);
        }
        Rect rect;
        if (dm.widthPixels > dm.heightPixels) {
            rect = new Rect(0, 0, dm.heightPixels, dm.widthPixels);
        } else {
            rect = new Rect(0, 0, dm.widthPixels, dm.heightPixels);
        }
        return rect.width();
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

}
