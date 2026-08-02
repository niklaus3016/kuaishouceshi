package com.baidu.mobads.demo.main.feeds;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.RefreshAndLoadMoreView;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.EntryResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.XAdEntryResponse;

import java.util.ArrayList;
import java.util.List;

public class FeedEntryAdActivity extends Activity {
    private NativeAdAdapter mAdapter;
    private static final String TAG = FeedEntryAdActivity.class.getSimpleName();
    private RefreshAndLoadMoreView mRefreshLoadView;
    private static final String AD_ID = "8587496";
    private BaiduNativeManager mBaiduNativeManager;
    BaiduNativeManager.EntryAdListener mEntryAdListener;
    List<EntryResponse> nrAdList = new ArrayList<EntryResponse>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_entry_ad);
         mEntryAdListener =
                new BaiduNativeManager.EntryAdListener() {
            @Override
            public void onNativeLoad(List<EntryResponse> responses) {
                Log.i(TAG, "onNativeLoad:" +
                        (responses != null ? responses.size() : null));
                // 一个广告只允许展现一次，多次展现、点击只会计入一次
                if (responses != null && responses.size() > 0) {
                    // 刷新时重制数据
                    if (mRefreshLoadView.isRefreshing()) {
                        nrAdList.clear();
                    }
                    nrAdList.addAll(responses);
                    mAdapter.notifyDataSetChanged();
                }
                mRefreshLoadView.onLoadFinish();
            }

            @Override
            public void onNativeFail(int errorCode, String message) {
                Log.w(TAG, "onLoadFail reason:" + message + "errorCode:" + errorCode);
                mRefreshLoadView.onLoadFinish();
            }

            @Override
            public void onNoAd(int code, String msg) {
                Log.i(TAG, "onNoAd reason:" + msg);
                mRefreshLoadView.onLoadFinish();
            }

            @Override
            public void onLpClosed() {
                Log.i(TAG, "onLpClosed.");
            }
        };

        mBaiduNativeManager = new BaiduNativeManager(this.getApplicationContext(), AD_ID);
        initView();
        mRefreshLoadView.setRefreshing(true);
        loadFeedEntryAd();
    }

    private void initView() {


        mRefreshLoadView = findViewById(R.id.refresh_container);
        mRefreshLoadView.setLoadAndRefreshListener(new RefreshAndLoadMoreView.LoadAndRefreshListener() {
            @Override
            public void onRefresh() {
                loadFeedEntryAd();
            }

            public void onLoadMore() {
                loadFeedEntryAd();
            }
        });
        final SearchView searchView = findViewById(R.id.search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                nrAdList.clear();
                mAdapter.notifyDataSetChanged();
                if (TextUtils.isEmpty(query)) {
                    return true;
                }
                RequestParameters requestParameters = new RequestParameters.Builder()
                        .addCustExt(ArticleInfo.PAGE_TITLE, query)
                        .addCustExt("cust_Key_这是key", "cust_Value_这是Value" + System.currentTimeMillis())
                        .addCustExt("AAAAAAA", "aaaaaa")
                        .build();
                mBaiduNativeManager.loadFeedEntryAd(requestParameters, mEntryAdListener);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        ListView list = mRefreshLoadView.getListView();
        mAdapter = new NativeAdAdapter(this);
        list.setAdapter(mAdapter);
    }

    private void loadFeedEntryAd() {
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

        // 发起词条（搜一搜）广告请求
        mBaiduNativeManager.loadFeedEntryAd(requestParameters, mEntryAdListener);
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
        public EntryResponse getItem(int position) {
            return nrAdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final XAdEntryResponse nrAd = (XAdEntryResponse) getItem(position);
            convertView = inflater.inflate(R.layout.layout_feed_entry, null);
            AQuery aq = new AQuery(convertView);
            aq.id(R.id.image_entry_ad).image(nrAd.getAdLogoUrl(), false, true);
            setUnionLogoClick(convertView, R.id.image_entry_ad, nrAd);
            aq.id(R.id.image_entry_logo).image(nrAd.getBaiduLogoUrl(), false, true);
            setUnionLogoClick(convertView, R.id.image_entry_ad, nrAd);
            aq.id(R.id.entry_text).text(nrAd.getTitle());

            List<View> clickViews = new ArrayList<>();
            List<View> creativeViews = new ArrayList<>();
            clickViews.add(convertView);

            /**
             * registerViewForInteraction()与BaiduNativeManager配套使用
             * 警告：调用该函数来发送展现，勿漏！
             */
            nrAd.registerViewForInteraction(convertView, clickViews, creativeViews,
                    new EntryResponse.EntryAdInteractionListener() {
                        @Override
                        public void onAdClick() {
                            Log.i(TAG, "onAdClick:" + nrAd.getTitle());
                        }

                        @Override
                        public void onADExposed() {
                            Log.i(TAG, "onADExposed:" + nrAd.getTitle());
                        }

                        @Override
                        public void onADExposureFailed(int reason) {
                            Log.i(TAG, "onADExposureFailed: " + reason);
                        }

                        @Override
                        public void onAdUnionClick() {
                            Log.i(TAG, "onADUnionClick");
                        }
                    });


            return convertView;
        }
    }

    private void setUnionLogoClick(View convertView, int id, final XAdEntryResponse nrAd) {
        View logo = convertView.findViewById(id);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.unionLogoClick();
            }
        });
    }
}