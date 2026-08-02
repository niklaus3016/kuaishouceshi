package com.baidu.mobads.demo.main.tools;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 模拟adx进行广告请求的测试demo
 */
public class MockBiddingServer {
    private static final String TAG = MockBiddingServer.class.getSimpleName();
    // adx的server地址
    private static final String SERVER = "http://mobads.baidu.com/sbid/std";

    // request parameter
    private final String ID = "id";
    private final String IP = "ip";
    private final String NAME = "name";
    private final String TOKEN = "token";
    private final String TIME_OUT = "tmax";
    private final String TIME = "tstart";
    private final String BID_FLOOR = "bid_floor";

    // response parameter
    private final String REQ_ID = "reqid";
    private final String BID_ID = "bidid";
    private final String STATUS = "status";
    private final String BID_LIST = "bids";

    private final String PRICE = "price";
    private final String ADM = "adm";
    private final String NURL = "nurl";
    private final String LURL = "lurl";


    private final int mPrice = 10;

    public void makeRequest(final String token, final RequestListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                Log.i(TAG, "===================== Server Running =====================");
                try {
                    URL url = new URL(SERVER);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(3000);
                    connection.setUseCaches(false);
                    connection.setRequestProperty("Content-type", "application/json");
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Cache-Control", "no-cache");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    if (token != null) {
                        JSONObject body = new JSONObject();
                        body.put(ID, "123456789abcdefgh");
                        body.put(NAME, "testsbid");
                        body.put(IP, "172.24.168.1");
                        body.put(TOKEN, token);
                        body.put(BID_FLOOR, 200);
                        body.put(TIME, System.currentTimeMillis());
                        body.put(TIME_OUT, 300);
                        postDataToOutputStream(body.toString(), connection);
                        connection.connect();
                        int code = connection.getResponseCode();
                        if (code == HttpURLConnection.HTTP_MOVED_TEMP
                                || code == HttpURLConnection.HTTP_MOVED_PERM) {
                            // 处理不同种协议跳转
                            connection.setInstanceFollowRedirects(false);
                            connection = openConnectionCheckRedirects(connection);
                            // 重定向的话重新为code进行赋值
                            code = connection.getResponseCode();
                        }
                        // 进行判断网络是否请求成功
                        if (code / 100 == 2) {
                            if (listener != null) {
                                // 如果是广告接口的话返回广告信息;
                                String response = getReadContent(connection);
                                if (response != null) {
                                    JSONObject jsonObject= new JSONObject(response);
                                    int status = jsonObject.optInt(STATUS);
                                    if (status == 0) {
                                        JSONArray list = jsonObject.optJSONArray(BID_LIST);
                                        if (list != null && list.length() > 0) {
                                            // 后端返回的广告标识
                                            String adId = "";
                                            int n = 0;
                                            JSONArray ads = new JSONArray();
                                            for (int i = 0; i < list.length(); i++) {
                                                JSONObject item = list.optJSONObject(i);
                                                int price = item.optInt(PRICE);
                                                if (price > mPrice) {
                                                    n++;
                                                    pingArray(item.optJSONArray(NURL));
                                                    // demo默认只展示一条广告
                                                    adId = item.optString("adid");
                                                    break;
                                                } else {
                                                    pingArray(item.optJSONArray(LURL));
                                                }
                                            }
                                            listener.onBiddingSuccess(adId);
                                        } else {
                                            Log.e(TAG, "Server run() empty list!");
                                            listener.onBiddingFailed();
                                        }
                                    } else {
                                        Log.e(TAG, "Server run() error code: " + status);
                                        listener.onBiddingFailed();
                                    }
                                } else {
                                    Log.e(TAG, "Server run() get null response!");
                                    listener.onBiddingFailed();
                                }
                            }
                        } else {
                            Log.e(TAG, "Server do not response!");
                            if (listener != null) {
                                listener.onBiddingFailed();
                            }
                        }
                    } else {
                        Log.e(TAG, "Server run() token is null!");
                        if (listener != null) {
                            listener.onBiddingFailed();
                        }
                    }
                } catch (Throwable tr) {
                    tr.printStackTrace();
                    if (listener != null) {
                        listener.onBiddingFailed();
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    Log.i(TAG, "===================== Server Finish =====================");
                }
            }
        });
        Log.i(TAG, "===================== Server Start =====================");
        thread.start();
    }

    private void pingArray(JSONArray array) {
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(0);
                Log.i(TAG, "pingArray: " + item);
            }
        }
    }

    // 用于post请求时为HttpURLConnection中写入数据
    private void postDataToOutputStream(String query,
                                        HttpURLConnection httpURLConnection) throws IOException {
        OutputStream os = null;
        BufferedWriter writer = null;
        try {
            os = httpURLConnection.getOutputStream();
            writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    // 读取网络中回来的信息的String类型
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

    // 处理重定向时候的方法
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
            // TODO Auto-generated catch block
            // delete printStackTrace
        }
        return conn;
    }

    public interface RequestListener {
        void onBiddingSuccess(String data);
        void onBiddingFailed();
    }
}
