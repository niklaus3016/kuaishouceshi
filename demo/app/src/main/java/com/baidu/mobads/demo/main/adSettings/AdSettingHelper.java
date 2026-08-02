package com.baidu.mobads.demo.main.adSettings;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * author: Lijinpeng
 * date: 2021/5/27
 * desc: 广告配置帮助类
 */
public class AdSettingHelper {

    private static class SingletonHolder {
        private static final AdSettingHelper INSTANCE = new AdSettingHelper();
    }

    private AdSettingHelper() {
    }

    public static final AdSettingHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }


    //应用动态申请系统权限
    public boolean checkSelfPermission(Context context, String permission) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                Method method = Context.class.getMethod("checkSelfPermission",
                        String.class);
                return (Integer) method.invoke(context, permission) == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    //从sp中获得广告配置的数据（boolean）
    public boolean getBooleanFromSetting(String key, boolean def) {
        return AdSettingSPUtils.getInstance().getBoolean(key, def);
    }

    //广告配置的数据存入sp（boolean）
    public void putBooleanToSetting(String key, boolean def) {
        AdSettingSPUtils.getInstance().putBoolean(key, def);
    }

    //从sp中获得广告配置的数据（String）
    public String getStringFromSetting(String key) {
        return AdSettingSPUtils.getInstance().getString(key, "");
    }

    //广告配置的数据存入sp（String）
    public void putStringToSetting(String key, String def) {
        AdSettingSPUtils.getInstance().putString(key, def);
    }

    //从sp中获得广告配置的数据（int）
    public int getIntFromSetting(String key, int def) {
        return AdSettingSPUtils.getInstance().getInt(key, def);
    }

    //广告配置的数据存入sp（int）
    public void putIntToSetting(String key, int def) {
        AdSettingSPUtils.getInstance().putInt(key, def);
    }


    //清空sp设置的数据
    public boolean clearValueFromSetting() {
        return AdSettingSPUtils.getInstance().clear();
    }

    public Integer getTreeMapKey(TreeMap<Integer, String> map, int position) {
        Integer integer = null;
        if (map != null) {
            Iterator iterator = map.keySet().iterator();
            int i = position;
            while (iterator.hasNext() && i >= 0) {
                integer = (Integer) iterator.next();
                i--;
            }
        }
        return integer;
    }


}
