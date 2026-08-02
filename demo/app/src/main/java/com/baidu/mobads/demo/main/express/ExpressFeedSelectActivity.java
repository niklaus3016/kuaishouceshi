package com.baidu.mobads.demo.main.express;

import android.widget.LinearLayout;

import com.baidu.mobads.demo.main.feeds.NativeFeedSelectActivity;

/**
 * 信息流优选模版的场景选择界面
 * 点击后跳转不同的Demo场景示例
 */
public class ExpressFeedSelectActivity extends NativeFeedSelectActivity {

    @Override
    protected void initView(LinearLayout layout) {
        bindButton(layout, "信息流列表", ExpressFeedAdActivity.class);
        bindButton(layout, "瀑布流列表", ExpressPortraitFeedActivity.class);
    }
}
