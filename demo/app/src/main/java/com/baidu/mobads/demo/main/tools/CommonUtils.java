package com.baidu.mobads.demo.main.tools;

import android.content.Context;

public class CommonUtils {

    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
