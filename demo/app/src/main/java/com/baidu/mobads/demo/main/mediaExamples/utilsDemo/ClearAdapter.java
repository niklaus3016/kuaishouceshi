package com.baidu.mobads.demo.main.mediaExamples.utilsDemo;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.cpu.view.NativeCPUView;
import com.baidu.mobads.sdk.api.IBasicCPUData;

import java.util.List;


public class ClearAdapter extends RecyclerView.Adapter<ClearAdapter.ViewHolder> {
    /** 储存广告数据的list */
    private List<IBasicCPUData> mList;
    private Context mContext;
    AQuery aq;

    public ClearAdapter(Context mContext, List<IBasicCPUData> mList) {
        this.mContext = mContext;
        this.mList = mList;
        aq = new AQuery(mContext);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = View.inflate(mContext, R.layout.feed_utils_demo_clear_recycler, null);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        final IBasicCPUData data = mList.get(i);

        viewHolder.nativeCPUView.setItemData(data, aq);
        // 展现时必须调用onImpression上报展现日志（影响计费）
        data.onImpression(viewHolder.nativeCPUView);
        // 添加点击
        viewHolder.nativeCPUView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 发送点击日志
                data.handleClick(viewHolder.nativeCPUView);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        NativeCPUView nativeCPUView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nativeCPUView = itemView.findViewById(R.id.demo_utils_clear_recycler_native_view);
        }
    }
}
