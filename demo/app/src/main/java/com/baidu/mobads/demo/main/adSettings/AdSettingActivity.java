package com.baidu.mobads.demo.main.adSettings;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.MobadsPermissionSettings;
import com.baidu.mobads.sdk.api.RewardVideoAd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * author: Lijinpeng
 * date: 2021/5/27
 * desc: 广告配置页面
 */
public class AdSettingActivity extends Activity {

    private LinearLayout mLayoutContainer;
    private LayoutInflater mLayoutInflater;
    private static final String TAG = "AdSettingActivity";
    private HashMap<Switch, Boolean> mSwitchHashMap;
    private HashMap<TextView, String> mCheckHashMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_setting);
        initView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.clear_sp_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting_menu_recover) {
            if (AdSettingHelper.getInstance().clearValueFromSetting()) {
                for (Switch chooseSwitch : mSwitchHashMap.keySet()) {
                    chooseSwitch.setChecked(mSwitchHashMap.get(chooseSwitch));
                }
                for (TextView textView : mCheckHashMap.keySet()) {
                    textView.setText(mCheckHashMap.get(textView));
                }
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void initView() {
        mLayoutContainer = (LinearLayout) findViewById(R.id.item_container_setting);
        mLayoutInflater = getLayoutInflater();
        mSwitchHashMap = new HashMap<>();
        mCheckHashMap = new HashMap<>();
        TreeMap<Integer, String> confirmMap = new TreeMap<>();
        confirmMap.put(RewardVideoAd.DOWNLOAD_APP_CONFIRM_ONLY_MOBILE, "仅非WIFI下弹窗");
        confirmMap.put(RewardVideoAd.DOWNLOAD_APP_CONFIRM_NEVER, "永不弹窗");

        bindContentItem("通用配置");
        bindChooseItem("SDK读取设备信息权限", "获取IMEI, 有助于提升ECPM（建议授权）",
                true, AdSettingProperties.COMMON_PERMISSION_PHONE_STATE, true);
        bindChooseItem("SDK读取应用列表权限", "有助于提升ECPM（建议授权）",
                true, AdSettingProperties.COMMON_PERMISSION_APP_LIST, false);
        bindChooseItem("SDK获取定位权限", "获取定位信息, 有助于精准投放",
                true, AdSettingProperties.COMMON_PERMISSION_LOCATION, false);
        bindChooseItem("SDK读写外部存储权限", "用于广告的下载和缓存",
                true, AdSettingProperties.COMMON_PERMISSION_STORAGE, false);
        bindContentItem("开屏广告");
        bindChooseItem("开屏开启下载弹窗", "开启后会覆盖掉工信部下载整改样式的设置", true,
                AdSettingProperties.SPLASH_USE_DIALOG_FRAME, false);
        bindChooseItem("开屏显示工信部下载整改样式", "显示下载类广告的“隐私”、“权限”等字段", true,
                AdSettingProperties.SPLASH_DISPLAY_DOWNLOAD, true);
        bindChooseItem("开启半屏广告模式", "推荐使用半屏广告", false,
                AdSettingProperties.SPLASH_NEED_APP_LOGO, true);
        bindContentItem("信息流广告");
        bindChooseItem("信息流开启下载弹窗", "点击下载类广告时，弹出Dialog", true,
                AdSettingProperties.FEED_AD_NEED_DOWN_DIALOG, false);
        bindChooseItem("书签样式静音状态", "视频广告静音状态，默认不设置", true,
                AdSettingProperties.FEED_BOOKMARK_MUTE, false);
        bindChooseItem("书签样式隐藏静音按钮", "视频广告是否隐藏静音按钮，默认展示", true,
                AdSettingProperties.FEED_BOOKMARK_HIDE_MUTE, false);
        bindChooseItem("书签样式隐藏广告LOGO", "书签组件内是否隐藏广告LOGO，默认展示", true,
                AdSettingProperties.FEED_BOOKMARK_HIDE_AD_LOGO, false);
        bindChooseItem("书签样式隐藏负反馈按钮", "书签组件内是否隐藏负反馈，默认展示", true,
                AdSettingProperties.FEED_BOOKMARK_HIDE_DISLIKE, false);
        bindChooseItem("书签样式限制点击区域", "书签组件的点击区域控制，默认全部可点", false,
                AdSettingProperties.FEED_BOOKMARK_REGION_CLICK, false);

        bindContentItem("激励视频");
//        bindCheckListItem("激励视频4G弹窗", null, true,
//                AdSettingProperties.REWARD_VIDEO_DOWNLOAD_CONFIRM_POLICY,
//                RewardVideoAd.DOWNLOAD_APP_CONFIRM_NEVER, confirmMap);
        bindChooseItem("激励视频SurfaceView渲染", "默认使用TextureView", true,
                AdSettingProperties.REWARD_VIDEO_USE_SURFACE, true);
        bindChooseItem("激励视频提示弹框", "点击跳过展示提示弹框", true,
                AdSettingProperties.REWARD_VIDEO_SHOW_DIALOG, false);
        bindChooseItem("全屏视频SurfaceView渲染", "默认使用TextureView", false,
                AdSettingProperties.FULL_SCREEN_VIDEO_USE_SURFACE, false);
        bindContentItem("插屏广告");
        bindChooseItem("开启全屏模式", "开启后插屏页面为全屏的", true,
                AdSettingProperties.INTERSTITIAL_FULL, false);
        bindChooseItem("开启下载弹窗", "开启后插屏页面下载有弹窗", true,
                AdSettingProperties.INTERSTITIAL_DOWNLOAD, false);
    }

    private void bindCheckListItem(final String title, String description, boolean showDivider,
                                   final String key, int def, final TreeMap<Integer, String> map) {
        View view = mLayoutInflater.inflate(R.layout.demo_setting_check_list_item, null);
        TextView textViewTitle = (TextView) view.findViewById(R.id.tv_setting_choose_title);
        textViewTitle.setText(title);
        TextView textViewDes = (TextView) view.findViewById(R.id.tv_setting_choose_des);
        if (!TextUtils.isEmpty(description)) {
            textViewDes.setText(description);
            textViewDes.setVisibility(View.VISIBLE);
        }
        View divider = view.findViewById(R.id.view_setting_choose_divider);
        divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);

        final TextView checkItem = (TextView) view.findViewById(R.id.tv_setting_check_title);

        final BottomDialog.Builder builder = new BottomDialog.Builder(AdSettingActivity.this)
                .setCancelText("取消")
                .addMenu(new ArrayList<String>(map.values()))
                .setCanCancel(true)
                .setTitle(title)
                .setItemListener(new BottomDialog.ItemClickListener() {
                    @Override
                    public void itemClickListener(String title, int position) {
                        Integer treeMapKey = AdSettingHelper.getInstance().getTreeMapKey(map, position);
                        if (treeMapKey != null) {
                            AdSettingHelper.getInstance()
                                    .putIntToSetting(key, treeMapKey);
                            checkItem.setText(title);
                        }
                    }
                });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.create().show();
            }
        });
        int result = AdSettingHelper.getInstance().getIntFromSetting(key, def);
        checkItem.setText(map.get(result));
        mCheckHashMap.put(checkItem, map.get(def));
        mLayoutContainer.addView(view);
    }


    private void bindChooseItem(String title, String description, boolean showDivider, final String key, boolean def) {
        View view = mLayoutInflater.inflate(R.layout.demo_setting_choose_item, null);
        TextView textViewTitle = (TextView) view.findViewById(R.id.tv_setting_choose_title);
        textViewTitle.setText(title);

        TextView textViewDes = (TextView) view.findViewById(R.id.tv_setting_choose_des);
        if (!TextUtils.isEmpty(description)) {
            textViewDes.setText(description);
            textViewDes.setVisibility(View.VISIBLE);
        }
        View divider = view.findViewById(R.id.view_setting_choose_divider);
        divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        final Switch chooseBtn = (Switch) view.findViewById(R.id.iv_setting_choose_button);

        boolean result = AdSettingHelper.getInstance().getBooleanFromSetting(key, def);
        chooseBtn.setChecked(result);

        chooseBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                AdSettingHelper.getInstance().putBooleanToSetting(key, isChecked);
                if (key.startsWith("permission")) {
                    setSDKPermission(key, isChecked);
                }
            }
        });
        textViewTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseBtn.setChecked(!chooseBtn.isChecked());
            }
        });
        mSwitchHashMap.put(chooseBtn, def);
        mLayoutContainer.addView(view);
    }

    private void bindInputItem(String title, String description, boolean showDivider, final String key, String def) {
        View view = mLayoutInflater.inflate(R.layout.demo_setting_input_item, null);
        TextView textViewTitle = (TextView) view.findViewById(R.id.tv_setting_input_title);
        textViewTitle.setText(title);

        TextView textViewDes = (TextView) view.findViewById(R.id.tv_setting_input_des);
        if (!TextUtils.isEmpty(description)) {
            textViewDes.setText(description);
            textViewDes.setVisibility(View.VISIBLE);
        }
        View divider = view.findViewById(R.id.view_setting_input_divider);
        divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);

        final TextView textValue = (TextView) view.findViewById(R.id.tv_setting_input_value);
        String result = AdSettingHelper.getInstance().getStringFromSetting(key);
        if (TextUtils.isEmpty(result)) {
            result = def;
        }
        textValue.setText(result);
        textValue.setTextColor(Color.parseColor(result));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdSettingActivity.this);
                builder.setTitle("请输入: ");
                final EditText editText = new EditText(AdSettingActivity.this);
                builder.setView(editText);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = editText.getText().toString();
                        try {
                            int color = Color.parseColor(input);
                            AdSettingHelper.getInstance().putStringToSetting(key, input);
                            textValue.setText(input);
                            textValue.setTextColor(color);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton("取消", null);
                builder.show();
            }
        });
        mCheckHashMap.put(textValue, def);
        mLayoutContainer.addView(view);
    }

    private void bindContentItem(String content) {
        View view = mLayoutInflater.inflate(R.layout.demo_setting_content_item, null);
        TextView textViewContent = (TextView) view.findViewById(R.id.tv_setting_content);
        textViewContent.setText(content);
        mLayoutContainer.addView(view);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000:
                updatePermissions(AdSettingProperties.COMMON_PERMISSION_PHONE_STATE, grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            case 1001:
                updatePermissions(AdSettingProperties.COMMON_PERMISSION_LOCATION, grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            case 1002:
                updatePermissions(AdSettingProperties.COMMON_PERMISSION_STORAGE, grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
            default:
                // nop
        }
    }


    public void setSDKPermission(String key, boolean value) {
        switch (key) {
            case AdSettingProperties.COMMON_PERMISSION_PHONE_STATE:
                MobadsPermissionSettings.setPermissionReadDeviceID(value);
                if (value) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (!AdSettingHelper.getInstance().checkSelfPermission(AdSettingActivity.this, Manifest.permission.READ_PHONE_STATE)) {
                            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1000);
                            return;
                        }
                    }
                }
                break;
            case AdSettingProperties.COMMON_PERMISSION_LOCATION:
                MobadsPermissionSettings.setPermissionLocation(value);
                if (value) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (!AdSettingHelper.getInstance().checkSelfPermission(AdSettingActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
                            return;
                        }
                    }
                }
                break;

            case AdSettingProperties.COMMON_PERMISSION_STORAGE:
                MobadsPermissionSettings.setPermissionStorage(value);
                if (value) {
                    if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
                        if (!AdSettingHelper.getInstance().checkSelfPermission(AdSettingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002);
                            return;
                        }
                    }
                }
                break;
            case AdSettingProperties.COMMON_PERMISSION_APP_LIST:
                MobadsPermissionSettings.setPermissionAppList(value);
                break;
        }
        updatePermissions(key, value);
    }


    private void updatePermissions(String permission, boolean granted) {
        Log.i(TAG, "permission=" + permission + "granted=" + granted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCheckHashMap != null) {
            mCheckHashMap.clear();
            mCheckHashMap = null;
        }
        if (mSwitchHashMap != null) {
            mSwitchHashMap.clear();
            mSwitchHashMap = null;
        }
    }
}