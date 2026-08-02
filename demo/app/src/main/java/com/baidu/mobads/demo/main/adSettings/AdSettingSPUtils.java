package com.baidu.mobads.demo.main.adSettings;

import android.content.Context;
import android.content.SharedPreferences;

import com.baidu.mobads.demo.main.MobadsApplication;

/**
 * author: Lijinpeng
 * date: 2021/5/27
 * desc: 广告配置sp封装类
 */
public class AdSettingSPUtils {

    private static final String SP_NAME = "ad_setting_sp";
    private volatile static AdSettingSPUtils sInstance;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEdit;

    private AdSettingSPUtils() {
        mSharedPreferences = MobadsApplication.getContext()
                .getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mEdit = mSharedPreferences.edit();
    }

    public static AdSettingSPUtils getInstance() {
        if (sInstance == null) {
            synchronized (AdSettingSPUtils.class) {
                if (sInstance == null) {
                    sInstance = new AdSettingSPUtils();
                }
            }
        }
        return sInstance;
    }

    public String getString(String key, String def) {
        return mSharedPreferences.getString(key, def);
    }

    public void putString(String key, String value) {
        mEdit.putString(key, value);
        mEdit.apply();
    }

    public void putBoolean(String key, boolean value) {
        mEdit.putBoolean(key, value);
        mEdit.apply();
    }


    public boolean getBoolean(String key, boolean def) {
        return mSharedPreferences.getBoolean(key, def);
    }

    public int getInt(String key, int def) {
        return mSharedPreferences.getInt(key, def);
    }

    public void putInt(String key, int value) {
        mEdit.putInt(key, value);
        mEdit.apply();
    }


    public boolean clear() {
        mEdit.clear();
        mEdit.apply();
        return true;
    }
}
