package com.baidu.mobads.demo.main.cpu.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.cpu.view.AbstractViewHolder;
import com.baidu.mobads.demo.main.cpu.view.OnePicViewHolder;
import com.baidu.mobads.demo.main.cpu.view.SpinnerItem;
import com.baidu.mobads.demo.main.cpu.view.ThreePicsViewHolder;
import com.baidu.mobads.demo.main.cpu.view.VideoViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.hot.HotActivity;
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

/*
1. 集成参考类：NativeCPUAdActivity
2. 原生渲染的内容联盟，请求广告成功后，返回的广告列表包含内容数据+广告。
3. 如果需要在锁屏场景下展示广告落地页，需要设置AppActivity.canLpShowWhenLocked(boolean canShow);默认为 false，广告展现前全局设置即可
4. 注意：内容联盟原生渲染需要您手动发送广告曝光和广告点击事件。漏发则无法计费。
* */
public class NativeCPUAdActivity extends Activity implements NativeCPUManager.CPUAdListener {
    public static final String TAG = NativeCPUAdActivity.class.getSimpleName();

    private final String YOUR_APP_ID = "c0da1ec4"; // 双引号中填写自己的APPSID
    private View cpuDataContainer;
    private ListView listView;
    private MyAdapter adapter;
    private Button showAd;
    private Button loadAd;
    private int mChannelId = 1001; // 默认娱乐频道
    private int mPageIndex = 1;
    private List<IBasicCPUData> nrAdList = new ArrayList<IBasicCPUData>();
    private NativeCPUManager mCpuManager;

    private RefreshAndLoadMoreView mRefreshLoadView;
    private boolean isDark;
    /**
     *  13sp: 小号字体
     *  18sp: 中号字体
     *  23sp: 大号字体
     */
    private int mDefaultTextSize = 18;
    private int mDefaultBgColor;
    private CPUAdRequest.Builder builder;
    // 个性化内容开关： "locknews":"0" : 不开启内容推荐，"locknews":"1" : 开启内容推荐
    private String mLocknews = "0";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_native);
        initAdListView();
        initSpinner();

         loadAd = findViewById(R.id.btn_load);
        loadAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mChannelId == 1090) {
                    /**
                     * 热榜频道接入方式与其它频道不同，具体配置信息需参考HotActivity
                     */
                    Intent intent = new Intent(NativeCPUAdActivity.this, HotActivity.class);
                    intent.putExtra("isdark",isDark);
                    intent.putExtra("textSize", mDefaultTextSize);
                    startActivity(intent);
                } else {
                    loadAd(mPageIndex);
                }

            }
        });
        showAd = findViewById(R.id.btn_show);
        showAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdList();
            }
        });
        showAd.setEnabled(false);
        // 设置详情也的actionbar颜色
        AppActivity.setActionBarColorTheme(ActionBarColorTheme.ACTION_BAR_GREEN_THEME);

        // 设置落地页的ActionBar是否显示title
        AppActivity.setIsShowActionBarTitle(true);
        /**
         * Step 1. NativeCPUManager，参数分别为： 上下文context（必须为Activity），appsid, 认证token, CPUAdListener（监听广告请求的成功与失败）
         * 注意：请将YOUR_AD_PLACE_ID，YOUR_AD_TOKEN替换为自己的ID和TOKEN
         * 建议提前初始化
         */
        mCpuManager = new NativeCPUManager(NativeCPUAdActivity.this, YOUR_APP_ID, this);

        builder = new CPUAdRequest.Builder();

        mDefaultBgColor = Color.WHITE;
        mDefaultTextSize = 18;
    }


    private void initAdListView() {
        cpuDataContainer = findViewById(R.id.cpuDataContainer);
        mRefreshLoadView = findViewById(R.id.native_list_view);
        mRefreshLoadView.setLoadAndRefreshListener(new RefreshAndLoadMoreView.LoadAndRefreshListener() {
            @Override
            public void onRefresh() {
                loadAd(++mPageIndex);
            }

            @Override
            public void onLoadMore() {
                loadAd(++mPageIndex);
            }
        });
        listView = mRefreshLoadView.getListView();

        listView.setCacheColorHint(Color.WHITE);


        adapter = new MyAdapter(this);

//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.i(TAG, "NativeCPUAdActivity.onItemClick");
//                IBasicCPUData nrAd = nrAdList.get(position);
//                nrAd.handleClick(view);
//            }
//        });

        cpuDataContainer.setVisibility(View.GONE);
    }

    private void initSpinner() {
        // 频道类目
        Spinner channelSpinner = (Spinner) this.findViewById(R.id.channel);
        channelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mChannelId = ((SpinnerItem) parent.getItemAtPosition(position)).getId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });
        List<SpinnerItem> list2 = new ArrayList<SpinnerItem>();
        list2.add(new SpinnerItem("娱乐频道", 1001));
        list2.add(new SpinnerItem("体育频道", 1002));
        list2.add(new SpinnerItem("财经频道", 1006));
        list2.add(new SpinnerItem("汽车频道", 1007));
        list2.add(new SpinnerItem("时尚频道", 1009));
        list2.add(new SpinnerItem("文化频道", 1036));
        list2.add(new SpinnerItem("科技频道", 1013));
        list2.add(new SpinnerItem("推荐频道", 1022));
        list2.add(new SpinnerItem("视频频道", 1057));
        list2.add(new SpinnerItem("图集频道", 1068));
        list2.add(new SpinnerItem("本地频道", 1080));
        list2.add(new SpinnerItem("热榜", 1090));
        ArrayAdapter<SpinnerItem> dataAdapter2 = new ArrayAdapter<SpinnerItem>(this,
                android.R.layout.simple_spinner_item, list2);
        dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(dataAdapter2);
    }

    public void showAdList() {
        cpuDataContainer.setVisibility(View.VISIBLE);
        listView.setAdapter(adapter);
    }

    public void loadAd(int pageIndex) {
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
                    .substring(0,16);
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
        builder.setSubChannelId("86784");

        // 个性化内容开关： "locknews":"0" : 不开启内容推荐，"locknews":"1" : 开启内容推荐
        builder.addExtra("locknews", mLocknews);
        mCpuManager.setRequestParameter(builder.build());
        mCpuManager.setRequestTimeoutMillis(5 * 1000); // 如果不设置，则默认5s请求超时

        /**
         * Step3：调用请求接口，请求广告
         */
        makeToast("Start loadAd!");
        mCpuManager.loadAd(pageIndex, mChannelId, true);

        showAd.setEnabled(false);
    }


    /**
     * 请求广告成功，返回广告列表
     * @param list 广告+内容数据
     */
    @Override
    public void onAdLoaded(List<IBasicCPUData> list) {
        if (mRefreshLoadView.isRefreshing()) {
            nrAdList.clear();
        }
        if (list != null && list.size() > 0) {
            nrAdList.addAll(list);
            showAd.setEnabled(true);
            if (nrAdList.size() == list.size()) {
                adapter.notifyDataSetChanged();
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
        adapter.notifyDataSetChanged();
//        Toast.makeText(NativeCPUAdActivity.this, "将为你减少类似推荐内容", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLpCustomEventCallBack(HashMap<String, Object> data,
                                        final NativeCPUManager.DataPostBackListener dataPostBackListener) {
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
            if (activty instanceof Activity) {
                Log.d(TAG, "onLpCustomEventCallBack: " + activty);
            }

            StringBuilder builder = new StringBuilder();
            if (args instanceof JSONObject) {
                // 内容落地页链接
                String contentUrl = ((JSONObject) args).optString("contentUrl");
                Log.d(TAG, "onLpCustomEventCallBack:contentUrl  = " + contentUrl);
                builder.append("args = ").append(args);
                // 分享
                if ("share".equals(act)) {
                    // 标题
                    String title = ((JSONObject) args).optString("title");
                    // 封面图片地址
                    String coverImg = ((JSONObject) args).optString("coverImg");
                    Log.d(TAG, "share:contentUrl=" + contentUrl + ", title=" + title + ", " +
                            "coverImg=" + coverImg);
                }
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
        Log.d(TAG, "onExitLp: 退出sdk详情页" );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cpu_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cpu_menu_light_mode:
                isDark = false;
                mDefaultBgColor = Color.WHITE;
                builder.setLpDarkMode(false);
                cpuDataContainer.setBackgroundColor(Color.WHITE);
                adapter.setStyleParam(mDefaultBgColor, mDefaultTextSize);
                AppActivity.setActionBarColorTheme(ActionBarColorTheme.ACTION_BAR_WHITE_THEME);
                break;
            case R.id.cpu_menu_dark_mode:
                isDark = true;
                mDefaultBgColor = Color.BLACK;
                builder.setLpDarkMode(true);
                cpuDataContainer.setBackgroundColor(Color.BLACK);
                adapter.setStyleParam(mDefaultBgColor, mDefaultTextSize);
                AppActivity.setActionBarColorTheme(ActionBarColorTheme.ACTION_BAR_BLACK_THEME);
                break;
            case R.id.cpu_menu_small:
                mDefaultTextSize = 13;
                builder.setLpFontSize(CpuLpFontSize.SMALL);
                adapter.setStyleParam(mDefaultBgColor, mDefaultTextSize);
                break;
            case R.id.cpu_menu_middle:
                mDefaultTextSize = 18;
                builder.setLpFontSize(CpuLpFontSize.REGULAR);
                adapter.setStyleParam(mDefaultBgColor, mDefaultTextSize);
                break;
            case R.id.cpu_menu_big:
                mDefaultTextSize = 23;
                builder.setLpFontSize(CpuLpFontSize.LARGE);
                adapter.setStyleParam(mDefaultBgColor, mDefaultTextSize);
                break;
            case R.id.cpu_menu_switch:
                mLocknews = "1";
                loadAd(++mPageIndex);
                break;
            default: break;
        }
        return true;
    }


    class MyAdapter extends BaseAdapter {
        LayoutInflater inflater;
        AQuery aq;
        public static final int THREE_PIC_LAYOUT = 0;
        public static final int VIDEO_LAYOUT = 1;
        public static final int ONE_PIC_LAYOUT = 2;

        private int bg = Color.WHITE;

        private int textSize = 18;

        private Context mCtx;

        public MyAdapter(Context context) {
            super();
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            aq = new AQuery(context);
            mCtx = context;
        }


        public void setStyleParam(int bgColor, int wordSize) {
            bg = bgColor;
            textSize = wordSize;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return nrAdList.size();
        }

        @Override
        public IBasicCPUData getItem(int position) {
            return nrAdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        // 根据数据划分出几种type，对应不同的布局
        @Override
        public int getItemViewType(int position) {
            IBasicCPUData cpuData = getItem(position);
            // news,image,video,ad
            String type = cpuData.getType();
            // 广告图片
            List<String> imageList = cpuData.getImageUrls();
            // 内容图片
            List<String> smallImageList = cpuData.getSmallImageUrls();
            if (type.equals("video") || (type.equals("ad") && (!TextUtils.isEmpty(cpuData.getVUrl())))) {
                return VIDEO_LAYOUT;
            }
            if ((smallImageList != null && smallImageList.size() >= 3)
                 || (imageList != null && imageList.size() >= 3)) {
                return THREE_PIC_LAYOUT;
            }
            if ((smallImageList != null && smallImageList.size() == 1)
                 || (imageList != null && imageList.size() == 1)) {
                return ONE_PIC_LAYOUT;
            }
            return -1;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int itemViewType = getItemViewType(position);
            IBasicCPUData nrAd = getItem(position);

            AbstractViewHolder holder = null;

            if (convertView == null) {
                switch (itemViewType) {
                    case ONE_PIC_LAYOUT :
                        convertView = inflater.inflate(R.layout.cpu_item_onepic, parent, false);
                        holder = new OnePicViewHolder(convertView);
                        convertView.setTag(holder);
                        break;
                    case THREE_PIC_LAYOUT :
                        convertView = inflater.inflate(R.layout.cpu_item_threepics, parent, false);
                        holder = new ThreePicsViewHolder(convertView);
                        convertView.setTag(holder);
                        break;
                    case VIDEO_LAYOUT :
                        convertView = inflater.inflate(R.layout.cpu_item_video2, parent, false);
                        holder = new VideoViewHolder(convertView);
                        convertView.setTag(holder);
                        break;
                    default:
                        throw new IllegalStateException("数据与布局不匹配");
                }
            } else {
                switch (itemViewType) {
                    case ONE_PIC_LAYOUT:
                        holder = (OnePicViewHolder) convertView.getTag();
                        break;
                    case THREE_PIC_LAYOUT:
                        holder = (ThreePicsViewHolder) convertView.getTag();
                        break;
                    case VIDEO_LAYOUT:
                        holder = (VideoViewHolder) convertView.getTag();
                        break;
                    default:
                        throw new IllegalStateException("数据与布局不匹配");
                }
            }
            holder.initWidgetWithData(nrAd, position);

            holder.setAttribute(bg, textSize);

//             展现时需要调用onImpression上报展现
//            nrAd.onImpression(convertView);

            return convertView;
        }


    }

    private void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
