package ir.blue_saffron.fv;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class M2Ncnn {
    public native boolean Init(AssetManager mgr);

    public native float[] Detect(Bitmap bitmap, boolean use_gpu);

    static {
        System.loadLibrary("m2mobilencnn");
    }

    public static float[] normalize(float[] arr) {
        double mod = 0.0;
        for (float i : arr)
            mod += i * i;
        double mag = Math.sqrt(mod);
        /*if (mag == 0)
            throw new Exception("The input vector is a zero vector");*/
        for (int i = 0; i < arr.length; i++) {
            arr[i] /= mag;
        }
        return arr;
    }

    public static double getSimilarity(float[] vec1, float[] vec2) throws Exception {
        if (vec1.length != vec2.length)
            throw new Exception("Wrong size");
        double mul = 0;
        for (int i = 0; i < vec1.length; i++)
            mul += vec1[i] * vec2[i];
        if (mul < 0)
            return 0;
        return mul;
    }
}
