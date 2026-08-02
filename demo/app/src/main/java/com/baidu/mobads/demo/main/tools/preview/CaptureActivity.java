/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.mobads.demo.main.tools.preview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.mobads.demo.main.R;
import com.baidu.mobads.demo.main.tools.preview.sean.CaptureImpl;
import com.baidu.mobads.demo.main.tools.preview.sean.callback.ZxingCallBack;
import com.google.zxing.Result;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements ZxingCallBack {
  public static int SCAN_CODE = 1000;
  public static final String RESULT_DATA = "result_data";
  private SurfaceView preview_view;
  private ImageView scopIm;
  @Override
  protected void onCreate( Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_qrscan);

    preview_view = findViewById(R.id.preview_view);
    scopIm = findViewById(R.id.scanBox);
    new CaptureImpl(this);
  }

  @Override
  public void scanResult(Result rawResult, Bitmap barcode) {

    final PreviewManager manager = new PreviewManager(this, rawResult.getText());
    manager.load(new PreviewManager.PreviewAdListener() {
      @Override
      public void onAdLoad() {
        // 请求成功
        manager.show();
      }

      @Override
      public void onAdFail(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
          @Override
          public void run() {
            Toast.makeText(CaptureActivity.this, "广告预览失败" + message, Toast.LENGTH_SHORT).show();
          }
        });
      }
    });
  }

  @Override
  public View getScopImage() {
    return scopIm;
  }

  @Override
  public SurfaceView getSurfaceView() {
    return preview_view;
  }

  @Override
  public Activity getContext() {
    return this;
  }
}