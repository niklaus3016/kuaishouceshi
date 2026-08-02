package com.baidu.mobads.demo.main.cpu.lockScreen;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
import com.baidu.mobads.sdk.api.AppActivity;
import com.baidu.mobads.sdk.api.CPUWebAdRequestParam;
import com.baidu.mobads.sdk.api.CpuAdView;
import com.baidu.mobads.sdk.api.CpuLpFontSize;

import java.util.Map;

/**
 * author: Lijinpeng
 * date: 2021/7/30
 * desc: 内容联盟锁屏场景 模板渲染锁屏页面
 */
public class LockScreenActivity extends Activity {
    private CpuAdView mCpuView;
    public static final String TAG = "LockScreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLockerWindow(getWindow());
        setContentView(R.layout.activity_lock_screen);
        initView();
    }

    private void initView() {

        LinearLayout layout = (LinearLayout) findViewById(R.id.ll_lock_content);
        CPUWebAdRequestParam cpuWebAdRequestParam = new CPUWebAdRequestParam.Builder()
                .setLpFontSize(CpuLpFontSize.REGULAR)
                .setLpDarkMode(false)
                .setCityIfLocalChannel("北京")
                .setCustomUserId(SharedPreUtils.getInstance().getString(SharedPreUtils.OUTER_ID))
                .build();

        mCpuView = new CpuAdView(this, "c0da1ec4", 1001, cpuWebAdRequestParam,
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

                    }

                    @Override
                    public void onExitLp() {

                    }
                });

        // 考虑到媒体在 锁屏开关等场景下 有刷新页面的需求，SDK目前将控件的创建与
        // 数据的请求进行拆分，媒体在已创建控件的前提下，可以直接调用下面这行代码来请求或着更新数据
        mCpuView.requestData();
        layout.addView(mCpuView);
        AppActivity.canLpShowWhenLocked(true);
    }

    private void setLockerWindow(Window window) {
        //FLAG_DISMISS_KEYGUARD用于去掉系统锁屏页，FLAG_SHOW_WHEN_LOCKED使Activity在锁屏时仍然能够显示
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}