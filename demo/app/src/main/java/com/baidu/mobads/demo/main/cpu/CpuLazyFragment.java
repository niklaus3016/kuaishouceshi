package com.baidu.mobads.demo.main.cpu;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.cpu.view.CpuAdapter;
import com.baidu.mobads.demo.main.tools.RefreshAndLoadMoreView;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
import com.baidu.mobads.sdk.api.ActionBarColorTheme;
import com.baidu.mobads.sdk.api.AppActivity;
import com.baidu.mobads.sdk.api.CPUAdRequest;
import com.baidu.mobads.sdk.api.CpuLpFontSize;
import com.baidu.mobads.sdk.api.IBasicCPUData;
import com.baidu.mobads.sdk.api.NativeCPUManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * author: LiJinPeng
 * date: 2022/6/14
 * 内容联盟原生渲染(云控频道)
 * CpuLazyFragment 懒加载方案
 */
public class CpuLazyFragment extends BaseLazyLoadFragment implements NativeCPUManager.CPUAdListener {


    private static final String TAG = CpuLazyFragment.class.getSimpleName();
    private int mChannelId;
    private int mPageIndex = 1;
    private List<IBasicCPUData> nrAdList = new ArrayList<IBasicCPUData>();
    private NativeCPUManager mCpuManager;
    private final String YOUR_APP_ID = "c0da1ec4"; // 双引号中填写自己的APPSID
    private RefreshAndLoadMoreView mRefreshLoadView;
    private ListView mListView;
    /**
     * 13sp: 小号字体
     * 18sp: 中号字体
     * 23sp: 大号字体
     */
    private int mDefaultTextSize = 18;
    private int mDefaultBgColor = Color.WHITE;
    private boolean isDark;
    private CPUAdRequest.Builder builder;
    private CpuAdapter myAdapter;

    // 个性化内容开关： "locknews":"0" : 不限制内容推荐，"locknews":"1" : 限制内容推荐
    private String mLocknews = "0";
    private View mFragmentView;


    @Override
    public void onLazyLoad() {
        if (!checkChannelId()) {
            return;
        }
        mRefreshLoadView.setRefreshing(true);
        mRefreshLoadView.doRefreshing();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFragmentView != null) {
            ((ViewGroup) mFragmentView.getParent()).removeView(mFragmentView);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mChannelId = bundle.getInt("channelId");
        }
    }

    @Override
    public View initView(LayoutInflater inflater, @Nullable ViewGroup container) {

        if (!checkChannelId()) {
            return inflater.inflate(R.layout.cpu_tips, container, false);
        }
        if (mFragmentView != null) {
            return mFragmentView;
        }
        mFragmentView = inflater.inflate(R.layout.cpu_list, container, false);
        mRefreshLoadView =
                (RefreshAndLoadMoreView) mFragmentView.findViewById(R.id.native_list_view);
        mRefreshLoadView.setLoadAndRefreshListener(new RefreshAndLoadMoreView.LoadAndRefreshListener() {
            @Override
            public void onRefresh() {
                loadAd(mPageIndex++);
            }

            @Override
            public void onLoadMore() {
                loadAd(mPageIndex++);
            }
        });
        // 设置详情也的actionbar颜色
        AppActivity.setActionBarColorTheme(ActionBarColorTheme.ACTION_BAR_GREEN_THEME);

        // 设置落地页的ActionBar是否显示title
        AppActivity.setIsShowActionBarTitle(true);
        mListView = mRefreshLoadView.getListView();

        mListView.setCacheColorHint(Color.WHITE);
        /**
         * Step 1. NativeCPUManager，参数分别为： 上下文context（必须为Activity），appsid, 认证token,
         * CPUAdListener（监听广告请求的成功与失败）
         * 注意：请将YOUR_AD_PLACE_ID，YOUR_AD_TOKEN替换为自己的ID和TOKEN
         * 建议提前初始化
         */
        mCpuManager = new NativeCPUManager(getActivity(), YOUR_APP_ID, this);

        builder = new CPUAdRequest.Builder();

        myAdapter = new CpuAdapter(getActivity());
        myAdapter.setData(nrAdList);
        mListView.setAdapter(myAdapter);

        return mFragmentView;
    }

    private boolean checkChannelId() {
        if (mChannelId == 1090 || mChannelId == 1085 || mChannelId == 1094 || mChannelId == 1095) {
            // 热榜频道和小视频频道接入方式和其他频道不一致
            // 热榜：具体配置信息需参考HotActivity
            // 小视频：具体配置信息需参考CpuVideoActivity
            return false;
        }
        return true;
    }

    private void loadAd(int pageIndex) {

        /**
         * Step2：构建请求参数
         */
        /**
         * 可选设置: 设置暗黑模式或调整内容详情页的字体大小
         */
        if (mDefaultTextSize == 13) {
            builder.setLpFontSize(CpuLpFontSize.SMALL);
        } else if (mDefaultTextSize == 18) {
            builder.setLpFontSize(CpuLpFontSize.REGULAR);
        } else if (mDefaultTextSize == 23) {
            builder.setLpFontSize(CpuLpFontSize.LARGE);
        }
        builder.setLpFontSize(CpuLpFontSize.REGULAR);
        builder.setLpDarkMode(isDark);
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

        // 当无法获得设备IMEI,OAID信息时，通过此字段获取内容 + 广告
        builder.setCustomUserId(outerId);


        // 如果媒体选择了本地频道，可以传入城市名字，否则会根据ip地址推送内容
        if (mChannelId == 1080) {
            // 城市名字建议传入 "XXX市" 或 "XXX县"
            builder.setCityIfLocalChannel("北京市");
        }

        // 设置子渠道，如果申请的话
        builder.setSubChannelId("102619");

        // 个性化内容开关： "locknews":"0" : 不限制内容推荐，"locknews":"1" : 限制内容推荐
        builder.addExtra("locknews", mLocknews);
        mCpuManager.setRequestParameter(builder.build());
        mCpuManager.setRequestTimeoutMillis(5 * 1000); // 如果不设置，则默认5s请求超时

        /**
         * Step3：调用请求接口，请求广告
         */
        makeToast("Start loadAd!");
        mCpuManager.loadAd(pageIndex, mChannelId, true);

    }


    /**
     * 请求广告成功，返回广告列表
     *
     * @param list 广告+内容数据
     */
    @Override
    public void onAdLoaded(List<IBasicCPUData> list) {
        if (mRefreshLoadView.isRefreshing()) {
            nrAdList.clear();
        }
        if (list != null && list.size() > 0) {
            nrAdList.addAll(list);
            if (nrAdList.size() == list.size()) {
                myAdapter.setData(nrAdList);
            }
            makeToast("Load ad success!");
        }
        mRefreshLoadView.onLoadFinish();
    }

    @Override
    public void onAdError(String msg, int errorCode) {
        mRefreshLoadView.onLoadFinish();
        Log.w(TAG, "onAdError reason:" + msg);
        makeToast("onAdError reason:" + msg);
    }

    @Override
    public void onVideoDownloadSuccess() {
        // 预留接口
    }

    @Override
    public void onVideoDownloadFailed() {
        // 预留接口
    }

    @Override
    public void onDisLikeAdClick(int position, String reason) {
        Log.d(TAG, "onMisLikeAdClick: position = " + position +
                ", reaason = " + reason);
        nrAdList.remove(position);
        myAdapter.setData(nrAdList);
    }

    @Override
    public void onLpCustomEventCallBack(HashMap<String, Object> data,
                                        NativeCPUManager.DataPostBackListener dataPostBackListener) {
        boolean isPageload = false, isThumbUp = false, isCollect = false;
        if (data != null) {

            Activity activity = AppActivity.getActivity();
//            Log.e("@@@@", "onLpCustomEventCallBack: " + activity );
            Object type = data.get("type");
            Object contentId = data.get("contentId");
            Object act = data.get("act");
            Object vduration = data.get("vduration");
            Object vprogress = data.get("vprogress");
            Object webContentH = data.get("webContentH");
            Object webScroolY = data.get("webScroolY");
            Object args = data.get("args");
            Object activty = data.get("activity");
            Log.d("lijinpeng", "onLpCustomEventCallBack: " + act);
            if (activty instanceof Activity) {
                Log.d(TAG, "onLpCustomEventCallBack: " + activty);
            }

            StringBuilder builder = new StringBuilder();
            if (args instanceof JSONObject) {
                String contentUrl = ((JSONObject) args).optString("contentUrl");
                Log.d(TAG, "onLpCustomEventCallBack:contentUrl  = " + contentUrl);
                builder.append("args = ").append(args);
            }

            if (type instanceof String) {
                builder.append("type = ").append(type);
            }
            if (contentId instanceof String) {
                builder.append(",contentId = ").append(contentId);
            }

            if (act instanceof String) {
                isPageload = "load".equals(act);
                isThumbUp = "thumbUp".equals(act);
                isCollect = "collect".equals(act);
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
            Log.d(TAG, "onLpCustomEventCallBack: " + builder);

        }
        // 为了避免防止无意义的持续交互带来的性能消耗，前端页面的其它行为建议直接返回null
        if (isPageload || isThumbUp || isCollect) {
            JSONObject cbParams = new JSONObject();
            try {
                cbParams.put("status", "0");
                JSONObject c = new JSONObject();

                c.put("updateStatus", 1); //int, 1代表更新成功，0代表失败
                c.put("likeNum", 10);
                c.put("isLiked", true);
                c.put("isCollected", true);
                cbParams.put("data", c);
                /**
                 *  媒体回传数据
                 */
                if (dataPostBackListener != null) {
                    Log.e("@@@@@@@", "onLpCustomEventCallBack: 媒体回传数据" + cbParams);
                    dataPostBackListener.postback(cbParams);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onExitLp() {
        Log.d(TAG, "onExitLp: 退出sdk详情页");
    }


    private void makeToast(String str) {
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }


}
