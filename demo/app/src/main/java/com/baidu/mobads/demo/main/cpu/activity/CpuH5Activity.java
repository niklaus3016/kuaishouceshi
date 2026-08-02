package com.baidu.mobads.demo.main.cpu.activity;

import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.cpu.CpuH5Fragment;
import com.baidu.mobads.demo.main.cpu.FragmentViewpager;
import com.baidu.mobads.sdk.api.CpuChannelListManager;
import com.baidu.mobads.sdk.api.CpuChannelResponse;

import java.util.ArrayList;
import java.util.List;


/**
 * 内容联盟模板渲染(云控频道) 集成参考类 CpuH5Activity
 *
 *  一、请求频道列表
 * 使用CpuChannelListManager 用于请求平台配置的频道列表
 *     1. 构造请求参数 CpuChannelListListener监听接口
 *     2. 执行loadChannelList请求频道列表
 *     3. 返回List<CpuChannelResponse> list 频道信息列表
 *
 *  二、请求内容+广告列表
 *  具体可参考CpuH5Fragment
 *
 */
public class CpuH5Activity extends FragmentActivity implements CpuChannelListManager.CpuChannelListListener {
    private Button mBtnRequest;
    private Button mBtnShow;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private LinearLayout cpuDataContainer;
    private final String YOUR_APP_ID = "c0da1ec4"; // 双引号中填写自己的APPSID
    private final String YOUR_SUB_CHANNEL_ID = "102619"; // 双引号中填写自己的子渠道scid
    private CpuChannelListManager mCpuChannelListManager;
    private static final String TAG = CpuH5Activity.class.getSimpleName();
    private LinearLayout linearLayout;
    private EditText editTextSubChannelId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_h5);
        initView();
        mCpuChannelListManager = new CpuChannelListManager(getApplicationContext(), CpuH5Activity.this);
    }

    private void initView() {
        mBtnRequest = (Button) findViewById(R.id.btn_request);
        cpuDataContainer = (LinearLayout) findViewById(R.id.cpuDataContainer);
        mBtnShow = (Button) findViewById(R.id.btn_show);
        linearLayout = (LinearLayout) findViewById(R.id.ll_request);
        editTextSubChannelId = (EditText) findViewById(R.id.edit_subid);
        mBtnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCpuChannelListManager != null) {
                    String scid = YOUR_SUB_CHANNEL_ID;
                    if (editTextSubChannelId != null) {
                        Editable text = editTextSubChannelId.getText();
                        if (text != null && !TextUtils.isEmpty(text.toString())) {
                            scid = text.toString();
                        }
                    }
                    Toast.makeText(CpuH5Activity.this, "开始请求频道", Toast.LENGTH_SHORT).show();
                    mCpuChannelListManager.loadChannelList(YOUR_APP_ID, scid);
                }

            }
        });

        mBtnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnRequest.setVisibility(View.GONE);
                mBtnShow.setVisibility(View.GONE);
                linearLayout.setVisibility(View.GONE);
                cpuDataContainer.setVisibility(View.VISIBLE);
            }
        });

        mBtnShow.setEnabled(false);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
    }

    @Override
    public void onChannelListLoaded(List<CpuChannelResponse> list) {
        if (list == null || list.size() < 1) {
            return;
        }

        int size = list.size();
        List<String> channelNames = new ArrayList<>();
        List<Fragment> fragments = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CpuChannelResponse cpuChannelInfo = list.get(i);
            int channelId = cpuChannelInfo.getChannelId();
            String channelName = cpuChannelInfo.getChannelName();
            CpuH5Fragment cpuH5Fragment = new CpuH5Fragment();
            Bundle bundle = new Bundle();
            bundle.putInt("channelId", channelId);
            cpuH5Fragment.setArguments(bundle);
            fragments.add(cpuH5Fragment);
            channelNames.add(channelName);

        }
        FragmentViewpager myViewpager = new FragmentViewpager(getSupportFragmentManager(),
                fragments, channelNames);
        viewPager.setAdapter(myViewpager);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        mBtnShow.setEnabled(true);
        Toast.makeText( CpuH5Activity.this, "频道加载成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChannelListError(String msg, int errorCode) {
        Log.d(TAG, "onChannelListError: " + "msg=" + msg + ", errorCode=" + errorCode);
        Toast.makeText(this, "频道加载失败", Toast.LENGTH_SHORT).show();
    }
}