package com.baidu.mobads.demo.main.cpu;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
import com.baidu.mobads.sdk.api.CPUWebAdRequestParam;
import com.baidu.mobads.sdk.api.CpuAdView;
import com.baidu.mobads.sdk.api.CpuLpFontSize;

import java.util.Map;
import java.util.UUID;

/**
 * author: LiJinPeng
 * date: 2022/6/14
 * 内容联盟模板渲染(云控频道)
 * CpuH5Fragment 默认预加载左右两侧fragment
 */
public class CpuH5Fragment extends BasePreLoadFragment {
    private static final String TAG = CpuH5Fragment.class.getSimpleName();
    private int mChannelId;
    // 测试id
    private static String DEFAULT_APPSID = "c0da1ec4";

    private CpuLpFontSize mDefaultCpuLpFontSize = CpuLpFontSize.REGULAR;
    private boolean isDarkMode = false;
    private CpuAdView mCpuView;
    // 个性化内容开关： "locknews":"0" : 不限制内容推荐，"locknews":"1" : 限制内容推荐
    private String mLocknews = "0";
    private View mViewGroup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mChannelId = bundle.getInt("channelId");
        }
        initData();
    }

    private void initData() {
        /**
         *  注意构建参数时，setCustomUserId 为必选项，
         *  传入的outerId是为了更好的保证能够获取到广告和内容
         *  outerId的格式要求： 包含数字与字母的16位 任意字符串
         */

        /**
         *  推荐的outerId获取方式：
         */

        SharedPreUtils sharedPreUtils = SharedPreUtils.getInstance();
        String outerId = sharedPreUtils.getString(SharedPreUtils.OUTER_ID);
        if (TextUtils.isEmpty(outerId)) {
            outerId = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 16);
            sharedPreUtils.putString(SharedPreUtils.OUTER_ID, outerId);
        }

        CPUWebAdRequestParam cpuWebAdRequestParam = new CPUWebAdRequestParam.Builder()
                .setLpFontSize(mDefaultCpuLpFontSize)
                .setLpDarkMode(isDarkMode)
                .setSubChannelId("102619")
                .setCityIfLocalChannel("北京")
                .setCustomUserId(outerId)
                // 个性化内容开关： "locknews":"0" : 不限制内容推荐，"locknews":"1" : 限制内容推荐
                .addExtra("locknews", mLocknews)
                .build();

        mCpuView = new CpuAdView(getActivity(), DEFAULT_APPSID, mChannelId, cpuWebAdRequestParam,
                new CpuAdView.CpuAdViewInternalStatusListener() {
                    @Override
                    public void loadDataError(String message) {
                        Log.d(TAG, "loadDataError: " + message);
                    }

                    @Override
                    public void onAdClick() {
                        Log.d(TAG, "onAdClick: ");
                    }

                    @Override
                    public void onAdImpression(String impressionAdNums) {
                        Log.d(TAG, "onAdImpression: impressionAdNums " + impressionAdNums);
                    }

                    @Override
                    public void onContentClick() {
                        Log.d(TAG, "onContentClick: ");
                    }

                    @Override
                    public void onContentImpression(String impressionContentNums) {
                        Log.d(TAG, "onContentImpression: impressionContentNums = " +
                                impressionContentNums);
                    }

                    @Override
                    public void onLpContentStatus(Map<String, Object> data) {
                        if (data != null) {
                            Object type = data.get("type");
                            Object contentId = data.get("contentId");
                            Object act = data.get("act");
                            Object vduration = data.get("vduration");
                            Object vprogress = data.get("vprogress");
                            Object webContentH = data.get("webContentH");
                            Object webScroolY = data.get("webScroolY");
                            StringBuilder builder = new StringBuilder();
                            if (type instanceof String) {
                                builder.append("type = ").append(type);
                            }
                            if (contentId instanceof String) {
                                builder.append(",contentId = ").append(contentId);
                            }

                            if (act instanceof String) {
                                builder.append(",act =  ").append(act);
                            }
                            if (vduration instanceof Integer) {
                                builder.append(",vduration =  ").append(vduration);
                            }

                            if (vprogress instanceof Integer) {
                                builder.append(",vprogress = ").append(vprogress);
                            }

                            if (webContentH instanceof Integer) {
                                builder.append(", webContentH = ").append(webContentH);
                            }
                            if (webScroolY instanceof Integer) {
                                builder.append(",webScroolY = ").append(webScroolY);
                            }
                            Log.d(TAG, "onLpCustomEventCallBack: " + builder.toString());

                        }

                    }

                    @Override
                    public void onExitLp() {
                        Log.d(TAG, "onExitLp: 退出sdk详情页");
                    }
                });
    }

    @Override
    public View initView(LayoutInflater inflater, @Nullable ViewGroup container) {
        if (mViewGroup != null) {
            return mViewGroup;
        }
        mViewGroup = inflater.inflate(R.layout.cpu_list_h5, container, false);
        initH5View();
        showSelectedCpuWebPage();
        return mViewGroup;
    }

    private void initH5View() {
        RelativeLayout parentLayout = (RelativeLayout) mViewGroup.findViewById(R.id.cpu_h5_view);
        RelativeLayout.LayoutParams reLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        reLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        parentLayout.addView(mCpuView, reLayoutParams);
    }

    @Override
    public void isVisible(boolean isVisible) {
        if (mCpuView != null) {
            if (isVisible) {
                mCpuView.onResume();
            } else {
                mCpuView.onPause();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mViewGroup != null) {
            ((ViewGroup) mViewGroup.getParent()).removeView(mViewGroup);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCpuView != null) {
            mCpuView.onDestroy();
        }
    }


    /**
     * 内容联盟模板渲染，展示频道
     */
    private void showSelectedCpuWebPage() {

        if (mCpuView != null) {
            // 考虑到媒体在 锁屏开关等场景下 有刷新页面的需求，SDK目前将控件的创建与
            // 数据的请求进行拆分，媒体在已创建控件的前提下，可以直接调用下面这行代码来请求或着更新数据
            mCpuView.requestData();
        }

    }

}
