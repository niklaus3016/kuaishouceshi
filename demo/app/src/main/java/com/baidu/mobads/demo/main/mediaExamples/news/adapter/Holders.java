package com.baidu.mobads.demo.main.mediaExamples.news.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.XNativeView;

import java.util.ArrayList;
import java.util.List;

public class Holders {
    // 信息流数据项的ViewHolder
    public static class FeedItemViewHolder extends RecyclerView.ViewHolder {
        View mTitle;

        public FeedItemViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.native_title);
        }
    }
    // 一般内容数据项的ViewHolder
    public static class FeedContentViewHolder extends FeedItemViewHolder {
        ImageView mLeftImage;
        ImageView mMidImage;
        ImageView mRightImage;
        TextView mAuthor;
        TextView mBottomDesc;
        ImageView mVideoPlay;

        public FeedContentViewHolder(View itemView) {
            super(itemView);
            mLeftImage = itemView.findViewById(R.id.image_left);
            mMidImage = itemView.findViewById(R.id.image_mid);
            mRightImage = itemView.findViewById(R.id.image_right);
            mAuthor = itemView.findViewById(R.id.bottom_first_text);
            mBottomDesc = itemView.findViewById(R.id.bottom_second_text);
            mVideoPlay = itemView.findViewById(R.id.video_play);
        }
    }
    // 信息流广告数据项的ViewHolder
    public static class FeedAdViewHolder extends FeedItemViewHolder {
        ImageView mIcon;
        TextView mText;
        TextView mBrandName;
        ImageView mAdLogo;
        ImageView mBdLogo;
        NativeResponse nrAd;

        RelativeLayout mDownLoadInfoContainer;
        TextView mAppVersion;
        TextView mAppPublisher;
        TextView mPrivacyLink;
        TextView mPermissionLink;

        public FeedAdViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.native_icon_image);
            mText = itemView.findViewById(R.id.native_text);
            mBrandName = itemView.findViewById(R.id.native_brand_name);
            mAdLogo = itemView.findViewById(R.id.native_adlogo);
            mBdLogo = itemView.findViewById(R.id.native_baidulogo);
            // 初始化下载类信息控件
            mDownLoadInfoContainer = itemView.findViewById(R.id.app_download_container);
            mAppVersion = itemView.findViewById(R.id.native_version);
            mAppPublisher = itemView.findViewById(R.id.native_publisher);
            mPrivacyLink = itemView.findViewById(R.id.native_privacy);
            mPermissionLink = itemView.findViewById(R.id.native_permission);
        }
    }
    // 大图+Icon+描述 信息流广告
    public static class FeedPicAdViewHolder extends FeedAdViewHolder {
        ImageView mImage;

        public FeedPicAdViewHolder(View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.native_main_image);
        }
    }
    // 三图并排 信息流广告
    public static class FeedTriPicAdViewHolder extends FeedAdViewHolder {
        List<ImageView> mImageList;

        public FeedTriPicAdViewHolder(View itemView) {
            super(itemView);
            mImageList = new ArrayList<ImageView>();
            mImageList.add((ImageView) itemView.findViewById(R.id.native_main1));
            mImageList.add((ImageView) itemView.findViewById(R.id.native_main2));
            mImageList.add((ImageView) itemView.findViewById(R.id.native_main3));
        }
    }
    // 信息流视频广告
    public static class FeedVideoAdViewHolder extends FeedAdViewHolder {
        XNativeView mVideo;

        public FeedVideoAdViewHolder(View itemView) {
            super(itemView);
            mVideo = itemView.findViewById(R.id.native_main_image);
        }
    }
    // 加载更多 数据项
    public static class LoadMoreViewHolder extends FeedItemViewHolder {
        TextView mTextView;
        ProgressBar mProgressBar;

        public LoadMoreViewHolder(View itemView) {
            super(itemView);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            mTextView = itemView.findViewWithTag("TextView");
            mProgressBar = itemView.findViewWithTag("ProgressBar");
        }
    }
}
