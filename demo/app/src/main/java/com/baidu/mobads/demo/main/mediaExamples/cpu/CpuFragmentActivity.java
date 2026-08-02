package com.baidu.mobads.demo.main.mediaExamples.cpu;

import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.mediaExamples.cpu.fragment.KeJiFragment;
import com.baidu.mobads.demo.main.mediaExamples.cpu.fragment.TuiJianFragment;
import com.baidu.mobads.demo.main.mediaExamples.cpu.fragment.VideoFragment;
import com.baidu.mobads.demo.main.mediaExamples.cpu.fragment.WenHuaFragment;
import com.baidu.mobads.demo.main.mediaExamples.cpu.fragment.YuLeFragment;

import java.util.ArrayList;

public class CpuFragmentActivity extends AppCompatActivity {




    private ArrayList<Fragment> fragments;
    private TabLayout tabLayout;
    private ViewPager viewPager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpu_fragment);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.container);




        TuiJianFragment tuiJianFragment = TuiJianFragment.newInstance("", "");
        YuLeFragment yuLeFragment = YuLeFragment.newInstance("", "");
        VideoFragment videoFragment = VideoFragment.newInstance("", "");
        WenHuaFragment wenHuaFragment = WenHuaFragment.newInstance("", "");
        KeJiFragment keJiFragment = KeJiFragment.newInstance("", "");


        fragments = new ArrayList<>();

        fragments.add(tuiJianFragment);
        fragments.add(yuLeFragment);
        fragments.add(videoFragment);
        fragments.add(wenHuaFragment);
        fragments.add(keJiFragment);
        tabLayout.setupWithViewPager(viewPager);










        viewPager.setAdapter(new A(getSupportFragmentManager()));






       viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
           @Override
           public void onPageScrolled(int i, float v, int i1) {

           }

           @Override
           public void onPageSelected(int i) {
               Fragment fragment = fragments.get(i);
               for (Fragment f : fragments) {
                   if (f != fragment) {

                   }
               }
           }

           @Override
           public void onPageScrollStateChanged(int i) {

           }
       });

        tabLayout.getTabAt(0).setText("推荐频道");

        tabLayout.getTabAt(1).setText("娱乐频道");
        tabLayout.getTabAt(2).setText("视频频道");
        tabLayout.getTabAt(3).setText("文化频道");
        tabLayout.getTabAt(4).setText("科技频道");



    }


    public class A extends FragmentPagerAdapter {

        public A(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}