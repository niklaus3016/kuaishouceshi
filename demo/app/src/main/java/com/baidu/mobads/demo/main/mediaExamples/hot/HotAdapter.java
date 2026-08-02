package com.baidu.mobads.demo.main.mediaExamples.hot;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.IBasicCPUData;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Random;

/**
 * author: ZhangYubin
 * date: 2020/12/27 6:00 PM
 * desc:
 */
public class HotAdapter extends BaseAdapter {
    private  int mTextSize = 17;
    private Context mCxt;
    private List<IBasicCPUData> nrAdList;
    private LayoutInflater mInflater;
    private OnClickCallBack mOnClickCallBack;
    private boolean isDark ;

    public HotAdapter(Context context, boolean dark, int textSize) {
        this.mCxt = context;
        isDark = dark;
        mTextSize = textSize;
        mInflater = LayoutInflater.from(mCxt);
    }

    public void addHotData(List<IBasicCPUData> nrAdList) {
        this.nrAdList = nrAdList;
        notifyDataSetChanged();
    }

    public String getRandomHotKey() {
        Random random = new Random();
        int i = random.nextInt(8);
        if (nrAdList != null && nrAdList.size() > i) {
            return nrAdList.get(i).getHotWord();
        }
       return "";
    }

    @Override
    public int getCount() {
        return nrAdList != null ? nrAdList.size() : 0;
    }

    @Override
    public IBasicCPUData getItem(int position) {
        return nrAdList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final IBasicCPUData nrAd = getItem(position);
        View view = mInflater.inflate(R.layout.cpu_hot_item, parent, false);
        TextView titleTv = view.findViewById(R.id.item_title);
        ImageView iconImg = view.findViewById(R.id.item_icon);
        TextView randTv = view.findViewById(R.id.item_rank);
        TextView randomTv = view.findViewById(R.id.item_hotlevel);

        randomTv.setText(Math.round(nrAd.getScore() * 1000000) + "人在看");
        randomTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize - 4);

        randTv.setText(String.valueOf(position + 1));
        randTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);

        titleTv.setText(nrAd.getHotWord() + "  ");
        if (position == 0) {
            randTv.setTextColor(mCxt.getResources().getColor(R.color.red));
            titleTv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, mCxt.getDrawable(R.drawable.ic_hot), null);
        } else if (position == 1) {
            randTv.setTextColor(mCxt.getResources().getColor(R.color.darkorange));
            titleTv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, mCxt.getDrawable(R.drawable.ic_hot), null);
        } else if (position == 2) {
            randTv.setTextColor(mCxt.getResources().getColor(R.color.khaki));
            titleTv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, mCxt.getDrawable(R.drawable.ic_hot), null);
        }
        if(isDark) {
            titleTv.setTextColor(mCxt.getResources().getColor(R.color.nb_read_menu_text));
        }
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);

        // 注意：媒体可以手动选择scheme 是https 还是http
        Glide.with(mCxt).load("https:" + nrAd.getImage()).into(iconImg);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickCallBack != null) {
                    nrAd.clickHotItem(v);
                    mOnClickCallBack.onClick(nrAd, v);
                }
            }
        });
        // 发送展现日志
        nrAd.onImpression(view);
        return view;
    }

    public void setOnClickCallBack(OnClickCallBack mOnClickCallBack) {
        this.mOnClickCallBack = mOnClickCallBack;
    }

    public interface OnClickCallBack {
        void onClick(IBasicCPUData iBasicCPUData, View view);
    }


}
