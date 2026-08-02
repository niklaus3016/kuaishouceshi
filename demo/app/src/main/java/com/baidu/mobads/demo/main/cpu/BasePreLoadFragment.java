package com.baidu.mobads.demo.main.cpu;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * author: LiJinPeng
 * date: 2022/6/14
 * BasePreLoadFragment 视图可见性检测 接入参考类
 */
public abstract class BasePreLoadFragment extends Fragment {

    // fragment是否可见
    private boolean isVisibleToUserInFrag = false;
    // 视图还没准备好
    boolean mIsPrepare = false;
    // 是否执行了onPause（处于跳转页面、home、锁屏等状态）
    boolean mIsPause = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = initView(inflater, container);

        mIsPrepare = true;
        if (isVisibleToUserInFrag) {
            isVisible(true);
        }

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        // 当可见性产生变化时
        if (isVisibleToUserInFrag != isVisibleToUser) {
            // 视图准备好
            if (mIsPrepare) {
                isVisible(isVisibleToUser);
            }
            // 更新当前fragment可见性
            isVisibleToUserInFrag = isVisibleToUser;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isVisibleToUserInFrag && mIsPrepare && mIsPause) {
            isVisible(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isVisibleToUserInFrag && mIsPrepare) {
            isVisible(false);
        }
        mIsPause = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mIsPrepare = false;
        isVisibleToUserInFrag = false;
        mIsPause = false;
    }

    //视图可见性变化接口，留给子类实现
    public abstract void isVisible(boolean isVisible);

    //初始化视图接口，子类必须实现
    public abstract View initView(LayoutInflater inflater, @Nullable ViewGroup container);
}
