package com.baidu.mobads.demo.main.mediaExamples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.mediaExamples.cpu.CpuFragmentActivity;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.adSettings.BottomDialog;
import com.baidu.mobads.demo.main.cpu.lockScreen.LockScreenReceiver;
import com.baidu.mobads.demo.main.cpu.lockScreen.LockScreenService;
import com.baidu.mobads.demo.main.mediaExamples.news.NewsDemoActivity;
import com.baidu.mobads.demo.main.mediaExamples.splashHotStart.SplashLoadActivity;
import com.baidu.mobads.demo.main.mediaExamples.utilsDemo.UtilsFeedsAdActivity;
import com.baidu.mobads.sdk.api.AppActivity;
import com.baidu.mobads.sdk.api.IBasicCPUData;
import com.baidu.mobads.sdk.api.NativeCPUManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.baidu.mobads.demo.main.cpu.lockScreen.LockScreenReceiver.LOCK_TYPE_SETTING;

public class MediaExamplesActivity extends Activity {

    private TextView lockButton;
    private String title = "内容联盟锁屏场景配置";
    private String cpuH5 = "模板渲染";
    private String close = "关闭";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_examples);
        initview();
    }

    private void initview() {
        final EditText editText = (EditText) findViewById(R.id.edit_text_url);
        Button openActivity = this.findViewById(R.id.cpu_open_activity);
        openActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new NativeCPUManager(MediaExamplesActivity.this, "c0da1ec4", new NativeCPUManager.CPUAdListener() {
                    @Override
                    public void onAdLoaded(List<IBasicCPUData> list) {
                          // 该方法不会回调
                    }

                    @Override
                    public void onAdError(String msg, int errorCode) {
                        // 该方法不会回调
                    }

                    @Override
                    public void onVideoDownloadSuccess() {
                        // 该方法不会回调
                    }

                    @Override
                    public void onVideoDownloadFailed() {
                        // 该方法不会回调
                    }

                    @Override
                    public void onDisLikeAdClick(int position, String reason) {
                        // 该方法不会回调
                    }

                    @Override
                    public void onLpCustomEventCallBack(HashMap<String, Object> data, NativeCPUManager.DataPostBackListener dataPostBackListener) {
                        if (data != null) {
                            Activity activity = AppActivity.getActivity();
                            Log.e("@@@@", "onLpCustomEventCallBack: " + activity );
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
                            Log.d("@@@@@@@", "onLpCustomEventCallBack: " + builder.toString() );

                        }

                    }

                    @Override
                    public void onExitLp() {
                        Log.d("@@@@@@", "onExitLp: ");
                    }
                }).openAppActivity("https://union.baidu.com/docs/sdk/AndroidSDK.html");
            }
        });

        // 工具类接入
        Button toolsButton = this.findViewById(R.id.tools_example);
        toolsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaExamplesActivity.this, UtilsFeedsAdActivity.class);
                startActivity(intent);
            }
        });

        // 资讯类接入
        Button newsButton = this.findViewById(R.id.news_example);
        newsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaExamplesActivity.this, NewsDemoActivity.class);
                startActivity(intent);
            }
        });
        // 开屏热启动接入实例
        Button splashButton = this.findViewById(R.id.splash_hot_start_example);
        splashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaExamplesActivity.this, SplashLoadActivity.class);
                startActivity(intent);
            }
        });

        Button cpufragment = this.findViewById(R.id.cpu_fragment);
        cpufragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaExamplesActivity.this, CpuFragmentActivity.class);
                startActivity(intent);
            }
        });


        initLockSettingView();
    }


    @Override
    protected void onStart() {
        super.onStart();
        lockButton.setText(getLockSwitchStatus());
    }

    private String getLockSwitchStatus() {
       if (LockScreenReceiver.LOCK_TYPE_SETTING == 0) {
           return cpuH5;
       }else {
           return close;
       }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initLockSettingView() {
        TextView lockText = this.findViewById(R.id.tv_setting_choose_title);
        lockText.setText(title);
        lockButton = this.findViewById(R.id.tv_setting_check_title);
        TextView lockDes = this.findViewById(R.id.tv_setting_choose_des);
        lockDes.setVisibility(View.VISIBLE);
        lockDes.setText("在应用设置中开启：锁屏显示/后台弹出界面权限");
        final LinearLayout linearLayout = this.findViewById(R.id.ll_setting_choose_parent);
        final List<String> list = new ArrayList<>();
        list.add(cpuH5);
        final BottomDialog.Builder builder = new BottomDialog.Builder(MediaExamplesActivity.this)
                .addMenu(list)
                .setCancelText(close)
                .setTitle(title)
                .setCancelListener(new BottomDialog.BottomItemClickListener() {
                    @Override
                    public void bottomItemClickListener(String title, View view) {
                        lockButton.setText(title);
                        LockScreenService.stopLockScreenService(getApplicationContext());
                        LOCK_TYPE_SETTING = -1;
                    }
                })
                .setItemListener(new BottomDialog.ItemClickListener() {
                    @Override
                    public void itemClickListener(String title, int position) {
                        if (LockScreenService.canBackgroundStart(MediaExamplesActivity.this)) {
                            lockButton.setText(title);
                            LockScreenService.startLockScreenService(getApplicationContext());
                            LOCK_TYPE_SETTING = position;
                        }
                    }
                });
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.create().show();
            }
        });
    }
}
