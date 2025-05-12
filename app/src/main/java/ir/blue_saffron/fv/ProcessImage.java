package ir.blue_saffron.fv;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.Vector;

import ir.bs.libs.mtcnn4android.Box;
import ir.bs.libs.mtcnn4android.m4aUtils;

public class ProcessImage {
    private Bitmap bm;
    private Box box;
    private Bitmap chip;
    private Point[] landmarks = null;
    private boolean chip_extracted = false;
    private float[] features;
    private int chip_width = 112;
    private double padding = 0.3;
    private boolean rotate = true;
    private long featuresTime = 0;

    public ProcessImage() {
        chip = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888);
    }

    public ProcessImage(int width, double padding, boolean rotate) {
        chip_width = width;
        this.padding = padding;
        this.rotate = rotate;
        chip = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
    }

    public void setManually(Bitmap org, float[] features) {
        bm = org;
        this.features = features;
    }

    public void setChipManually(Bitmap chip) {
        chip_extracted = true;
        this.chip = chip;
    }

    public boolean detectFaces(Bitmap bitmap) {
        bm = m4aUtils.copyBitmap(bitmap);
//        bm = bitmap;

        int cfi = 0;
        Vector<Box> boxes = G.mtcnn.detectFaces(bm, 40);
        if (boxes.size() == 0) {
            Log.d("OpenCV", "size 0");
            return false;
        } else if (boxes.size() > 1) {
            Mat bboxs = new Mat(boxes.size(), 4, CvType.CV_64F);
            for (int i = 0; i < boxes.size(); i++)
                for (int j = 0; j < boxes.size(); j++)
                    bboxs.put(i, j, boxes.get(i).box[j]);

            cfi = MyCV.center_face_index(bboxs, new Point(bm.getHeight(), bm.getWidth()));
        }
        box = boxes.get(cfi);
        landmarks = new Point[5];
        for (int i = 0; i < landmarks.length; i++)
            landmarks[i] = new Point(box.landmark[i].x, box.landmark[i].y);
        chip_extracted = false;
        features = null;
        return true;
    }

    public Bitmap getFace() {
        if (!chip_extracted) {
            Utils.matToBitmap(MyCV.extractImageChips(bm, landmarks, chip_width, padding, rotate), chip);
            chip_extracted = true;
        }
        return m4aUtils.copyBitmap(chip);
    }

    public Bitmap getDrawImage() {
        return getDrawImage(Color.rgb(244, 67, 54));
    }

    public Bitmap getDrawImage(int color) {
        m4aUtils.drawRect(bm, box.transform2Rect(), color);
//        m4aUtils.drawPoints(bm, box.landmark);
        return bm;
    }

    public Bitmap getOriginal() {
        if (bm.getWidth() != 256) {
            return Bitmap.createScaledBitmap(bm, 256, 256, true);
        }
        return bm;
    }

    public float[] getFeature() {
        if (features == null) {
            featuresTime = System.currentTimeMillis();
            float[] f1 = G.m2Ncnn.Detect(chip, false);
            float[] f2 = G.m2Ncnn.Detect(MyUtils.flip(chip, true, false), false);
            features = new float[512];
            for (int i = 0; i < f1.length; i++) {
                features[i] = f1[i] + f2[i];
            }
            featuresTime = System.currentTimeMillis() - featuresTime;
            features = M2Ncnn.normalize(features);
        }
        return features;
    }

    public long getFeaturesTime() {
        return featuresTime;
    }

    public Point[] getLandmarks() {
        return landmarks;
    }
}
