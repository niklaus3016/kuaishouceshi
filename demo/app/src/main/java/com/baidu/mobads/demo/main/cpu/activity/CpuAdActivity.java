package com.baidu.mobads.demo.main.cpu.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.novelprod.CpuNovelActivity;

/**
 * 内容联盟合集类
 */
public class CpuAdActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cpu);
        initView();
    }

    private void initView() {
        bindButton(R.mipmap.ic_cpu, "内容联盟原生渲染（云控频道）", CpuNativeActivity.class);
        bindButton(R.mipmap.ic_cpu, "内容联盟模板渲染（云控频道）", CpuH5Activity.class);
        bindButton(R.mipmap.ic_cpu, "内容联盟原生渲染", NativeCPUAdActivity.class);
        bindButton(R.mipmap.ic_cpu, "内容联盟模板渲染", CpuExpressActivity.class);
        bindButton(R.mipmap.ic_cpu, "内容联盟小说产品", CpuNovelActivity.class);
        bindButton(R.mipmap.ic_portrait_video, "内容联盟小视频", CpuVideoActivity.class);

    }

    private void bindButton(int iconId, String name, final Class clz) {
        LinearLayout cpuListContainer = findViewById(R.id.cpu_container);
        LayoutInflater inflater = getLayoutInflater();

        View btn = inflater.inflate(R.layout.demo_ad_list_item, null);
        TextView textView = btn.findViewById(R.id.item_name);
        textView.setText(name);

        ImageView icon = btn.findViewById(R.id.left_icon);
        icon.setImageResource(iconId);

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CpuAdActivity.this, clz));
            }
        });
        cpuListContainer.addView(btn);
    }

}
