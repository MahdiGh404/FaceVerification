package ir.blue_saffron.fv;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.TypedValue;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import ir.bs.libs.mtcnn4android.MTCNN;
import ir.bs.libs.mtcnn4android.m4aUtils;

public class G extends Application {
    static Bitmap bmp;
    static MTCNN mtcnn;
    static M2Ncnn m2Ncnn;
    static int padding_8;
    static int padding_16;
    static double threshold = 0.6;

    @Override
    public void onCreate() {
        super.onCreate();
        SP.init(this);

        padding_8 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        padding_16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
    }

    static void setM2Ncnn(AssetManager assets) {
        if (m2Ncnn == null) {
            m2Ncnn = new M2Ncnn();
            m2Ncnn.Init(assets);
        }
    }

    static void setMTcnn(AssetManager assets) {
        if (mtcnn == null)
            mtcnn = new MTCNN(assets);
    }

    static void updateBitmap(Mat mat) {
        if (bmp == null)
            bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(mat, bmp);
    }

    static Bitmap getBitmap() {
        return m4aUtils.copyBitmap(bmp);
    }
}
