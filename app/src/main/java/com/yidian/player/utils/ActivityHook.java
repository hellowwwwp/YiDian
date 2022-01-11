package com.yidian.player.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;

@SuppressWarnings("all")
public class ActivityHook {

    /**
     * java.lang.IllegalStateException: Only fullscreen opaque activities can request orientation
     * <p>
     * 修复android 8.0的设备中 window 不能同时设置为透明和竖屏的问题
     * <p>
     * 在Activity中onCreate()中super之前调用
     */
    public static void hookOrientation(@NonNull Activity activity) {
        if (activity.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O
                && Build.VERSION.SDK_INT == Build.VERSION_CODES.O
                && isTranslucentOrFloating(activity)) {
            fixOrientation(activity);
        }
    }

    /**
     * 设置屏幕不固定，绕过检查
     */
    private static void fixOrientation(@NonNull Activity activity) {
        try {
            Field mActivityInfoField = Activity.class.getDeclaredField("mActivityInfo");
            mActivityInfoField.setAccessible(true);
            ActivityInfo activityInfo = (ActivityInfo) mActivityInfoField.get(activity);
            //设置屏幕不固定
            activityInfo.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查屏幕 横竖屏或者锁定就是固定
     */
    private static boolean isTranslucentOrFloating(@NonNull Activity activity) {
        boolean isTranslucentOrFloating = false;
        try {
            Class<?> styleableClass = Class.forName("com.android.internal.R$styleable");
            Field WindowField = styleableClass.getDeclaredField("Window");
            WindowField.setAccessible(true);
            int[] styleableRes = (int[]) WindowField.get(null);
            //先获取到TypedArray
            final TypedArray typedArray = activity.obtainStyledAttributes(styleableRes);
            Class<?> ActivityInfoClass = ActivityInfo.class;
            //调用检查是否屏幕旋转
            Method isTranslucentOrFloatingMethod = ActivityInfoClass.getDeclaredMethod("isTranslucentOrFloating", TypedArray.class);
            isTranslucentOrFloatingMethod.setAccessible(true);
            isTranslucentOrFloating = (boolean) isTranslucentOrFloatingMethod.invoke(null, typedArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isTranslucentOrFloating;
    }

}