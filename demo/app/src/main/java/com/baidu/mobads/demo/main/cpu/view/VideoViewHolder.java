package com.baidu.mobads.demo.main.cpu.view;

import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.CpuVideoView;
import com.baidu.mobads.sdk.api.IBasicCPUData;

import java.util.ArrayList;
import java.util.List;

import static com.baidu.mobads.demo.main.cpu.activity.NativeCPUAdActivity.TAG;

/**
 * author: ZhangYubin
 * date: 2021/2/18 3:21 PM
 * desc:
 */
public class VideoViewHolder extends AbstractViewHolder {

    private final CpuVideoView cpuVideoView;

    private final ConstraintLayout container;
    public VideoViewHolder(View view) {
        super(view);
        container = view.findViewById(R.id.video_container);
        cpuVideoView = view.findViewById(R.id.cpu_video_container);

    }

    @Override
    public void initWidgetWithData(final IBasicCPUData nrAd, int position) {
        super.initWidgetWithData(nrAd, position);
        cpuVideoView.setVideoConfig(nrAd);
        cpuVideoView.setCpuVideoStatusListener(new CpuVideoView.CpuVideoStatusListener() {
            @Override
            public void playRenderingStart() {
                Log.d(TAG, "playRenderingStart: ");
            }

            @Override
            public void playPause() {
                Log.d(TAG, "playPause: " );
            }

            @Override
            public void playResume() {
                Log.d(TAG, "playResume: ");
            }

            @Override
            public void playCompletion() {
                Log.d(TAG, "playCompletion: ");
            }

            @Override
            public void playError() {
                Log.d(TAG, "playError: ");
            }
        });

        List<View> clickViews = new ArrayList<>();
        List<View> creativeViews = new ArrayList<>();

        if (titleView != null) {
            clickViews.add(titleView);
        }

        /**
         * 注册可点击的View，点击和曝光会在内部完成
         * @Param view 广告容器或广告View
         * 【【 该View只负责发送展现 】】
         * @Param clickViews 可点击的View，默认展示下载整改弹框。
         * @Param creativeViews 带有广告文案之类的View，点击不会触发下载整改弹框。
         * @Param interactionListener
         * 点击、曝光回调
         */
        nrAd.registerViewForInteraction(container, clickViews, creativeViews, new IBasicCPUData.CpuNativeStatusCB() {
            @Override
            public void onAdDownloadWindowShow() {
                Log.d(TAG, "onAdDownloadWindowShow: ");
            }

            @Override
            public void onPermissionShow() {
                Log.d(TAG, "onPermissionShow: ");
            }

            @Override
            public void onPermissionClose() {
                Log.d(TAG, "onPermissionClose: ");
            }

            @Override
            public void onPrivacyClick() {
                Log.d(TAG, "onPrivacyClick: ");
            }
            @Override
            public void onPrivacyLpClose() {
                Log.d(TAG, "onPrivacyLpClose: ");
            }

            /**
             *  内容/广告 的 展现、点击行为的告知
             * @param act
             */
            @Override
            public void onNotifyPerformance(String act) {
                Log.d(TAG, "performance: "  + act + ",nrAd.hashCode = " + nrAd.hashCode());
                List<Integer> contentAttributesList = nrAd.getContentAttributesList();
                if (contentAttributesList != null && contentAttributesList.size() > 0) {
                    Integer integer = contentAttributesList.get(0);
                    Log.d(TAG, "type:" + nrAd.getType() + ",contentAttributesList:" + integer);
                }
                Log.d(TAG, "type:" + nrAd.getType() + ",ReadCounts:" + nrAd.getReadCounts());
                Log.d(TAG, "type:" + nrAd.getType() + ",CommentCounts:" + nrAd.getCommentCounts());
                if (container != null && nrAd.isNeedDownloadApp()) {
                    Toast.makeText(container.getContext(), "开始下载，可长按下载按钮取消下载",
                            Toast.LENGTH_LONG).show();
                }
            }

            /**
             * 下载进度回传
             * @param pkg
             * @param progress 下载进度
             */
            @Override
            public void onAdStatusChanged(String pkg, int progress) {
                Log.d(TAG, "pkg = " + pkg  + ", onAdStatusChanged: " + progress);
                if (mApdownloadTv != null) {
                    mApdownloadTv.setProgress();
                }
            }
        });


    }
}