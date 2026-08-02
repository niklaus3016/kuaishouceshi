package com.baidu.mobads.demo.main.tools;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;

import java.lang.reflect.Method;

/**
 * 该类为自动化测试专用，仅支持scheme跳转
 * */
public class AutoTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        try{
            Class<?> clazz = Class.forName("com.baidu.mobads.demo.main.AutoTestTools");
            Object classInstance = clazz.newInstance();
            Method method = clazz.getMethod("startTest", Activity.class);
            method.invoke(classInstance, this);
        } catch (Throwable throwable) {
            Toast.makeText(AutoTestActivity.this, "跳转失败，请检查配置",Toast.LENGTH_LONG).show();
            finish();
        }

    }

}
