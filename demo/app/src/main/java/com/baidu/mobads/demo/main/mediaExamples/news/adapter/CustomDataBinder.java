package com.baidu.mobads.demo.main.mediaExamples.news.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.news.bean.FeedItem;
import com.baidu.mobads.demo.main.mediaExamples.news.utils.FeedParseHelper;
import com.baidu.mobads.sdk.api.BDMarketingTextView;
import com.baidu.mobads.sdk.api.BDRefinedActButton;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.XAdNativeResponse;

import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.AD_FEED_PIG_PIC;
import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.AD_FEED_TRI_PIC;
import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.AD_FEED_VIDEO;
import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.CONTENT_BIG_PIC;
import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.CONTENT_TRI_PIC;
import static com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsListAdapter.CONTENT_VIDEO;

import java.util.ArrayList;
import java.util.List;

public class CustomDataBinder {
    private View mItemView;
    private AQuery mAq;

    public CustomDataBinder(View itemView) {
        mItemView = itemView;
        mAq = new AQuery(itemView);
    }

    public void bindContentViews(FeedItem item, Holders.FeedContentViewHolder contentViewHolder) {
        if (contentViewHolder.mTitle instanceof TextView) {
            ((TextView) contentViewHolder.mTitle).setText(item.getTitle());
        }
        contentViewHolder.mAuthor.setText(item.getAuthor());
        contentViewHolder.mVideoPlay.setVisibility(View.GONE);
        String bottomDesc = item.getBottomDesc();
        switch (item.getItemType()) {
            case CONTENT_BIG_PIC:
                mAq.id(R.id.image_big_pic).image(item.getmLeftImageUrl());
                bottomDesc = FeedParseHelper.getTransformedDateString(bottomDesc);
                break;
            case CONTENT_TRI_PIC:
                mAq.id(R.id.image_left).image(item.getmLeftImageUrl());
                mAq.id(R.id.image_mid).image(item.getmMidImageUrl());
                mAq.id(R.id.image_right).image(item.getmRightImageUrl());
                bottomDesc = FeedParseHelper.getTransformedDateString(bottomDesc);
                break;
            case CONTENT_VIDEO:
                mAq.id(R.id.image_big_pic).image(item.getmLeftImageUrl());
                contentViewHolder.mVideoPlay.setVisibility(View.VISIBLE);
                bottomDesc = FeedParseHelper.getFormatPlayCounts(Integer.parseInt(bottomDesc));
                break;
            default:
        }
        contentViewHolder.mBottomDesc.setText(bottomDesc);
    }

    public void bindAdViews(FeedItem item, Holders.FeedAdViewHolder adViewHolder) {
        final NativeResponse nrAd = item.getNrAd();
        adViewHolder.nrAd = nrAd;
        // 为AdView的各个组件加载广告数据
        if (adViewHolder.mTitle instanceof BDMarketingTextView) {
            BDMarketingTextView titleView = (BDMarketingTextView) adViewHolder.mTitle;
            titleView.setTextFontSizeSp(16);
            titleView.setTextMaxLines(2);
            titleView.setEllipsize(TextUtils.TruncateAt.END); // 不支持跑马灯式省略
            titleView.setAdData(nrAd, nrAd.getTitle());
            if (nrAd instanceof XAdNativeResponse) {
            // 如果有营销挂件需要的字段才展示营销挂件
            if (TextUtils.isEmpty(((XAdNativeResponse) nrAd).getMarketingICONUrl())
                    || TextUtils.isEmpty(((XAdNativeResponse) nrAd).getMarketingDesc())) {
                titleView.setLabelVisibility(View.GONE);
            }
            }
        } else if (adViewHolder.mTitle instanceof TextView) {
            mAq.id(adViewHolder.mTitle).text(nrAd.getTitle());
        }
        mAq.id(adViewHolder.mText).text(nrAd.getDesc());
        mAq.id(adViewHolder.mBrandName).text(nrAd.getBrandName());
        mAq.id(adViewHolder.mIcon).image(nrAd.getIconUrl(), false, true);
        mAq.id(adViewHolder.mAdLogo).image(nrAd.getAdLogoUrl(), false, true);
        mAq.id(adViewHolder.mBdLogo).image(nrAd.getBaiduLogoUrl(), false, true);
        // 设置百香果logo的点击跳转事件
        View.OnClickListener unionLogoClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.unionLogoClick();
            }
        };
        mAq.id(adViewHolder.mAdLogo).clicked(unionLogoClicked);
        mAq.id(adViewHolder.mBdLogo).clicked(unionLogoClicked);
        // 加载广告的图片资源
        // 此处仅举例三类广告：1.大图+ICON+描述 2.三图并排 3.使用NativeView的视频广告
        switch (item.getItemType()) {
            case AD_FEED_PIG_PIC:
                mAq.id(((Holders.FeedPicAdViewHolder) adViewHolder).mImage)
                        .image(nrAd.getImageUrl(), false, true);
                break;
            case AD_FEED_TRI_PIC:
                for (int i = 0; i < 3; i++) {
                    mAq.id(((Holders.FeedTriPicAdViewHolder) adViewHolder).mImageList.get(i))
                            .image(nrAd.getMultiPicUrls().get(i), false, true);
                }
                break;
            case AD_FEED_VIDEO:
                ((Holders.FeedVideoAdViewHolder) adViewHolder).mVideo.setNativeItem(nrAd);
        }
        // 若为下载类广告，展示隐私权限信息
        if (FeedParseHelper.isDownloadAd(nrAd)) {
            adViewHolder.mAppVersion.setText("版本 " + nrAd.getAppVersion());
            adViewHolder.mAppPublisher.setText(nrAd.getPublisher());
            adViewHolder.mPrivacyLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nrAd.privacyClick();
                }
            });
            adViewHolder.mPermissionLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nrAd.permissionClick();
                }
            });
            if (item.getItemType() != AD_FEED_VIDEO) {
                // 非视频广告需要展示 隐私权限信息的展示卡片
                adViewHolder.mDownLoadInfoContainer.setVisibility(View.VISIBLE);
                adViewHolder.mBrandName.setVisibility(View.GONE);

                TextView adAppName = adViewHolder.itemView.findViewById(R.id.app_name);
                adAppName.setText(nrAd.getBrandName());
                adAppName.setVisibility(View.VISIBLE);
                initActButton(adViewHolder.mDownLoadInfoContainer, nrAd);
            } else {
                // 视频广告需要展示 权限隐私信息字段
                adViewHolder.mAppVersion.setVisibility(View.VISIBLE);
                adViewHolder.mAppPublisher.setVisibility(View.VISIBLE);
                adViewHolder.mPrivacyLink.setVisibility(View.VISIBLE);
                adViewHolder.mPermissionLink.setVisibility(View.VISIBLE);
            }
        } else {
            if (item.getItemType() != AD_FEED_VIDEO) {
                // 非视频广告需要隐藏 隐私权限信息的展示卡片
                adViewHolder.mDownLoadInfoContainer.setVisibility(View.GONE);
                adViewHolder.mBrandName.setVisibility(View.VISIBLE);
            } else {
                // 视频广告需要隐藏 权限隐私信息字段
                adViewHolder.mAppVersion.setVisibility(View.GONE);
                adViewHolder.mAppPublisher.setVisibility(View.GONE);
                adViewHolder.mPrivacyLink.setVisibility(View.GONE);
                adViewHolder.mPermissionLink.setVisibility(View.GONE);
            }
        }

        // 为广告视图设置点击
        List<View> clickViews = new ArrayList<>();
        List<View> creativeViews = new ArrayList<>();
        clickViews.add(adViewHolder.itemView);
        nrAd.registerViewForInteraction(adViewHolder.itemView, clickViews, creativeViews, null);
    }

    private void initActButton(RelativeLayout container, NativeResponse nrAd) {
        Context context = container.getContext();
        BDRefinedActButton actButton = new BDRefinedActButton(context);
        actButton.setAdData(nrAd);
        RelativeLayout.LayoutParams actButtonParams =
                new RelativeLayout.LayoutParams(dip2px(context, 64),
                        dip2px(context, 24));
        actButtonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        actButtonParams.addRule(RelativeLayout.CENTER_VERTICAL);
        actButtonParams.rightMargin = dip2px(context, 8);
        container.addView(actButton, actButtonParams);
    }

    private int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
