package com.baidu.mobads.demo.main.mediaExamples.news.utils;

public class IdIterator {
    private int id = 0;

    public synchronized int next() {
        return id++;
    }
}
