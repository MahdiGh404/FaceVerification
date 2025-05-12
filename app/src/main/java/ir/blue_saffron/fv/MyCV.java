package ir.blue_saffron.fv;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MyCV {
    static int argmax(double[] array) {
        double max = array[0];
        int re = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
                re = i;
            }
        }
        return re;
    }

    static int floorDiv2(int x) {
        int r = x / 2;
        if ((x ^ 2) < 0 && (r * 2 != x))
            r--;
        return r;
    }

    public static Mat extractImageChips(Bitmap img, Point[] points, int desired_size, double padding, boolean rotate) {
        Mat mat = new Mat();
        Bitmap bmp32 = img.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
        return extractImageChips(mat, points, desired_size, padding, rotate);
    }

    public static Mat extractImageChips(Mat img, Point[] points) {
        return extractImageChips(img, points, 112, 0.3, true);
    }

    public static Mat extractImageChips(Mat img, Point[] points, int desired_size, double padding, boolean rotate) {
        // points = left eye, right eye, nose, left mouth, right mouth

        // average positions of face points
        double[] mean_face_shape_x = {0.224152, 0.75610125, 0.490127, 0.254149, 0.726104};
        double[] mean_face_shape_y = {0.2119465, 0.2119465, 0.628106, 0.780233, 0.780233};

        Point[] to_points = new Point[5];
        for (int i = 0; i < 5; i++) {
            double x = (padding + mean_face_shape_x[i]) / (2 * padding + 1) * desired_size;
            double y = (padding + mean_face_shape_y[i]) / (2 * padding + 1) * desired_size;
            to_points[i] = new Point(x, y);
        }

        // compute the similar transform
        // Mat[] tran_b_m = find_tfrom_between_shapes(points, to_points);
        // Mat tran_b = tran_b_m[0];
        // Mat tran_m = tran_b_m[1];
        Mat tran_m = find_tfrom_between_shapes(points, to_points);

        Mat probe_vec = new Mat(2, 1, CvType.CV_64F);
        probe_vec.put(0, 0, 1.0);
        probe_vec.put(1, 0, 0.0);
        Mat tran_m_probe_vec = new Mat();
        Core.gemm(tran_m, probe_vec, 1, new Mat(), 0, tran_m_probe_vec);

        double scale = Core.norm(tran_m_probe_vec);
        double angle = 180.0 / Math.PI * Math.atan2(tran_m_probe_vec.get(1, 0)[0], tran_m_probe_vec.get(0, 0)[0]);

        Point from_center = new Point(
                floorDiv2((int) (points[0].x + points[1].x)),
                floorDiv2((int) (points[0].y + points[1].y))
        );
        Point to_center = new Point(desired_size * 0.5, desired_size * 0.4);

        double ex = to_center.x - from_center.x;
        double ey = to_center.y - from_center.y;

        Mat rot_mat = Imgproc.getRotationMatrix2D(from_center, rotate ? -1 * angle : 0, scale);
        rot_mat.put(0, 2, rot_mat.get(0, 2)[0] + ex);
        rot_mat.put(1, 2, rot_mat.get(1, 2)[0] + ey);

        Mat chip = new Mat();
        Imgproc.warpAffine(img, chip, rot_mat, new Size(desired_size, desired_size), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(0, 0, 0, 255));
        // Imgproc.warpAffine(img, chip, rot_mat, new Size(desired_size, desired_size));

        return chip;
    }

    private static Mat find_tfrom_between_shapes(Point[] from_points, Point[] to_points) {
        double sigma_from = 0.0;
        // double sigma_to = 0.0;

        Mat mat_cov = Mat.zeros(2, 2, CvType.CV_64F);
        // compute the mean and cov
        double mfx = 0.0; // mean Xs
        double mfy = 0.0; // mean Ys
        for (Point point : from_points) {
            mfx += point.x;
            mfy += point.y;
        }
        Point mean_from = new Point(mfx / 5, mfy / 5);
        mfx = mfy = 0.0;
        for (Point point : to_points) {
            mfx += point.x;
            mfy += point.y;
        }
        Point mean_to = new Point(mfx / 5, mfy / 5);

        for (int i = 0; i < from_points.length; i++) {
            Point f_point = from_points[i];
            Point t_point = to_points[i];
            double temp_dis = Math.sqrt(Math.pow(f_point.x - mean_from.x, 2) + Math.pow(f_point.y - mean_from.y, 2));
            sigma_from += temp_dis * temp_dis;
            // temp_dis = Math.sqrt(Math.pow(t_point.x - mean_to.x, 2) + Math.pow(t_point.y - mean_to.y, 2));
            // sigma_to += temp_dis * temp_dis;

            double tmp1 = t_point.x - mean_to.x;
            double tmp2 = t_point.y - mean_to.y;
            double tmp3 = f_point.x - mean_from.x;
            double tmp4 = f_point.y - mean_from.y;
            double[] tmp = {tmp1 * tmp3, tmp1 * tmp4, tmp2 * tmp3, tmp2 * tmp4};

            //cov += (to_shape_points[i].transpose() - mean_to.transpose()) * (from_shape_points[i] - mean_from)

            mat_cov.put(0, 0, mat_cov.get(0, 0)[0] + tmp[0]);
            mat_cov.put(0, 1, mat_cov.get(0, 1)[0] + tmp[1]);
            mat_cov.put(1, 0, mat_cov.get(1, 0)[0] + tmp[2]);
            mat_cov.put(1, 1, mat_cov.get(1, 1)[0] + tmp[3]);
        }
        sigma_from = sigma_from / 5;
        // sigma_to = sigma_to / 5;
        Core.multiply(mat_cov, new Scalar(1 / 5.0), mat_cov);

        // compute the affine matrix
        Mat s = Mat.ones(2, 2, CvType.CV_64F);
        s.put(0, 1, 0.0);
        s.put(1, 0, 0.0);

        Mat mat_w = new Mat();
        Mat mat_u = new Mat();
        Mat mat_vt = new Mat();
        Core.SVDecomp(mat_cov, mat_w, mat_u, mat_vt);

        if (Core.determinant(mat_cov) < 0) {
            if (mat_w.get(1, 0)[0] < mat_w.get(0, 0)[0]) {
                s.put(1, 1, -1);
            } else {
                s.put(0, 0, -1);
            }
        }

        // Mat r = mat_u * s * mat_vt;
        Mat mat_tmp = new Mat();
        Core.gemm(mat_u, s, 1, new Mat(), 0, mat_tmp);
        Mat mat_r = new Mat();
        Core.gemm(mat_tmp, mat_vt, 1, new Mat(), 0, mat_r);

        double c = 1.0;
        if (sigma_from != 0) {
            Mat mat_w_as_d = Mat.zeros(2, 2, CvType.CV_64F);
            mat_w_as_d.put(0, 0, mat_w.get(0, 0)[0]);
            mat_w_as_d.put(1, 1, mat_w.get(1, 0)[0]);
            Mat mat_ds = new Mat();
            Core.gemm(mat_w_as_d, s, 1, new Mat(), 0, mat_ds);
            c = 1.0 / sigma_from * Core.trace(mat_ds).val[0];
        }

        // Mat mat_mean_from = new Mat(1, 2, CvType.CV_64F);
        // mat_mean_from.put(0, 0, mean_from.x);
        // mat_mean_from.put(0, 1, mean_from.y);
        // Mat mat_c_r_mean_from = new Mat();
        // Core.gemm(mat_r, mat_mean_from.t(), c, new Mat(), 0, mat_c_r_mean_from);
        // Mat mat_mean_to = new Mat(1, 2, CvType.CV_64F);
        // mat_mean_to.put(0, 0, mean_to.x);
        // mat_mean_to.put(0, 1, mean_to.y);
        // Mat tran_b = new Mat();
        // Core.subtract(mat_mean_to.t(), mat_c_r_mean_from, tran_b);

        Mat tran_m = new Mat();
        Core.multiply(mat_r, new Scalar(c), tran_m);

        // return new Mat[]{tran_b, tran_m};
        return tran_m;
    }

    public static int center_face_index(Mat bboxs, Point img_size) {
        Point img_center = new Point(img_size.x / 2, img_size.y / 2);

        Mat det_2_0 = new Mat();
        Core.subtract(bboxs.col(2), bboxs.col(0), det_2_0);
        Mat det_3_1 = new Mat();
        Core.subtract(bboxs.col(3), bboxs.col(1), det_3_1);
        Mat bounding_box_size = new Mat();
        Core.multiply(det_2_0, det_3_1, bounding_box_size);

        Mat det_0_2 = new Mat();
        Core.add(bboxs.col(0), bboxs.col(2), det_0_2);
        Core.multiply(det_0_2, new Scalar(0.5), det_0_2);
        Core.subtract(det_0_2, new Scalar(img_center.y), det_0_2);

        Mat det_1_3 = new Mat();
        Core.add(bboxs.col(1), bboxs.col(3), det_1_3);
        Core.multiply(det_1_3, new Scalar(0.5), det_1_3);
        Core.subtract(det_1_3, new Scalar(img_center.x), det_1_3);

        Mat offsets = det_0_2.t();
        offsets.push_back(det_1_3.t());
        Core.pow(offsets, 2, offsets);

        double[] diffs = new double[offsets.cols()];
        for (int i = 0; i < offsets.cols(); i++) {
            double col_sum = Core.sumElems(offsets.col(i)).val[0];
            diffs[i] = bounding_box_size.get(i, 0)[0] - col_sum * 2;
        }
        return MyCV.argmax(diffs);
    }
}
