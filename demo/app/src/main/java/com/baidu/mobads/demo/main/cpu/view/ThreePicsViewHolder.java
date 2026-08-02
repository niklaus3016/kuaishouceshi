package com.baidu.mobads.demo.main.cpu.view;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.IBasicCPUData;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import static com.baidu.mobads.demo.main.cpu.activity.NativeCPUAdActivity.TAG;

/**
 * author: ZhangYubin
 * date: 2021/2/18 3:22 PM
 * desc:
 */
public class ThreePicsViewHolder extends AbstractViewHolder {

    private final ImageView imageView0;
    private final ImageView imageView1;
    private final ImageView imageView2;
    private final LinearLayout container;
    public ThreePicsViewHolder(View view) {
        super(view);
        container = view.findViewById(R.id.threepic_container);
        imageView0 = view.findViewById(R.id.image_left);
        imageView1 = view.findViewById(R.id.image_mid);
        imageView2 = view.findViewById(R.id.image_right);
    }

    @Override
    public void initWidgetWithData(final IBasicCPUData nrAd, int position) {
        super.initWidgetWithData(nrAd, position);

        if (nrAd.getType().equals("ad")) {
            Glide.with(mCtx).load(imageList.get(0)).into(imageView0);
            Glide.with(mCtx).load(imageList.get(1)).into(imageView1);
            Glide.with(mCtx).load(imageList.get(2)).into(imageView2);
        } else {
            Glide.with(mCtx).load(smallImageList.get(0)).into(imageView0);
            Glide.with(mCtx).load(smallImageList.get(1)).into(imageView1);
            Glide.with(mCtx).load(smallImageList.get(2)).into(imageView2);
        }


        List<View> clickViews = new ArrayList<>();
        List<View> creativeViews = new ArrayList<>();


        if (titleView != null) {
            clickViews.add(titleView);
        }

        clickViews.add(imageView0);
        clickViews.add(imageView1);
        clickViews.add(imageView2);


        /**
         * 注册可点击的View，点击和曝光会在内部完成
         * @Param view 广告容器或广告View
         * 【【 该View只负责发送展现 】】
         * @Param clickViews 可点击的View，默认展示下载整改弹框。
         * @Param creativeViews 带有广告文案之类的View，点击不会触发下载整改弹框。
         * @Param interactionListener 点击、曝光回调
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
//                Log.d(TAG, "performance: "  + act);
                List<Integer> contentAttributesList = nrAd.getContentAttributesList();
                if (contentAttributesList != null && contentAttributesList.size() > 0) {
                    Integer integer = contentAttributesList.get(0);
                    Log.d(TAG, "type:" + nrAd.getType() + ",contentAttributesList:" + integer);
                }
                Log.d(TAG, "type:" + nrAd.getType() + ",ReadCounts:" + nrAd.getReadCounts());
                Log.d(TAG, "type:" + nrAd.getType() + ",CommentCounts:" + nrAd.getCommentCounts());
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
