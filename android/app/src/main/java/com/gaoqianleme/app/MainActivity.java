package com.yuexuxingzuo.app;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private boolean shouldCheckRisk = true;
    private long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 60 * 1000; // 1分钟
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 注册插件（保持原来的顺序）
        registerPlugin(BaiduAdPlugin.class);
        registerPlugin(TTSPlugin.class);
        registerPlugin(RiskCheckPlugin.class);
        registerPlugin(AudioPlugin.class);
        
        super.onCreate(savedInstanceState);
        
        // 冷启动风控检测
        performRiskCheck("coldStart", true);
        
        // 配置 WebView 以支持现代 CSS 特性
        configureWebView();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次回到前台都检测，但有间隔限制
        if (shouldCheckRisk) {
            performRiskCheck("resume", false);
        }
    }
    
    private void performRiskCheck(String trigger, boolean forceCheck) {
        long currentTime = System.currentTimeMillis();
        
        // 如果不是强制检测，检查间隔
        if (!forceCheck) {
            if (currentTime - lastCheckTime < CHECK_INTERVAL) {
                return;
            }
        }
        
        RiskDetector.RiskResult result = RiskDetector.checkAllRisks(this);
        if (result.hasRisk) {
            showRiskDialog(result.riskDescription);
        }
        
        lastCheckTime = currentTime;
    }
    
    public void performRiskCheckFromFrontend() {
        performRiskCheck("frontend", true);
    }
    
    private void showRiskDialog(String riskDesc) {
        shouldCheckRisk = false;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("风险环境提示");
        builder.setMessage(
            "检测到当前设备开启开发者调试/投屏/多开/外挂工具等风险环境，\n" +
            "为保障账号安全与活动公平，请关闭相关功能后重启APP；\n" +
            "仍存在风险将无法正常使用本应用。\n\n" +
            "检测到的风险：" + riskDesc
        );
        builder.setCancelable(false);
        
        builder.setPositiveButton("关闭并退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
        
        builder.setNegativeButton("重启校验", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                finish();
            }
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void configureWebView() {
        WebView webView = getBridge().getWebView();
        WebSettings settings = webView.getSettings();
        
        // 启用 JavaScript
        settings.setJavaScriptEnabled(true);
        
        // 启用 DOM storage
        settings.setDomStorageEnabled(true);
        
        // 启用数据库
        settings.setDatabaseEnabled(true);
        
        // 启用混合内容模式
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 启用宽视口
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        
        // 启用硬件加速
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        
        // 设置用户代理，避免某些网站对 WebView 的限制
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + " MobileApp/1.0");
    }
}
