package com.baidu.mobads.demo.main.mediaExamples.news.bean;



import com.baidu.mobads.sdk.api.NativeResponse;

import java.util.Map;

/**
 * 信息流表项的数据model，内容信息流元素和广告信息流元素共用
 */
public class FeedItem {

    // 自己的内容信息流表项属性
    private long itemId;
    private int itemType;
    private String title;
    private String mLeftImageUrl;
    private String mMidImageUrl;
    private String mRightImageUrl;
    private String author;
    private String bottomDesc;
    // 添加这一属性，为请求到的广告响应
    private NativeResponse nrAd;

    // 一般类型的信息流表项
    public FeedItem(long itemId, int itemType, Map<String, String> parameters) {
        this(itemId, itemType, parameters, null);
    }

    // 包含广告的信息流表项
    public FeedItem(long itemId, int itemType, NativeResponse nrAd) {
        this(itemId, itemType, null, nrAd);
    }

    public FeedItem(long itemId, int itemType, Map<String, String> parameters, NativeResponse nrAd) {
        this.itemId = itemId;
        this.itemType = itemType;
        // 取出内容参数
        if (null != parameters) {
            this.title = parameters.get("title");
            this.bottomDesc = parameters.get("bottom_desc");
            this.author = parameters.get("author");
            this.mLeftImageUrl = parameters.get("left_image");
            this.mMidImageUrl = parameters.get("mid_image");
            this.mRightImageUrl = parameters.get("right_image");
        }
        // 取出广告
        this.nrAd = nrAd;
    }

    public long getItemId() {
        return itemId;
    }

    public String getBottomDesc() {
        return bottomDesc;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getmLeftImageUrl() {
        return mLeftImageUrl;
    }

    public String getmMidImageUrl() {
        return mMidImageUrl;
    }

    public String getmRightImageUrl() {
        return mRightImageUrl;
    }

    public NativeResponse getNrAd() {
        return nrAd;
    }

    public int getItemType() {
        return itemType;
    }
}
