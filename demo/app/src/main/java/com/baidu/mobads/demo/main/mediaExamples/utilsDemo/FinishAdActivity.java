package com.baidu.mobads.demo.main.mediaExamples.utilsDemo;

import android.app.Activity;
import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * 这个是媒体接入示例中信息流工具样式的结束页面的
 * 全屏广告接入示例。
 * * 需要注意的点为：1.发送点击日志2.发送展现日志
 */
public class FinishAdActivity extends Activity {
    /** 大图+ICON+描述的广告位 */
    private static final String BIG_PIC_AD_PLACE_ID = "2058628";
    /** 用于请求广告 */
    private BaiduNativeManager mBaiduNativeManager;
    /** 显示广告的大view */
    RelativeLayout mAdView;
    /** 广告右上角的退出按钮 */
    ImageView mExsit;
    /** 广告正中间的图 */
    ImageView mAdImage;
    /** 广告的icon */
    ImageView mAdIcon;
    /** 广告的标题 */
    TextView mTitle;
    /** 广告的内容 */
    TextView mContent;
    /** 广告触发的按钮 */
    Button mButton;
    /** 百度广告的logo */
    ImageView mAdLogo;
    /** 百香果logo */
    ImageView mBaiduLogo;
    /** 控制刷新的组件 */
    private SwipeRefreshLayout mRefreshLayout;
    /** 下载明示信息的布局*/
    private RelativeLayout downloadLayout;
    /** 下载明示信息版本号*/
    private TextView version;
    /** 下载明示信息公司名*/
    private TextView Company;
    /** 下载明示信息隐私*/
    private TextView Privacy;
    /** 下载明示信息权限*/
    private TextView Permission;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_utils_demo_finish);
        // 初始化各个控件
        init();
        mBaiduNativeManager = new BaiduNativeManager(this.getApplicationContext(), BIG_PIC_AD_PLACE_ID);
        // 添加退出按钮的点击事件
        initExitButton();
        // 进行请求然后赋值
        loadFeedAd();


    }

    /** 初始化各个组件 */
    private void init() {
        /** 显示广告的view*/
        mAdView = findViewById(R.id.demo_utils_finish_ad_view);
        /** 广告右上角的退出按钮*/
        mExsit = findViewById(R.id.demo_utils_finish_exist);
        /** 广告正中间的图*/
        mAdImage = findViewById(R.id.demo_utils_finish_image);
        /** 广告的icon*/
        mAdIcon = findViewById(R.id.demo_utils_finish_icon);
        /** 广告的标题*/
        mTitle = findViewById(R.id.demo_utils_finish_title);
        /** 广告的内容*/
        mContent = findViewById(R.id.demo_utils_finish_content);
        /** 广告触发的按钮*/
        mButton = findViewById(R.id.demo_utils_finish_button);
        /** 百度广告的logo*/
        mAdLogo = findViewById(R.id.demo_utils_finish_ad_logo);
        /** 百香果logo*/
        mBaiduLogo = findViewById(R.id.demo_utils_finish_baidu_logo);
        /** 下载明示信息的布局*/
        downloadLayout = findViewById(R.id.demo_utils_finish_download);
        /** 下载明示信息版本号*/
        version = findViewById(R.id.demo_utils_finish_version);
        /** 下载明示信息公司名*/
        Company = findViewById(R.id.demo_utils_finish_company);
        /** 下载明示信息隐私*/
        Privacy = findViewById(R.id.demo_utils_finish_Privacy);
        /** 下载明示信息权限*/
        Permission =findViewById(R.id.demo_utils_finish_Permission);
        /** 为刷新的控件添加事件*/
        mRefreshLayout = findViewById(R.id.demo_utils_finish_swipe);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 刷新时进行的操作
                loadFeedAd();
            }
        });
    }
    /** 初始化下载明示信息*/
    private void initDownload(final NativeResponse mResponse) {
        if(!TextUtils.isEmpty(mResponse.getAppVersion())&&
                !TextUtils.isEmpty(mResponse.getPublisher()) &&
                !TextUtils.isEmpty(mResponse.getAppPrivacyLink())&&
                !TextUtils.isEmpty(mResponse.getAppPermissionLink())) {
            downloadLayout.setVisibility(View.VISIBLE);
            version.setText("版本 "+mResponse.getAppVersion());
            Company.setText(mResponse.getPublisher());
            // 添加隐私权限的点击事件
            Privacy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mResponse.privacyClick();
                }
            });
            Permission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mResponse.permissionClick();
                }
            });
        } else {
            // 不是下载类广告隐藏明示信息
            downloadLayout.setVisibility(View.GONE);
        }
    }
    /** 进行广告的请求 */
    private void loadFeedAd() {
        // 若与百度进行相关合作，可使用如下接口上报广告的上下文
        RequestParameters requestParameters = new RequestParameters.Builder()
                .build();

        mBaiduNativeManager.loadFeedAd(requestParameters, new BaiduNativeManager.FeedAdListener() {
                    @Override
                    public void onNativeLoad(List<NativeResponse> nativeResponses) {
                        mRefreshLayout.setRefreshing(false);
                        // 一个广告只允许展现一次，多次展现、点击只会计入一次
                        if (nativeResponses != null && nativeResponses.size() > 0) {
                            //取出数据,这里仅为示例展示，仅仅取出第一个数据，实际情况有多条广告，应分别进行取出
                            final NativeResponse mResponse = nativeResponses.get(0);
                            //给各个控件进行赋值
                            Glide.with(FinishAdActivity.this).load(mResponse.getImageUrl()).into(mAdImage);
                            Glide.with(FinishAdActivity.this).load(mResponse.getAdLogoUrl()).into(mAdLogo);
                            Glide.with(FinishAdActivity.this).load(mResponse.getBaiduLogoUrl()).into(mBaiduLogo);
                            // 这里用glide来设置圆形
                            RequestOptions options = RequestOptions.circleCropTransform()
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)//不做磁盘缓存
                                    .skipMemoryCache(true);//不做内存缓存
                            Glide.with(FinishAdActivity.this).load(mResponse.getIconUrl()) //图片地址
                                    .apply(options)
                                    .into(mAdIcon);
                            mContent.setText(mResponse.getTitle());
                            // 如果没有品牌名，就把品牌名改为精选推荐
                            if (TextUtils.isEmpty(mResponse.getBrandName())) {
                                // 修改平牌名
                                mTitle.setText("精选推荐");
                            } else {
                                // 修改平牌名
                                mTitle.setText(mResponse.getBrandName());
                            }
                            // 初始化下载明示信息的控件
                            initDownload(mResponse);
                            // 为按钮赋值
                            mButton.setText(getBtnText(mResponse));
                            // 发送展现日志
                            // 这里面的第一个值为view，就是展示广告的view，一般为外层的RelativeLayout和LinearLayout
                            List<View> clickViews = new ArrayList<>();
                            List<View> creativeViews = new ArrayList<>();
                            clickViews.add(mAdView);
                            clickViews.add(mTitle);
                            clickViews.add(mAdIcon);
                            clickViews.add(mAdImage);
                            clickViews.add(mContent);
                            creativeViews.add(mButton);
                            // 为广告中所有的元素都添加点击
                            mResponse.registerViewForInteraction(mAdView, clickViews, creativeViews,
                                    new NativeResponse.AdInteractionListener() {
                                        @Override
                                        public void onAdClick() {
                                            /** 信息流点击回调 */
                                        }

                                        @Override
                                        public void onADExposed() {
                                            /** 信息流曝光回调 */
                                        }

                                        @Override
                                        public void onADExposureFailed(int reason) {
                                            Log.i("FinishAdActivity" , "onADExposureFailed: " + reason);
                                        }

                                        @Override
                                        public void onADStatusChanged() {
                                            /** 下载状态回调 */
                                        }

                                        @Override
                                        public void onAdUnionClick() {

                                        }
                                    });


                            // 为公司logo加上点击
                            mAdLogo.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    mResponse.unionLogoClick();
                                }
                            });
                            mBaiduLogo.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    mResponse.unionLogoClick();
                                }
                            });
                        }
                    }

                    @Override
                    public void onNativeFail(int errorCode, String message, NativeResponse nativeResponse) {

                    }

                    @Override
                    public void onNoAd(int code, String msg, NativeResponse nativeResponse) {

                    }

                    @Override
                    public void onVideoDownloadSuccess() {

                    }

                    @Override
                    public void onVideoDownloadFailed() {

                    }

                    @Override
                    public void onLpClosed() {

                    }
                }
        );
    }

    /** 添加退出按钮的点击事件 */
    private void initExitButton() {
        if (mExsit != null) {
            mExsit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }
    }

    // 获取安装状态、下载进度所对应的按钮文案
    private String getBtnText(NativeResponse nrAd) {
        if (nrAd == null) {
            return "";
        }
        if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                || nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_DEEP_LINK) {
            int status = nrAd.getDownloadStatus();
            if (status >= 0 && status <= 100) {
                return "下载中：" + status + "%";
            } else if (status == 101) {
                return "点击安装";
            } else if (status == 102) {
                return "继续下载";
            } else if (status == 103) {
                return "点击启动";
            } else if (status == 104) {
                return "重新下载";
            } else {
                return "点击下载";
            }
        }
        return "查看详情";
    }
}
