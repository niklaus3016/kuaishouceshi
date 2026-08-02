package com.baidu.mobads.demo.main.mediaExamples.news;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter;
import com.baidu.mobads.demo.main.mediaExamples.news.bean.FeedItem;
import com.baidu.mobads.demo.main.mediaExamples.news.loader.AbstractFeedLoader;
import com.baidu.mobads.demo.main.mediaExamples.news.loader.FeedAdLoader;
import com.baidu.mobads.demo.main.mediaExamples.news.loader.FeedContentLoader;
import com.baidu.mobads.demo.main.mediaExamples.news.utils.IdIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 资讯类接入示例：列表页的碎片，根据碎片类型展示不同广告
 */
public class NewsListFragment extends Fragment {
    private static final String TAG = "FeedListFragment";
    // 碎片类型
    public static final int FRAG_AD_BIG_PIC = 0;
    public static final int FRAG_AD_TRI_PIC = 1;
    public static final int FRAG_AD_VIDEO = 2;
    public static final int FRAG_AD_MIXED = 3;
    // 广告位ID
    private static final String FEED_VIDEO_AD_PLACE_ID = "2362913";        // 信息流视频
    private static final String BIG_PIC_AD_PLACE_ID = "2058628";        // 大图+ICON+描述
    private static final String SANTU_AD_PLACE_ID = "5887568";          // 三图
    // 应用ID
    private static final String YOUR_APP_ID = "c0da1ec4"; // 双引号中填写自己的APPSID
    // 用FeedItem封装了信息流的项目
    private List<FeedItem> itemList = Collections.synchronizedList(new ArrayList<FeedItem>());
    private NewsListAdapter mAdapter;
    private IdIterator idIterator;

    private int fragmentType = FRAG_AD_BIG_PIC;
    private static final int CONTENT_NUM = 4;
    private String title;
    private boolean contentReady = false;

    private Context context;
    private View contentView;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView mRecycleView;
    private FeedContentLoader mContentLoader;
    private FeedAdLoader mAdLoader;
    private Map<Integer, FeedAdLoader> mixedAdLoader;
    private Map<Integer, FeedContentLoader> mixedContentLoader;

    private static final String BUNDLE_FRAG_TYPE = "frag_type";
    private static final String BUNDLE_FRAG_TITLE = "frag_title";


    public NewsListFragment() {
        // Required empty public constructor
        idIterator = new IdIterator();
        mixedAdLoader = new HashMap<>();
        mixedContentLoader = new HashMap<>();
    }

    /**
     * FeedListFragment 的工厂方法，需要指定碎片类型
     */
    public static NewsListFragment newInstance(int type, String title) {
        NewsListFragment fragment = new NewsListFragment();
        fragment.fragmentType = type;
        fragment.title = title;
        return fragment;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(BUNDLE_FRAG_TYPE, fragmentType);
        outState.putString(BUNDLE_FRAG_TITLE, title);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        if (savedInstanceState != null) {
            fragmentType = savedInstanceState.getInt(BUNDLE_FRAG_TYPE, FRAG_AD_BIG_PIC);
            idIterator = new IdIterator();
            title = savedInstanceState.getString(BUNDLE_FRAG_TITLE, "大图");
        }
        AbstractFeedLoader.LoadListener adLoadListener = new AbstractFeedLoader.LoadListener() {
            @Override
            public void onLoadComplete() {
                if (contentReady && itemList.isEmpty()) {
                    // 数据列表为空说明为首次初始化
                    showMore();
                }
            }

            @Override
            public void onLoadException(String msg, int errorCode) {

            }
        };
        AbstractFeedLoader.LoadListener contentLoadListener = new AbstractFeedLoader.LoadListener() {
            @Override
            public void onLoadComplete() {
                contentReady = true;
                if (itemList.isEmpty()) {
                    // 数据列表为空说明为首次初始化
                    showMore();
                }
            }

            @Override
            public void onLoadException(String msg, int errorCode) {

            }
        };
        /**
         * 根据碎片展示类型，创建BaiduNative对象，参数分别为： 上下文context，广告位ID
         * 注意：请将adPlaceId替换为自己的广告位ID
         * 注意：信息流广告对象，与广告位id一一对应，同一个对象可以多次发起请求
         */
        switch (fragmentType) {
            case FRAG_AD_BIG_PIC:
                mAdLoader = FeedAdLoader.newInstance(context, BIG_PIC_AD_PLACE_ID,
                        3, idIterator, adLoadListener);
                mContentLoader = FeedContentLoader.newInstance(context, NewsListAdapter.CONTENT_BIG_PIC,
                        5, idIterator, contentLoadListener);
                break;
            case FRAG_AD_TRI_PIC:
                mAdLoader = FeedAdLoader.newInstance(context, SANTU_AD_PLACE_ID,
                        3, idIterator, adLoadListener);
                mContentLoader = FeedContentLoader.newInstance(context, NewsListAdapter.CONTENT_TRI_PIC,
                        5, idIterator, contentLoadListener);
                break;
            case FRAG_AD_VIDEO:
                mAdLoader = FeedAdLoader.newInstance(context, FEED_VIDEO_AD_PLACE_ID,
                        2, idIterator, adLoadListener);
                mContentLoader = FeedContentLoader.newInstance(context, NewsListAdapter.CONTENT_VIDEO,
                        5, idIterator, contentLoadListener);
                break;
            case FRAG_AD_MIXED:
                FeedAdLoader bigPicAdLoader = FeedAdLoader.newInstance(context,
                        BIG_PIC_AD_PLACE_ID, 3, idIterator, adLoadListener);
                FeedAdLoader triPicAdLoader = FeedAdLoader.newInstance(context,
                        SANTU_AD_PLACE_ID, 3, idIterator, adLoadListener);
                FeedAdLoader videoAdLoader = FeedAdLoader.newInstance(context,
                        FEED_VIDEO_AD_PLACE_ID, 2, idIterator, adLoadListener);
                mixedAdLoader.put(NewsListAdapter.AD_FEED_PIG_PIC, bigPicAdLoader);
                mixedAdLoader.put(NewsListAdapter.AD_FEED_TRI_PIC, triPicAdLoader);
                mixedAdLoader.put(NewsListAdapter.AD_FEED_VIDEO, videoAdLoader);

                FeedContentLoader bigPicContentLoader = FeedContentLoader.newInstance(context,
                        NewsListAdapter.CONTENT_BIG_PIC, 5, idIterator, contentLoadListener);
                FeedContentLoader triPicContentLoader = FeedContentLoader.newInstance(context,
                        NewsListAdapter.CONTENT_TRI_PIC, 5, idIterator, contentLoadListener);
                FeedContentLoader videoContentLoader = FeedContentLoader.newInstance(context,
                        NewsListAdapter.CONTENT_VIDEO, 5, idIterator, contentLoadListener);
                mixedContentLoader.put(NewsListAdapter.CONTENT_BIG_PIC, bigPicContentLoader);
                mixedContentLoader.put(NewsListAdapter.CONTENT_TRI_PIC, triPicContentLoader);
                mixedContentLoader.put(NewsListAdapter.CONTENT_VIDEO, videoContentLoader);
                break;
            default:
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_news_list, container, false);
        initView();
        Log.i(TAG, "onCreateView:" + " " + fragmentType + " " + savedInstanceState);
        if (itemList.isEmpty()) {
            // 数据列表为空说明为首次初始化，刷新数据列表
            refreshList();
        }
        return contentView;
    }

    private void initView() {
        mAdapter = new NewsListAdapter(context, itemList);
        mAdapter.setOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                Toast.makeText(context, "Item " + position + " selected.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), NewsDetailsActivity.class));
            }
        });
        refreshLayout = contentView.findViewById(R.id.frag_refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });
        mRecycleView = contentView.findViewById(R.id.frag_container);
        mRecycleView.setLayoutManager(new LinearLayoutManager(context));
        mRecycleView.setAdapter(mAdapter);
        mRecycleView.addOnScrollListener(new FeedScrollListener());
    }

    private class FeedScrollListener extends RecyclerView.OnScrollListener {
        // 标记是否正在向最后一个滑动
        boolean isSlidingToLast = true;
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
            // 当处于停止滚动状态时
            if (null != manager && RecyclerView.SCROLL_STATE_IDLE == newState) {
                // 获取最后一个完全显示的数据项的position
                int lastVisibleItem = manager.findLastCompletelyVisibleItemPosition();
                int totalItemCount = manager.getItemCount();
                // 判断是否滚动到底部，并且同时是向后滚动
                if ((totalItemCount - 1) == lastVisibleItem && isSlidingToLast) {
                    // 显示更多信息流内容
                    showMore();
                }
            }
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            // dx用来判断横向滑动方向，dy用来判断纵向滑动方向
            isSlidingToLast = dx > 0 || dy > 0;
            // 该方法调起频繁，建议不要做耗时的操作
        }
    }

    // 清空并刷新所有内容
    public void refreshList() {
        if (refreshLayout != null) {
            refreshLayout.setRefreshing(false);
        }
        int prevNum = itemList.size();
        itemList.clear();
        if (mAdapter != null) {
            mAdapter.notifyItemRangeRemoved(0, prevNum);
        }
        showMore();
    }

    // 展示更多数据
    private void showMore() {
        List<FeedItem> feedList = getShowList();
        int insertNum = feedList.size();
        if (insertNum > 0) {
            itemList.addAll(feedList);
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(itemList.size() - insertNum, insertNum);
            }
            // 初次加载或刷新时，返回第一项显示，否则会停留在最后一项
            if (itemList.size() == insertNum && mRecycleView != null) {
                mRecycleView.scrollToPosition(0);
            }
        }
    }

    // 根据预加载广告内容，获得展示的信息流表项数据
    private List<FeedItem> getShowList() {
        List<FeedItem> feedList = new ArrayList<FeedItem>();
        // 只有内容加载完成，才会显示信息流
        if (contentReady) {
            List<FeedItem> adList = getAdList(1);
            // 如果有缓存的广告，则直接用来与一般内容混合
            if (0 < adList.size()) {
                feedList = mixWithContent(adList);
            } else {
                // 否则只生成一般内容
                Toast.makeText(context, "No ads data preloaded. Add common data.", Toast.LENGTH_SHORT).show();
                feedList = getContentList(CONTENT_NUM);
            }
        }
        return feedList;
    }

    // 根据广告响应与一般内容混合产生数据项
    private List<FeedItem> mixWithContent(List<FeedItem> adItems) {
        if (null == adItems || 0 == adItems.size()) {
            return null;
        }
        List<FeedItem> displayList = new ArrayList<FeedItem>();
        // 每条广告前添加一般内容
        for (FeedItem item : adItems) {
            // 添加一般内容，媒体可加载自己的内容
            List<FeedItem> content = getContentList(CONTENT_NUM);
            displayList.addAll(content);
            displayList.add(item);
        }
        return displayList;
    }

    private List<FeedItem> getContentList(int num) {
        List<FeedItem> contentList = new ArrayList();
        if (fragmentType == FRAG_AD_MIXED) {
            // 随机选择所有类型内容，可自行优化内容选择策略
            Random random = new Random();
            Integer[] keys = mixedContentLoader.keySet().toArray(new Integer[0]);
            for (int i = 0; i < num; i++) {
                FeedContentLoader loader = mixedContentLoader.get(keys[random.nextInt(keys.length)]);
                if (loader != null) {
                    contentList.addAll(loader.getDataLoaded(1));
                }
            }
        } else {
            contentList = mContentLoader.getDataLoaded(num);
        }
        return contentList;
    }

    private List<FeedItem> getAdList(int num) {
        List<FeedItem> adList = new ArrayList();
        if (fragmentType == FRAG_AD_MIXED) {
            // 随机选择一种类型广告，可自行优化广告选择策略
            Random random = new Random();
            Integer[] keys = mixedAdLoader.keySet().toArray(new Integer[0]);
            FeedAdLoader loader = mixedAdLoader.get(keys[random.nextInt(keys.length)]);
            if (loader != null) {
                adList = loader.getDataLoaded(num);
            }
        } else {
            adList = mAdLoader.getDataLoaded(num);
        }
        return adList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdLoader != null) {
            mAdLoader.release();
        }
        if (!mixedAdLoader.isEmpty()) {
            for (FeedAdLoader loader : mixedAdLoader.values()) {
                loader.release();
            }
            mixedAdLoader.clear();
        }
        if (mContentLoader != null) {
            mContentLoader.release();
        }
    }
}