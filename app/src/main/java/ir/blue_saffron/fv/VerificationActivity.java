package ir.blue_saffron.fv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class VerificationActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private static final String TAG = "VerificationActivity";
    LinearLayout linearLayoutPersonImages;
    TextView textViewName;
    ImageView imageViewTarget;
    TextView textViewResult;
    TextView textViewTemp;
    ImageView imageViewGreen;
    ImageView imageViewRed;

    ProcessImage processImage = new ProcessImage();
    List<float[]> features = new ArrayList<>();
    int greenColor, redColor;

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
        setContentView(R.layout.activity_verification);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        linearLayoutPersonImages = findViewById(R.id.linearLayoutPersonImages);
        textViewName = findViewById(R.id.textViewName);
        imageViewTarget = findViewById(R.id.imageViewTarget);
        textViewResult = findViewById(R.id.textViewResult);
        textViewTemp = findViewById(R.id.textViewTemp);
        textViewTemp.setText("");
        imageViewGreen = findViewById(R.id.imageViewGreen);
        imageViewRed = findViewById(R.id.imageViewRed);
        greenColor = ContextCompat.getColor(this, R.color.green_700);
        redColor = ContextCompat.getColor(this, R.color.red_500);

        findViewById(R.id.buttonGallery).setOnClickListener(view -> startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), PICK_IMAGE));
        findViewById(R.id.buttonTakeImage).setOnClickListener(view -> startActivityForResult(new Intent(this, CameraCvActivity.class), PersonActivity.NEW_IMAGE));

        if (!getIntent().hasExtra(PersonActivity.PERSON_ID)) {
            Toast.makeText(this, "No Person ID", Toast.LENGTH_LONG).show();
            finish();
        }
        int personId = getIntent().getIntExtra(PersonActivity.PERSON_ID, -1);

        loadPersonImages(personId);
    }

    private void loadPersonImages(int personId) {
        File personsFolder = new File(getDataDir(), "persons");
        File personFolder = new File(personsFolder, String.valueOf(personId));

        textViewName.setText(SP.getPersonName(personId));

        int paddingDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        linearLayoutPersonImages.removeAllViews();
        for (int i = 1; i < personFolder.list((file, s) -> s.toLowerCase().endsWith(".png")).length; i++) {
            File personImageFile = new File(personFolder, i + ".png");
            Bitmap b = MyUtils.file2Bitmap(personImageFile);

//            ProcessImage processImage = new ProcessImage();
//            float[] feature = new float[512];
            float[] feature = MyUtils.readFloatFile(new File(personFolder, (i) + ".features"), 512);
            features.add(feature);
//            processImage.setManually(b, feature);

            ImageView imageView = new ImageView(this);
            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), b);
            drawable.setCircular(true);
            imageView.setImageDrawable(drawable);
            imageView.setPadding(paddingDp / 2, 0, paddingDp / 2, 0);
            linearLayoutPersonImages.addView(imageView);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PersonActivity.NEW_IMAGE) {
                processNewImage(G.getBitmap());
            }
            if (requestCode == PICK_IMAGE) {
                new GalleryAsync().execute(data.getData());
            }
        }
    }

    private void processNewImage(Bitmap bitmap) {
        if (processImage.detectFaces(bitmap)) {
            processImage.getFace();

            imageViewTarget.setMinimumHeight(imageViewTarget.getHeight() * 2);
            imageViewTarget.setMinimumWidth(imageViewTarget.getHeight() * 2);

            if (compareFeatures(processImage.getFeature())) {
                imageViewTarget.setImageBitmap(processImage.getDrawImage(greenColor));
                textViewResult.setTextColor(greenColor);
                imageViewGreen.setVisibility(View.VISIBLE);
                imageViewRed.setVisibility(View.INVISIBLE);
            } else {
                imageViewTarget.setImageBitmap(processImage.getDrawImage(redColor));
                textViewResult.setTextColor(redColor);
                imageViewGreen.setVisibility(View.INVISIBLE);
                imageViewRed.setVisibility(View.VISIBLE);
            }
        } else {
            imageViewTarget.setImageBitmap(bitmap);
            textViewResult.setText("No face found!");
            imageViewGreen.setVisibility(View.INVISIBLE);
            imageViewRed.setVisibility(View.VISIBLE);
        }
    }

    private boolean compareFeatures(float[] fCur) {
        String res = "";
        double[] sim = new double[features.size()];
        boolean ret = false;
        try {
            for (int i = 0; i < features.size(); i++) {
                sim[i] = M2Ncnn.getSimilarity(features.get(i), fCur);
                if (sim[i] > G.threshold) {
//                    res += " 1";
                    ret = true;
                } //else res += " 0";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        res += "\n";
        double max = 0;
        for (double v : sim) {
            max = Math.max(v, max);
            res += v + "\n";
        }

        if (SP.getDebug()) {
            textViewTemp.setText(res);
            textViewResult.setText("Similarity is: " + max + "\n" + processImage.getFeaturesTime() + " ms");
        } else {
            textViewResult.setText(ret ? "Same Person" : "Different Person" + "\n" + processImage.getFeaturesTime() + " ms");
        }
        return ret;
    }

    class GalleryAsync extends AsyncTask<Uri, Void, Bitmap> {
        private AlertDialog dialog;

        public GalleryAsync() {
            dialog = GalleryAsyncMethods.dialog(VerificationActivity.this);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Bitmap doInBackground(Uri... uris) {
            return GalleryAsyncMethods.doInBackground(VerificationActivity.this, uris);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            dialog.dismiss();
            if (bitmap == null)
                Toast.makeText(VerificationActivity.this, "No face found", Toast.LENGTH_SHORT).show();
            else
                processNewImage(bitmap);
        }
    }
}