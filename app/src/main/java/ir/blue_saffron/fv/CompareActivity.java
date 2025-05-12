package ir.blue_saffron.fv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

public class CompareActivity extends AppCompatActivity {

    private static final String TAG = "CompareActivity";
    public static final int FIRST_IMAGE = 0;
    public static final int SECOND_IMAGE = 1;
    public static final int FIRST_PICK_IMAGE = 2;
    public static final int SECOND_PICK_IMAGE = 3;

    ImageView imageView1, imageView2, imageView3, imageView4;
    TextView textView;
    boolean firstOK = false;
    boolean secondOK = false;
    ImageView imageViewGreen;
    ImageView imageViewRed;
    int greenColor, redColor;

    ProcessImage processImage1 = new ProcessImage();
    ProcessImage processImage2 = new ProcessImage();

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        textView = findViewById(R.id.textView);
        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);
        imageView3 = findViewById(R.id.imageView3);
        imageView4 = findViewById(R.id.imageView4);

        imageViewGreen = findViewById(R.id.imageViewGreen);
        imageViewRed = findViewById(R.id.imageViewRed);
        greenColor = ContextCompat.getColor(this, R.color.green_700);
        redColor = ContextCompat.getColor(this, R.color.red_500);

        findViewById(R.id.buttonCamera1).setOnClickListener(view -> startActivityForResult(new Intent(this, CameraCvActivity.class), FIRST_IMAGE));
        findViewById(R.id.buttonCamera2).setOnClickListener(view -> startActivityForResult(new Intent(this, CameraCvActivity.class), SECOND_IMAGE));
        findViewById(R.id.buttonGallery1).setOnClickListener(view -> startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), FIRST_PICK_IMAGE));
        findViewById(R.id.buttonGallery2).setOnClickListener(view -> startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), SECOND_PICK_IMAGE));
        findViewById(R.id.buttonVerify).setOnClickListener(view -> {
            if (!firstOK) {
                new AlertDialog.Builder(this)
                        .setTitle("Alert")
                        .setMessage("No face found in First image")
                        .setNegativeButton("OK", null)
                        .show();
                return;
            }
            if (!secondOK) {
                new AlertDialog.Builder(this)
                        .setTitle("Alert")
                        .setMessage("No face found in Second image")
                        .setNegativeButton("OK", null)
                        .show();
                return;
            }
            try {
                float[] vec1 = M2Ncnn.normalize(processImage1.getFeature());
                float[] vec2 = M2Ncnn.normalize(processImage2.getFeature());
                double s = M2Ncnn.getSimilarity(vec1, vec2);
                if (SP.getDebug())
                    textView.setText("Similarity: " + s + "\n" + ((processImage1.getFeaturesTime() + processImage2.getFeaturesTime()) / 2) + " ms");
                else
                    textView.setText((s > G.threshold ? "Same Person" : "Different Person") + "\n" + ((processImage1.getFeaturesTime() + processImage2.getFeaturesTime()) / 2) + " ms");
                textView.setTextColor(s > G.threshold ? greenColor : redColor);
                imageViewGreen.setVisibility(s > G.threshold ? View.VISIBLE : View.INVISIBLE);
                imageViewRed.setVisibility(s > G.threshold ? View.INVISIBLE : View.VISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FIRST_IMAGE) {
                addImage1(G.getBitmap());
            }
            if (requestCode == SECOND_IMAGE) {
                addImage2(G.getBitmap());
            }
            if (requestCode == FIRST_PICK_IMAGE) {
                new GalleryAsync(FIRST_PICK_IMAGE).execute(data.getData());
            }
            if (requestCode == SECOND_PICK_IMAGE) {
                new GalleryAsync(SECOND_PICK_IMAGE).execute(data.getData());
            }
        }
    }

    private void addImage1(Bitmap b) {
        if (processImage1.detectFaces(b)) {
            processImage1.getFace();
            if (SP.getDebug())
                imageView3.setImageBitmap(processImage1.getFace());
            imageView1.setImageBitmap(processImage1.getDrawImage());
            firstOK = true;
        } else {
            Toast.makeText(CompareActivity.this, "No face found", Toast.LENGTH_SHORT).show();
            imageView1.setImageBitmap(b);
            firstOK = false;
        }
        textView.setText("");
        imageViewGreen.setVisibility(View.INVISIBLE);
        imageViewRed.setVisibility(View.INVISIBLE);
    }

    private void addImage2(Bitmap b) {
        if (processImage2.detectFaces(b)) {
            processImage2.getFace();
            if (SP.getDebug())
                imageView4.setImageBitmap(processImage2.getFace());
            imageView2.setImageBitmap(processImage2.getDrawImage());
            secondOK = true;
        } else {
            Toast.makeText(CompareActivity.this, "No face found", Toast.LENGTH_SHORT).show();
            imageView2.setImageBitmap(b);
            secondOK = false;
        }
        textView.setText("");
        imageViewGreen.setVisibility(View.INVISIBLE);
        imageViewRed.setVisibility(View.INVISIBLE);
    }

    class GalleryAsync extends AsyncTask<Uri, Void, Bitmap> {
        private final AlertDialog dialog;
        private final int index;

        public GalleryAsync(int index) {
            this.index = index;
            dialog = GalleryAsyncMethods.dialog(CompareActivity.this);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Bitmap doInBackground(Uri... uris) {
            return GalleryAsyncMethods.doInBackground(CompareActivity.this, uris);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            dialog.dismiss();
            if (bitmap == null)
                Toast.makeText(CompareActivity.this, "No face found", Toast.LENGTH_SHORT).show();
            else {
                if (this.index == FIRST_PICK_IMAGE)
                    addImage1(bitmap);
                else
                    addImage2(bitmap);
            }
        }
    }
}