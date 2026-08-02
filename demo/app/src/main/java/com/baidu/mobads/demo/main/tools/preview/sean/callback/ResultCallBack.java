package com.baidu.mobads.demo.main.tools.preview.sean.callback;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;

import com.baidu.mobads.demo.main.tools.preview.sean.camera.CameraManager;
import com.google.zxing.Result;

public interface ResultCallBack {
    /**
     * 返回结果
     * @param rawResult
     * @param barcode
     * @param scaleFactor
     */
    void handleResult(Result rawResult, Bitmap barcode, float scaleFactor);

    /**
     * 扫码框宽
     * @return
     */
    double getScanWith();

    /**
     * 扫码框高
     * @return
     */
    double getScanHeight();

    /**
     * 设置裁剪区
     * @return
     */
    Rect getScanRect();
    void setScanRect(Rect scanRect);
    CameraManager getCameraManager();
    Handler getHandler();

    void onCreate();
    void onResume();
    void onPause();
    void onDestroy();
}
