package com.seu.detection.utils;

import com.seu.detection.bean.ModelInfo;

public class ModelManager {
    public static native int loadModelFromFileSync(String modelName, String modelpath);

    public static native int unloadModelSync();

    public static native float[] runModelSync(ModelInfo modelInfo, float[] buf);

    public static native boolean modelCompatibilityProcessFromFile(String onlinemodelpath, String onlinemodeparapath, String framework, String offlinemodelpath, String offlinemodelversion);
}
