package com.baidu.mobads.demo.main.cpu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

public class FragmentViewpager extends FragmentPagerAdapter {

    List<Fragment> fragmentList;
    List<String> stringList;

    public FragmentViewpager(@NonNull FragmentManager fm, List<Fragment> fragment,
                             List<String> stringList) {

        super(fm);
        this.fragmentList = fragment;
        this.stringList = stringList;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
    @Override
    public long getItemId(int position) {
        return fragmentList.get(position).hashCode();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return stringList.get(position);
    }
}


