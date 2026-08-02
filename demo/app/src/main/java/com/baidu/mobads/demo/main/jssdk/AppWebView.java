package com.baidu.mobads.demo.main.jssdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.View;
import android.webkit.WebChromeClient;

/**
 * Created by shsun on 16/11/10.
 */
public class AppWebView extends WebView {

    public AppWebView(Context context) {
        super(context);
        init();
    }

    public AppWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public AppWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint({ "InlinedApi", "SetJavaScriptEnabled" })
    private void init() {
        setClickable(true);
        WebSettings settings = getSettings();
        // basic
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setNeedInitialFocus(false);
        settings.setAllowFileAccess(true);

        String dbPath =
                this.getContext().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();

        // support android API 7-
        try {
            // API 7, LocalStorage/SessionStorage
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setDatabasePath(dbPath);
            // API 7， Web SQL Database, 需要重载方法（WebChromeClient）才能生效，无法只通过反射实现
        } catch (Exception e) {
            ;
        }

        // API 33 废弃
//        try {
//            // API 7， Application Storage
//            settings.setAppCacheEnabled(true);
//            settings.setAppCachePath(dbPath);
//            settings.setAppCacheMaxSize(5 * 1024 * 1024);
//        } catch (Exception e) {
//            ;
//        }

        try {
            // API 5， Geolocation
            settings.setGeolocationEnabled(true);
            settings.setGeolocationDatabasePath(dbPath);
        } catch (Exception e) {
            ;
        }

        try {
            // API 19, open debug
            if (Build.VERSION.SDK_INT >= 19) {
                // WebView.setWebContentsDebuggingEnabled(true);
                ;
            }
        } catch (Exception e) {
            ;
        }
        // setWebViewClient(new XMyWebViewClient());
        setWebChromeClient(new WebChromeClient());
        // 关闭硬件加速，否则webview设置为透明，但是实际却是黑色背景
        try {
            if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
                this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        } catch (Exception e) {
            ;
        }
        
        ViewGroup.LayoutParams p =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        this.setLayoutParams(p);
        this.setScrollContainer(false);
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(0, 0);
    }
}
