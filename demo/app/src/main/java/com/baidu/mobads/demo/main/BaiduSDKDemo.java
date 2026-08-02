package com.baidu.mobads.demo.main;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.adSettings.AdDocumentActivity;
import com.baidu.mobads.demo.main.adSettings.AdSettingHelper;
import com.baidu.mobads.demo.main.adSettings.AdSettingProperties;
import com.baidu.mobads.demo.main.cpu.activity.CpuAdActivity;

import com.baidu.mobads.demo.main.express.ExpressFeedSelectActivity;
import com.baidu.mobads.demo.main.feeds.NativeFeedSelectActivity;
import com.baidu.mobads.demo.main.feeds.video.FeedPortraitVideoActivity;
import com.baidu.mobads.demo.main.fullvideo.FullScreenVideoActivity;
import com.baidu.mobads.demo.main.jssdk.HybridInventoryActivity;
import com.baidu.mobads.demo.main.permission.BasePermissionActivity;
import com.baidu.mobads.demo.main.rewardvideo.RewardVideoActivity;
import com.baidu.mobads.demo.main.search.InsiteActivity;
import com.baidu.mobads.sdk.api.AdSettings;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashFocusParams;
import com.baidu.mobads.tools.ToolsActivity;

/**
 * Demo主界面，广告产品的展示列表
 * SDK 接入文档：https://union.baidu.com/docs/sdk/AndroidSDK.html
 * <p>
 * 集成提示：
 * 1. 请参考Demo中AndroidManifest配置相关配置，注意appsid（应用id）
 * 2. 设置SDK可以使用的权限，包含：设备信息、定位、存储、APP LIST。 注意：建议授权SDK读取设备信息，SDK会在应用获得系统权限后自行获取IMEI等设备信息，有助于提升ECPM。
 * 3. 接入广告的时候需要配置apid（广告位id），广告的appsid、appid、包名。三者有绑定关系，需要与百青藤后台配置的一致。且注意id前后不能出现空格。
 * 4. 如果您是更新SDK，请检查相关产品API是否变更。
 * 5. 广告业务提示：SDK中单次请求的广告不支持多次展现，多次展现只计费一次。信息流广告需要您手动发送广告的曝光和点击事件给SDK。
 */

public class BaiduSDKDemo extends BasePermissionActivity {

    public static final String TAG = "BaiduSDKDemo";

    private LinearLayout mAdTypeList;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * 【注意】开屏点睛需要开屏和主页的窗口具有特性 {@linkplain Window.FEATURE_ACTIVITY_TRANSITIONS}
         * Tips: 一般具有Material Design风格的App主题，系统会默认开启该特性
         *       若没有该特性，可以在{@link #setContentView(int)}之前
         *       调用 {@link #requestWindowFeature(int)} 即可开启特性，如下：
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        }
        setContentView(R.layout.main);
        SplashFocusParams params = new SplashFocusParams.Builder()
                .setIconRightMarginDp(15)
                .setIconBottomMarginDp(95)
                .build();
        /**
         * 开屏点睛需要在onCreate阶段注册，配合在开屏页的{@link SplashAd#finishAndJump(Intent)}使用
         * Tips: 请在{@link #setContentView(int)}之后调用该方法
         */
        SplashAd.registerEnterTransition(this, params, new SplashAd.SplashFocusAdListener() {
            @Override
            public void onAdIconShow() {
                Log.i(TAG, "onAdIconShow");
            }

            @Override
            public void onAdClick() {
                Log.i(TAG, "onAdClick");
            }

            @Override
            public void onAdClose() {
                Log.i(TAG, "onAdClose");
            }

            @Override
            public void onLpClosed() {
                Log.i(TAG, "onLpClosed");
            }
        });

        initView();

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");

        // 您可以在这里执行您的业务逻辑，比如发统计给服务器，让服务器统计退出概率， 或者APP运行时长.

        /**
         * 百度广告联盟建议您在退出APP前做两件事情
         *
         * 1. 通过BaiduXAdSDKContext.exit()来告知AdSDK，以便AdSDK能够释放资源.
         *
         * 2. 使用下面两行代码种的任意一行来冷酷无情的强制退出当前进程，以确保App本身资源得到释放。
         *      android.os.Process.killProcess(android.os.Process.myPid());
         *      System.exit(0);
         */
        // android.os.Process.killProcess(android.os.Process.myPid());
        // System.exit(0);

        super.onDestroy();
    }

    private void initView() {
        mAdTypeList = findViewById(R.id.item_container);
        LayoutInflater inflater = getLayoutInflater();

        bindButton(inflater, R.mipmap.ic_interstitial, "开屏", RSplashManagerActivity.class, true);

        bindButton(inflater, R.mipmap.ic_interstitial, "模板插屏(新插屏)", ExpressInterstitialActivity.class, true);

        bindButton(inflater, R.mipmap.ic_feed, "信息流-优选模板", ExpressFeedSelectActivity.class, true);

        bindButton(inflater, R.mipmap.ic_feed, "信息流-自渲染", NativeFeedSelectActivity.class, false);

        bindText(inflater, "");  // 添加空的TextView作为间隔

        bindButton(inflater, R.mipmap.ic_reward_video, "激励视频", RewardVideoActivity.class, true);

        bindButton(inflater, R.mipmap.ic_portrait_video, "竖版视频", FeedPortraitVideoActivity.class, true);

        bindButton(inflater, R.mipmap.ic_reward_video, "全屏视频", FullScreenVideoActivity.class, true);

        bindText(inflater, "");

        bindButton(inflater, R.mipmap.ic_cpu, "内容联盟", CpuAdActivity.class, true);
        bindButton(inflater, R.mipmap.ic_jssdk, "JSSDK", HybridInventoryActivity.class, true);
        bindButton(inflater, R.mipmap.ic_feed, "站内搜索", InsiteActivity.class, true);
        bindButton(inflater, R.mipmap.ic_interstitial, "促安装弹窗", PromoteInstallActivity.class, false);

        bindButton(inflater, R.drawable.ic_document, "SDK测试工具", ToolsActivity.class, false);
        bindButton(inflater, R.drawable.ic_document, "SDK接入文档", AdDocumentActivity.class, true);

        bindText(inflater, "");  // 添加空的TextView作为间隔
        // sdk version
        bindText(inflater, "v " + AdSettings.getSDKVersion());
        bindText(inflater, "");  // 添加空的TextView作为间隔
    }

    private void bindButton(LayoutInflater inflater, int iconId, String name, final Class clz, boolean showDivider) {
        View btn = inflater.inflate(R.layout.demo_ad_list_item, null);
        TextView textView = btn.findViewById(R.id.item_name);
        textView.setText(name);
        ImageView icon = btn.findViewById(R.id.left_icon);
        icon.setImageResource(iconId);
        View divider = btn.findViewById(R.id.divider);
        divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clz == RSplashManagerActivity.class) {
                    Intent intent = new Intent(BaiduSDKDemo.this, clz);
                    intent.putExtra("need_app_logo",
                            AdSettingHelper.getInstance().getBooleanFromSetting(AdSettingProperties.SPLASH_NEED_APP_LOGO, true));
                    intent.putExtra("exit_after_lp", false);
                    startActivity(intent);
                } else {
                    Log.e("this", clz + "");
                    startActivity(new Intent(BaiduSDKDemo.this, clz));
                }
            }
        });
        mAdTypeList.addView(btn);
    }

    private void bindText(LayoutInflater inflater, String text) {
        View item = inflater.inflate(R.layout.demo_ad_list_empty_view, null);
        TextView textView = item.findViewById(R.id.text);
        ImageView sdkIcon = item.findViewById(R.id.sdk_icon);
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
            sdkIcon.setVisibility(View.GONE);
        } else {
            textView.setText(text);
        }
        mAdTypeList.addView(item);
    }

    @Override
    public void onBackPressed() {
        /**
         * Tips：如果你的App需要实现点两次返回键后退出，请在该方法内做退出控制，而不是
         * 覆写{@link #finish()}方法，这是因为系统会在触发退场动画后，再调用finish方法；
         * 这会导致如果设置了过渡动画，用户退出时就会先看到退场动画，但应用实际并未退出，且用户无法进行操作（卡死）
         */
        super.onBackPressed();
    }

    @Override
    public void finishAfterTransition() {
        /**
         * Tips：如果你的App没有退出动画，推荐覆写本方法，转而调用{@link #finish()}，
         * 这样可以避免触发过渡动画的反转退出动画
         */
        super.finish();
    }
}