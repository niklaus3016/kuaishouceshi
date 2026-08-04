package com.mijingxingzuo.app;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsInitCallback;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.SdkConfig;

/**
 * 快手 SDK 初始化工具类。
 * <p>
 * 1. 强烈建议媒体调用 SDK 方法前先判断 SDK 是否已初始化，
 *    未初始化时先尝试补初始化，避免进程恢复导致空指针问题。
 * 2. SDK 只在主进程初始化一次；重复调用 init 会被忽略。
 */
public final class KSSdkInitUtil {
    private static final String TAG = "KSAdSDK";

    private static final String APP_ID = "3538300001";     // 快手联盟正式 AppId
    private static final String APP_NAME = "秘境星座";      // 应用真实名称
    private static final String APP_WB_KEY = "cK7PgwbAr";  // 对外可传空或实际申请值

    private static volatile boolean sHasInit;
    private static volatile boolean sHasStart;
    private static Context sAppContext;

    private KSSdkInitUtil() {
    }

    public static synchronized void initSDK(@NonNull Context context) {
        if (sHasInit) {
            return;
        }
        Log.i(TAG, "init sdk start");
        sHasInit = true;
        sAppContext = context.getApplicationContext();
        final long startTime = System.currentTimeMillis();

        KsAdSDK.init(sAppContext, new SdkConfig.Builder()
                .appId(APP_ID)
                .appName(APP_NAME)
                .showNotification(true)
                .customController(UserDataObtainController.getInstance())
                .debug(BuildConfig.DEBUG)
                .setInitCallback(new KsInitCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "init success, cost: " + (System.currentTimeMillis() - startTime) + "ms");
                    }

                    @Override
                    public void onFail(int code, String msg) {
                        Log.e(TAG, "init fail code:" + code + " msg:" + msg);
                        // 初始化失败允许降级：下次调用 getLoadManager 时会再次尝试
                        sHasInit = false;
                    }
                })
                .setStartCallback(new KsInitCallback() {
                    @Override
                    public void onSuccess() {
                        sHasStart = true;
                        Log.i(TAG, "start success");
                    }

                    @Override
                    public void onFail(int code, String msg) {
                        sHasStart = false;
                        Log.e(TAG, "start fail code:" + code + " msg:" + msg);
                    }
                })
                .build());

        // init 完成后主动 start，避免首次加载广告时才 start 带来延迟
        try {
            KsAdSDK.start();
        } catch (Throwable t) {
            Log.e(TAG, "KsAdSDK.start 异常: " + t.getMessage(), t);
        }
    }

    /**
     * 隐私同意后调用：更新 CustomController 的同意状态，
     * 如果此前未同意导致未 init/start，则此时补一次初始化。
     */
    public static synchronized void onUserAgreePrivacy(@NonNull Context context) {
        UserDataObtainController.getInstance().setUserAgree(true);
        if (!sHasInit) {
            initSDK(context);
        }
    }

    public static boolean hasInit() {
        return sHasInit;
    }

    private static void checkSDKInit() {
        if (!sHasInit && sAppContext != null) {
            // 进程恢复等场景下的补初始化
            initSDK(sAppContext);
        }
    }

    /**
     * 获取快手 SDK 的 LoadManager。
     * 若 SDK 尚未初始化，则尝试补初始化；补初始化失败时返回 null，
     * 调用方需对返回 null 的情况做降级处理，避免抛异常导致崩溃。
     */
    @NonNull
    public static KsLoadManager getLoadManager() {
        checkSDKInit();
        return KsAdSDK.getLoadManager();
    }

    /**
     * 返回快手 SDK 场景参数构造器。
     */
    public static KsScene.Builder createKSSceneBuilder(long posId) {
        checkSDKInit();
        KsScene.Builder builder = null;
        try {
            builder = new KsScene.Builder(posId);
            // 跳转第三方 App 后返回时调起本 App 所用 scheme
            builder.setBackUrl("ksad://returnback");
        } catch (Throwable e) {
            Log.e(TAG, "createKSSceneBuilder 异常: " + e.getMessage(), e);
        }
        return builder;
    }

    /**
     * 获取服务端竞价 token（S2S Bidding 时使用）。
     * 非必须；仅当接入方走服务端竞价流程时才需要。
     */
    public static String getBidRequestToken(long posId) {
        try {
            KsScene scene = createKSSceneBuilder(posId).build();
            String token = getLoadManager().getBidRequestToken(scene);
            if (TextUtils.isEmpty(token)) {
                Log.w(TAG, "getBidRequestToken 返回空");
            }
            return token;
        } catch (Throwable t) {
            Log.e(TAG, "getBidRequestToken 异常: " + t.getMessage(), t);
            return "";
        }
    }
}
