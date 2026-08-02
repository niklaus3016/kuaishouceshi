package com.baidu.mobads.demo.main.tools;

import android.content.Context;
import android.content.SharedPreferences;

import com.baidu.mobads.demo.main.MobadsApplication;

/**
 * author: ZhangYubin
 * date: 2020/11/18 8:31 PM
 * desc:
 */
public class SharedPreUtils {
    private static SharedPreUtils sInstance;
    private SharedPreferences sharedReadable;
    private SharedPreferences.Editor sharedWritable;
    private static final String SHARED_NAME = "outerId_pref";
    public static final String OUTER_ID = "outerId";

    public static SharedPreUtils getInstance(){
        if(sInstance == null){
            synchronized (SharedPreUtils.class){
                if (sInstance == null){
                    sInstance = new SharedPreUtils();
                }
            }
        }
        return sInstance;
    }


    private SharedPreUtils(){

        sharedReadable =  MobadsApplication.getContext()
                .getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
        sharedWritable = sharedReadable.edit();
    }

    public String getString(String key){
        return sharedReadable.getString(key,"");
    }

    public void putString(String key, String value){
        sharedWritable.putString(key,value);
        sharedWritable.apply();
    }

    public void putInt(String key, int value){
        sharedWritable.putInt(key, value);
        sharedWritable.apply();
    }

    public void putBoolean(String key, boolean value){
        sharedWritable.putBoolean(key, value);
        sharedWritable.apply();
    }

    public int getInt(String key, int def){
        return sharedReadable.getInt(key, def);
    }

    public boolean getBoolean(String key, boolean def){
        return sharedReadable.getBoolean(key, def);
    }

}
