package com.mijingxingzuo.app;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.kwad.sdk.api.ApiConst;
import com.kwad.sdk.api.KsInnerAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsRewardVideoAd;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.kwad.sdk.api.SdkConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CapacitorPlugin(name = "KuaiShouAd")
public class KuaiShouAdPlugin extends Plugin {

    private static final String TAG = "KuaiShouAdPlugin";

    /** 缓存的激励视频广告对象 */
    @Nullable
    private KsRewardVideoAd mRewardVideoAd;

    /** 当前加载的广告位 posId（long 型） */
    private long mCurrentPosId;

    /** 用于 showRewardVideoAd 时 resolve 的 pending call（兼容旧逻辑，保持可选） */
    @Nullable
    private PluginCall pendingShowCall;

    /** 防止 onRewardVerify 两个重载重复触发 */
    private boolean mRewardHandled;

    /** 广告是否已真正展示（用于区分"正常关闭"和"未展示就失效"） */
    private volatile boolean isAdShown;

    // ================================================================
    // 对外 Capacitor 方法
    // ================================================================

    @PluginMethod
    public void loadRewardVideoAd(PluginCall call) {
        String adId = call.getString("adId");
        if (adId == null || adId.isEmpty()) {
            call.reject("广告ID不能为空");
            return;
        }

        long posId;
        try {
            posId = Long.parseLong(adId);
        } catch (NumberFormatException e) {
            call.reject("广告ID格式错误: " + adId);
            return;
        }
        mCurrentPosId = posId;
        mRewardHandled = false; // 重置激励回调标志位
        isAdShown = false;      // 重置展示标志位

        Log.d(TAG, "加载激励视频广告, posId: " + posId + " (raw: " + adId + ")");

        Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activity 为空");
            return;
        }

        // 确保 SDK 已初始化
        if (!KSSdkInitUtil.hasInit()) {
            Log.w(TAG, "SDK 未初始化，触发初始化后再加载广告");
            UserDataObtainController.getInstance().setUserAgree(true);
            KSSdkInitUtil.initSDK(activity.getApplicationContext());
        }

        // 延迟等待 SDK 初始化完成（init 回调可能需要数百毫秒）
        activity.runOnUiThread(() -> {
            // 再检查一次 SDK 状态
            if (!KSSdkInitUtil.hasInit()) {
                Log.w(TAG, "SDK 仍未就绪（可能初始化失败），尝试继续加载");
            }
            doLoadRewardVideoAd(call, activity, posId);
        });
    }

    @PluginMethod
    public void showRewardVideoAd(PluginCall call) {
        Log.d(TAG, "显示激励视频广告, mRewardVideoAd=" + mRewardVideoAd);

        Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activity 为空");
            return;
        }

        if (mRewardVideoAd == null) {
            call.reject("广告未加载");
            return;
        }

        activity.runOnUiThread(() -> {
            try {
                pendingShowCall = call;
                isAdShown = true; // 标记为已展示

                // 设置监听（在 show 前挂载，与官方Demo一致，避免预加载阶段误触发）
                setRewardListener(mRewardVideoAd);

                // 展示配置：默认竖屏播放（与 App 屏幕方向一致）
                KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
                        .showLandscape(false)
                        .build();

                mRewardVideoAd.showRewardVideoAd(activity, videoPlayConfig);
                Log.d(TAG, "showRewardVideoAd 调用完成, 等待回调");
                // showRewardVideoAd 异步展示，真正结果通过回调通知，不在此立刻 resolve
            } catch (Exception e) {
                Log.e(TAG, "展示激励视频广告异常: " + e.getMessage(), e);
                pendingShowCall = null;
                isAdShown = false;
                call.reject("展示广告异常: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void isReady(PluginCall call) {
        JSObject result = new JSObject();
        boolean ready = mRewardVideoAd != null;
        result.put("ready", ready);
        call.resolve(result);
    }

    /**
     * 前端上报用户已同意隐私协议；SDK 会在此时真正 init（若之前尚未 init）。
     * 若业务未单独调用此方法，则首次 loadRewardVideoAd 时会默认视为同意。
     */
    @PluginMethod
    public void onUserAgreePrivacy(PluginCall call) {
        Activity activity = getActivity();
        if (activity != null) {
            KSSdkInitUtil.onUserAgreePrivacy(activity.getApplicationContext());
        }
        call.resolve();
    }

    // ================================================================
    // 内部实现
    // ================================================================

    private void doLoadRewardVideoAd(@NonNull final PluginCall call,
                                     @NonNull Activity activity,
                                     final long posId) {
        try {
            Log.d(TAG, "doLoadRewardVideoAd start, posId=" + posId
                    + " hasInit=" + KSSdkInitUtil.hasInit());

            // 屏幕方向（默认竖屏）
            int screenOrientation = SdkConfig.SCREEN_ORIENTATION_PORTRAIT;
            try {
                int requested = activity.getRequestedOrientation();
                if (requested == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        || requested == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        || requested == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    screenOrientation = SdkConfig.SCREEN_ORIENTATION_LANDSCAPE;
                }
            } catch (Throwable ignore) {
            }

            // 服务端回调扩展数据（此处留空占位，业务如需 S2S 激励回调再填充 thirdUserId / extraData）
            Map<String, String> rewardExtraData = new HashMap<>();

            KsScene.Builder sceneBuilder = KSSdkInitUtil.createKSSceneBuilder(posId);
            if (sceneBuilder == null) {
                Log.e(TAG, "createKSSceneBuilder 返回 null，posId 可能无效: " + posId);
                call.reject("广告场景创建失败，posId 无效: " + posId);
                return;
            }
            sceneBuilder.screenOrientation(screenOrientation)
                    .rewardCallbackExtraData(rewardExtraData);

            KsScene scene = sceneBuilder.build();
            Log.d(TAG, "KsScene built, posId=" + posId + " scene=" + scene);

            final long startTime = System.currentTimeMillis();

            KsLoadManager loadManager = KSSdkInitUtil.getLoadManager();
            Log.d(TAG, "loadManager=" + loadManager);

            loadManager.loadRewardVideoAd(scene, new KsLoadManager.RewardVideoAdListener() {
                @Override
                public void onError(int code, String msg) {
                    Log.e(TAG, "激励视频广告请求失败 code=" + code + " msg=" + msg
                            + " posId=" + posId + " cost=" + (System.currentTimeMillis() - startTime) + "ms");
                    JSObject err = new JSObject();
                    err.put("code", code);
                    err.put("error", msg == null ? "未知错误" : msg);
                    notifyListeners("onAdFailed", err);
                    // 视频下载失败事件（语义映射，与百度端事件对齐）
                    notifyListeners("onVideoDownloadFailed", err);
                }

                @Override
                public void onRewardVideoResult(@Nullable List<KsRewardVideoAd> adList) {
                    // 视频广告数据请求完成（未缓存资源，可在线播放）
                    Log.d(TAG, "激励视频广告数据请求成功 (onRewardVideoResult), adList.size="
                            + (adList != null ? adList.size() : "null")
                            + " cost=" + (System.currentTimeMillis() - startTime) + "ms");
                    notifyListeners("onAdLoaded", new JSObject());

                    cacheFirstAdIfAvailable(adList);
                }

                @Override
                public void onRewardVideoAdLoad(@Nullable List<KsRewardVideoAd> adList) {
                    // 视频广告数据+资源缓存全部完成（本地播放流畅，对应 onVideoDownloadSuccess）
                    Log.d(TAG, "激励视频广告数据+资源缓存成功 (onRewardVideoAdLoad), adList.size="
                            + (adList != null ? adList.size() : "null")
                            + " cost=" + (System.currentTimeMillis() - startTime) + "ms");

                    cacheFirstAdIfAvailable(adList);

                    notifyListeners("onVideoDownloadSuccess", new JSObject());
                }
            });

            Log.d(TAG, "loadRewardVideoAd request sent, posId=" + posId);
            call.resolve();
        } catch (Throwable t) {
            Log.e(TAG, "加载激励视频广告异常: " + t.getMessage(), t);
            call.reject("加载广告异常: " + t.getMessage());
        }
    }

    private void cacheFirstAdIfAvailable(@Nullable List<KsRewardVideoAd> adList) {
        if (adList == null || adList.isEmpty()) {
            return;
        }
        KsRewardVideoAd first = adList.get(0);
        if (first != null) {
            mRewardVideoAd = first;
            // 注意：不在此处挂载 setRewardListener，避免预加载阶段 SDK 触发 onPageDismiss
            // 时把缓存清空。监听器统一在 showRewardVideoAd 调用前挂载（与官方Demo一致）。
            Log.d(TAG, "广告已缓存, posId=" + mCurrentPosId + " ad=" + first);
        }
    }

    private void setRewardListener(@NonNull final KsRewardVideoAd rewardVideoAd) {
        // 内部广告（广告中卡片等）交互监听
        rewardVideoAd.setInnerAdInteractionListener(new KsInnerAd.KsInnerAdInteractionListener() {
            @Override
            public void onAdClicked(KsInnerAd ksInnerAd) {
                Log.d(TAG, "激励视频内部广告点击, type=" + (ksInnerAd != null ? ksInnerAd.getType() : "?"));
                notifyListeners("onAdClicked", new JSObject());
            }

            @Override
            public void onAdShow(KsInnerAd ksInnerAd) {
                Log.d(TAG, "激励视频内部广告曝光, type=" + (ksInnerAd != null ? ksInnerAd.getType() : "?"));
                notifyListeners("onAdShow", new JSObject());
            }
        });

        // 主激励视频广告交互监听
        rewardVideoAd.setRewardAdInteractionListener(new KsRewardVideoAd.RewardAdInteractionListener() {
            @Override
            public void onAdClicked() {
                Log.d(TAG, "激励视频广告点击");
                notifyListeners("onAdClicked", new JSObject());
            }

            @Override
            public void onPageDismiss() {
                Log.d(TAG, "激励视频广告关闭 (onPageDismiss), isAdShown=" + isAdShown);

                if (isAdShown) {
                    // 正常关闭流程：广告已展示，用户看完或跳过
                    notifyListeners("onAdClose", new JSObject());

                    // 如果 onRewardVerify 没有触发（例如用户跳过），则广告关闭时 resolve pendingShowCall
                    if (pendingShowCall != null) {
                        Log.d(TAG, "广告关闭时 resolve pendingShowCall（未获得激励）");
                        JSObject result = new JSObject();
                        result.put("rewardVerify", false);
                        result.put("ecpm", 0);
                        pendingShowCall.resolve(result);
                        pendingShowCall = null;
                    }
                } else {
                    // 未展示就失效：预加载的广告在 show 之前被 SDK 回收/过期
                    Log.w(TAG, "广告未展示就被关闭（预加载失效），通知前端 onAdExpired");
                    JSObject expired = new JSObject();
                    expired.put("posId", mCurrentPosId);
                    expired.put("reason", "expired_before_show");
                    notifyListeners("onAdExpired", expired);
                }

                // 展示完毕或失效后清空缓存广告，避免重复展示过期素材
                mRewardVideoAd = null;
                isAdShown = false;
            }

            @Override
            public void onVideoPlayError(int code, int extra) {
                Log.e(TAG, "激励视频广告播放出错 code=" + code + " extra=" + extra);
                JSObject err = new JSObject();
                err.put("code", code);
                err.put("extra", extra);
                notifyListeners("onVideoPlayError", err);
            }

            @Override
            public void onVideoPlayEnd() {
                Log.d(TAG, "激励视频广告播放完成");
                notifyListeners("onVideoPlayEnd", new JSObject());
            }

            @Override
            public void onVideoSkipToEnd(long playDuration) {
                Log.d(TAG, "激励视频广告跳过播放完成, playDuration=" + playDuration);
                JSObject data = new JSObject();
                data.put("playDuration", playDuration);
                notifyListeners("onVideoSkipToEnd", data);
            }

            @Override
            public void onVideoPlayStart() {
                Log.d(TAG, "激励视频广告播放开始 (onVideoPlayStart)");
                notifyListeners("onAdShow", new JSObject());
                notifyListeners("onVideoPlayStart", new JSObject());
            }

            /**
             * 激励成功回调（简化版）。只会回调一次。
             */
            @Override
            public void onRewardVerify() {
                Log.d(TAG, "激励视频广告获得激励 (onRewardVerify simple)");
                handleRewardVerify(null);
            }

            /**
             * 激励成功回调（扩展版，携带 IS_FRAUD / ERRORCODE 等附加信息）。
             * 只会回调一次。若此重载被触发，则上一个简化版不会再触发。
             */
            @Override
            public void onRewardVerify(Map<String, Object> extraMap) {
                Log.d(TAG, "激励视频广告获得激励 (onRewardVerify with extra)");
                handleRewardVerify(extraMap);
            }

            @Override
            public void onRewardStepVerify(int taskType, int currentTaskStatus) {
                // 分阶段激励：看视频/浏览落地页/下载使用App等深度任务
                Log.d(TAG, "激励视频分阶段激励 taskType=" + taskType
                        + " currentTaskStatus=" + currentTaskStatus);
                JSObject data = new JSObject();
                data.put("taskType", taskType);
                data.put("currentTaskStatus", currentTaskStatus);
                notifyListeners("onRewardStepVerify", data);
            }

            @Override
            public void onExtraRewardVerify(int extraRewardType) {
                Log.d(TAG, "激励视频广告额外奖励 extraRewardType=" + extraRewardType);
                JSObject data = new JSObject();
                data.put("extraRewardType", extraRewardType);
                notifyListeners("onExtraRewardVerify", data);
            }
        });
    }

    /**
     * 统一处理激励回调（简单版与扩展版共用）。
     * 前端通过 onRewardVerify 事件收到结果，同时 resolve pendingShowCall（若存在）。
     * 通过 mRewardHandled 标志位防止两个重载重复触发。
     */
    private void handleRewardVerify(@Nullable Map<String, Object> extraMap) {
        if (mRewardHandled) {
            Log.w(TAG, "onRewardVerify 已处理过，跳过重复触发");
            return;
        }
        mRewardHandled = true;

        JSObject result = new JSObject();
        result.put("rewardVerify", true);

        // 将扩展信息注入结果（含反作弊字段）
        if (extraMap != null) {
            for (Map.Entry<String, Object> entry : extraMap.entrySet()) {
                try {
                    result.put(entry.getKey(), entry.getValue());
                } catch (Throwable ignore) {
                }
            }
            // 常见字段：反作弊标记
            Object isFraud = extraMap.get(ApiConst.EXTRA_KEY_FRAUD);
            Object fraudCode = extraMap.get(ApiConst.EXTRA_KEY_ERRORCODE);
            if (isFraud != null) result.put("IS_FRAUD", isFraud);
            if (fraudCode != null) result.put("IS_FRAUD_ERROR_CODE", fraudCode);
        }

        // eCPM：快手侧不在激励回调直接给 eCPM 数值（一般走 S2S 返给服务端），
        // 因此此处默认传 0，前端 useAdManager 会根据广告位生成模拟 eCPM。
        result.put("ecpm", 0);
        result.put("posId", mCurrentPosId);

        Log.d(TAG, "激励回调结果: rewardVerify=true ecpm=0 posId=" + mCurrentPosId);

        notifyListeners("onRewardVerify", result);

        if (pendingShowCall != null) {
            pendingShowCall.resolve(result);
            pendingShowCall = null;
        }
    }
}
