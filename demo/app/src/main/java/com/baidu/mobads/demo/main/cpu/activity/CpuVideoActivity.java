package com.baidu.mobads.demo.main.cpu.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.mobads.sdk.api.CpuAdView;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
import com.baidu.mobads.sdk.api.CpuLpFontSize;
import com.baidu.mobads.sdk.api.CPUWebAdRequestParam;

import java.util.Map;
import java.util.UUID;
/**
 * 内容联盟小视频 集成参考类
 */
public class CpuVideoActivity extends Activity {

    public static final String TAG = "CpuVideoActivity";
    private static final String APP_ID = "d77e414";
    private static final int CHANNEL_ID = 1085; // 小视频频道
    private RelativeLayout mVideoContainer;
    private CpuAdView mCpuView;
    private View mSettingView;
    private EditText mEditText;

    private CpuLpFontSize mDefaultCpuLpFontSize = CpuLpFontSize.REGULAR;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cpu_video_main);
        initView();
        fetchCPUVideo();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 检测广告组件是否需要处理返回按键
        if (mCpuView.onKeyBackDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("设置");
        menu.add("暗黑模式");
        menu.add("小号字体10px");
        menu.add("中号字体12px");
        menu.add("大号字体14px");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ("设置".equals(item.getTitle())) {
            mSettingView.setVisibility(View.VISIBLE);
        } else if ("暗黑模式".equals(item.getTitle())) {
            isDarkMode = true;
        } else if ("小号字体10px".equals(item.getTitle())) {
            mDefaultCpuLpFontSize = CpuLpFontSize.SMALL;
        } else if ("中号字体12px".equals(item.getTitle())) {
            mDefaultCpuLpFontSize = CpuLpFontSize.REGULAR;
        } else {
            mDefaultCpuLpFontSize = CpuLpFontSize.LARGE;
        }
        fetchCPUVideo();
        return true;
    }

    private void fetchCPUVideo() {
        // demo设置，支持替换appsid
        String appsid = mEditText.getText().toString();
        if (TextUtils.isEmpty(appsid)) {
            appsid = APP_ID;
        }

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
                .setCustomUserId(outerId)
                // 个性化内容开关： "locknews":"0" : 不开启内容推荐，"locknews":"1" : 开启内容推荐
                .addExtra("locknews", "1")
                .build();

        // 小视频频道的内容
        mCpuView = new CpuAdView(this, appsid, CHANNEL_ID, cpuWebAdRequestParam, new CpuAdView.CpuAdViewInternalStatusListener() {
            @Override
            public void loadDataError(String message) {
                Log.i(TAG, "loadDataError: " + message);
            }

            @Override
            public void onAdClick() {
                Log.i(TAG, "onAdClick: ");
            }

            @Override
            public void onAdImpression(String impressionAdNums) {
                Log.i(TAG, "onAdImpression: " + impressionAdNums );
            }

            @Override
            public void onContentClick() {
                Log.i(TAG, "onContentClick: ");
            }

            @Override
            public void onContentImpression(String impressionContentNums) {
                Log.i(TAG, "onContentImpression: " + impressionContentNums );
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
                    Log.d(TAG, "onLpCustomEventCallBack: " + builder.toString() );

                }

            }

            @Override
            public void onExitLp() {
                Log.d(TAG, "onExitLp: 退出sdk详情页");   
            }
        });
        RelativeLayout.LayoutParams reLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        reLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mVideoContainer.addView(mCpuView, reLayoutParams);

        // 考虑到媒体在 锁屏开关等场景下 有刷新页面的需求，SDK目前将控件的创建与
        // 数据的请求进行拆分，媒体在已创建控件的前提下，可以直接调用下面这行代码来请求或着更新数据
        mCpuView.requestData();
        Toast.makeText(this, "加载AppSid:" + appsid, Toast.LENGTH_SHORT).show();
    }

    private void initView() {
        mSettingView = findViewById(R.id.settings);
        mSettingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSettingView.setVisibility(View.GONE);
                mVideoContainer.removeView(mCpuView);
                fetchCPUVideo();
            }
        });
        mEditText = findViewById(R.id.appsid);
        mVideoContainer = findViewById(R.id.cpu_video_container);
    }
}
