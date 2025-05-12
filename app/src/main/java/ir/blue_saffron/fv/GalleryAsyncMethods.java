package ir.blue_saffron.fv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;

public class GalleryAsyncMethods {

    static AlertDialog dialog(Context context) {
        ProgressBar progressBar = new ProgressBar(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(G.padding_16, G.padding_16, G.padding_16, G.padding_16);
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        textView.setLayoutParams(params);
        textView.setPadding(G.padding_16, 0, 0, 0);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setText("Loading...\nPre Processing Large Image");
        linearLayout.addView(progressBar);
        linearLayout.addView(textView);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCustomTitle(linearLayout)
                .setCancelable(false)
                .create();
//        dialog.show();
        return dialog;
    }

    public static Bitmap doInBackground(Context context, Uri... uris) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uris[0]);
            bitmap = MyUtils.modifyOrientation(bitmap, ImageFilePath.getPath(context, uris[0]));
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w > 256 && h > 256) {
                int scale = w > h ? w / 256 : h / 256;
                w = w / scale;
                h = h / scale;
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);

            ProcessImage processImageGallery = new ProcessImage(256, 1, false);
            if (processImageGallery.detectFaces(resizedBitmap)) {
                return processImageGallery.getFace();
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
