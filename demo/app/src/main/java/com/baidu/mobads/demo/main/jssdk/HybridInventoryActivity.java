package com.baidu.mobads.demo.main.jssdk;

import com.baidu.mobads.sdk.api.BaiduHybridAdManager;
import com.baidu.mobads.sdk.api.BaiduHybridAdViewListener;
import com.baidu.mobads.demo.main.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * A
 * 
 * @author shsun
 * 
 */
public class HybridInventoryActivity extends Activity {

    public static final String AD_NUM = "AD_NUM";

    private static final String TAG = "HybridInventoryActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hybrid_app_main);

        //
        final WebView webView = (WebView) this.findViewById(R.id.webView);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.getSettings().setJavaScriptEnabled(true);

        // 创建百度BaiduHybridAdManager, BaiduHybridAdManager对象和WebView对象是一一对应的关系
        final BaiduHybridAdManager hybridAdManager = new BaiduHybridAdManager();        
        hybridAdManager.setBaiduHybridAdViewListener(new BaiduHybridAdViewListener() {
            
            @Override
            public void onAdShow(int adSequence, String adId) {
                
            }
            
            @Override
            public void onAdFailed(int adSequence, String adId, String reason) {
                
            }
            
            @Override
            public void onAdClick(int adSequence, String adId) {
                
            }
        });
        
        //
        //
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                // 之前百度SDK将JavaScript代码注入到App的webView内，
                // 这段JavaScript代码会和Java代码交互, 此处是BaiduSDK处理JavaScript交互
                boolean b = hybridAdManager.shouldOverrideUrlLoading(view, url);
                if (b) {
                    return true;
                } else {
                    // App原来的处理代码放在这里
                    ;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // 一定要在onPageStarted调用百度广告SDK的onPageStarted                
                hybridAdManager.onPageStarted(webView,url,favicon);
                
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "onPageFinished");

                super.onPageFinished(view, url);

                hybridAdManager.injectJavaScriptBridge(webView);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.i(TAG, "onReceivedError");

                // !!!!!!!!!!!!!!!!!!!!!!!!!!! 非常非常重要 !!!!!!!!!!!!!!!!!!!!!!!!!!!
                // 一定要在onReceivedError调用百度广告SDK的onReceivedError
                hybridAdManager.onReceivedError(view, errorCode, description, failingUrl);

                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.i(TAG, "onLoadResource");
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                Log.i(TAG, "onReceivedSslError");

            }
        });
        
        //设置响应js 的Alert()函数  
        webView.setWebChromeClient(new WebChromeClient() {  
            @Override  
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {  
                AlertDialog.Builder b = new AlertDialog.Builder(HybridInventoryActivity.this);  
                b.setTitle("Alert");  
                b.setMessage(message);  
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
                        result.confirm();  
                    }  
                });  
                b.setCancelable(false);  
                b.create().show();  
                return true;  
            }  
            //设置响应js 的Confirm()函数  
            @Override  
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {  
                AlertDialog.Builder b = new AlertDialog.Builder(HybridInventoryActivity.this);  
                b.setTitle("Confirm");  
                b.setMessage(message);  
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
                        result.confirm();  
                    }  
                });  
                b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {  
                    @Override  
                    public void onClick(DialogInterface dialog, int which) {  
                        result.cancel();  
                    }  
                });  
                b.create().show();
                return true;  
            }
        });

        webView.loadUrl("http://mobads.baidu.com/ads/indexlp.html");
        // san tu
//        webView.loadUrl(" https://appin.baidu.com/page/route/game?source=1&jssdk1=6143024&jssdk2=6143019&sdkid=5887568");
        // da tu
//        webView.loadUrl(" https://appin.baidu.com/page/route/game?source=1&jssdk1=6143024&jssdk2=6143019&sdkid=2058628");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
