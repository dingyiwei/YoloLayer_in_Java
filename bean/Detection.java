package com.seu.detection.bean;

import android.support.annotation.NonNull;

import static java.lang.Double.max;

public class Detection {
    private final int cls;
    private final double x;
    private final double y;
    private final double w;
    private final double h;
    private final double prop;
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    private final String[] labels = {"armored", "cannon", "oilTank", "soldier", "tank"};

    private final int offset = 4096;
    private final double outputSize = 416;

    public Detection(int cls, XYWH xywh, double conf) {
        this.cls = cls;
        x = xywh.x;
        y = xywh.y;
        w = xywh.w;
        h = xywh.h;
        prop = conf;
        x1 = x - w / 2;
        x2 = x + w / 2;
        y1 = y - h / 2;
        y2 = y + h / 2;
    }

    public Detection(int cls, XYXY xyxy, double conf) {
        this.cls = cls;
        prop = conf;
        x1 = xyxy.x1;
        x2 = xyxy.x2;
        y1 = xyxy.y1;
        y2 = xyxy.y2;
        x = (x1 + x2) / 2;
        y = (y1 + y2) / 2;
        w = x2 - x1;
        h = y2 - y1;
    }

    public int getCls() { return cls; }
    public String getLabelStr() { return labels[cls]; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getW() { return w; }
    public double getH() { return h; }
    public double getProp() { return prop; }
    public float getLeft() { return (float) x1; }
    public float getRight() { return (float) x2; }
    public float getTop() { return (float) y1; }
    public float getBottom() { return (float) y2; }
    public double getX1() { return x1; }
    public double getX2() { return x2; }
    public double getY1() { return y1; }
    public double getY2() { return y2; }

    @Override
    @NonNull
    public String toString() {
        return getLabelStr() + "(" + prop + "): [" + x + ", " + y + ", " + w + ", " + h + "]";
    }

    public Detection addClassOffset() {
        return new Detection(cls, new XYWH(x + cls * offset, y + cls * offset, w, h), prop);
    }

    public Detection removeClassOffset() {
        return new Detection(cls, new XYWH(x - cls * offset, y - cls * offset, w, h), prop);
    }

    public Detection scale(int imageW, int imageH) {
        double gain = outputSize / max(imageW, imageH);
        double padX = (outputSize - imageW * gain) / 2;
        double padY = (outputSize - imageH * gain) / 2;
        double scaledX1 = clip((x1 - padX) / gain, imageW);
        double scaledY1 = clip((y1 - padY) / gain, imageH);
        double scaledX2 = clip((x2 - padX) / gain, imageW);
        double scaledY2 = clip((y2 - padY) / gain, imageH);
        return new Detection(cls, new XYXY(scaledX1, scaledY1, scaledX2, scaledY2), prop);
    }

    private double clip(double x, int w) {
        if (x < 0) {
            return 0;
        } else if (x > w) {
            return w;
        } else {
            return x;
        }
    }
}
