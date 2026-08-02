package com.baidu.mobads.demo.main.adSettings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;

/**
 * author: Lijinpeng
 * date: 2021/5/27
 * desc: SDK文档页面    Android SDK接入文档2.0：https://union.baidu.com/docs/sdk/AndroidSDK.html
 */
public class AdDocumentActivity extends Activity {

    private WebView mWebView;
    //SDK 2.0文档
    private static final String SDK_DOCUMENT_PATH = "https://union.baidu.com/docs/sdk/AndroidSDK.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_document);
        initView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == R.id.document_menu_copy) {
            if (copyText(getApplicationContext())) {
                Toast.makeText(AdDocumentActivity.this, "已复制链接", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AdDocumentActivity.this, "复制失败", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initView() {
        mWebView = (WebView) findViewById(R.id.web_document);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressbar_document);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);

            }


            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);

            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                return super.onJsConfirm(view, url, message, result);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });


        WebSettings settings = mWebView.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setJavaScriptEnabled(true);

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        mWebView.loadUrl(SDK_DOCUMENT_PATH);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            if (mWebView.getParent() != null) {
                ViewGroup parent = (ViewGroup) mWebView.getParent();
                parent.removeView(mWebView);
            }
            mWebView.destroy();
            mWebView = null;
        }

    }

    //复制文档链接
    public boolean copyText(Context context) {
        try {
            ClipboardManager cmb = (ClipboardManager) (context.getApplicationContext()).getSystemService(Context.CLIPBOARD_SERVICE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                cmb.setText(SDK_DOCUMENT_PATH);
            } else {
                ClipData clipData = ClipData.newPlainText("复制文档链接", SDK_DOCUMENT_PATH);
                cmb.setPrimaryClip(clipData);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}