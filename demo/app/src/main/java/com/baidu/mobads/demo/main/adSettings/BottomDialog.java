package com.baidu.mobads.demo.main.adSettings;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;

import java.util.ArrayList;
import java.util.List;
/**
 * author: Lijinpeng
 * date: 2021/7/30
 * desc: 广告配置dialog封装类
 */
public class BottomDialog extends Dialog {


    private BottomDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    public interface ItemClickListener {
        void itemClickListener(String title, int position);
    }

    public interface BottomItemClickListener {
        void bottomItemClickListener(String title,View view);
    }

    private static class Params {
        private List<String> menuList = new ArrayList<>();
        private ItemClickListener itemListener;
        private BottomItemClickListener bottomItemClickListener;
        private String menuTitle;
        private String cancelText;
        private Context context;
        private boolean canCancel = true;

    }

    public static class Builder {
        private final Params params;

        public Builder(Context context) {
            params = new Params();
            params.context = context;
        }

        public Builder setCanCancel(boolean canCancel) {
            this.params.canCancel = canCancel;
            return this;
        }

        public Builder setTitle(String title) {
            this.params.menuTitle = title;
            return this;
        }

        public Builder addMenu(List<String> list) {
            if (list != null) {
                this.params.menuList = list;
            }
            return this;
        }

        public Builder setCancelListener(BottomItemClickListener bottomItemClickListener) {
            params.bottomItemClickListener = bottomItemClickListener;
            return this;
        }

        public Builder setCancelText(String text) {
            params.cancelText = text;
            return this;
        }

        public Builder setItemListener(ItemClickListener itemListener) {
            params.itemListener = itemListener;
            return this;
        }

        public BottomDialog create() {
            final BottomDialog dialog = new BottomDialog(params.context, R.style.Theme_Light_NoTitle_Dialog);
            initWindow(dialog.getWindow());
            ViewGroup.LayoutParams lpItem = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ViewGroup.MarginLayoutParams lpDivider = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            lpDivider.setMargins(50, 0, 50, 0);

            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setCornerRadius(18);
            gradientDrawable.setColor(Color.parseColor("#FFFFFFFF"));
            LinearLayout layContainer = new LinearLayout(params.context);
            layContainer.setBackground(gradientDrawable);
            layContainer.setOrientation(LinearLayout.VERTICAL);


            int dip1 = (int) (1 * params.context.getResources().getDisplayMetrics().density + 0.5f);
            int spacing = dip1 * 12;

            //弹窗标题样式
            if (!TextUtils.isEmpty(params.menuTitle)) {
                TextView tTitle = new TextView(params.context);
                tTitle.setLayoutParams(lpItem);
                tTitle.setGravity(Gravity.CENTER);
                tTitle.setTextColor(Color.GRAY);
                tTitle.setText(params.menuTitle);
                tTitle.setPadding(0, spacing, 0, spacing);
                layContainer.addView(tTitle);
                View viewDivider = new View(params.context);
                viewDivider.setLayoutParams(lpDivider);
                viewDivider.setBackgroundColor(0xFFCED2D6);
                layContainer.addView(viewDivider);
            }

            //弹窗条目的样式
            for (int i = 0; i < params.menuList.size(); i++) {
                final String title = params.menuList.get(i);
                final int position = i;
                TextView bbm = new TextView(params.context);
                bbm.setLayoutParams(lpItem);
                bbm.setPadding(0, spacing, 0, spacing);
                bbm.setGravity(Gravity.CENTER);
                bbm.setText(title);
                bbm.setTextColor(Color.BLUE);
                bbm.setTextSize(17);
                if (params.itemListener != null) {
                    bbm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            params.itemListener.itemClickListener(title, position);
                            dialog.dismiss();
                        }
                    });
                }
                layContainer.addView(bbm);
                if (i != params.menuList.size() - 1) {
                    View viewDivider = new View(params.context);
                    viewDivider.setLayoutParams(lpDivider);
                    viewDivider.setBackgroundColor(0xFFCED2D6);
                    layContainer.addView(viewDivider);
                }
            }

            //弹窗整体布局
            LinearLayout linearLayout = new LinearLayout(params.context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setPadding(spacing, spacing, spacing, spacing);
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = spacing;
            linearLayout.addView(layContainer, layoutParams);

            //弹窗取消按钮样式
            if (!TextUtils.isEmpty(params.cancelText)) {
                TextView btnCancel = new TextView(params.context);
                btnCancel.setGravity(Gravity.CENTER);
                btnCancel.setTextColor(Color.BLUE);
                btnCancel.setTextSize(17);
                btnCancel.setBackground(gradientDrawable);
                btnCancel.setLayoutParams(lpItem);
                btnCancel.setPadding(0, spacing, 0, spacing);
                btnCancel.setText(params.cancelText);
                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (params.bottomItemClickListener != null) {
                            params.bottomItemClickListener.bottomItemClickListener(params.cancelText,v);
                        }
                        dialog.dismiss();
                    }
                });

                linearLayout.addView(btnCancel);
            }
            dialog.setContentView(linearLayout);
            dialog.setCanceledOnTouchOutside(params.canCancel);
            dialog.setCancelable(params.canCancel);
            return dialog;
        }


        public BottomDialog show() {
            BottomDialog dialog = this.create();
            dialog.show();
            return dialog;
        }

        private void initWindow(Window window) {
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
            window.setGravity(Gravity.BOTTOM);
        }
    }


}
