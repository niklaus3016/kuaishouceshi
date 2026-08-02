package com.baidu.mobads.demo.main.novelprod;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


import com.baidu.mobads.demo.main.MobadsApplication;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
//import com.baidu.searchbox.novelcoreinterface.NovelExternalApi;
//import com.baidu.searchbox.novelinterface.NovelTraceApi;
//import com.haha.tt.api.CPUNovelAd;
//import com.haha.tt.api.CPUWebAdRequestParam;
//import com.haha.tt.api.NovelSDKConfig;
import com.baidu.mobads.sdk.api.CPUNovelAd;
import com.baidu.mobads.sdk.api.CPUWebAdRequestParam;
import com.baidu.mobads.sdk.api.NovelSDKConfig;
//import com.baidu.searchbox.discovery.novel.database.NovelBookInfo;
//import com.baidu.searchbox.novelinterface.NovelTraceDelegate;
//import com.baidu.searchbox.novelinterface.info.NovelInfo;

import java.util.UUID;



/**
 * 前提：需要接入小说SDK，否则会出异常
 */
public class CpuNovelActivity extends Activity {

    public static final String TAG = "CpuNovelActivity";

    public static final String TEST_APPSID = "c0da1ec4";

    private FrameLayout mNovelContainer;
    private Button mNightBt, mLightBt;
    private CPUNovelAd cpuNovelAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_novel);
        mNovelContainer = findViewById(R.id.novel_container);
        mNightBt = findViewById(R.id.night);
        mLightBt = findViewById(R.id.light);
//        mNightBt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                NovelExternalApi.setExternalMediaNightMode(true);
//            }
//        });
//        mLightBt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                NovelExternalApi.setExternalMediaNightMode(false);
//            }
//        });
//
//        // 媒体可通过设置该接口获取更多的信息
//        NovelTraceApi.setTraceDelegate(new NovelTraceDelegate() {
//            @Override
//            public void feedShow(long l) {
//                Log.d(TAG, "feedShow: l = " + l);
//            }
//
//            @Override
//            public void feedQuit(long l) {
//                Log.d(TAG, "feedQuit: l = " + l);
//            }
//
//            @Override
//            public void feedDuration(float v) {
//                Log.d(TAG, "feedDuration: v = " + v);
//            }
//
//            @Override
//            public void enterReader(NovelInfo novelInfo) {
//                Log.d(TAG, "enterReader: " + novelInfo.novelName);
//            }
//
//            @Override
//            public void quitReader(NovelInfo novelInfo) {
//                Log.d(TAG, "quitReader: " + novelInfo.readerDuration);
//            }
//        });

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

        // 媒体可以通过该方法检测小说SDK是否初始化成功
        boolean initNovelSDK = NovelSDKConfig.isInitNovelSDK();
        if (!initNovelSDK) {
            // 若没有初始化成功，额外提供一个初始化方法
            // 第一个参数要求是Application类型
            NovelSDKConfig.attachBaseContext(MobadsApplication.getContext(), "Your AppSid", "Your App Name");
        }


        CPUWebAdRequestParam cpuWebAdRequestParam = new CPUWebAdRequestParam.Builder()
                .setCustomUserId(outerId)
                .setSubChannelId("88066") // 填写媒体申请的子渠道，如果没有申请不用设置该方法
                .build();
         cpuNovelAd = new CPUNovelAd(this, TEST_APPSID, cpuWebAdRequestParam,
                new CPUNovelAd.CpuNovelListener() {
                    @Override
                    public void onAdClick() {
                        Log.e(TAG, "onAdClick: "  );
                    }

                    @Override
                    public void onAdImpression() {
                        Log.e(TAG, "onAdImpression: " );
                    }

                    @Override
                    public void onReadTime(long duration) {
                        Log.e(TAG, "onReadTime:  = " + duration);
                    }
                });
        View novelView = cpuNovelAd.getNovelView();
        if (novelView != null) {
            mNovelContainer.addView(novelView);
        }

    }

    @Override
    protected void onDestroy() {
        // 调用该方法降低内存泄露
        if (cpuNovelAd != null) {
            cpuNovelAd.destory();
        }
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cpu_novel_menu, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.cpu_novel_history :
//                final NovelBookInfo recommendBookInfo = NovelExternalApi.getReadHistoryBookInfo();
//                Dialog dialog = new Dialog(CpuNovelActivity.this);
//                View inflate = LayoutInflater.from(CpuNovelActivity.this).inflate(R.layout.dialog_novel, null);
//                ImageView coverimg = inflate.findViewById(R.id.novel_cover);
//                TextView author = inflate.findViewById(R.id.novel_name);
//                TextView chapter = inflate.findViewById(R.id.novel_chapter);
//                Glide.with(CpuNovelActivity.this).load(recommendBookInfo.getCoverUrl()).into(coverimg);
//                author.setText(recommendBookInfo.getName());
//                chapter.setText(recommendBookInfo.getCurrentChapterName());
//                dialog.setContentView(inflate);
//                dialog.setTitle("阅读历史");
//                dialog.setCanceledOnTouchOutside(true);
//                dialog.show();
//
//                inflate.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        // 注意：调用小说aar方法，发送点击日志
//                        Log.e(TAG, "onClick: 点击书籍");
////                        NovelExternalApi.openReader(CpuNovelActivity.this, recommendBookInfo);
//                        NovelExternalApi.openReaderFromHistory(CpuNovelActivity.this, recommendBookInfo);
//                        NovelExternalApi.onReadHistoryClick();
//                        NovelExternalApi.setExtInfoService(new NovelExternalApi.IExtInfoService() {
//                            @Override
//                            public Map<String, String> getExtInfo() {
//                                HashMap<String, String> test = new HashMap<>();
//                                test.put("bb", "a");
//                                test.put("vv", "b");
//                                return test;
//                            }
//                        });
//                        NovelExternalApi.setExtInfo("ds", "c");
//                    }
//                });
//
//                coverimg.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
//                    @Override
//                    public void onViewAttachedToWindow(View v) {
//                        // 注意：调用小说aar方法，发送展现日志
//                        Log.e(TAG, "onViewAttachedToWindow: " );
//                        NovelExternalApi.onReadHistoryShow();
//                    }
//
//                    @Override
//                    public void onViewDetachedFromWindow(View v) {
//
//                    }
//                });
//
////
////                Log.e(TAG, "onOptionsItemSelected: recommendBookInfo = " + recommendBookInfo.toString());
//                break;
//
//            default:
//                break;
//        }
//        return true;
//    }

}