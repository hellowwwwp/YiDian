package com.yidian.player.utils;

import android.content.Context;

import com.yidian.player.YiDianApp;

import androidx.annotation.NonNull;

/**
 * @author: wangpan
 * @email: p.wang@aftership.com
 * @date: 2022/1/11
 */
public class DeviceUtils {

    private static int screenStatusBarHeight;
    private static int navigationBarHeight;

    @NonNull
    public static Context getAppContext() {
        return YiDianApp.Companion.getApplication();
    }

    public static int getStatusBarHeight() {
        final Context context = getAppContext();
        int identifier = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (identifier > 0) {
            return context.getResources().getDimensionPixelSize(identifier);
        }
        return getCommonStatusBarHeight(context);
    }

    private static int getCommonStatusBarHeight(@NonNull Context context) {
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            int height = (int) clazz.getField("status_bar_height").get(clazz.newInstance());
            return context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getNavigationBarHeight() {
        final Context context = getAppContext();
        if (context.getResources().getIdentifier("config_showNavigationBar", "bool", "android") != 0) {
            int identifier = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            return context.getResources().getDimensionPixelSize(identifier);
        }
        return 0;
    }

}
