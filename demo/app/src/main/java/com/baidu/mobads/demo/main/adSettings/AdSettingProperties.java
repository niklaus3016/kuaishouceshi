package com.baidu.mobads.demo.main.adSettings;

/**
 * author: Lijinpeng
 * date: 2021/5/27
 * desc: 广告配置属性 (sp的key)
 */
public class AdSettingProperties {

    // 权限:获取位置
    public static final String COMMON_PERMISSION_LOCATION = "permission_location";

    // 权限:获取外部存储
    public static final String COMMON_PERMISSION_STORAGE = "permission_storage";

    // 权限:获取应用列表
    public static final String COMMON_PERMISSION_APP_LIST = "permission_app_list";

    // 权限:获取设备信息
    public static final String COMMON_PERMISSION_PHONE_STATE = "permission_read_phone_state";

    //开屏广告：选择半屏广告和全屏广告
    public static final String SPLASH_NEED_APP_LOGO = "splash_need_logo";

    //插屏广告：开启全屏模式
    public static final String INTERSTITIAL_FULL = "interstitial_full";

    //插屏广告：开启全屏模式
    public static final String INTERSTITIAL_DOWNLOAD = "interstitial_download";

    //开屏广告：点击开屏下载类广告时，是否弹出Dialog
    public static final String SPLASH_USE_DIALOG_FRAME = "splash_use_dialog_frame";

    //开屏广告：点击开屏下载类广告时，是否弹出Dialog
    public static final String SPLASH_OCCUPY = "splash_occupy";

    //开屏广告：是否展示工信部下载整改样式
    public static final String SPLASH_DISPLAY_DOWNLOAD = "splash_display_download";

    //信息流广告：点击开屏下载类广告时，是否弹出Dialog
    public static final String FEED_AD_NEED_DOWN_DIALOG = "feed_ad_down_dialog";
    public static final String FEED_BOOKMARK_HIDE_AD_LOGO = "feed_bm_hide_ad_logo";
    public static final String FEED_BOOKMARK_HIDE_MUTE = "feed_bm_hide_mute";
    public static final String FEED_BOOKMARK_HIDE_DISLIKE = "feed_bm_hide_dislike";
    public static final String FEED_BOOKMARK_MUTE = "feed_bm_mute";
    public static final String FEED_BOOKMARK_REGION_CLICK = "feed_bm_region_click";

    //激励视频：是否设置为SurfaceView
    public static final String REWARD_VIDEO_USE_SURFACE = "reward_use_surface";

    // 激励视频：点击跳过是否展示提示弹框
    public static final String REWARD_VIDEO_SHOW_DIALOG = "reward_show_dialog";

    // 激励视频：4G弹窗配置
    public static final String REWARD_VIDEO_DOWNLOAD_CONFIRM_POLICY = "reward_download_confirm_policy";

    //全屏视频：是否设置为SurfaceView
    public static final String FULL_SCREEN_VIDEO_USE_SURFACE = "full_screen_use_surface";




}
