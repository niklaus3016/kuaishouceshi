package com.baidu.mobads.demo.main.mediaExamples.utilsDemo;

import com.baidu.mobads.sdk.api.SplashAd;

// 用来保存开屏对象的单例类
public class SplashList {
    private static final SplashList splashList=new SplashList();//恶汉模式，直接先实例化
    private   SplashAd mSplashAd;

    private SplashList(){

    }
    public static SplashList getInstance(){
        return splashList;
    }
    public SplashAd getSplashAd () {
        return mSplashAd;
    }
    public void setSplashAd(SplashAd splashAd) {
        mSplashAd = splashAd;
    }
}
