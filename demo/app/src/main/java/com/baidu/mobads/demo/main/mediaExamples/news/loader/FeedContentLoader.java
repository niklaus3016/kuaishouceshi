package com.baidu.mobads.demo.main.mediaExamples.news.loader;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter;
import com.baidu.mobads.demo.main.mediaExamples.news.bean.FeedItem;
import com.baidu.mobads.demo.main.mediaExamples.news.utils.IdIterator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容加载器，用于加载资讯类Demo的一般内容项
 * 对外提供方法：
 * {@link #getDataLoaded(int)}: 父类方法，从数据队列中获取指定数量的{@link FeedItem}对象用于展现
 *
 * 初始化方法：{@link #newInstance(Context, int, int, IdIterator, LoadListener)}
 *
 * 【-------=====警告=====-------】
 * 此加载器是为了填充Demo的展示内容，使用了桩数据做加载模拟，媒体需要自行获取内容数据。
 */
public class FeedContentLoader extends AbstractFeedLoader {
    private static final String TAG = "FeedContentLoader";
    private static final String[] contentBigPicArray = {
            ContentDataSamples.CONTENT_BIG_PIC_01,
            ContentDataSamples.CONTENT_BIG_PIC_02,
            ContentDataSamples.CONTENT_BIG_PIC_03,
            ContentDataSamples.CONTENT_BIG_PIC_04
    };
    private static final String[] contentTriPicArray = {
            ContentDataSamples.CONTENT_TRI_PIC_01,
            ContentDataSamples.CONTENT_TRI_PIC_02,
            ContentDataSamples.CONTENT_TRI_PIC_03,
            ContentDataSamples.CONTENT_TRI_PIC_04
    };
    private static final String[] contentVideoArray = {
            ContentDataSamples.CONTENT_VIDEO_01,
            ContentDataSamples.CONTENT_VIDEO_02,
            ContentDataSamples.CONTENT_VIDEO_03
    };
    private int contentType;
    private List<String> contentList;

    private FeedContentLoader(Context context, int contentType, int threshold,
                              IdIterator iterator, LoadListener listener) {
        super(context, threshold, iterator, listener);
        this.contentType = contentType;
        switch (contentType) {
            case NewsListAdapter.CONTENT_BIG_PIC:
                contentList = Arrays.asList(contentBigPicArray);
                break;
            case NewsListAdapter.CONTENT_TRI_PIC:
                contentList = Arrays.asList(contentTriPicArray);
                break;
            case NewsListAdapter.CONTENT_VIDEO:
                contentList = Arrays.asList(contentVideoArray);
                break;
            default:
        }
    }

    public static FeedContentLoader newInstance(Context context, int contentType, int threshold,
                                                IdIterator iterator, LoadListener listener) {
        final FeedContentLoader loader =
                new FeedContentLoader(context, contentType, threshold, iterator, listener);
        // 初始化NativeCPUManager
        loader.initManager();
        // 实例化后开始请求内容
        loader.load();
        return loader;
    }

    @Override
    protected void initManager() {

    }

    @Override
    protected void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 模拟网络加载500毫秒
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Collections.shuffle(contentList);
                for (String content : contentList) {
                    try {
                        int id = idIterator.next();
                        JSONObject contentJob = new JSONObject(content);
                        Log.w(TAG, "loadData count = " + bufferQueue.size());
                        FeedItem item = getItemFromJob(id, contentType, contentJob);
                        bufferQueue.offer(item);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isLoading = false;
                        checkAndLoad();
                    }
                });
            }
        }).start();

    }

    private FeedItem getItemFromJob(int id, int itemType, JSONObject job) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("title", job.optString("title"));
        params.put("bottom_desc", job.optString("bottom_desc"));
        params.put("author", job.optString("author"));
        params.put("left_image", job.optString("left_image"));
        params.put("mid_image", job.optString("mid_image"));
        params.put("right_image", job.optString("right_image"));
        Log.w(TAG, "loadData: JSON!!! " + params);
        return new FeedItem(id, itemType, params);
    }

}
