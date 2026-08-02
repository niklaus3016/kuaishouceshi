package com.baidu.mobads.demo.main.feeds.video;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.androidquery.AQuery;
import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BDRefinedActButton;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.INativeVideoListener;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.XAdNativeResponse;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.XNativeView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 信息流视频广告的接入示例，演示了信息流视频播放器的接入方式，以及视频播放器的一些基本操作
 */
public class FeedNativeVideoActivity extends Activity {

    private static final String TAG = "FeedNativeVideoActivity";
    /** 广告位id */
    private String mAdPlaceId = "2362913";
    private NativeResponse mNativeAd;
    private BaiduNativeManager mBaiduNativeManager;
    private VideoAdapter mVideoAdapter;
    private XNativeView mFirstNativeView;
    private List<Object> mList = new ArrayList<Object>();
    private float density;
    private boolean onlyFetch = false;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.feed_native_video_list);
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        density = metric.density;
        ListView listView = findViewById(R.id.feed_native_video_list);
        mVideoAdapter = new VideoAdapter();
        listView.setAdapter(mVideoAdapter);
        /**
         * Step 1. 创建BaiduNative对象，参数分别为： 上下文context，广告位ID
         * 注意：请将adPlaceId替换为自己的广告位ID
         * 注意：信息流广告对象，与广告位id一一对应，同一个对象可以多次发起请求
         */
        mBaiduNativeManager = new BaiduNativeManager(this, mAdPlaceId, true);
        fetchAd();
    }

    private void fetchAd() {
        // 创建requestParameters对象，传递请求参数
        final RequestParameters requestParameters = new RequestParameters.Builder()
                .setWidth((int) (640 * density))
                .setHeight((int) (360 * density))
                .addCustExt("cust_Key_这是key", "cust_Value_这是Value" + System.currentTimeMillis())
                .addCustExt("AAAAAAA", "aaaaaa")
                .addCustExt(ArticleInfo.PAGE_TITLE, "真测试书名")
                .build();
        /**
         * 设置仅在Wi-Fi环境下缓存视频文件，默认在4G下也会缓存视频文件
         * 注意：需要提前打开缓存功能，此设置才有意义
         */
        mBaiduNativeManager.setCacheVideoOnlyWifi(true);
        // 请求广告
        final BaiduNativeManager.FeedAdListener feedAdListener = new BaiduNativeManager.FeedAdListener() {
            @Override
            public void onNativeFail(int errorCode, String message, NativeResponse nativeResponse) {
                Log.i(TAG, "onLoadFail:" + message + "errorCode:" + errorCode);
                Toast.makeText(FeedNativeVideoActivity.this, "没有收到视频广告，请检查",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onVideoDownloadSuccess() {
                Log.i(TAG, "onVideoDownloadSuccess");
                Toast.makeText(FeedNativeVideoActivity.this, "视频缓存成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoDownloadFailed() {
                Log.i(TAG, "onVideoDownloadFailed");
            }

            @Override
            public void onLpClosed() {
                Log.i(TAG, "onLpClosed");
            }

            @Override
            public void onNativeLoad(List<NativeResponse> nativeResponses) {
                Log.i(TAG, "onADLoaded");
                if (nativeResponses != null && nativeResponses.size() > 0) {
                    // 这里每次都取第一条广告来做展示,模拟多条广告;实际开发过程中需要开发者自己处理
                    mNativeAd = nativeResponses.get(0);

                    mList.addAll(nativeResponses);
                    Toast.makeText(FeedNativeVideoActivity.this,
                            String.format("请求到%s条广告!", nativeResponses.size()),
                            Toast.LENGTH_SHORT).show();
                    if (!onlyFetch) {
                        // 展现请求到的广告
                        mVideoAdapter.notifyDataListChanged(mList);
                    }
                }
            }

            @Override
            public void onNoAd(int code, String msg, NativeResponse nativeResponse) {
                Log.i(TAG, "onNoAd");
            }
        };
        mBaiduNativeManager.loadFeedAd(requestParameters,feedAdListener);
        Toast.makeText(this, "开始请求广告...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0x01, Menu.NONE, "后台请求广告");
        menu.add(Menu.NONE, 0x02, Menu.NONE, "缓存广告视频");
        menu.add(Menu.NONE, 0x03, Menu.NONE, "渲 染 广 告");
        return true;
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case 0x01:
                // 请求广告
                onlyFetch = true;
                mList.clear();
                mVideoAdapter.notifyDataListChanged(mList);
                fetchAd();
                return true;
            case 0x02:
                // 缓存视频
                Toast.makeText(this, "缓存上一次请求到的视频", Toast.LENGTH_SHORT).show();
                if (mBaiduNativeManager != null && mNativeAd != null) {
                    ((XAdNativeResponse) mNativeAd).preloadVideoMaterial();
                }
                return true;
            case 0x03:
                // 展现请求到的广告
                mVideoAdapter.notifyDataListChanged(mList);
                return true;
            default:
                // nothing
        }
        return false;
    }

    class VideoAdapter extends BaseAdapter {

        private List<Object> mDataList = new ArrayList<Object>();

        public void notifyDataListChanged(List<Object> dataList) {
            mDataList.clear();
            mDataList.addAll(dataList);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 注意：信息流广告的返回可以同时包含多种类型：视频、图文、纯文字等
            // 视频类型的广告参考如下代码进行渲染，其他类型（图片）可以参考FeedAdActivity的渲染方式
            Log.e(TAG, "getView=" + position);
            ViewHolder viewHolder = null;
            // 视频广告类型，媒体可以自定义，SDK提供了渲染视频的控件，该控件同时区分是大图还是视频，开发者只需要传入广告对象即可
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.feed_native_video, null);
                viewHolder = new ViewHolder(convertView);
                // 【可选配置】是否显示视频播放的进度条
                viewHolder.mNativeView.setShowProgress(true);
                viewHolder.mNativeView.setProgressBarColor(Color.GRAY);
                viewHolder.mNativeView.setProgressBackgroundColor(Color.BLACK);
                viewHolder.mNativeView.setProgressHeightInDp(1);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final AQuery aq = new AQuery(convertView);
            final NativeResponse ad = (NativeResponse) mDataList.get(position);
            aq.id(viewHolder.mIcon).image(ad.getIconUrl(),
                    false, true);
            aq.id(viewHolder.mDescription).text(ad.getDesc());
            String title = ad.getTitle();
            if (ad.getAdMaterialType().equals(NativeResponse.MaterialType.LIVE.getValue())) {
                title = ad.getAuthorName();
            }
            aq.id(viewHolder.mTitle).text(title);
            aq.id(viewHolder.mBaiduLog).image(ad.getBaiduLogoUrl());
            setUnionLogoClick(viewHolder.mBaiduLog, ad);
            aq.id(viewHolder.mAdLog).image(ad.getAdLogoUrl());
            setUnionLogoClick(viewHolder.mAdLog, ad);
            // 弹幕组件
            final RelativeLayout bulletContainer = convertView.findViewById(R.id.bullet_view_container);
            if (bulletContainer != null) {
                bulletContainer.removeAllViews();
                bulletContainer.setVisibility(View.VISIBLE);
                // 渲染弹幕组件应尽量在广告素材的上层，弹幕播放完成会自动移除自身
                // @param width 弹幕容器宽度，请使用MATCH_PARENT或大等于120dp的数值
                // @param height 弹幕容器高度，请使用WRAP_CONTENT或0，让容器自适应弹幕的高度
                View bulletView = ad.renderBulletView(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                // 若广告不支持弹幕组件，则返回空
                if (bulletView != null) {
                    bulletContainer.addView(bulletView);
                }
            }
            // 设置广告数据给视频播放组件
            // 注意：渲染视频广告需要关注混淆, 否则会影响视频播放，混淆配置参考接入文档的说明
            viewHolder.mNativeView.setNativeItem(ad);
            // 需要启用下载弹窗时可置为true
            viewHolder.mNativeView.setUseDownloadFrame(true);
            viewHolder.mNativeView.setVideoMute(false);
            // 设置该监听，在点击播放按钮之后，用户知道当前播放的是哪个视频组件，拿到当前播放的视频组件，可以主动控制视频的播放和暂停（根据媒体自己的业务场景）
            viewHolder.mNativeView.setNativeViewClickListener(new XNativeView.INativeViewClickListener() {
                @Override
                public void onNativeViewClick(XNativeView nativeView) {
                    Log.e(TAG, "当前播放的视频组件是=" + nativeView);
                }
            });

            BDRefinedActButton button = convertView.findViewById(R.id.native_refinedbtn);
            button.setAdData(ad);

            ImageView exitView= convertView.findViewById(R.id.ad_exit);
            exitView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 删除次item的数据并且刷新
                    mDataList.remove(ad);
                    notifyDataSetChanged();
                }
            });
            // 如果为下载广告，展示四个字段，并且给4个新字段进行赋值
            if(isDownloadAd(ad)) {
                viewHolder.appLayout.setVisibility(View.VISIBLE);
                viewHolder.publisher.setText(ad.getPublisher());
                viewHolder.appVersion.setText("版本 " + ad.getAppVersion());
                // 给隐私添加点击事件
                viewHolder.appPrivacy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ad.privacyClick();
                    }
                });
                // 给权限添加点击事件
                viewHolder.appPermission.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ad.permissionClick();
                    }
                });
                // 给功能添加点击事件
                viewHolder.appFunction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ad.functionClick();
                    }
                });
                // 给隐私、权限注册事件回调
                final XNativeView nativeView = viewHolder.mNativeView;
                ad.setAdPrivacyListener(new NativeResponse.AdDownloadWindowListener() {
                    @Override
                    public void adDownloadWindowShow() {
                        Log.i(TAG, "adDownloadWindowShow: ");
                        // 使弹窗出现时暂停
                        nativeView.pause();
                    }

                    @Override
                    public void adDownloadWindowClose() {
                        Log.i(TAG, "adDownloadWindowClose: ");
                        // 使弹窗消失时继续
                        nativeView.resume();
                    }

                    @Override
                    public void onADPrivacyClick() {
                        Log.i(TAG, "onADPrivacyClick: " + ad.getTitle());
                    }

                    @Override
                    public void onADFunctionClick() {
                        Log.i(TAG, "onADFunctionClick: " + ad.getTitle());
                    }

                    @Override
                    public void onADPermissionShow() {
                        Log.i(TAG, "onADPermissionShow: " + ad.getTitle());
                    }

                    @Override
                    public void onADPermissionClose() {
                        Log.i(TAG, "onADPermissionClose: " + ad.getTitle());
                    }
                });
            }
            viewHolder.mNativeView.setNativeVideoListener(new INativeVideoListener() {
                @Override
                public void onCompletion() {
                    Log.i(TAG, "onCompletion: " + ad.getTitle());
                }

                @Override
                public void onError() {
                    Log.i(TAG, "onError: " + ad.getTitle());
                }

                @Override
                public void onRenderingStart() {
                    Log.i(TAG, "onRenderingStart: " + ad.getTitle());
                }

                @Override
                public void onPause() {
                    Log.i(TAG, "onPause: " + ad.getTitle());
                }

                @Override
                public void onResume() {
                    Log.i(TAG, "onResume: " + ad.getTitle());
                }
            });
            List<View> clickViews = new ArrayList<>();
            List<View> creativeViews = new ArrayList<>();
            clickViews.add(convertView);
            creativeViews.add(button);
            /**
             * 注册可点击的View，点击和曝光会在内部完成
             * @Param view 广告容器或广告View
             * @Param clickViews 可点击的View，默认展示下载整改弹框
             * @Param creativeViews 带有广告文案之类的View，点击不会触发下载整改弹框
             * @Param interactionListener 点击、曝光回调
             */
            ad.registerViewForInteraction(convertView, clickViews, creativeViews,
                    new NativeResponse.AdInteractionListener() {
                @Override
                public void onAdClick() {
                    Log.i(TAG, "onAdClick: " + ad.getTitle());
                }

                @Override
                public void onADExposed() {
                    Log.i(TAG, "onADExposed: " + ad.getTitle());
                }

                @Override
                public void onADExposureFailed(int reason) {
                    Log.i(TAG , "onADExposureFailed: " + reason);
                }

                @Override
                public void onADStatusChanged() {
                    Log.i(TAG, "onADStatusChanged: " + ad.getTitle());
                }

                @Override
                public void onAdUnionClick() {
                    Log.i(TAG, "onAdUnionClick: " + ad.getTitle());
                }
            });
            // 让第一个视频尝试自动播放（如果mssp上配置为自动播放，那么调用该方法会直接播放该视频）
            if (mFirstNativeView == null && position == 0) {
                mFirstNativeView = viewHolder.mNativeView;
                // 自动播放api
                mFirstNativeView.render();
            } else {
                viewHolder.mNativeView.render();
            }
            return convertView;
        }

        private class ViewHolder {
            TextView mTitle;
            TextView mDescription;
            ImageView mAdLog;
            ImageView mBaiduLog;
            ImageView mIcon;
            XNativeView mNativeView;
            LinearLayout appLayout;
            TextView publisher;
            TextView appVersion;
            TextView appPrivacy;
            TextView appPermission;
            TextView appFunction;
            public ViewHolder(View convertView) {
                appLayout=convertView.findViewById(R.id.native_app_layout);
                appVersion=convertView.findViewById(R.id.native_version);
                appPrivacy=convertView.findViewById(R.id.native_privacy);
                appFunction=convertView.findViewById(R.id.native_function);
                appPermission=convertView.findViewById(R.id.native_permission);
                publisher=convertView.findViewById(R.id.native_publisher);
                mTitle = (TextView) convertView.findViewById(R.id.native_title);
                mDescription = (TextView) convertView.findViewById(R.id.native_text);
                mNativeView = (XNativeView) convertView.findViewById(R.id.videoview);
                mBaiduLog = (ImageView) convertView.findViewById(R.id.native_baidulogo);
                mAdLog = (ImageView) convertView.findViewById(R.id.native_adlogo);
                mIcon = (ImageView) convertView.findViewById(R.id.native_icon_image);
            }
        }
    }

    // 点击联盟logo打开官网
    private void setUnionLogoClick(ImageView logo, final NativeResponse nrAd) {
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nrAd.unionLogoClick();
            }
        });
    }

    /**
     * 可见性区域检测
     *
     * @param view                需要检测的View
     * @param minPercentageViewed 最小百分比 50 百分之50
     *
     * @return 是否满足可见性检测
     */
    private static boolean isVisible(final View view, final int minPercentageViewed) {
        if (view == null || view.getVisibility() != View.VISIBLE
                || view.getParent() == null) {
            return false;
        }
        Rect mClipRect = new Rect();

        if (!view.getGlobalVisibleRect(mClipRect)) {
            // Not visible
            return false;
        }

        final long visibleViewArea = (long) mClipRect.height()
                * mClipRect.width();
        final long totalViewArea = (long) view.getHeight() * view.getWidth();

        if (totalViewArea <= 0) {
            return false;
        }

        return 100 * visibleViewArea >= minPercentageViewed * totalViewArea;
    }

    private boolean isDownloadAd(NativeResponse nrAd) {
        return nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                && !TextUtils.isEmpty(nrAd.getAppVersion()) && !TextUtils.isEmpty(nrAd.getPublisher())
                && !TextUtils.isEmpty(nrAd.getAppPrivacyLink()) && !TextUtils.isEmpty(nrAd.getAppPermissionLink());
    }
}

