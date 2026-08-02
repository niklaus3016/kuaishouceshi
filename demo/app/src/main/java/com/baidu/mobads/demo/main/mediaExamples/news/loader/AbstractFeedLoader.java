package com.baidu.mobads.demo.main.mediaExamples.news.loader;

import android.content.Context;
import android.util.Log;

import com.baidu.mobads.demo.main.mediaExamples.news.bean.FeedItem;
import com.baidu.mobads.demo.main.mediaExamples.news.utils.IdIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 抽象的信息流加载器，用于加载资讯类Demo中的表项
 *
 * 需要实体子类继承实现的方法：
 * {@link #initManager()}: 负责初始化加载器的具体manager
 * {@link #loadData()}: 负责加载请求数据
 */
public abstract class AbstractFeedLoader {
    private static final String TAG = "FeedLoader";

    protected static final int DEFAULT_THRESHOLD = 5;
    protected static int mThreshold;

    protected LinkedBlockingQueue<FeedItem> bufferQueue;
    protected IdIterator idIterator;
    protected Context mContext;
    protected LoadListener mListener;
    protected int loadCount = 0;

    protected boolean isLoading = false;
    protected boolean finished = false;

    public AbstractFeedLoader(Context context, int threshold, IdIterator iterator, final LoadListener listener) {
        mContext = context;
        idIterator = iterator;
        mListener = listener;
        mThreshold = threshold;
        bufferQueue = new LinkedBlockingQueue<FeedItem>();
    }

    protected abstract void initManager();

    protected abstract void loadData();

    protected void load() {
        if (!isLoading && !finished && loadCount < 3) {
            isLoading = true;
            loadCount++;
            loadData();
        }
    }

    public List<FeedItem> getDataLoaded(final int contentNum) {
        int num = Math.min(contentNum, mThreshold);
        List<FeedItem> list = new ArrayList<FeedItem>();
        for (int i = 0; i < num; i++) {
            FeedItem item = bufferQueue.poll();
            if (null != item) {
                list.add(item);
            }
        }
        // 如果数量不足，就发起加载请求
        loadCount = 0;
        checkAndLoad();
        return list;
    }

    protected synchronized void checkAndLoad() {
        if (bufferQueue.size() < mThreshold) {
            Log.i(TAG, "onAdLoaded: size = " + bufferQueue.size());
            // 若内容buffer数量不足，就继续请求
            load();
        } else {
            mListener.onLoadComplete();
        }
    }

    public void release() {
        finished = true;
        bufferQueue.clear();
    }

    public interface LoadListener {

        void onLoadComplete();

        void onLoadException(String msg, int errorCode);
    }
}
