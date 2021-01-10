package com.seu.detection.utils;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.seu.detection.bean.Detection;
import com.seu.detection.bean.Track;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static com.seu.detection.utils.Constant.meanValueOfBlue;
import static com.seu.detection.utils.Constant.meanValueOfGreen;
import static com.seu.detection.utils.Constant.meanValueOfRed;

public class Untils {
    private static final String TAG = Untils.class.getSimpleName();
    private static BufferedInputStream bis = null;
    private static InputStream fileInput = null;
    private static FileOutputStream fileOutput = null;
    private static ByteArrayOutputStream byteOut = null;

    public static byte[] getModelBufferFromModelFile(String modelPath){
        try{
            bis = new BufferedInputStream(new FileInputStream(modelPath));
            byteOut = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int size = 0;
            while((size = bis.read(buffer,0,1024)) != -1){
                byteOut.write(buffer,0,size);
            }
            return byteOut.toByteArray();

        }catch (Exception e){
            return  new byte[0];
        }finally {
            releaseResource(byteOut);
            releaseResource(bis);
        }
    }

    private static void releaseResource(Closeable resource){

        if(resource != null){
            try {
                resource.close();
                resource = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static float[] getPixels(String framework,Bitmap bitmap, int resizedWidth, int resizedHeight){
        if(framework == null){
            return  null;
        }
        else if(framework.equals("caffe")){
            return getPixelForCaffe(bitmap,resizedWidth, resizedHeight);
        }
        else if(framework.equals("tensorflow")){
            return getPixelForTensorFlow(bitmap,resizedWidth, resizedHeight);
        }
        return  null;
    }

    private static float[] getPixelForCaffe(Bitmap bitmap, int resizedWidth, int resizedHeight) {
        int channel = 3;
        float[] buff = new float[channel * resizedWidth * resizedHeight];

        int rIndex, gIndex, bIndex;
        for (int i = 0; i < resizedHeight; i++) {
            for (int j = 0; j < resizedWidth; j++) {
                bIndex = i * resizedWidth + j;
                gIndex = bIndex + resizedWidth * resizedHeight;
                rIndex = gIndex + resizedWidth * resizedHeight;

                int color = bitmap.getPixel(j, i);

                //buff[bIndex] = (float) (blue(color) - meanValueOfBlue);
                //buff[gIndex] = (float) (green(color) - meanValueOfGreen);
                //buff[rIndex] = (float) (red(color) - meanValueOfRed);
                buff[bIndex] = (float)red(color) / 255;
                buff[gIndex] = (float)green(color) / 255;
                buff[rIndex] = (float)blue(color) / 255;
            }
        }

        return buff;
    }

    private static float[] getPixelForTensorFlow(Bitmap bitmap, int resizedWidth, int resizedHeight) {
        int channel = 3;
        float[] buff = new float[channel * resizedWidth * resizedHeight];

        int rIndex, gIndex, bIndex;
        int k = 0;
        for (int i = 0; i < resizedHeight; i++) {
            for (int j = 0; j < resizedWidth; j++) {
                rIndex = i * resizedWidth + j;
                gIndex = rIndex + resizedWidth * resizedHeight;
                bIndex = gIndex + resizedWidth * resizedHeight;

                int color = bitmap.getPixel(j, i);

                buff[rIndex] = (float) ((red(color) - meanValueOfRed))/255;
                buff[gIndex] = (float) ((green(color) - meanValueOfGreen))/255;
                buff[bIndex] = (float) ((blue(color) - meanValueOfBlue))/255;

            }
        }

        return buff;
    }

    public static boolean copyModelsFromAssetToAppModels(AssetManager am,String sourceModelName,String destDir){

        try {
            fileInput = am.open(sourceModelName);
            String filename = destDir + sourceModelName;

            fileOutput = new FileOutputStream(filename);
            byteOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = fileInput.read(buffer)) != -1) {
                byteOut.write(buffer, 0, len);
            }
            fileOutput.write(byteOut.toByteArray());
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "copyModelsFromAssetToAppModels : " + ex);
            return false;
        }finally {
            releaseResource(byteOut);
            releaseResource(fileOutput);
            releaseResource(fileInput);
        }
    }

    public static boolean isExistModelsInAppModels(String modelname,String savedir){

        File dir = new File(savedir);
        File[] currentfiles = dir.listFiles();
        if(currentfiles == null){
            return false;
        }else{
            for(File file: currentfiles){
                if(file.getName().equals(modelname)){
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    public static Bitmap drawDetectionBoxesOnImage(Bitmap image, @NonNull ArrayList<Detection> detections) {
        Canvas canvas = new Canvas(image);
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStrokeWidth(3);
        for (Detection d : detections) {
            canvas.drawRect(d.getLeft(), d.getTop(), d.getRight(), d.getBottom(), boxPaint);
            canvas.drawText(String.format("%s: %.2f", d.getLabelStr(), d.getProp()), d.getLeft(), d.getTop(), textPaint);
        }
        return image;
    }

    @SuppressLint("DefaultLocale")
    public static Bitmap drawTrackBoxesOnImage(Bitmap image, @NonNull ArrayList<Track> tracks) {
        Canvas canvas = new Canvas(image);
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setStrokeWidth(3);
        for (Track t : tracks) {
            canvas.drawRect(t.getLeft(), t.getTop(), t.getRight(), t.getBottom(), boxPaint);
            canvas.drawText(String.format("Id: %d", t.getTrackId()), t.getLeft(), t.getTop(), textPaint);
        }
        return image;
    }
}
