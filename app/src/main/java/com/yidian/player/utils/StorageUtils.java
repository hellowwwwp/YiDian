package com.yidian.player.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class StorageUtils {

    /**
     * 判断是否有外置 SDCard
     */
    public static boolean isExternalSdcardMounted(@NonNull Context context) {
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumeState = storageManager.getClass().getMethod("getVolumeState", String.class);
            String state = (String) getVolumeState.invoke(storageManager, getExternalSdCardRoot(context));
            return Environment.MEDIA_MOUNTED.equals(state);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取外置 SDCard 目录
     */
    @Nullable
    public static String getExternalSdCardRoot(@NonNull Context context) {
        return getStoragePath(context, true);
    }

    @Nullable
    public static String getStoragePath(@NonNull Context context, boolean isRemovable) {
        try {
            StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Method getVolumeListMethod = storageManager.getClass().getMethod("getVolumeList");
            Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");
            Method getDirectoryMethod;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getDirectoryMethod = storageVolumeClass.getMethod("getDirectory");
            } else {
                getDirectoryMethod = storageVolumeClass.getMethod("getPath");
            }
            Method isRemovableMethod = storageVolumeClass.getMethod("isRemovable");
            Object storageVolumeList = getVolumeListMethod.invoke(storageManager);
            int length = Array.getLength(storageVolumeList);
            String path;
            for (int i = 0; i < length; i++) {
                Object storageVolume = Array.get(storageVolumeList, i);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    path = ((File) getDirectoryMethod.invoke(storageVolume)).getAbsolutePath();
                } else {
                    path = (String) getDirectoryMethod.invoke(storageVolume);
                }
                if (isRemovable == ((Boolean) isRemovableMethod.invoke(storageVolume))) {
                    return path;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
