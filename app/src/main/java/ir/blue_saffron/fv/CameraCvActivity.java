package ir.blue_saffron.fv;


import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraCvActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    JavaCameraView javaCameraView;
    ImageView imageView;
    Bitmap bmpTmp;
    Mat matTmp;
    Mat mRGBA, mRGBAT;
    Bitmap last_frame_bitmap;


    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i("OpenCV", "OpenCV loaded successfully");
                javaCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_cv);

        imageView = findViewById(R.id.imageView);
        javaCameraView = findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setMaxFrameSize(224, 224);
        javaCameraView.setMaxFrameSize(352, 288);

        findViewById(R.id.btnCapture).setOnClickListener(view -> {
            setGlobalMat();
            setResult(Activity.RESULT_OK);
            finish();
        });
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int w, int h) {
//        Log.d("opencv", "wh" + w + " " + h);
        mRGBA = new Mat(w, h, CvType.CV_8UC4);
        mRGBAT = Mat.zeros(w, h, CvType.CV_8UC4);
        matTmp = new Mat(h, w, CvType.CV_8UC4);
        last_frame_bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmpTmp = Bitmap.createBitmap(h, w, Bitmap.Config.RGB_565);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    void setGlobalMat() {
        Core.flip(mRGBA.t(), matTmp, -1);
        G.updateBitmap(matTmp);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        mRGBAT = mRGBA.t();
        Core.flip(mRGBAT, mRGBAT, -1);
        Imgproc.resize(mRGBAT, mRGBAT, mRGBA.size());
        return mRGBAT;
    }
}