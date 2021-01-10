package com.seu.detection.utils;

import android.util.Log;

public class ModelJNILoader {
    private static final String TAG = ModelJNILoader.class.getSimpleName();

    private ModelJNILoader() {}

    public static boolean loadJNISo() {
        try {
            System.loadLibrary("hiai");
            Log.d(TAG, "load hiai successfully");
            return true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "failed to load native library: " + e.getMessage());
            return false;
        }
    }
}
