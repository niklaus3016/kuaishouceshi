package com.baidu.mobads.demo.main.rewardvideo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;
import com.baidu.mobads.demo.main.permission.BasePermissionActivity;
import com.baidu.mobads.sdk.api.RewardVideoAd;

public class RewardTaskBActivity extends BasePermissionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_task_b_activity);

        Button btn1 = this.findViewById(R.id.btn_show_reward);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 展示激励视频广告
                showLoadedVideo(false);
            }
        });

        Button btn2 = this.findViewById(R.id.btn_show_reward_switch);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 展示激励视频广告（切换上下文）
                showLoadedVideo(true);
            }
        });
    }

    /**
     * 展示已加载的激励视频广告，需要提前进行load
     */
    private void showLoadedVideo(boolean switchContext) {
        RewardVideoAd mRewardVideoAd = RewardTaskAActivity.GlobalRewardAd.getInstance().mRewardVideoAd;
        if (mRewardVideoAd == null || RewardTaskAActivity.GlobalRewardAd.getInstance().needReload) {
            toastText("请在加载成功后进行广告展示！");
            return;
        }
        // 1. 强烈建议在收到onVideoDownloadSuccess回调、视频物料缓存完成后再展示广告，
        //    提升激励视频的播放体验，否则有播放卡顿的风险。
        // 2. 在展示前可以调用isReady接口判断广告是否就绪：
        //    此接口会判断本地是否存在【未展示 & 未过期 & 已缓存】的广告
        if (!mRewardVideoAd.isReady()) {
            toastText("视频广告未缓存/已展示/已过期");
            return;
        }
        // 是否在跳过按钮后展示弹框 (默认点击跳过不展示弹框) , 可在广告配置页面配置
        boolean isShowDialog = AdSettingHelper.getInstance()
                .getBooleanFromSetting(AdSettingProperties.REWARD_VIDEO_SHOW_DIALOG, false);
        mRewardVideoAd.setShowDialogOnSkip(isShowDialog);
        // show之前必须调用load请求广告，否则无效
        if (switchContext) {
            mRewardVideoAd.show(RewardTaskBActivity.this);
        } else {
            mRewardVideoAd.show();
        }
    }

    private void toastText(String text) {
        Toast.makeText(RewardTaskBActivity.this, text, Toast.LENGTH_SHORT).show();
    }
}