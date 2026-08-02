package com.baidu.mobads.demo.main.cpu.view;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.sdk.api.IBasicCPUData;

import java.util.List;

public class CpuAdapter extends BaseAdapter {
    LayoutInflater inflater;
    AQuery aq;
    public static final int THREE_PIC_LAYOUT = 0;
    public static final int VIDEO_LAYOUT = 1;
    public static final int ONE_PIC_LAYOUT = 2;

    private int bg = Color.WHITE;

    private int textSize = 18;

    private Context mCtx;
    private List<IBasicCPUData> nrAdList;

    public CpuAdapter(Context context) {
        super();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        aq = new AQuery(context);
        mCtx = context;
    }


    public void setStyleParam(int bgColor, int wordSize) {
        bg = bgColor;
        textSize = wordSize;
        notifyDataSetChanged();
    }

    public void setData(List<IBasicCPUData> nrAdList) {
       this.nrAdList = nrAdList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return nrAdList.size();
    }

    @Override
    public IBasicCPUData getItem(int position) {
        return nrAdList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 根据数据划分出几种type，对应不同的布局
    @Override
    public int getItemViewType(int position) {
        IBasicCPUData cpuData = getItem(position);
        // news,image,video,ad
        String type = cpuData.getType();
        // 广告图片
        List<String> imageList = cpuData.getImageUrls();
        // 内容图片
        List<String> smallImageList = cpuData.getSmallImageUrls();
        if (type.equals("video") || (type.equals("ad") && (!TextUtils.isEmpty(cpuData.getVUrl())))) {
            return VIDEO_LAYOUT;
        }
        if ((smallImageList != null && smallImageList.size() >= 3)
                || (imageList != null && imageList.size() >= 3)) {
            return THREE_PIC_LAYOUT;
        }
        if ((smallImageList != null && smallImageList.size() == 1)
                || (imageList != null && imageList.size() == 1)) {
            return ONE_PIC_LAYOUT;
        }
        return -1;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int itemViewType = getItemViewType(position);
        IBasicCPUData nrAd = getItem(position);

        AbstractViewHolder holder = null;

        if (convertView == null) {
            switch (itemViewType) {
                case ONE_PIC_LAYOUT:
                    convertView = inflater.inflate(R.layout.cpu_item_onepic, parent, false);
                    holder = new OnePicViewHolder(convertView);
                    convertView.setTag(holder);
                    break;
                case THREE_PIC_LAYOUT:
                    convertView = inflater.inflate(R.layout.cpu_item_threepics, parent, false);
                    holder = new ThreePicsViewHolder(convertView);
                    convertView.setTag(holder);
                    break;
                case VIDEO_LAYOUT:
                    convertView = inflater.inflate(R.layout.cpu_item_video2, parent, false);
                    holder = new VideoViewHolder(convertView);
                    convertView.setTag(holder);
                    break;
                default:
                    throw new IllegalStateException("数据与布局不匹配");
            }
        } else {
            switch (itemViewType) {
                case ONE_PIC_LAYOUT:
                    holder = (OnePicViewHolder) convertView.getTag();
                    break;
                case THREE_PIC_LAYOUT:
                    holder = (ThreePicsViewHolder) convertView.getTag();
                    break;
                case VIDEO_LAYOUT:
                    holder = (VideoViewHolder) convertView.getTag();
                    break;
                default:
                    throw new IllegalStateException("数据与布局不匹配");
            }
        }
        holder.initWidgetWithData(nrAd, position);

        holder.setAttribute(bg, textSize);


        return convertView;
    }


}
