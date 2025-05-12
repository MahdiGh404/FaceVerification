package ir.blue_saffron.fv;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static boolean FORCE_UPDATE_PERSONS = false;
    private boolean permissionRequested = false;
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE"};

    ChipGroup chipGroup;
    ArrayList<Pair<Bitmap, Integer>> tags = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SP.setDebug(false);
        copyPersons();

        findViewById(R.id.imageViewFV).setOnLongClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle("Debug")
                    .setSingleChoiceItems(new CharSequence[]{"Enable", "Disable"}, SP.getDebug() ? 0 : 1, (dialogInterface, i) -> {
                        SP.setDebug(i == 0);
                    })
                    .show();
            return false;
        });
        chipGroup = findViewById(R.id.chainGroup);
        chipGroup.setChipSpacingVertical(chipGroup.getChipSpacingHorizontal());

        findViewById(R.id.button_verify).setOnClickListener(view -> {
            if (chipGroup.getChildCount() == 1) {
                new AlertDialog.Builder(this)
                        .setTitle("No Person found")
                        .setMessage("Please add one person and try again")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Select Person")
                    .setAdapter(new ArrayAdapter<Pair<Bitmap, Integer>>(this, android.R.layout.simple_selectable_list_item, android.R.id.text1, tags) {
                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View v = super.getView(position, convertView, parent);
                            TextView tv = v.findViewById(android.R.id.text1);
                            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), tags.get(position).first);
                            drawable.setCircular(true);
                            tv.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                            int dp5 = (int) (7 * getResources().getDisplayMetrics().density + 0.5f);
                            tv.setCompoundDrawablePadding(dp5);
                            tv.setPadding(G.padding_16, 0, G.padding_16, 0);
                            tv.setText(SP.getPersonName(tags.get(position).second));
                            return v;
                        }
                    }, (dialogInterface, i) -> {
                        Intent intent = new Intent(this, VerificationActivity.class);
                        intent.putExtra(PersonActivity.PERSON_ID, tags.get(i).second);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        findViewById(R.id.button_add_person).setOnClickListener(view -> {
            FORCE_UPDATE_PERSONS = true;
            startActivity(new Intent(this, PersonActivity.class));
        });
        findViewById(R.id.button_compare).setOnClickListener(view -> startActivity(new Intent(this, CompareActivity.class)));

        updatePersons();
    }

    private void updatePersons() {
        tags.clear();
        File personsFolder = new File(getDataDir(), "persons");
        if (!personsFolder.exists())
            return;
        for (File personFile : personsFolder.listFiles()) {
            Bitmap chipBmp = MyUtils.file2Bitmap(new File(personFile, "0.png"));

            tags.add(new Pair<>(chipBmp, Integer.parseInt(personFile.getName())));
        }
        setTag(tags);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        G.setMTcnn(getAssets());
        G.setM2Ncnn(getAssets());
        if (FORCE_UPDATE_PERSONS) {
            FORCE_UPDATE_PERSONS = false;
            chipGroup.removeAllViews();
            Chip chip = new Chip(this);
            chip.setPadding(G.padding_8, G.padding_8, G.padding_8, G.padding_8);
            chip.setText("Face Verification - Hossein Zaaferani");
            chipGroup.addView(chip);

            updatePersons();
        }

        if (!permissionRequested && !allPermissionsGranted()) {
            permissionRequested = true;
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                new AlertDialog.Builder(this)
                        .setTitle("Alert")
                        .setMessage("Please allow all permissions")
                        .setCancelable(false)
                        .setPositiveButton("Allow", (dialogInterface, i) -> ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS))
                        .setNegativeButton("Exit", (dialogInterface, i) -> finish())
                        .show();
            }
        }
    }

    private void setTag(final List<Pair<Bitmap, Integer>> tagList) {
        for (int index = 0; index < tagList.size(); index++) {
            final Pair<Bitmap, Integer> bs = tagList.get(index);
            final Bitmap tagIcon = bs.first;
            final int personId = bs.second;
            final String tagName = SP.getPersonName(personId);

            final Chip chip = new Chip(this);
            int paddingDp = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8,
                    getResources().getDisplayMetrics()
            );
            chip.setPadding(paddingDp, paddingDp, paddingDp, paddingDp);
            chip.setText(tagName);

//            Bitmap src = TestActivity.readFromAssets(getAssets(), "w_10136.jpg");
            RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), tagIcon);
            drawable.setCircular(true);

            chip.setChipIcon(drawable);
            chip.setChipIconSize(chip.getChipIconSize() * 2);
            chip.setChipMinHeight(chip.getChipIconSize() + paddingDp);
            chip.setOnClickListener(view -> {
                new AlertDialog.Builder(this)
                        .setTitle(tagName)
                        .setSingleChoiceItems(new CharSequence[]{"Edit", "Delete", "Verification"}, -1, (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            if (i == 0) {
                                FORCE_UPDATE_PERSONS = true;
                                Intent intent = new Intent(this, PersonActivity.class);
                                intent.putExtra(PersonActivity.PERSON_ID, personId);
                                startActivity(intent);
                            } else if (i == 1) {
                                AlertDialog dialog = new AlertDialog.Builder(this)
                                        .setTitle(tagName)
                                        .setMessage("Delete person?")
                                        .setPositiveButton("Delete", (d_, i_) -> {
                                            MyUtils.deletePersonDirAndImages(new File(getDataDir(), "persons/" + personId));
                                            SP.removePersonName(personId);
                                            tags.remove(bs);
                                            chipGroup.removeView(chip);
                                            Toast.makeText(this, tagName + " removed", Toast.LENGTH_SHORT).show();
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .create();
                                dialog.show();
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.red_500));
                            } else if (i == 2) {
                                Intent intent = new Intent(this, VerificationActivity.class);
                                intent.putExtra(PersonActivity.PERSON_ID, personId);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            chipGroup.addView(chip);
        }
    }

    private void copyPersons() {
        if (SP.getDefaultPersonsDone())
            return;
        try {
            String[] names = new String[]{
                    "مهدی پاکدل",
                    "محمدرضا گلزار",
                    "طناز طباطبایی",
                    "جواد عزتی",
                    "پریناز ایزدیار",
            };
            for (int i = 1; i <= 5; i++) {
                int pId = SP.getLastPersonId();
                File personFolder = new File(getDataDir() + "/persons", String.valueOf(pId));
                if (!personFolder.exists())
                    personFolder.mkdirs();
                SP.putPersonName(pId, names[i - 1]);
                for (String path : getAssets().list("persons/" + i)) {
                    File dest = new File(personFolder, path);
                    InputStream in = getAssets().open("persons/" + i + "/" + path);
                    dest.createNewFile();
                    FileOutputStream out = new FileOutputStream(dest);
                    byte[] buff = new byte[in.available()];
                    in.read(buff);
                    out.write(buff);
                    out.close();
                    in.close();
                }
                SP.increaseLastPersonId();
            }
            SP.setDefaultPersonsDone(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}