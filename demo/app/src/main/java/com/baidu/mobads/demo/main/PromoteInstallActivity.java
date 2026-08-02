package com.baidu.mobads.demo.main;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.baidu.mobads.sdk.api.IPromoteInstallAdInfo;
import com.baidu.mobads.sdk.api.PromoteInstallManager;

public class PromoteInstallActivity extends Activity {

    private IPromoteInstallAdInfo promoteInstallAdInfo;
    private Button view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_install);
        final PromoteInstallManager promoteInstallManager = new PromoteInstallManager(this,
                new PromoteInstallManager.PromoteInstallListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("PromoteInstallActivity", "onSuccess");
                    }

                    @Override
                    public void onFail(String msg) {
                        Log.d("PromoteInstallActivity", "onFail:" + msg);
                    }
                });


        Button button = (Button) findViewById(R.id.pro_install_button_dialog);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promoteInstallManager.showPromoteInstallDialog();
            }
        });
        Button viewById = (Button) findViewById(R.id.pro_install_button_element);
        viewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promoteInstallAdInfo = promoteInstallManager.getPromoteInstallAdInfo();
                if (promoteInstallAdInfo != null && view != null) {
                    view.setEnabled(true);
                    Log.d("PromoteInstallActivity",
                            "getBrandName:" + promoteInstallAdInfo.getBrandName());
                    Log.d("PromoteInstallActivity",
                            "getAppVersion:" + promoteInstallAdInfo.getAppVersion());
                    Log.d("PromoteInstallActivity",
                            "getAppPublisher:" + promoteInstallAdInfo.getAppPublisher());
                    Log.d("PromoteInstallActivity",
                            "getIconUrl:" + promoteInstallAdInfo.getIconUrl());
                    Log.d("PromoteInstallActivity",
                            "getECPMLevel:" + promoteInstallAdInfo.getECPMLevel());
                    Log.d("PromoteInstallActivity",
                            "getPermissionUrl:" + promoteInstallAdInfo.getPermissionUrl());
                    Log.d("PromoteInstallActivity",
                            "getFunctionUrl:" + promoteInstallAdInfo.getFunctionUrl());
                    Log.d("PromoteInstallActivity",
                            "getUnionLogoUrl:" + promoteInstallAdInfo.getUnionLogoUrl());
                    Log.d("PromoteInstallActivity",
                            "getPrivacyUrl:" + promoteInstallAdInfo.getPrivacyUrl());
                }
            }
        });
        view = (Button) findViewById(R.id.pro_install_button);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (promoteInstallAdInfo != null) {
                    promoteInstallAdInfo.handleAdInstall();
                }
            }
        });


    }
}