package com.yuexuxingzuo.app;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.Locale;

@CapacitorPlugin(name = "TTSPlugin")
public class TTSPlugin extends Plugin {
    private TextToSpeech tts;
    private boolean ttsInitialized = false;
    private String pendingText = null;

    @PluginMethod
    public void speak(PluginCall call) {
        String text = call.getString("text");
        if (text == null) {
            call.reject("Text is required");
            return;
        }

        if (tts == null) {
            // 保存待播放的文本
            pendingText = text;
            initializeTTS(call);
        } else if (ttsInitialized) {
            speakText(call, text);
        } else {
            // TTS 正在初始化，保存文本
            pendingText = text;
            call.resolve();
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        if (tts != null) {
            tts.stop();
        }
        call.resolve();
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        if (tts != null) {
            tts.stop();
        }
        call.resolve();
    }

    private void initializeTTS(final PluginCall call) {
        tts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.CHINESE);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTSPlugin", "Language not supported");
                        call.reject("Language not supported");
                    } else {
                        ttsInitialized = true;
                        // 初始化成功后立即播放待播放的文本
                        if (pendingText != null) {
                            speakText(null, pendingText);
                            pendingText = null;
                        }
                        call.resolve();
                    }
                } else {
                    Log.e("TTSPlugin", "TTS initialization failed");
                    call.reject("TTS initialization failed");
                }
            }
        });
    }

    private void speakText(PluginCall call, String text) {
        if (ttsInitialized && text != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            if (call != null) {
                JSObject result = new JSObject();
                result.put("success", true);
                call.resolve(result);
            }
        } else if (call != null) {
            call.reject("TTS not initialized");
        }
    }

    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}