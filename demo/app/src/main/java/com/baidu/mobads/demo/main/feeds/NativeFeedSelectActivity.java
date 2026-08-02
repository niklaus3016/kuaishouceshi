package com.baidu.mobads.demo.main.feeds;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.baidu.mobads.demo.main.feeds.video.FeedNativeVideoActivity;

/**
 * 信息流自渲染广告的场景选择界面
 * 点击后跳转不同的Demo场景示例
 */
public class NativeFeedSelectActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        LinearLayout.LayoutParams params = new LinearLayout
                .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        setContentView(layout, params);
        initView(layout);
    }

    protected void initView(LinearLayout layout) {
        // 添加信息流图文示例入口
        bindButton(layout, "图文广告列表", FeedAdActivity.class);
        // 添加信息流RecyclerView示例入口
        bindButton(layout, "RecyclerView列表", FeedAdRecycleActivity.class);
        // 添加信息流视频示例入口
        bindButton(layout, "信息流视频", FeedNativeVideoActivity.class);
        // 添加信息流RecyclerView示例入口
        bindButton(layout, "书签样式示例", FeedBookmarkAdActivity.class);
        // 添加信息流视频示例入口
        // bindButton(layout, "竖版视频", FeedPortraitVideoActivity.class);
    }

    protected void bindButton(LinearLayout container, String text, final Class<?> clazz) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NativeFeedSelectActivity.this, clazz));
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout
                .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 48;
        params.bottomMargin = 48;
        container.addView(btn, params);
    }
}
