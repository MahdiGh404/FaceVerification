package ir.blue_saffron.fv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;

public class PersonActivity extends AppCompatActivity {

    public static final String TAG = "PersonActivity";
    public static final int NEW_IMAGE = 0;
    private static final int PICK_IMAGE = 100;
    public static final String PERSON_ID = "PERSON_ID";
    int personId = -1;
    GridLayout gridLayout;
    TextInputLayout textInputLayout;
    TextInputEditText editTextName;
    ArrayList<ProcessImage> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        gridLayout = findViewById(R.id.gridLayout);
        textInputLayout = findViewById(R.id.textInput);
        editTextName = findViewById(R.id.editTextName);
        editTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0)
                    textInputLayout.setError("Insert Person Name");
                else
                    textInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        if (getIntent().hasExtra(PERSON_ID)) {
            LoadImages(getIntent().getIntExtra(PERSON_ID, -1));
        }

        findViewById(R.id.btnSave).setOnClickListener(view -> {
            String name = editTextName.getText().toString().trim();
            editTextName.setText(name);
            if (name.isEmpty()) {
                textInputLayout.setError("Insert Person Name");
                editTextName.requestFocus();
                return;
            }
            if (images.size() < 3) {
                new AlertDialog.Builder(this)
                        .setTitle("Alert")
                        .setMessage("Please add at least three images")
                        .setNegativeButton("Ok", null)
                        .show();
                return;
            }
            SaveImages(name);
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btnAdd).setOnClickListener(view ->
                startActivityForResult(new Intent(this, CameraCvActivity.class), NEW_IMAGE)
        );

        findViewById(R.id.buttonGallery).setOnClickListener(view -> startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), PICK_IMAGE));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (images.size() > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Alert")
                    .setMessage("Exit without save?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        super.onBackPressed();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else super.onBackPressed();
    }

    private void addImage(Bitmap b) {
        ProcessImage processImage = new ProcessImage();
        if (!processImage.detectFaces(b)) {
            new AlertDialog.Builder(this)
                    .setTitle("No face found!")
                    .setMessage("Please try again")
                    .setNegativeButton("Ok", null)
                    .show();
            return;
        }
        // TODO
//        processImage.setChipManually(b);
        processImage.getFace();
        processImage.getFeature();
        images.add(processImage);

        addImage2GridView(processImage, b);
    }

    private void addImage2GridView(ProcessImage processImage, Bitmap b) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(b);
        gridLayout.addView(imageView);

        GridLayout.LayoutParams param = new GridLayout.LayoutParams();
        param.height = 200;
        param.width = 0;
        param.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 0.1f);
        imageView.setLayoutParams(param);

        imageView.setOnClickListener(view1 -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Delete image?")
                    .setPositiveButton("Delete", (dialogInterface, i) -> {
                        images.remove(processImage);
                        gridLayout.removeView(imageView);
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.red_500));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == NEW_IMAGE) {
                addImage(G.getBitmap());
            }
            if (requestCode == PICK_IMAGE) {
                new GalleryAsync().execute(data.getData());
            }
        }
    }

    private void LoadImages(int pid) {
        personId = pid;
        File personsFolder = new File(getDataDir(), "persons");
        File personFolder = new File(personsFolder, String.valueOf(personId));

        editTextName.setText(SP.getPersonName(personId));

        Bitmap firstChip = null;
        for (int i = 0; i < personFolder.list((file, s) -> s.toLowerCase().endsWith(".png")).length; i++) {
            File personImageFile = new File(personFolder, i + ".png");
            Bitmap b = MyUtils.file2Bitmap(personImageFile);

            if (i == 0) {
                firstChip = b;
                continue;
            }
            ProcessImage processImage = new ProcessImage();
//            float[] feature = new float[512];//TODO
            float[] feature = MyUtils.readFloatFile(new File(personFolder, (i) + ".features"), 512);
            processImage.setManually(b, feature);
            if (i == 1) {
                processImage.setChipManually(firstChip);
            }

            images.add(processImage);
            addImage2GridView(processImage, b);
        }
    }

    private void SaveImages(String name) {
        File personsFolder = new File(getDataDir(), "persons");
        if (!personsFolder.exists())
            personsFolder.mkdirs();
        File personFolder;
        if (personId == -1) {
            personId = SP.getLastPersonId();
            SP.increaseLastPersonId();
            personFolder = new File(personsFolder, String.valueOf(personId));
            if (!personFolder.exists())
                personFolder.mkdir();
        } else {
            // remove old images
            personFolder = new File(personsFolder, String.valueOf(personId));
            for (File file : personFolder.listFiles()) {
                file.delete();
            }
        }

        for (int i = 0; i < images.size(); i++) {
            ProcessImage pi = images.get(i);
            if (i == 0) {
                if (pi.getLandmarks() == null)
                    pi.detectFaces(pi.getOriginal());
                MyUtils.bitmap2File(pi.getFace(), new File(personFolder, "0.png"));
            }
            MyUtils.bitmap2File(pi.getOriginal(), new File(personFolder, (i + 1) + ".png"));

            MyUtils.writeFloatFile(new File(personFolder, (i + 1) + ".features"), pi.getFeature());
        }

        SP.putPersonName(personId, name);
    }

    class GalleryAsync extends AsyncTask<Uri, Void, Bitmap> {
        private AlertDialog dialog;

        public GalleryAsync() {
            dialog = GalleryAsyncMethods.dialog(PersonActivity.this);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Bitmap doInBackground(Uri... uris) {
            return GalleryAsyncMethods.doInBackground(PersonActivity.this, uris);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            dialog.dismiss();
            if (bitmap == null)
                Toast.makeText(PersonActivity.this, "No face found", Toast.LENGTH_SHORT).show();
            else
                addImage(bitmap);
        }
    }
}