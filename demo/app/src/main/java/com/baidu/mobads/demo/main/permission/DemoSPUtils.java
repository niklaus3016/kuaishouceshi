package com.baidu.mobads.demo.main.permission;

import android.content.Context;
import android.content.SharedPreferences;

public class DemoSPUtils {

    private static final String MOBADS_PERMISSIONS = "mobads_permissions";

    public static void setBoolean(Context context, String key, boolean value) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(MOBADS_PERMISSIONS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(MOBADS_PERMISSIONS, Context.MODE_PRIVATE);
            return preferences.getBoolean(key, defValue);
        }
        return defValue;
    }

}
