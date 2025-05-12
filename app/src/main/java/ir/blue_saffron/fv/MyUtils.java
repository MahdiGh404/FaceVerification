package ir.blue_saffron.fv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MyUtils {
    public static void deletePersonDirAndImages(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
                new File(dir, children[i]).delete();
            dir.delete();
        }
    }


    public static void bitmap2File(Bitmap bitmap, File bitmapFile) {
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(bitmapFile));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap file2Bitmap(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void writeFloatFile(File file, float[] floats) {
        RandomAccessFile aFile = null;
        try {
            aFile = new RandomAccessFile(file.getPath(), "rw");
            FileChannel outChannel = aFile.getChannel();

            //one float 4 bytes
            ByteBuffer buf = ByteBuffer.allocate(4 * floats.length);
            buf.clear();
            buf.asFloatBuffer().put(floats);

            //while(buf.hasRemaining())
            {
                outChannel.write(buf);
            }
            //outChannel.close();
            buf.rewind();

            float[] out = new float[3];
            buf.asFloatBuffer().get(out);

            outChannel.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (aFile != null) {
                try {
                    aFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static float[] readFloatFile(File file, int length) {
        float[] readback = new float[length];
        RandomAccessFile rFile = null;
        try {
            rFile = new RandomAccessFile(file.getPath(), "rw");
            FileChannel inChannel = rFile.getChannel();
            ByteBuffer buf_in = ByteBuffer.allocate(length * 4);
            buf_in.clear();

            inChannel.read(buf_in);

            buf_in.rewind();
            buf_in.asFloatBuffer().get(readback);

            inChannel.close();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } finally {
            if (rFile != null) {
                try {
                    rFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return readback;
    }
}
