package org.smartwarehouse.localization;

import android.graphics.Color;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ColorBlobDetector {
    // Colours
    private final static Scalar LOWERTHRESHOLD = new Scalar(110, 100, 100); // Dull Red color – lower hsv values
    private final static Scalar UPPERTHRESHOLD = new Scalar(120, 255, 255); // Dull Red color – higher hsv values

    // Mats
    private static Mat mHsv = new Mat();
    private static Mat mMaskMat = new Mat();
    private static Mat mDilatedMat = new Mat();

    // Mat
    private static Mat ImageMat;

    // Found
    private static List<Dimension> foundMarkers = new ArrayList<Dimension>();
    private static List<Dimension> boundaries = new ArrayList<Dimension>();
    private static List<Double> heights = new ArrayList<Double>();
    private static List<Dimension> bottomMarkers = new ArrayList<Dimension>();

    private static Dimension bottomBoundary;
    private static Dimension leftBoundary;
    private static Dimension rightBoundary;
    private static Dimension topBoundary;

    // Colors
    private static final int[] colors = {Color.RED, Color.BLUE, Color.GREEN};

    public ColorBlobDetector(Mat img) {
        this.ImageMat = img;
        clear();
        Imgproc.cvtColor(ImageMat, mHsv, Imgproc.COLOR_BGR2HSV);
        findMarkers();
    }

    private static void findMarkers() {
        Core.inRange(mHsv, LOWERTHRESHOLD, UPPERTHRESHOLD, mMaskMat);

        // erode and dilate
        filtering();

        // contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mDilatedMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        final List<Point> detected = new ArrayList<Point>();
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            if (Imgproc.contourArea(contours.get(contourIdx)) > 50) {
                Moments moments = Imgproc.moments(contours.get(contourIdx));
                final Point centroid = new Point();
                centroid.x = moments.get_m10() / moments.get_m00();
                centroid.y = moments.get_m01() / moments.get_m00();
                detected.add(centroid);
            }
        }

        Collections.sort(detected, new Comparator<Point>() {
            public int compare(Point s1, Point s2) {
                return Double.compare(s1.y, s2.y);
            }
        });

        int c = 0;
        double height = -1;
        double tempSum = 0;
        double tempCount = 0;
        double maxX = Double.MIN_VALUE;
        double minX = Double.MAX_VALUE;
        for (int i = 0; i < detected.size(); i++) {
            if (height < 0) {
                height = detected.get(i).y;
            }
            if (height >= 0) {
                if (Math.abs(detected.get(i).y - height) > 300) {
                    if (boundaries.isEmpty()) {
                        topBoundary = new Dimension(minX, maxX, tempSum / tempCount, Orientation.HORIZONTAL, Color.CYAN);
                    }
                    boundaries.add(new Dimension(minX, maxX, tempSum / tempCount, Orientation.HORIZONTAL, Color.CYAN));
                    heights.add(tempSum / tempCount);
                    bottomMarkers.clear();
                    maxX = minX = detected.get(i).x;
                    tempSum = 0;
                    tempCount = 0;
                    c++;
                }
                if (maxX < detected.get(i).x) {
                    maxX = detected.get(i).x;
                }
                if (minX > detected.get(i).x) {
                    minX = detected.get(i).x;
                }
            }

            height = detected.get(i).y;
            bottomMarkers.add(new Dimension(detected.get(i).x, detected.get(i).y, 50, colors[c]));
            foundMarkers.add(new Dimension(detected.get(i).x, detected.get(i).y, 50, colors[c]));
            tempCount++;
            tempSum += height;
        }
        bottomBoundary = new Dimension(minX, maxX, tempSum / tempCount, Orientation.HORIZONTAL, colors[c]);
        boundaries.add(bottomBoundary);

        heights.add(tempSum / tempCount);

        // Last set of markers are the lowest
        if (heights.size() > 0) {
            // left
            leftBoundary = new Dimension(heights.get(0), heights.get(heights.size() - 1), minX, Orientation.VERTICAL, Color.CYAN);
            boundaries.add(leftBoundary);
            // right
            rightBoundary = new Dimension(heights.get(0), heights.get(heights.size() - 1), maxX, Orientation.VERTICAL, Color.CYAN);
            boundaries.add(rightBoundary);
        }
    }

    public static Dimension getBottomBoundary() {
        return bottomBoundary;
    }

    public static Dimension getTopBoundary() {
        return topBoundary;
    }

    public static Dimension getLeftBoundary() {
        return leftBoundary;
    }

    public static Dimension getRightBoundary() {
        return rightBoundary;
    }

    public static List<Dimension> getMarkers() {
        return foundMarkers;
    }

    public static List<Dimension> getBoundaries() {
        return boundaries;
    }

    public static List<Dimension> getBinLabels() {
        Collections.sort(bottomMarkers, new Comparator<Dimension>() {
            public int compare(Dimension d1, Dimension d2) {
                return Double.compare(d2.x, d1.x);
            }
        });
        for (Dimension d : bottomMarkers) {
            Log.d("Sorted Bin Labels", d.toString());
        }
        return bottomMarkers;
    }

    private static void filtering() {
        Imgproc.erode(mMaskMat, mDilatedMat, new Mat());
        Imgproc.erode(mMaskMat, mDilatedMat, new Mat());
        Imgproc.erode(mMaskMat, mDilatedMat, new Mat());
    }

    private void clear() {
        foundMarkers = new ArrayList<Dimension>();
        boundaries = new ArrayList<Dimension>();
        heights = new ArrayList<Double>();
        bottomMarkers = new ArrayList<Dimension>();
    }
}
