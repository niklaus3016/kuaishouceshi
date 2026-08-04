package com.mijingxingzuo.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.IOException;

@CapacitorPlugin(name = "AudioPlugin")
public class AudioPlugin extends Plugin {
    private static final String TAG = "AudioPlugin";
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    @PluginMethod
    public void play(PluginCall call) {
        String filePath = call.getString("filePath");
        Double volumeValue = call.getDouble("volume", 0.8);
        float volume = volumeValue != null ? volumeValue.floatValue() : 0.8f;
        boolean loop = call.getBoolean("loop", false);

        Log.d(TAG, "=== play() 被调用 ===");
        Log.d(TAG, "filePath: " + filePath);
        Log.d(TAG, "volume: " + volume);
        Log.d(TAG, "loop: " + loop);

        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "文件路径为空");
            call.reject("文件路径不能为空");
            return;
        }

        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "Context 为空");
            call.reject("Context 为空");
            return;
        }

        // 初始化 AudioManager
        if (audioManager == null) {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        try {
            // 释放之前的播放器
            if (mediaPlayer != null) {
                Log.d(TAG, "释放之前的播放器");
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // 构建完整的文件名
            String fullFileName = filePath;
            if (!filePath.endsWith(".m4a") && !filePath.endsWith(".mp3") && !filePath.endsWith(".wav")) {
                fullFileName = filePath + ".m4a";
            }

            Log.d(TAG, "尝试加载音频文件：" + fullFileName);

            // 检查文件是否存在
            String[] assets = context.getAssets().list("");
            boolean fileExists = false;
            for (String asset : assets) {
                if (asset.equals(fullFileName)) {
                    fileExists = true;
                    break;
                }
            }
            
            if (!fileExists) {
                Log.e(TAG, "音频文件不存在于 assets: " + fullFileName);
                call.reject("音频文件不存在：" + fullFileName);
                return;
            }

            Log.d(TAG, "音频文件存在，开始创建 MediaPlayer");

            // 从 assets 加载
            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getAssets().openFd(fullFileName);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.setLooping(loop);
            
            // 设置播放完成监听
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "音频播放完成");
            });
            
            // 设置错误监听
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer 错误：what=" + what + ", extra=" + extra);
                return false;
            });
            
            // 使用异步 prepare
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "音频准备完成，开始播放");
                mp.start();
                call.resolve();
            });
            
            Log.d(TAG, "开始异步准备音频...");
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "IO 错误：" + e.getMessage(), e);
            call.reject("IO 错误：" + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "播放音频失败：" + e.getMessage(), e);
            call.reject("播放音频失败：" + e.getMessage());
        }
    }

    @PluginMethod
    public void pause(PluginCall call) {
        Log.d(TAG, "pause() 被调用");
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "暂停播放");
            call.resolve();
        } else {
            Log.w(TAG, "没有正在播放的音频");
            call.reject("没有正在播放的音频");
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Log.d(TAG, "stop() 被调用");
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "停止播放并释放资源");
                call.resolve();
            } catch (Exception e) {
                Log.e(TAG, "停止播放失败：" + e.getMessage());
                call.reject("停止播放失败：" + e.getMessage());
            }
        } else {
            Log.w(TAG, "没有正在播放的音频");
            call.resolve(); // 没有播放也算成功
        }
    }

    @PluginMethod
    public void setVolume(PluginCall call) {
        Double volumeValue = call.getDouble("volume", 0.8);
        float volume = volumeValue != null ? volumeValue.floatValue() : 0.8f;
        
        Log.d(TAG, "setVolume() 被调用：" + volume);
        
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
            Log.d(TAG, "设置音量：" + volume);
            call.resolve();
        } else {
            Log.w(TAG, "没有正在播放的音频");
            call.reject("没有正在播放的音频");
        }
    }
}
