package com.baidu.mobads.demo.main.mediaExamples.news;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.news.adapter.NewsPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 资讯类接入示例：标签+列表页
 */
public class NewsDemoActivity extends FragmentActivity {

    private static final String TAG = "FeedAdDemoActivity";
    private final String[] labels = {"混排", "大图", "三图", "视频"};

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private NewsPagerAdapter adapter;
    private List<NewsListFragment> fragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        tabLayout = findViewById(R.id.feed_tab_bar);
        viewPager = findViewById(R.id.feed_demo_pager);
        // Initialize the data first.
        initData();
        initListener();
    }

    private void initData() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = (int) (dm.widthPixels * 0.1);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        tabLayout.setLayoutParams(params);
        fragmentList = new ArrayList();
        for (final String label : labels) {
            if (label.equals("大图")) {
                fragmentList.add(NewsListFragment.newInstance(NewsListFragment.FRAG_AD_BIG_PIC, label));
            } else if (label.equals("视频")) {
                fragmentList.add(NewsListFragment.newInstance(NewsListFragment.FRAG_AD_VIDEO, label));
            } else if (label.equals("三图")) {
                fragmentList.add(NewsListFragment.newInstance(NewsListFragment.FRAG_AD_TRI_PIC, label));
            } else {
                fragmentList.add(NewsListFragment.newInstance(NewsListFragment.FRAG_AD_MIXED, label));
            }
        }
        adapter = new NewsPagerAdapter(this, getSupportFragmentManager(), fragmentList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        // Bind tabLayout with viewPager.
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setBackgroundColor(Color.WHITE);
        tabLayout.setTabTextColors(Color.GRAY, Color.BLACK);
        // 设置自定义tab
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (null != tab) {
                tab.setCustomView(adapter.loadTabView(i, tabLayout));
            }
        }
        tabLayout.setSelectedTabIndicatorColor(Color.RED);
        tabLayout.setTabIndicatorFullWidth(false);
        TabLayout.Tab tabPrimary = tabLayout.getTabAt(0);
        if (null != tabPrimary) {
            View tabText = tabPrimary.getCustomView();
            if (tabText instanceof TextView) {
                ((TextView) tabText).setTextColor(Color.BLACK);
                ((TextView) tabText).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                ((TextView) tabText).setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
            }
        }
    }

    private void initListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View tabText = tab.getCustomView();
                if (tabText instanceof TextView) {
                    ((TextView) tabText).setTextColor(Color.BLACK);
                    ((TextView) tabText).setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    ((TextView) tabText).setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View tabText = tab.getCustomView();
                if (tabText instanceof TextView) {
                    ((TextView) tabText).setTextColor(Color.GRAY);
                    ((TextView) tabText).setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    ((TextView) tabText).setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL));
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                fragmentList.get(position).refreshList();
            }
        });
    }
}