package com.baidu.mobads.demo.main.cpu.view;

/**
 * author: ZhangYubin
 * date: 2021/2/18 3:27 PM
 * desc:
 */
public class SpinnerItem {
    /**
     * 名称
     */
    private String mName;
    /**
     * id
     */
    private int mId;

    public SpinnerItem(String name, int id) {
        mName = name;
        mId = id;
    }

    @Override
    public String toString() {
        return mName;
    }

    public int getId() {
        return mId;
    }
}
