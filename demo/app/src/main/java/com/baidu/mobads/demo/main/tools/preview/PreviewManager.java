package com.baidu.mobads.demo.main.tools.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;

public class PreviewManager {

    private Context mContext;
    private String mUrlString;
    private String mProd;
    private String mMapString;
    private HashMap<String, JSONArray> map = new HashMap<>();

    private PreviewAdListener mListener;

    public PreviewManager(Context context, String urlString) {
        mContext = context;
        mUrlString = urlString;
    }

    public void load(PreviewAdListener listener) {
        mListener = listener;
        // 解析url分析广告类型
        if (TextUtils.isEmpty(mUrlString) || !mUrlString.contains("http")) {
            if (mListener != null) {
                mListener.onAdFail(mUrlString);
            }
            return;
        }
        mProd = Uri.parse(mUrlString).getQueryParameter("prod");
        // 请求url
        makeRequest();
    }

    public void show() {
        Intent intent = new Intent(mContext, PreviewActivity.class);
        if ("rvideo".equals(mProd)) {
            // 激励视频多广告返回太大了，改为存sp。
            SharedPreferences pref = mContext.getApplicationContext().getSharedPreferences("rvideoData",MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("mapString",mMapString);
            editor.commit();
        } else {
            intent.putExtra("mapString", mMapString);
        }
        intent.putExtra("prod", mProd);
        mContext.startActivity(intent);
        if ("rvideo".equals(mProd) || "fvideo".equals(mProd)) {
            ((Activity)mContext).finish();
        }

    }

    public void makeRequest() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(mUrlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(30000);
                    connection.setUseCaches(false);
                    connection.setRequestProperty("Content-type", "application/json");
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Cache-Control", "no-cache");
                    connection.setRequestMethod("GET");
                    connection.connect();

                    int code = connection.getResponseCode();
                    // 30*重定向处理
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP
                            || code == HttpURLConnection.HTTP_MOVED_PERM) {
                        connection.setInstanceFollowRedirects(false);
                        connection = openConnectionCheckRedirects(connection);
                        // 重定向的话重新为code进行赋值
                        code = connection.getResponseCode();
                    }
                    if (code / 100 == 2) {
                        // 如果是广告接口的话返回广告信息;
                        String response = getReadContent(connection);
                        if (response != null) {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.optJSONArray("ad") != null ||
                            !TextUtils.isEmpty(jsonObject.optString("ad"))) {
                                mMapString = response;
                                if (mListener != null) {
                                    mListener.onAdLoad();
                                    return;
                                }
                            }
                        } else {
                            Log.e("previewManager", "Server run() get null response!");
                        }
                    }
                    if (mListener != null) {
                        mListener.onAdFail("广告解析失败");
                    }
                } catch (Throwable tr) {
                    if (mListener != null) {
                        mListener.onAdFail("请求发生异常");
                    }
                    tr.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
        thread.start();
    }

    public String getReadContent(HttpURLConnection connection) throws Exception {
        String data = null;
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        try {
            inputStream = connection.getInputStream();
            baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buf = new byte[128];
            while ((len = inputStream.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            baos.flush();
            data = baos.toString();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            baos = null;
        }
        return data;
    }

    private HttpURLConnection openConnectionCheckRedirects(HttpURLConnection conn) {
        try {
            String location;
            while (true) {
                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM) {
                    location = conn.getHeaderField("Location");

                    URL mURL = new URL(location);
                    conn = (HttpURLConnection) mURL.openConnection();
                    conn.setConnectTimeout(conn.getConnectTimeout());
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestProperty("Range", "bytes=0-");
                    continue;
                }
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    // 广告请求回调
    public interface PreviewAdListener {

        void onAdLoad();

        void onAdFail(String message);
    }
}
