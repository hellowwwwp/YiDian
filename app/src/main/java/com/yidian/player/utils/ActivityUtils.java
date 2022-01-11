package com.yidian.player.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.media.AudioManager;
import android.view.Window;
import android.view.WindowManager;

import com.blankj.utilcode.util.BrightnessUtils;
import com.yidian.player.YiDianApp;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/13
 */
public class ActivityUtils {

    @Nullable
    public static Activity getActivity(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * 获取系统最大亮度值
     */
    public static int getSystemMaxBrightness() {
        try {
            Resources system = Resources.getSystem();
            int resId = system.getIdentifier("config_screenBrightnessSettingMaximum", "integer", "android");
            if (resId > 0) {
                return system.getInteger(resId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 255;
    }

    public static int getSystemMinBrightness() {
        try {
            Resources system = Resources.getSystem();
            int resId = system.getIdentifier("config_screenBrightnessSettingMinimum", "integer", "android");
            if (resId >= 0) {
                return system.getInteger(resId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 获取当前系统亮度值
     */
    public static int getSystemCurrentBrightness() {
        return BrightnessUtils.getBrightness();
    }

    /**
     * 设置窗口的亮度值
     */
    public static void setWindowBrightness(@NonNull Window window, @FloatRange(from = 0f, to = 1f) float brightness) {
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.screenBrightness = brightness;
        window.setAttributes(attributes);
    }

    public static int getMaxVolume() {
        Context context = YiDianApp.application.getApplicationContext();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static int getCurrentVolume() {
        Context context = YiDianApp.application.getApplicationContext();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static void setVolume(int volume) {
        Context context = YiDianApp.application.getApplicationContext();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

}
