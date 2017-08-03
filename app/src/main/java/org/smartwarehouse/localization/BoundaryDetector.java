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
import org.smartwarehouse.object.Boundary;
import org.smartwarehouse.object.Centroid;
import org.smartwarehouse.object.Orientation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BoundaryDetector {
    // Colours
    private final Scalar LOWERTHRESHOLD = new Scalar(115, 50, 100); // Dull Red color – lower hsv values
    private final Scalar UPPERTHRESHOLD = new Scalar(125, 255, 255); // Dull Red color – higher hsv values

    // Mats
    private Mat mHsv = new Mat();
    private Mat mMaskMat = new Mat();
    private Mat mDilatedMat = new Mat();

    // Mat
    private Mat ImageMat;

    // Found
    private List<Centroid> foundMarkers = new ArrayList<Centroid>();
    private List<Boundary> boundaries = new ArrayList<Boundary>();
    private List<Double> heights = new ArrayList<Double>();
    private List<Centroid> bottomMarkers = new ArrayList<Centroid>();

    private Boundary bottomBoundary = new Boundary(-1, -1, -1, Orientation.HORIZONTAL, Color.BLACK);
    private Boundary leftBoundary = new Boundary(-1, -1, -1, Orientation.VERTICAL, Color.BLACK);
    private Boundary rightBoundary = new Boundary(-1, -1, -1, Orientation.VERTICAL, Color.BLACK);
    private Boundary topBoundary = new Boundary(-1, -1, -1, Orientation.HORIZONTAL, Color.BLACK);

    // Colors
    private final int[] colors = {Color.RED, Color.BLUE, Color.GREEN};

    public BoundaryDetector(Mat img) {
        this.ImageMat = img;
        clear();
        Imgproc.cvtColor(ImageMat, mHsv, Imgproc.COLOR_BGR2HSV);
        findMarkers();
    }

    private void findMarkers() {
        Core.inRange(mHsv, LOWERTHRESHOLD, UPPERTHRESHOLD, mMaskMat);

        // erode and dilate
        filtering();

        // contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mDilatedMat, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        final List<Point> detected = new ArrayList<Point>();
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            if (Imgproc.contourArea(contours.get(contourIdx)) > 500) {
                Moments moments = Imgproc.moments(contours.get(contourIdx));
                final Point centroid = new Point();
                centroid.x = moments.get_m10() / moments.get_m00();
                centroid.y = moments.get_m01() / moments.get_m00();
                if (!detected.contains(centroid)){
                    detected.add(centroid);
                }
            } else {
                Log.d("Marker", Imgproc.contourArea(contours.get(contourIdx)) + "");
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
                        topBoundary = new Boundary(minX, maxX, tempSum / tempCount, Orientation.HORIZONTAL, Color.CYAN);
                    }
                    boundaries.add(new Boundary(minX, maxX, tempSum / tempCount, Orientation.HORIZONTAL, Color.CYAN));
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
            bottomMarkers.add(new Centroid(detected.get(i).x, detected.get(i).y, 50, colors[c]));
            foundMarkers.add(new Centroid(detected.get(i).x, detected.get(i).y, 50, colors[c]));
            tempCount++;
            tempSum += height;
        }
        bottomBoundary = new Boundary(minX, maxX, tempSum / tempCount, Orientation.HORIZONTAL, colors[c]);
        boundaries.add(bottomBoundary);

        heights.add(tempSum / tempCount);

        // Last set of markers are the lowest
        if (heights.size() > 0) {
            // left
            leftBoundary = new Boundary(heights.get(0), heights.get(heights.size() - 1), minX, Orientation.VERTICAL, Color.CYAN);
            boundaries.add(leftBoundary);
            // right
            rightBoundary = new Boundary(heights.get(0), heights.get(heights.size() - 1), maxX, Orientation.VERTICAL, Color.CYAN);
            boundaries.add(rightBoundary);
        }
    }

    private void filtering() {
        Imgproc.erode(mMaskMat, mDilatedMat, new Mat());
        Imgproc.erode(mMaskMat, mDilatedMat, new Mat());
        Imgproc.erode(mMaskMat, mDilatedMat, new Mat());
    }

    private void clear() {
        foundMarkers = new ArrayList<Centroid>();
        boundaries = new ArrayList<Boundary>();
        heights = new ArrayList<Double>();
        bottomMarkers = new ArrayList<Centroid>();
    }

    // Getters
    public Boundary getBottomBoundary() {
        return bottomBoundary;
    }

    public Boundary getTopBoundary() {
        return topBoundary;
    }

    public Boundary getLeftBoundary() {
        return leftBoundary;
    }

    public Boundary getRightBoundary() {
        return rightBoundary;
    }

    public List<Centroid> getMarkers() {
        return foundMarkers;
    }

    public List<Boundary> getBoundaries() {
        return boundaries;
    }

    public List<Centroid> getBottomMarkers() {
        Collections.sort(bottomMarkers, new Comparator<Centroid>() {
            public int compare(Centroid d1, Centroid d2) {
                return Double.compare(d1.getX(), d2.getX());
            }
        });
        for (Centroid d : bottomMarkers) {
            Log.d("Sorted Bottom Markers", d.toString());
        }
        return bottomMarkers;
    }
}
