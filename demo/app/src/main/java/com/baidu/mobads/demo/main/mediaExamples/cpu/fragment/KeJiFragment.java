package com.baidu.mobads.demo.main.mediaExamples.cpu.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.androidquery.AQuery;
import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.cpu.view.AbstractViewHolder;
import com.baidu.mobads.demo.main.cpu.view.OnePicViewHolder;
import com.baidu.mobads.demo.main.cpu.view.ThreePicsViewHolder;
import com.baidu.mobads.demo.main.cpu.view.VideoViewHolder;
import com.baidu.mobads.demo.main.tools.SharedPreUtils;
import com.baidu.mobads.sdk.api.CPUAdRequest;
import com.baidu.mobads.sdk.api.CpuVideoView;
import com.baidu.mobads.sdk.api.IBasicCPUData;
import com.baidu.mobads.sdk.api.NativeCPUManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link KeJiFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class KeJiFragment extends Fragment implements NativeCPUManager.CPUAdListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private List<IBasicCPUData> nrAdList = new ArrayList<IBasicCPUData>();
    private NativeCPUManager mCpuManager;
    private CPUAdRequest.Builder builder;
    private MyAdapter adapter;


    public KeJiFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment KeJiFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static KeJiFragment newInstance(String param1, String param2) {
        KeJiFragment fragment = new KeJiFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mCpuManager = new NativeCPUManager(getActivity(), "c0da1ec4", this);
        builder = new CPUAdRequest.Builder();
        adapter = new MyAdapter(getActivity());
        loadAd(1);
    }

    @Override
    public void onPause() {
        super.onPause();

//        adapter.pause();
    }

    @Override
    public void onResume() {
        super.onResume();

//        adapter.resume();
    }

    public void loadAd(int pageIndex) {

        /**
         *  注意构建参数时，setCustomUserId 为必选项，
         *  传入的outerId是为了更好的保证能够获取到广告和内容
         *  outerId的格式要求： 包含数字与字母的16位 任意字符串
         */

        /**
         *  推荐的outerId获取方式：
         */
        SharedPreUtils sharedPreUtils = SharedPreUtils.getInstance();
        String outerId = sharedPreUtils.getString(SharedPreUtils.OUTER_ID);
        if (TextUtils.isEmpty(outerId)) {
            outerId = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0,16);
            sharedPreUtils.putString(SharedPreUtils.OUTER_ID, outerId);
        }

        // 当无法获得设备IMEI,OAID,ANDROIDID信息时，通过此字段获取内容 + 广告
        builder.setCustomUserId(outerId);



        // 设置子渠道，如果申请的话
        builder.setSubChannelId("86784");

        mCpuManager.setRequestParameter(builder.build());
        mCpuManager.setRequestTimeoutMillis(5 * 1000); // 如果不设置，则默认5s请求超时

        mCpuManager.loadAd(pageIndex, 1013, true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflate = inflater.inflate(R.layout.fragment_ke_ji, container, false);
        ListView listview = inflate.findViewById(R.id.listview);
        listview.setAdapter(adapter);
        return inflate;
    }

    @Override
    public void onAdLoaded(List<IBasicCPUData> list) {
        if (list != null && list.size() > 0) {
            nrAdList.addAll(list);
            if (nrAdList.size() == list.size()) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onAdError(String msg, int errorCode) {

    }

    @Override
    public void onVideoDownloadSuccess() {

    }

    @Override
    public void onVideoDownloadFailed() {

    }

    @Override
    public void onDisLikeAdClick(int position, String reason) {

    }

    @Override
    public void onLpCustomEventCallBack(HashMap<String, Object> data, NativeCPUManager.DataPostBackListener dataPostBackListener) {

    }

    @Override
    public void onExitLp() {

    }

    class MyAdapter extends BaseAdapter {
        LayoutInflater inflater;
        AQuery aq;
        public static final int THREE_PIC_LAYOUT = 0;
        public static final int VIDEO_LAYOUT = 1;
        public static final int ONE_PIC_LAYOUT = 2;

        private int bg = Color.WHITE;

        private int textSize = 18;

        private Context mCtx;
        private CpuVideoView mPlayingVideo;

        public MyAdapter(Context context) {
            super();
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            aq = new AQuery(context);
            mCtx = context;
        }

        public void pause() {
//            if (mPlayingVideo != null && mPlayingVideo.mFeedPortraitVideoView != null) {
//                mPlayingVideo.mFeedPortraitVideoView.pause();
//            }

        }

        public void  resume() {
//            if (mPlayingVideo != null && mPlayingVideo.mFeedPortraitVideoView != null) {
//                mPlayingVideo.mFeedPortraitVideoView.resume();
//            }

        }

        public void setStyleParam(int bgColor, int wordSize) {
            bg = bgColor;
            textSize = wordSize;
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
                    case ONE_PIC_LAYOUT :
                        convertView = inflater.inflate(R.layout.cpu_item_onepic, parent, false);
                        holder = new OnePicViewHolder(convertView);
                        convertView.setTag(holder);
                        break;
                    case THREE_PIC_LAYOUT :
                        convertView = inflater.inflate(R.layout.cpu_item_threepics, parent, false);
                        holder = new ThreePicsViewHolder(convertView);
                        convertView.setTag(holder);
                        break;
                    case VIDEO_LAYOUT :
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

//            holder.setA(new VideoViewHolder.A() {
//                @Override
//                public void playRenderingStart(CpuVideoView cpuVideoView) {
//                    mPlayingVideo = cpuVideoView;
//                }
//
//                @Override
//                public void playPause() {
//
//                }
//
//                @Override
//                public void playResume() {
//
//                }
//
//                @Override
//                public void playCompletion() {
//
//                }
//
//                @Override
//                public void playError() {
//
//                }
//            });

//             展现时需要调用onImpression上报展现
//            nrAd.onImpression(convertView);

            return convertView;
        }


    }
}