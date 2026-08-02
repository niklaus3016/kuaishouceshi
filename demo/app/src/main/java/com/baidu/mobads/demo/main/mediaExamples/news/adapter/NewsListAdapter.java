package com.baidu.mobads.demo.main.mediaExamples.news.adapter;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.FeedAdViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.FeedContentViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.FeedItemViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.FeedPicAdViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.FeedTriPicAdViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.FeedVideoAdViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.Holders.LoadMoreViewHolder;
import com.baidu.mobads.demo.main.mediaExamples.news.bean.FeedItem;
import com.baidu.mobads.sdk.api.NativeResponse;

import java.util.ArrayList;
import java.util.List;

import static android.widget.LinearLayout.HORIZONTAL;

/**
 * 资讯列表的Adapter，
 */
public class NewsListAdapter extends RecyclerView.Adapter<FeedItemViewHolder> {

    // 标志不同类型的表项
    public static final int AD_FEED_PIG_PIC = 0;
    public static final int AD_FEED_TRI_PIC = 1;
    public static final int AD_FEED_VIDEO = 2;
    public static final int CONTENT_BIG_PIC = 3;
    public static final int CONTENT_TRI_PIC = 4;
    public static final int CONTENT_VIDEO = 5;
    public static final int FOOTER_LOAD_MORE = 6;

    private FeedItemViewHolder viewHolder;
    private OnItemClickListener listener;
    private List<FeedItem> itemList;
    private Context context;
    private final String TAG;

    /**
     * Adapter的构造器
     * @param context 上下文
     * @param itemList 数据项列表
     */
    public NewsListAdapter(Context context, List<FeedItem> itemList) {
        this.context = context;
        this.TAG = context.getClass().getSimpleName();
        this.itemList = itemList;
        setHasStableIds(true); // 可以防止相同图片的重复加载，但是必须重写getItemId方法
    }

    @NonNull
    @Override
    public FeedItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        int resource;
        View item;
        // 根据viewType设定相应的视图资源
        switch (viewType) {
            case AD_FEED_PIG_PIC:
                resource = R.layout.feed_native_listview_ad_row;
                item = LayoutInflater.from(viewGroup.getContext()).inflate(resource, viewGroup, false);
                return new FeedPicAdViewHolder(item);
            case AD_FEED_TRI_PIC:
                resource = R.layout.feed_native_santu_item3;
                item = LayoutInflater.from(viewGroup.getContext()).inflate(resource, viewGroup, false);
                return new FeedTriPicAdViewHolder(item);
            case AD_FEED_VIDEO:
                resource = R.layout.feed_native_video_item;
                item = LayoutInflater.from(viewGroup.getContext()).inflate(resource, viewGroup, false);
                return new FeedVideoAdViewHolder(item);
            case CONTENT_BIG_PIC:
            case CONTENT_TRI_PIC:
            case CONTENT_VIDEO:
                resource = R.layout.news_content_item;
                item = LayoutInflater.from(viewGroup.getContext()).inflate(resource, viewGroup, false);
                return new FeedContentViewHolder(item);
            case FOOTER_LOAD_MORE:
                item = getFooterLoadMoreView();
                return new LoadMoreViewHolder(item);
            default:
                throw new IllegalArgumentException("Unsupported item type. 不支持的项目类型。");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final FeedItemViewHolder feedItemViewHolder, final int position) {
        this.viewHolder = feedItemViewHolder;
        if (position >= itemList.size()) {
            viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        final FeedItem item = itemList.get(position);
        // 为RecyclerView的数据项设置点击监听
        feedItemViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != listener) {
                    listener.onClick(feedItemViewHolder.getAdapterPosition());
                }
            }
        });
        // 对viewHolder数据的加载与还原
        CustomDataBinder binder = new CustomDataBinder(viewHolder.itemView);
        if (feedItemViewHolder instanceof FeedContentViewHolder) {
            // 为一般内容项，根据model中数据还原
            FeedContentViewHolder contentViewHolder = (FeedContentViewHolder) feedItemViewHolder;
            binder.bindContentViews(item, contentViewHolder);

        } else if (feedItemViewHolder instanceof FeedAdViewHolder) {
            // 为广告项，根据model与广告本体的数据进行还原
            FeedAdViewHolder adViewHolder = (FeedAdViewHolder) feedItemViewHolder;
            binder.bindAdViews(item, adViewHolder);
        }
    }

    // 配合setHasStableIds(true)保证相同图片不重复加载，不重写会导致数据项重复
    @Override
    public long getItemId(int position) {
        if (position >= itemList.size()) {
            return -1; // 最后一个loadMore条目的ID
        }
        return itemList.get(position).getItemId();
    }

    @Override
    public int getItemCount() {
        return itemList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= itemList.size()) {
            return FOOTER_LOAD_MORE;
        }
        return itemList.get(position).getItemType();
    }

    // 数据项的点击监听接口
    public interface OnItemClickListener {
        void onClick(int position);
    }

    // 获得一个加载更多的Footer视图
    private LinearLayout getFooterLoadMoreView() {
        // 横向线性布局
        LinearLayout loadMore = new LinearLayout(context);
        loadMore.setOrientation(HORIZONTAL);
        loadMore.setGravity(Gravity.CENTER);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setTag("ProgressBar");
        TextView textView = new TextView(context);
        textView.setTag("TextView");
        textView.setText("加载中");
        LinearLayout.LayoutParams pbParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadMore.addView(progressBar, pbParams);
        LinearLayout.LayoutParams tvParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadMore.addView(textView, tvParams);
        return loadMore;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull FeedItemViewHolder viewHolder) {
        super.onViewAttachedToWindow(viewHolder);
        if (viewHolder instanceof FeedAdViewHolder) {
            FeedAdViewHolder holder = (FeedAdViewHolder) viewHolder;
            final NativeResponse nrAd = holder.nrAd;
            if (holder instanceof FeedVideoAdViewHolder) {
                // 若为视频广告，调用render，对于设置自动播放的广告会自动播放
                ((FeedVideoAdViewHolder) holder).mVideo.render();
            }
            /**
             * registerViewForInteraction()与BaiduNativeManager配套使用
             * 警告：调用该函数来发送展现，勿漏！
             */
            Log.e(TAG, "onViewAttachedToWindow: inter");
            List<View> clickViews = new ArrayList<>();
            List<View> creativeViews = new ArrayList<>();
            clickViews.add(holder.itemView);

            nrAd.registerViewForInteraction(holder.itemView, clickViews, creativeViews,
                    new NativeResponse.AdInteractionListener() {
                @Override
                public void onAdClick() {
                    Log.i(TAG, "onAdClick:" + nrAd.getTitle());
                }

                @Override
                public void onADExposed() {
                    Log.i(TAG, "onADExposed:" + nrAd.getTitle());
                }

                @Override
                public void onADExposureFailed(int reason) {
                    Log.i(TAG , "onADExposureFailed: " + reason);
                }

                @Override
                public void onADStatusChanged() {
                    Log.i(TAG, "onADStatusChanged:" + getBtnText(nrAd));
                }

                @Override
                public void onAdUnionClick() {
                    Log.i(TAG, "onAdUnionClick");
                }
            });
        }
    }

    // 获取安装状态、下载进度所对应的按钮文案
    private String getBtnText(NativeResponse nrAd) {
        if (null == nrAd) {
            return "";
        }
        if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
                || nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_DEEP_LINK) {
            int status = nrAd.getDownloadStatus();
            if (0 <= status && status <= 100) {
                return "下载中：" + status + "%";
            } else if (status == 101) {
                return "点击安装";
            } else if (status == 102) {
                return "继续下载";
            } else if (status == 103) {
                return "点击启动";
            } else if (status == 104) {
                return "重新下载";
            } else {
                return "点击下载";
            }
        }
        return "查看详情";
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull FeedItemViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }
}
