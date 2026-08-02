package com.baidu.mobads.demo.main.cpu.activity;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Spinner;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
import com.baidu.mobads.sdk.api.CPUWebAdRequestParam;
import com.baidu.mobads.sdk.api.CpuAdView;
import com.baidu.mobads.sdk.api.CpuLpFontSize;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 内容联盟模板渲染 集成参考类
 */
public class CpuExpressActivity extends Activity {

    public static final String TAG = "CpuAdActivity";
    // 测试id
    private static String DEFAULT_APPSID = "c0da1ec4";

    private CpuLpFontSize mDefaultCpuLpFontSize = CpuLpFontSize.REGULAR;
    private boolean isDarkMode = false;
    private CpuAdView mCpuView;
    // 个性化内容开关： "locknews":"0" : 不开启内容推荐，"locknews":"1" : 开启内容推荐
    private String mLocknews = "0";

    public enum CpuChannel {

        /**
         * 推荐频道
         */
        CHANNEL_RECOMMEND(1022),

        /**
         * 娱乐频道
         */
        CHANNEL_ENTERTAINMENT(1001),
        /**
         * 体育频道
         */
        CHANNEL_SPORT(1002),
        /**
         * 图片频道
         */
        CHANNEL_PICTURE(1003),
        /**
         * 手机频道
         */
        CHANNEL_MOBILE(1005),
        /**
         * 财经频道
         */
        CHANNEL_FINANCE(1006),
        /**
         * 汽车频道
         */
        CHANNEL_AUTOMOTIVE(1007),
        /**
         * 房产频道
         */
        CHANNEL_HOUSE(1008),
        /**
         * 热点频道
         */

        CHANNEL_HOTSPOT(1021),

        /**
         * 本地频道
         */
        CHANNEL_LOCAL(1080),

        /**
         * 热榜频道
         */
        CHANNEL_HOT(1090),

        /**
         * 健康频道
         */
        CHANNEL_HEALTH(1043),

        /**
         * 母婴频道
         */
        CHANNEL_MOTHER(1042);

        private int value;

        CpuChannel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cpu_express);
        initSpinner();
        Button button = (Button) this.findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showSelectedCpuWebPage();
            }
        });
    }

    /**
     *
     *  内容联盟模板渲染，展示频道
     *
     */
    private void showSelectedCpuWebPage() {


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

        CPUWebAdRequestParam cpuWebAdRequestParam = new CPUWebAdRequestParam.Builder()
                .setLpFontSize(mDefaultCpuLpFontSize)
                .setLpDarkMode(isDarkMode)
                .setCityIfLocalChannel("北京")
                .setCustomUserId(outerId)
                // 个性化内容开关： "locknews":"0" : 不开启内容推荐，"locknews":"1" : 开启内容推荐
                .addExtra("locknews", mLocknews)
                .build();

        mCpuView = new CpuAdView(this, getAppsid(), getChannel().getValue(), cpuWebAdRequestParam,
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
                            Object args = data.get("args");
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
                            if (args instanceof JSONObject) {
                                // 分享
                                if ("share".equals(act)) {
                                    // 内容链接
                                    String contentUrl = ((JSONObject) args).optString("contentUrl");
                                    // 标题
                                    String title = ((JSONObject) args).optString("title");
                                    // 封面图片地址
                                    String coverImg = ((JSONObject) args).optString("coverImg");
                                    Log.d(TAG, "share:contentUrl=" + contentUrl + ", title=" + title + ", " +
                                            "coverImg=" + coverImg);
                                }
                            }
                            Log.d(TAG, "onLpCustomEventCallBack: " + builder.toString() );

                        }

                    }

                    @Override
                    public void onExitLp() {
                        Log.d(TAG, "onExitLp: 退出sdk详情页");
                    }
                });

        // 考虑到媒体在 锁屏开关等场景下 有刷新页面的需求，SDK目前将控件的创建与
        // 数据的请求进行拆分，媒体在已创建控件的前提下，可以直接调用下面这行代码来请求或着更新数据
        mCpuView.requestData();


        final RelativeLayout parentLayout = (RelativeLayout) this.findViewById(R.id.parent_block);
        RelativeLayout.LayoutParams reLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        reLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        parentLayout.addView(mCpuView, reLayoutParams);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 检测广告组件是否需要处理返回按键
        if (mCpuView != null && mCpuView.onKeyBackDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 初始化下拉框
     */
    private void initSpinner() {
        Spinner channelSpinner = (Spinner) this.findViewById(R.id.channel);
        channelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });
        List<SpinnerItem> list = new ArrayList<SpinnerItem>();
        list.add(new SpinnerItem("推荐频道", CpuChannel.CHANNEL_RECOMMEND));
        list.add(new SpinnerItem("娱乐频道", CpuChannel.CHANNEL_ENTERTAINMENT));
        list.add(new SpinnerItem("体育频道", CpuChannel.CHANNEL_SPORT));
        list.add(new SpinnerItem("图片频道", CpuChannel.CHANNEL_PICTURE));
        list.add(new SpinnerItem("手机频道", CpuChannel.CHANNEL_MOBILE));
        list.add(new SpinnerItem("财经频道", CpuChannel.CHANNEL_FINANCE));
        list.add(new SpinnerItem("汽车频道", CpuChannel.CHANNEL_AUTOMOTIVE));
        list.add(new SpinnerItem("房产频道", CpuChannel.CHANNEL_HOUSE));
        list.add(new SpinnerItem("热点频道", CpuChannel.CHANNEL_HOTSPOT));
        list.add(new SpinnerItem("本地频道", CpuChannel.CHANNEL_LOCAL));
        list.add(new SpinnerItem("热榜", CpuChannel.CHANNEL_HOT));
        list.add(new SpinnerItem("健康频道", CpuChannel.CHANNEL_HEALTH));
        list.add(new SpinnerItem("母婴频道", CpuChannel.CHANNEL_MOTHER));
        ArrayAdapter<SpinnerItem> dataAdapter = new ArrayAdapter<SpinnerItem>(this,
                android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelSpinner.setAdapter(dataAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCpuView != null) {
            mCpuView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCpuView != null) {
            mCpuView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCpuView != null) {
            mCpuView.onDestroy();
        }
    }

    /**
     * 获取appsid
     *
     * @return
     */
    private String getAppsid() {
        return DEFAULT_APPSID;
    }

    /**
     * 获取频道
     *
     * @return
     */
    private CpuChannel getChannel() {
        Spinner channelSpinner = (Spinner) this.findViewById(R.id.channel);
        SpinnerItem selectedItem = (SpinnerItem) channelSpinner.getSelectedItem();
        return selectedItem.getChannel();
    }




    class SpinnerItem extends Object {
        /**
         * 频道名称
         */
        String text;
        /**
         * 频道id
         */
        CpuChannel channel;

        public SpinnerItem(String text, CpuChannel cpuChannel) {
            this.text = text;
            this.channel = cpuChannel;
        }

        @Override
        public String toString() {
            return text;
        }

        CpuChannel getChannel() {
            return channel;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cpu_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cpu_menu_light_mode:
                isDarkMode = false;
                break;
            case R.id.cpu_menu_dark_mode:
                isDarkMode = true;
                break;
            case R.id.cpu_menu_small:
                mDefaultCpuLpFontSize = CpuLpFontSize.SMALL;
                break;
            case R.id.cpu_menu_middle:
                mDefaultCpuLpFontSize = CpuLpFontSize.REGULAR;
                break;
            case R.id.cpu_menu_big:
                mDefaultCpuLpFontSize = CpuLpFontSize.LARGE;
                break;
            case R.id.cpu_menu_switch:
                mLocknews = "1";
                break;
            default: break;
        }
        showSelectedCpuWebPage();
        return true;
    }
}
