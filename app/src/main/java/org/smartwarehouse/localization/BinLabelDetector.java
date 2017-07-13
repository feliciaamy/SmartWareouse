package org.smartwarehouse.localization;

import android.graphics.Color;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Amy on 30/5/17.
 */

public class BinLabelDetector {
    private static Mat ImageMat;
    private static Mat filteredMat = new Mat();

    // Found
    private static List<Dimension> potentialLabels = new ArrayList<Dimension>();
    private static List<Dimension> centroids = new ArrayList<Dimension>();


    public BinLabelDetector(Mat img) {
        this.ImageMat = img;
        clear();
        findPotentialLabels();
    }

    private static void findPotentialLabels() {
        filtering();
        List<MatOfPoint> coordinates = new ArrayList<MatOfPoint>();
        Imgproc.findContours(filteredMat, coordinates, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        final List<Point> potentialLabels = new ArrayList<Point>();
        final List<Float> ratio = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            if (Imgproc.contourArea(coordinates.get(i)) > 4500) {
                Rect rect = Imgproc.boundingRect(coordinates.get(i));

                float ratiod = ((float) rect.height / (float) rect.width);
                if (0 < ratiod && ratiod < 0.4) {
                    // Drawing of rectangle
                    Moments moments = Imgproc.moments(coordinates.get(i));
                    final Point centroid = new Point();
                    centroid.x = moments.get_m10() / moments.get_m00();
                    centroid.y = moments.get_m01() / moments.get_m00();
                    centroids.add(new Dimension(centroid.x, centroid.y, 20, Color.BLACK));
                    potentialLabels.add(new Dimension(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, Color.MAGENTA));
//                    Imgproc.rectangle(ImageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 20);
                }
            }
        }
    }

    public static List<Dimension> getPotentialLabels() {
        return potentialLabels;
    }

    public static List<Dimension> getEliminatedLabels(Dimension bottom, Dimension right, Dimension left) {
        return eliminateBinLabel(bottom, right, left);
    }

    private static void filtering() {
        Mat filteredMatX = new Mat();
        Mat filteredMatY = new Mat();
        Imgproc.cvtColor(ImageMat, filteredMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Sobel(filteredMat, filteredMatX, CvType.CV_32F, 1, 0);
        Imgproc.Sobel(filteredMat, filteredMatY, CvType.CV_32F, 0, 1);
        Core.subtract(Mat.ones(filteredMatY.size(), CvType.CV_32F), filteredMatX, filteredMat);
        Core.convertScaleAbs(filteredMat, filteredMat);
        Size size = new Size(9, 9);
        Imgproc.blur(filteredMat, filteredMat, size);
        Imgproc.threshold(filteredMat, filteredMat, 30, 255, Imgproc.THRESH_BINARY);
        Size size1 = new Size(21, 7);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size1);
        Imgproc.morphologyEx(filteredMat, filteredMat, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.erode(filteredMat, filteredMat, kernel);
        Imgproc.dilate(filteredMat, filteredMat, kernel);
    }

    public static List<Dimension> getEliminatedCentroids(List<Dimension> binLabels) {
        List<Dimension> eliminatedCentroids = new ArrayList<Dimension>();
        for (Dimension label : binLabels) {
            double x = (label.getRight() + label.getLeft()) / 2;
            double y = (label.getBottom() + label.getTop()) / 2;
            eliminatedCentroids.add(new Dimension(x, y, 20, Color.BLACK));
        }

        return eliminatedCentroids;
    }

    // Return eliminated and sorted Bin Label
    private static List<Dimension> eliminateBinLabel(Dimension b, Dimension r, Dimension l) {
        List<Dimension> eliminatedLabels = new ArrayList<Dimension>();

        Log.d("Boundary", b.toString());
        if (b.getOrientation() == Orientation.VERTICAL) {
            Log.d("ORIENTATION ERROR", "wrong orientation");
        }
        if (r.getOrientation() == Orientation.HORIZONTAL || l.getOrientation() == Orientation.HORIZONTAL) {
            Log.d("ORIENTATION ERROR", "wrong orientation");
        }
        for (Dimension label : potentialLabels) {
            Log.d("label", label.toString());
            if (isInLine(label, b)) {
                if(label.getLeft() > l.getCenter() && label.getRight() < r.getCenter()){
                    Log.d("Correct label", label.toString());
                    eliminatedLabels.add(label);
                }
            }
        }
        Collections.sort(eliminatedLabels, new Comparator<Dimension>() {
            public int compare(Dimension d1, Dimension d2) {
                return Double.compare(d1.getLeft(), d2.getLeft());
            }
        });
        for (Dimension d : eliminatedLabels) {
            Log.d("Sorted Bin Labels", d.toString());
        }
        return eliminatedLabels;
    }

    private static boolean isInLine(Dimension label, Dimension line) {
        if (line.getShape() == Shape.LINE && label.getShape() == Shape.RECTANGLE) {
            double height = Math.abs(label.getTop() - label.getBottom());
            if (Math.abs((label.getTop() + label.getBottom()) / 2 - line.getCenter()) < height * 2) {
                return true;
            }
        }
        return false;
    }

    private void clear() {
        potentialLabels = new ArrayList<Dimension>();
        filteredMat = new Mat();
    }
}
