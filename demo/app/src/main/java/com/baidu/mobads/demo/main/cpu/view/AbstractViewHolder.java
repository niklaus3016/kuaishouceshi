package com.baidu.mobads.demo.main.cpu.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.CustomProgressButton;
import com.baidu.mobads.sdk.api.IBasicCPUData;

import java.util.List;

import static com.baidu.mobads.demo.main.cpu.activity.NativeCPUAdActivity.TAG;
import static com.baidu.mobads.demo.main.mediaExamples.news.utils.FeedParseHelper.getFormatPlayCounts;
import static com.baidu.mobads.demo.main.mediaExamples.news.utils.FeedParseHelper.getTransformedDateString;

/**
 * author: ZhangYubin
 * date: 2021/2/18 3:20 PM
 * desc:
 */
public abstract class AbstractViewHolder extends RecyclerView.ViewHolder{
    protected TextView titleView;

    protected TextView bottom_00first_text;
    protected TextView bottom_first_text;
    protected ImageView bottom_container_adlogo;
    protected TextView bottom_second_text;
    protected TextView bottom_container_mislike;
    protected TextView bottom_container_mislike2;
    protected Context mCtx;
    protected List<String> imageList; // 广告图片
    protected List<String> smallImageList; // 内容图片

    protected RelativeLayout downloadContainer;
    protected TextView mAppNameTv;
    protected TextView mAppVerTv;
    protected TextView mAppPrivacyTv;
    protected TextView mAppPermissionTv;
    protected TextView mAppPublisherTv;
    protected CustomProgressButton mApdownloadTv;

    protected LinearLayout mInfoContainer;

    public AbstractViewHolder(View view) {
        super(view);
        mCtx = view.getContext();
        titleView = view.findViewById(R.id.textView);
        bottom_00first_text = view.findViewById(R.id.bottom_00first_text);
        bottom_first_text = view.findViewById(R.id.bottom_first_text);
        bottom_container_adlogo = view.findViewById(R.id.bottom_container_adlogo);
        bottom_second_text = view.findViewById(R.id.bottom_second_text);
        bottom_container_mislike = view.findViewById(R.id.bottom_container_mislike);
        bottom_container_mislike2 = view.findViewById(R.id.bottom_container_mislike2);
        downloadContainer = view.findViewById(R.id.download_container);
        mAppNameTv = view.findViewById(R.id.app_name);
        mAppVerTv = view.findViewById(R.id.app_ver);
        mAppPrivacyTv = view.findViewById(R.id.privacy);
        mAppPermissionTv = view.findViewById(R.id.permission);
        mAppPublisherTv = view.findViewById(R.id.publisher);
        mApdownloadTv = view.findViewById(R.id.download);

        mInfoContainer = view.findViewById(R.id.bottom_info_container);

    }


    public void initWidgetWithData(final IBasicCPUData nrAd, final int position) {
        imageList = nrAd.getImageUrls();
        smallImageList = nrAd.getSmallImageUrls();
        titleView.setText(nrAd.getTitle());
        if (downloadContainer != null) {
            downloadContainer.setVisibility(View.GONE);
        }
        String type = nrAd.getType();
        if (type.equals("news")) {
            bottom_first_text.setText(nrAd.getAuthor());
            bottom_second_text.setText(getTransformedDateString(nrAd.getUpdateTime()));
            bottom_container_adlogo.setVisibility(View.GONE);
            bottom_container_mislike.setVisibility(View.GONE);
        } else if (type.equals("image")) {
            bottom_first_text.setText(nrAd.getAuthor());
            bottom_second_text.setText(getTransformedDateString(nrAd.getUpdateTime()));
            bottom_container_adlogo.setVisibility(View.GONE);
            bottom_container_mislike.setVisibility(View.GONE);
        } else if (type.equals("video")) {
            bottom_first_text.setText(nrAd.getAuthor());
            bottom_second_text.setText(getFormatPlayCounts(nrAd.getPlayCounts()));
            bottom_container_adlogo.setVisibility(View.GONE);
            bottom_container_mislike.setVisibility(View.GONE);
        } else {
            bottom_first_text.setText("精选推荐");
            bottom_second_text.setText("广告");
            bottom_container_adlogo.setVisibility(View.VISIBLE);
            bottom_container_mislike.setVisibility(View.VISIBLE);

            bottom_second_text.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForUrl("https://union.baidu.com");
                }
            });

            bottom_container_adlogo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForUrl("https://union.baidu.com");
                }
            });

            bottom_container_mislike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 处理 "不喜欢" icon 的API
                    nrAd.handleDislikeClick(bottom_container_mislike, position);
                }
            });

            if (nrAd.isNeedDownloadApp()) {

                if (mApdownloadTv != null) {
                    mApdownloadTv.initWithCPUResponse(nrAd);
                    mApdownloadTv.setTextColor(Color.parseColor("#FFFFFF"));
                    // 字体大小适配屏幕
                    DisplayMetrics metrics = mCtx.getResources().getDisplayMetrics();
                    int textSize = (int) (12 * metrics.scaledDensity + 0.5f);
                    mApdownloadTv.setTextSize(textSize);
                    mApdownloadTv.setTypeFace(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC));
                    mApdownloadTv.setForegroundColor(Color.parseColor("#3388FF"));
                    mApdownloadTv.setBackgroundColor(Color.parseColor("#D7E6FF"));
                    // 设置长按取消下载能力
                    mApdownloadTv.useLongClick(true);
                }

                if (mAppNameTv != null) {
                    mAppNameTv.setText(nrAd.getBrandName());
                }
               if (mAppVerTv != null) {
                   mAppVerTv.setText("版本:" + nrAd.getAppVersion());
               }
               if (mAppPermissionTv != null) {
                   mAppPermissionTv.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           startActivityForUrl(nrAd.getAppPermissionUrl());
                       }
                   });
               }
               if (bottom_container_mislike2 != null) {
                   bottom_container_mislike2.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           // 处理 "不喜欢" icon 的API
                           nrAd.handleDislikeClick(bottom_container_mislike2, position);
                       }
                   });
               }
               if (mAppPublisherTv != null) {
                   mAppPublisherTv.setText(nrAd.getAppPublisher());
               }

               if (mAppPrivacyTv != null) {
                   mAppPrivacyTv.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           // show app privacy
                           startActivityForUrl(nrAd.getAppPrivacyUrl());
                       }
                   });
               }

                if (downloadContainer != null) {
                    downloadContainer.setVisibility(View.VISIBLE);
                }
                if (mInfoContainer != null) {
                    mInfoContainer.setVisibility(View.GONE);
                }

            } else {
                if (mInfoContainer != null) {
                    mInfoContainer.setVisibility(View.VISIBLE);
                }
                if (downloadContainer != null) {
                    downloadContainer.setVisibility(View.GONE);
                }

            }

        }
        bottom_00first_text.setText(nrAd.getLabel());
        if (TextUtils.isEmpty(nrAd.getLabel())) {
            bottom_00first_text.setVisibility(View.GONE);
        }

//        /**
//         * 单条内容设置回调
//         */
//        nrAd.setStatusListener(new IBasicCPUData.CpuNativeStatusCB() {
//            @Override
//            public void onAdDownloadWindowShow() {
//                Log.d(TAG, "onAdDownloadWindowShow: ");
//            }
//
//            @Override
//            public void onPermissionShow() {
//                Log.d(TAG, "onPermissionShow: ");
//            }
//
//            @Override
//            public void onPermissionClose() {
//                Log.d(TAG, "onPermissionClose: ");
//            }
//
//            @Override
//            public void onPrivacyClick() {
//                Log.d(TAG, "onPrivacyClick: ");
//            }
//
//            @Override
//            public void onPrivacyLpClose() {
//                Log.d(TAG, "onPrivacyLpClose: ");
//            }
//
//            /**
//             *  内容/广告 的 展现、点击行为的告知
//             * @param act
//             */
//            @Override
//            public void onNotifyPerformance(String act) {
//                Log.d(TAG, "performance: "  + act);
//            }
//        });

        ////////////////////////////////////////////



    }

    public void setAttribute(int bg, int textSize) {
        switch (textSize) {
            case 13:
                bottom_container_adlogo.setScaleX(0.7f);
                bottom_container_adlogo.setScaleY(0.7f);
                break;
            case 18:
                bottom_container_adlogo.setScaleX(1.0f);
                bottom_container_adlogo.setScaleY(1.0f);
                break;
            case 23:
                bottom_container_adlogo.setScaleX(1.5f);
                bottom_container_adlogo.setScaleY(1.5f);
                break;
        }

        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        bottom_00first_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 6);
        bottom_first_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 4);
        bottom_second_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 4);





        int impureWhite =  Color.parseColor("#CBCBCB");
        int topTextColor = bg == Color.WHITE ? Color.BLACK : impureWhite;
        int bottomTextColor = bg == Color.WHITE ? Color.GRAY : impureWhite;
        titleView.setTextColor(topTextColor);
        bottom_first_text.setTextColor(bottomTextColor);
        bottom_second_text.setTextColor(bottomTextColor);
        if (mAppNameTv != null) {
            mAppNameTv.setTextColor(bottomTextColor);
            mAppNameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 8);
        }
        if (mAppVerTv != null) {
            mAppVerTv.setTextColor(bottomTextColor);
            mAppVerTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 8);
        }
        if (mAppPermissionTv != null) {
            mAppPermissionTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 8);
        }
        if (mAppPrivacyTv != null) {
            mAppPrivacyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 8);
        }
        if (mAppPublisherTv != null) {
            mAppPublisherTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 8);
        }

    }


    private void startActivityForUrl(String url) {
        try {
            Intent intent = new Intent();
            intent.setData(Uri.parse(url));
            intent.setAction(Intent.ACTION_VIEW);
            mCtx.startActivity(intent);
        } catch (Throwable tr) {
            Log.e(TAG, "Show url error: " + tr.getMessage());
        }
    }
}
