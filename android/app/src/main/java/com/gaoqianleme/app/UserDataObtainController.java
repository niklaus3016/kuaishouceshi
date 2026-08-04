package com.mijingxingzuo.app;

import androidx.annotation.NonNull;

import com.kwad.sdk.api.KsCustomController;

/**
 * 控制快手 SDK 获取用户设备信息的开关。
 * <p>
 * 非必选参数；媒体可根据自身诉求继承 KsCustomController，
 * 重写相关方法控制 SDK 是否允许获取对应维度的设备/权限信息。
 * <p>
 * 所有方法均与用户隐私同意状态绑定；未同意前返回 false，
 * 同意后可根据业务需要开放（此处默认开放，以提升广告填充和 eCPM）。
 */
public class UserDataObtainController extends KsCustomController {

    private volatile boolean userAgree;

    private UserDataObtainController() {
        // 默认先按未同意处理，等用户明确同意隐私后调用 setUserAgree(true)
        this.userAgree = false;
    }

    private static class Holder {
        private static final UserDataObtainController sInstance = new UserDataObtainController();
    }

    @NonNull
    public static UserDataObtainController getInstance() {
        return Holder.sInstance;
    }

    public void setUserAgree(boolean userAgree) {
        this.userAgree = userAgree;
    }

    public boolean isUserAgree() {
        return userAgree;
    }

    @Override
    public boolean canReadLocation() {
        // 地理位置：同意后允许，有助于提升广告转化和 eCPM
        return userAgree;
    }

    @Override
    public boolean canUsePhoneState() {
        // 手机硬件信息（IMEI 等）：同意后允许
        return userAgree;
    }

    @Override
    public boolean canUseOaid() {
        // 设备 OAID：同意后允许
        return userAgree;
    }

    @Override
    public boolean canUseMacAddress() {
        // Mac 地址：同意后允许
        return userAgree;
    }

    @Override
    public boolean canReadInstalledPackages() {
        // 已安装应用列表：同意后允许（避免投放错误广告）
        return userAgree;
    }

    @Override
    public boolean canUseStoragePermission() {
        // 存储权限：同意后允许（视频缓存更流畅）
        return userAgree;
    }

    @Override
    public boolean canUseNetworkState() {
        // 网络状态：同意后允许（广告流畅性）
        return userAgree;
    }
}
