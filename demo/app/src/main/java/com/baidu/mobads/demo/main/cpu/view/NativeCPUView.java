package com.baidu.mobads.demo.main.cpu.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.IBasicCPUData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.graphics.Color.parseColor;

public class NativeCPUView extends RelativeLayout {

    private final static long TIME_SECOND_YEAR = 365 * 24 * 60 * 60;
    private final static long TIME_SECOND_MONTH = 30 * 24 * 60 * 60;
    private final static long TIME_SECOND_DAY = 24 * 60 * 60;
    private final static long TIME_SECOND_HOUR = 60 * 60;
    private final static long TIME_SECOND_MINUTE = 60;

    private View mContainer;
    private TextView mTopTextView;
    private ImageView mImageLeft;
    private ImageView mImageMid;
    private ImageView mImageRight;
    private ImageView mImageSingleBig;
    private ImageView mBtnPlayVideo;
    private TextView mVideoTime;
    private View mBottomContainer;
    private TextView mBottom00FirstView;
    private TextView mBottomFirstView;
    private ImageView mBottomContainerAdlogo;
    private TextView mBottomSecondView;
    private ImageView mDislikeButton;
    private View mAppDownloadContainer;
    private TextView mAppName;
    private TextView mAppVersion;
    private TextView mAppPrivacyLink;
    private TextView mAppPermissionLink;
    private TextView mAppPublisher;

    private String mType;   // news,image,video,ad
    private String mTitle;
    private String mLeftImageUrl;
    private String mMidImageUrl;
    private String mRightImageUrl;
    private String mBottom00FirstText;
    private String mBottomFirstText;
    private String mBottomSecondText;
    private String mVideoDuration;

    public NativeCPUView(Context context) {
        this(context, null);
    }

    public NativeCPUView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NativeCPUView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContainer = inflater.inflate(R.layout.native_cpu_view, this, true);
        mTopTextView = mContainer.findViewById(R.id.top_text_view);
        mImageLeft = mContainer.findViewById(R.id.image_left);
        mImageMid = mContainer.findViewById(R.id.image_mid);
        mImageRight = mContainer.findViewById(R.id.image_right);
        mImageSingleBig = mContainer.findViewById(R.id.image_big_pic);
        mBtnPlayVideo = mContainer.findViewById(R.id.video_play);
        mVideoTime =  mContainer.findViewById(R.id.video_duration);
        mBottomContainer = mContainer.findViewById(R.id.bottom_container);
        mBottom00FirstView = mContainer.findViewById(R.id.bottom_00first_text);
        mBottomFirstView = mContainer.findViewById(R.id.bottom_first_text);
        mBottomContainerAdlogo  = mContainer.findViewById(R.id.bottom_container_adlogo);
        mBottomSecondView = mContainer.findViewById(R.id.bottom_second_text);
        mDislikeButton = mContainer.findViewById(R.id.dislike_icon);
        mAppDownloadContainer = mContainer.findViewById(R.id.app_download_container);
        mAppName = mContainer.findViewById(R.id.app_name);
        mAppVersion = mContainer.findViewById(R.id.app_version);
        mAppPrivacyLink = mContainer.findViewById(R.id.privacy_link);
        mAppPermissionLink = mContainer.findViewById(R.id.permission_link);
        mAppPublisher = mContainer.findViewById(R.id.app_publisher);
    }

    public void setAttribute(int bg, int textSize) {
        int impureWhite =  Color.parseColor("#CBCBCB");
        int topTextColor = bg == Color.WHITE ? Color.BLACK : impureWhite;
        int bottomTextColor = bg == Color.WHITE ? Color.GRAY : impureWhite;

        mContainer.setBackgroundColor(bg);
        mTopTextView.setTextColor(topTextColor);
        mTopTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        mBottomContainer.setBackgroundColor(bg);
        mBottomFirstView.setTextColor(bottomTextColor);
        mBottomFirstView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 4);
        mBottomSecondView.setTextColor(bottomTextColor);
        mBottomSecondView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 4);
        mAppDownloadContainer.setBackgroundColor(bg);
        mAppName.setTextColor(topTextColor);
        mAppName.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        mAppVersion.setTextColor(topTextColor);
        mAppVersion.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        mAppPrivacyLink.setTextColor(topTextColor);
        mAppPrivacyLink.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        mAppPermissionLink.setTextColor(topTextColor);
        mAppPermissionLink.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        mAppPublisher.setTextColor(topTextColor);
        mAppPublisher.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        mBottom00FirstView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 6);


        switch (textSize) {
            case 13:
                mBottomContainerAdlogo.setScaleX(0.7f);
                mBottomContainerAdlogo.setScaleY(0.7f);
                break;
            case 18:
                mBottomContainerAdlogo.setScaleX(1.0f);
                mBottomContainerAdlogo.setScaleY(1.0f);
                break;
            case 23:
                mBottomContainerAdlogo.setScaleX(1.5f);
                mBottomContainerAdlogo.setScaleY(1.5f);
                break;
        }

    }

    public void setItemData(final IBasicCPUData data, AQuery aq) {
        if (data != null) {
            mType = data.getType();  // news,image,video,ad
            mTitle = data.getTitle();
            mVideoDuration = converSceond2Minute(data.getDuration());
            mBottom00FirstText = data.getLabel();

            readImageUrls(data);
            if ("ad".equalsIgnoreCase(mType)) {
                // 广告类型
                mBottomFirstText = data.getBrandName();
                if (TextUtils.isEmpty(mBottomFirstText)) {
                    mBottomFirstText = "精选推荐";
                }
                mBottomSecondText = "广告";

                mBottomContainerAdlogo.setVisibility(VISIBLE);
                mBottomContainerAdlogo.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivityForUrl("https://union.baidu.com");
                    }
                });
                mBottomSecondView.setClickable(true);
                mBottomSecondView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivityForUrl("https://union.baidu.com");
                    }
                });
                // 下载广告
                mBottomContainer.setVisibility(data.isNeedDownloadApp() ? GONE : VISIBLE);
                mAppDownloadContainer.setVisibility(data.isNeedDownloadApp() ? VISIBLE : GONE);
                mAppName.setText(data.getBrandName());
                mAppVersion.setText("版本:" + data.getAppVersion());
                mAppPublisher.setText(data.getAppPublisher());
                mAppPrivacyLink.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // show app privacy
                        startActivityForUrl(data.getAppPrivacyUrl());
                    }
                });
                mAppPermissionLink.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // show app permission list
                        startActivityForUrl(data.getAppPermissionUrl());
                    }
                });
            } else {
                mAppDownloadContainer.setVisibility(GONE);
                mBottomContainer.setVisibility(VISIBLE);
                mBottomContainerAdlogo.setVisibility(GONE);
                // 内容类型
                if ("news".equalsIgnoreCase(mType)) {
                    // 资讯
                    mBottomFirstText = data.getAuthor();
                    mBottomSecondText = getTransformedDateString(data.getUpdateTime());
                } else if ("image".equalsIgnoreCase(mType)) {
                    // 图集
                    mBottomFirstText = data.getAuthor();
                    mBottomSecondText = getTransformedDateString(data.getUpdateTime());
                } else if ("video".equalsIgnoreCase(mType)) {
                    // 视频
                    
                    mBottomFirstText = data.getAuthor();
                    mBottomSecondText = getFormatPlayCounts(data.getPlayCounts());
                }
            }
            bindView(aq);
        }
    }

    private String converSceond2Minute(int duration) {
        int second =  duration % 60;
        int minute =  duration / 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minute, second);
    }

    private void readImageUrls(IBasicCPUData data) {
        List<String> imageList = data.getImageUrls(); // 广告图片
        List<String> smallImageList = data.getSmallImageUrls(); // 内容图片


        if (smallImageList != null && smallImageList.size() == 3) {
            // 三图内容
            mLeftImageUrl = smallImageList.get(0);
            mMidImageUrl = smallImageList.get(1);
            mRightImageUrl = smallImageList.get(2);
        } else if (imageList != null && imageList.size() == 3) {
            // 三图广告
            mLeftImageUrl = imageList.get(0);
            mMidImageUrl = imageList.get(1);
            mRightImageUrl =  imageList.get(2);
        } else if (smallImageList != null && smallImageList.size() == 1) {
            // 内容图片只有1张
            mLeftImageUrl = smallImageList.get(0);
        } else if (smallImageList != null && smallImageList.size() == 2) {
            // 内容图片只有2张
            mLeftImageUrl = smallImageList.get(0);
            mMidImageUrl = smallImageList.get(1);
        } else if (imageList != null && imageList.size() == 1) {
            // 一图广告
            mLeftImageUrl = imageList.get(0);
        } else if (imageList != null && imageList.size() == 2) {
            // 二图广告
            mLeftImageUrl = imageList.get(0);
            mMidImageUrl = imageList.get(1);
        } else {
            // 视频缩略图
            mLeftImageUrl = data.getThumbUrl();
            mMidImageUrl = "";
            mRightImageUrl = "";
        }
    }

    private void bindView(AQuery aq) {
        if (aq != null) {
            boolean isAd = "ad".equalsIgnoreCase(mType);
            boolean isVideo = "video".equalsIgnoreCase(mType);
            bindData2View(mTopTextView, aq, mTitle, 1);
            if (!TextUtils.isEmpty(mMidImageUrl) && !TextUtils.isEmpty(mRightImageUrl)) {
                // 三图
                bindData2View(mImageLeft, aq, mLeftImageUrl, 2);
                bindData2View(mImageMid, aq, mMidImageUrl, 2);
                bindData2View(mImageRight, aq, mRightImageUrl, 2);
                mImageSingleBig.setVisibility(GONE);
            } else {
                // 大图
                bindData2View(mImageSingleBig, aq, mLeftImageUrl, 2);
                mImageLeft.setVisibility(GONE);
                mImageMid.setVisibility(GONE);
                mImageRight.setVisibility(GONE);
            }
            mVideoTime.setText(mVideoDuration);
            mBtnPlayVideo.setVisibility(isVideo ? VISIBLE : GONE);
            mVideoTime.setVisibility(isVideo ? VISIBLE : GONE);
            bindData2View(mBottom00FirstView, aq, mBottom00FirstText,1);
            bindData2View(mBottomFirstView, aq, mBottomFirstText, 1);
            bindData2View(mBottomSecondView, aq, mBottomSecondText, 1);
            // mDislikeButton.setVisibility(isAd ? INVISIBLE : VISIBLE);
        }
    }

    /**
     * @param view
     * @param aq
     * @param data
     * @param dataType 数据类型：1-字符串；2-图片URL；
     */
    private void bindData2View(View view, AQuery aq, String data, int dataType) {
        if (TextUtils.isEmpty(data)) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            if (dataType == 1) {
                aq.id(view).text(data);
            } else if (dataType == 2) {
                // 通过callback的方式渲染ImageView，避免AQuery直接渲染后将View.GONE的控件显示出来
                aq.id(view).image(data, false, true, 0, 0,
                        new BitmapAjaxCallback() {
                            @Override
                            protected void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status) {
                                if (iv.getVisibility() == View.VISIBLE) {
                                    iv.setImageBitmap(bm);
                                }
                            }
                        });
            }
        }
    }

    private String getTransformedDateString(String updateTime) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date date = simpleDateFormat.parse(updateTime);
            if (date == null) {
                return updateTime;
            }
            long updateTimeMilli = date.getTime();
            long timeNowMilli = System.currentTimeMillis();
            if (timeNowMilli < updateTimeMilli) {
                return updateTime;
            } else {
                long gapSecond = (timeNowMilli - updateTimeMilli) / 1000;
                if (gapSecond  < TIME_SECOND_MINUTE) {
                    return "刚刚";
                } else if (gapSecond < TIME_SECOND_HOUR) {
                    int minute = (int) (gapSecond / TIME_SECOND_MINUTE);
                    return minute + "分钟前";
                } else if (gapSecond < TIME_SECOND_DAY) {
                    int hour = (int) (gapSecond / TIME_SECOND_HOUR);
                    return hour + "小时前";
                } else if (gapSecond < TIME_SECOND_MONTH) {
                    int day = (int) (gapSecond / TIME_SECOND_DAY);
                    return day + "天前";
                } else if (gapSecond < TIME_SECOND_YEAR) {
                    int month = (int) (gapSecond / TIME_SECOND_MONTH);
                    return month + "月前";
                } else {
                    int year = (int) (gapSecond / TIME_SECOND_YEAR);
                    return year + "年前";
                }
            }
        } catch (Throwable tr) {
            return updateTime;
        }
    }

    private String getFormatPlayCounts(int playCounts) {
        StringBuilder sb = new StringBuilder("播放: ");
        if (playCounts < 0) {
            sb.append(0);
        } else if (playCounts < 10000) {
            sb.append(playCounts);
        } else {
            sb.append(playCounts / 10000);
            int remain = playCounts % 10000;
            if (remain > 0) {
                sb.append(".").append(remain / 1000);
            }
            sb.append("万");
        }
        return sb.toString();
    }

    private void startActivityForUrl(String url) {
        try {
            Intent intent = new Intent();
            intent.setData(Uri.parse(url));
            intent.setAction(Intent.ACTION_VIEW);
            getContext().startActivity(intent);
        } catch (Throwable tr) {
            Log.e("NativeCPUView", "Show url error: " + tr.getMessage());
        }
    }


}
