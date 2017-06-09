package org.smartwarehouse.localization;

import android.graphics.Color;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Amy on 31/5/17.
 */

public class BoxDetector {
    private static Mat ImageMat;
    private static Mat filteredMat = new Mat();

    // Found
    private static List<Dimension> boxes = new ArrayList<Dimension>();
    private static List<Dimension> centroids = new ArrayList<Dimension>();


    public BoxDetector(Mat img) {
        this.ImageMat = img;
        clear();
        findBoxes();
    }

    private static void findBoxes() {
        filtering();
        List<MatOfPoint> coordinates = new ArrayList<MatOfPoint>();
        Imgproc.findContours(filteredMat, coordinates, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        final List<Point> potentialboxes = new ArrayList<Point>();
        final List<Float> ratio = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            if (Imgproc.contourArea(coordinates.get(i)) > 50000 && Imgproc.contourArea(coordinates.get(i)) < 200000) {
                Rect rect = Imgproc.boundingRect(coordinates.get(i));

                float ratiod = ((float) rect.height / (float) rect.width);
                if (0.4 < ratiod && ratiod < 0.55) {
                    // Drawing of rectangle
                    boxes.add(new Dimension(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, Color.GREEN));
                    Imgproc.rectangle(ImageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 20);
                    Moments moments = Imgproc.moments(coordinates.get(i));
                    final Point centroid = new Point();
                    centroid.x = moments.get_m10() / moments.get_m00();
                    centroid.y = moments.get_m01() / moments.get_m00();
                    centroids.add(new Dimension(centroid.x, centroid.y, 20, Color.BLACK));
                    Log.d("(Drawn) Ratio: ", ratiod + ", Area: " + Imgproc.contourArea(coordinates.get(i)));
                } else {
                    Log.d("Ratio: ", ratiod + ", Area: " + Imgproc.contourArea(coordinates.get(i)));
                }
            }
        }
    }

    public static List<Dimension> getBoxes() {
        return boxes;
    }

    private static void filtering() {
        Core.bitwise_not(ImageMat, filteredMat);
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(filteredMat, filteredMat, 115, 255, Imgproc.THRESH_BINARY);

        Size size1 = new Size(9, 5); // 2,2 // the smaller, the smaller the area of the rect
        Size size2 = new Size(1, 1); //1,1
        Size size3 = new Size(3, 3);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size1);
        Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size2);
        Imgproc.erode(filteredMat, filteredMat, kernel2);
        Imgproc.dilate(filteredMat, filteredMat, kernel);
        Imgproc.erode(filteredMat, filteredMat, kernel2);
        Core.bitwise_not(filteredMat, filteredMat);
    }

    public Mat getFilteredMat() {
        return filteredMat;
    }

    public static List<Dimension> getEliminatedBoxes(List<Dimension> boundaries) {
        List<Dimension> eliminatedBoxes = new ArrayList<Dimension>();
        double eps = 5;
        for (Dimension box : boxes) {
            int score = 0;
            double x = (box.getRight() + box.getLeft()) / 2;
            double y = (box.getBottom() + box.getTop()) / 2;
            for (Dimension b : boundaries) {
                if (b.getOrientation() == Orientation.HORIZONTAL) {
                    if (y - b.getCenter() < eps) {
                        score -= 5;
                    } else {
                        score += 5;
                    }
                } else {
                    if (x - b.getCenter() < eps) {
                        score -= 1;
                    } else {
                        score += 1;
                    }
                }
            }
            if (score == 0) {
                eliminatedBoxes.add(box);
            }
        }
        return eliminatedBoxes;
    }

    public static List<Dimension> getEliminatedCentroids(List<Dimension> boxes) {
        List<Dimension> centroids = new ArrayList<Dimension>();
        for (Dimension box : boxes) {
            double x = (box.getRight() + box.getLeft()) / 2;
            double y = (box.getBottom() + box.getTop()) / 2;
            centroids.add(new Dimension(x, y, 20, Color.BLACK));
        }
        return centroids;
    }

    private void clear() {
        boxes = new ArrayList<Dimension>();
        centroids = new ArrayList<Dimension>();
        filteredMat = new Mat();
    }

    public static List<Dimension> getCentroids() {
        return centroids;
    }
}