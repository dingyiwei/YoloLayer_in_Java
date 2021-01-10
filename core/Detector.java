package com.seu.detection.core;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.seu.detection.bean.Detection;
import com.seu.detection.bean.ModelInfo;
import com.seu.detection.bean.XYWH;
import com.seu.detection.utils.ModelManager;
import com.seu.detection.utils.Untils;

import java.util.ArrayList;
import java.util.Comparator;

import static java.lang.Math.exp;
import static java.lang.Math.max;
import static java.lang.Math.min;

public class Detector {
    private static final String TAG = Detector.class.getSimpleName();
    private final ModelInfo modelInfo;
    private final float stride = 32;
    private final float[] anchors = {81, 82, 135, 150, 344, 310};

    public Detector(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public ArrayList<Detection> detectOnImage(@NonNull Bitmap image, double thres) {
        int imageW = image.getWidth();
        int imageH = image.getHeight();
        int modelInputW = modelInfo.getInput_W();
        int modelInputH = modelInfo.getInput_H();
        final int outputW = modelInfo.getOutput_W();
        final int outputSize = modelInfo.getOutput_W() * modelInfo.getOutput_H();

        Bitmap resizedImage = Bitmap.createScaledBitmap(image, modelInputW, modelInputH, true);

        float[] inputData = Untils.getPixels(modelInfo.getFramework(), resizedImage, modelInputW, modelInputH);
        float[] modelOutput = ModelManager.runModelSync(modelInfo, inputData);

        ArrayList<Detection> detections = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < outputSize; j++) {
                int base = outputSize * 10 * i;
                double conf = sigmoid(modelOutput[base + j + 4 * outputSize]);
                if (conf < thres) {
                    continue;
                }
                int cls = -1;
                double cls_conf = -Float.MAX_VALUE;
                for (int k = 0; k < 5; k++) {
                    double v = modelOutput[base + j + (k + 5) * outputSize];
                    if (v > cls_conf) {
                        cls_conf = v;
                        cls = k;
                    }
                }
                cls_conf = sigmoid(cls_conf) * conf;
                if (cls_conf < thres) {
                    continue;
                }
                double x = (sigmoid(modelOutput[base + j]) + j % outputW) * stride - 15;
                double y = (sigmoid(modelOutput[base + j + outputSize]) + j / outputW) * stride - 15;
                double w = exp(modelOutput[base + j + 2 * outputSize]) * anchors[i * 2];
                double h = exp(modelOutput[base + j + 3 * outputSize]) * anchors[i * 2 + 1];
                Detection det = new Detection(cls, new XYWH(x, y, w, h), conf);
                det = det.scale(imageW, imageH);
                Log.d(TAG, det.toString());
                Log.d(TAG, det.getLabelStr() + " x1: " + det.getX1() + " y1: " + det.getY1() + " x2: " + det.getX2() + " y2: " + det.getY2());
                detections.add(det);
            }
        }

        detections = nms(detections, 0.5);
        return detections;
    }

    public ArrayList<ArrayList<Detection>> detectOnImages(@NonNull ArrayList<Bitmap> images, double thres) {
        ArrayList<ArrayList<Detection>> detList = new ArrayList<>();
        for (Bitmap img : images) {
            long t1 = System.currentTimeMillis();
            detList.add(detectOnImage(img, thres));
            long t2 = System.currentTimeMillis();
            Log.d(TAG, "detect time: " + (t2 - t1));
        }
        return detList;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + exp(-x));
    }

    private ArrayList<Detection> nms(ArrayList<Detection> detections, double thres) {
        ArrayList<Detection> detectionsWithOffset = new ArrayList<>();
        for (Detection det : detections) {
            detectionsWithOffset.add(det.addClassOffset());
        }
        detectionsWithOffset.sort(new Comparator<Detection>() {
            @Override
            public int compare(Detection o1, Detection o2) {
                int p1 = (int) (o1.getProp() * 100);
                int p2 = (int) (o2.getProp() * 100);
                return p2 - p1;
            }
        });
        int detSize = detectionsWithOffset.size();
        boolean[] used = new boolean[detSize];
        ArrayList<Detection> res = new ArrayList<>();
        int detIdx = 0;
        while (true) {
            while (detIdx < detSize && used[detIdx]) {
                detIdx++;
            }
            if (detIdx == detSize) {
                break;
            }
            res.add(detectionsWithOffset.get(detIdx).removeClassOffset());
            used[detIdx] = true;
            for (int i = detIdx + 1; i < used.length; i++) {
                if (iou(detectionsWithOffset.get(detIdx), detectionsWithOffset.get(i)) > thres) {
                    used[i] = true;
                }
            }
        }
        return res;
    }

    private double iou(Detection a, Detection b) {
        double inter = (max(a.getX2(), b.getX2()) - min(a.getX1(), b.getX1())) * (max(a.getY2(), b.getY2()) - min(a.getY1(), b.getY1()));
        double area1 = a.getW() * a.getH();
        double area2 = b.getW() * b.getH();
        return inter / (area1 + area2 - inter);
    }
}
