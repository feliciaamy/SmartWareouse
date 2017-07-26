package org.smartwarehouse.localization;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.objdetect.CascadeClassifier;
import org.smartwarehouse.object.*;
import org.smartwarehouse.object.Orientation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Amy on 31/5/17.
 */

public class BoxDetector {
    private static Mat ImageMat;
    private static Mat filteredMat = new Mat();

    // Found
    private static List<Label> boxes = new ArrayList<Label>();
    //    private static List<Label> boxesTemp = new ArrayList<>();
    private static List<Centroid> centroids = new ArrayList<Centroid>();
    private static double avgArea = 0;
    private static double avgLength = 0;
    private static double avgHeight = 0;

    public BoxDetector(Mat img, boolean applyThreshold) {
        this.ImageMat = img;
        clear();
        List<Label> boxHaar = findBoxesHaar();
        if (applyThreshold) {
            Log.d("Threshold", "Filter with threshold");
            boxes = findBoxesThreshold(boxHaar);
        } else {
            boxes = boxHaar;
        }
    }

    private List<Label> findBoxesHaar() {
        List<Label> boxesTemp = new ArrayList<>();
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File cascadeDirER = new File(sdcard, "cascade_trials1.xml");
//        readFile(cascadeDirER);
        //        File file = new File("cascade.xml");
        if (cascadeDirER.exists()) {
            Log.d("Cascade", "exists");
        } else {
            Log.e("Cascade", "not exists");
        }

        // Do something else.
        CascadeClassifier cascadeClassifier = new CascadeClassifier(cascadeDirER.getAbsolutePath());
        if (!cascadeClassifier.load(cascadeDirER.getAbsolutePath())) {
            Log.e("HAAR", "Cannot find cascade.xml");
        }

        if (cascadeClassifier.empty()) {
            Log.e("Cascade", "empty");
        } else {
            Log.d("Cascade", "Successful");
        }

        Imgproc.cvtColor(ImageMat, filteredMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(filteredMat, filteredMat);
        MatOfRect boxesRect = new MatOfRect();
        boxesRect.reshape(5);
        cascadeClassifier.detectMultiScale(filteredMat, boxesRect); //Might need to change
        Rect[] boxesArray = boxesRect.toArray();

        double totArea = 0;
        for (Rect box : boxesArray) {
            Label newLabel = new Label(box.x, box.y, box.x + box.width, box.y + box.height, Color.GREEN);
            if (boxesTemp.contains(newLabel)) {
                Log.d("SAME", newLabel.toString());
            } else if (newLabel.getArea() > 300000 && newLabel.getArea() < 450000) {
                totArea += newLabel.getArea();
                Log.d("Area", newLabel.getArea() + "");
                boxesTemp.add(newLabel);
                centroids.add(new Centroid(box.x + box.width / 2, box.y + box.height / 2, 20, Color.BLACK));
            }
        }
        avgArea = totArea / boxesTemp.size();
        Log.d("Average Area", avgArea + "");
        Log.d("Haar", "Boxes #: " + boxesTemp.size());
        Log.d("Haar", "Centroids #: " + centroids.size());
        return boxesTemp;
    }

    private static List<Label> findBoxesThreshold(List<Label> boxesTemp) {
        List<Label> filtered = new ArrayList<Label>();
        filtering();
        List<MatOfPoint> coordinates = new ArrayList<MatOfPoint>();
        Imgproc.findContours(filteredMat, coordinates, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        final List<Float> ratio = new ArrayList<>();
        double totArea = 0;
        for (int i = 0; i < coordinates.size(); i++) {
            Rect box = Imgproc.boundingRect(coordinates.get(i));
            if (Imgproc.contourArea(coordinates.get(i)) > 100000) {
                float ratiod = ((float) box.height / (float) box.width);
                if (0.4 < ratiod && ratiod < 0.55) {
                    // Drawing of rectangle
                    Label newLabel = new Label(box.x, box.y, box.x + box.width, box.y + box.height, Color.GREEN);
                    for (Label b : boxesTemp) {
                        if (newLabel.isInside(b)) {
                            filtered.add(newLabel);
                            Log.d("x, y", box.x + ", " + box.y);
                            Log.d("width, height", box.width + ", " + box.height);
                        } else {
                            Log.d("threshold", box.width + ", " + box.height);
                        }
                    }
                }
            }
        }
        return filtered;
    }
//    private static void findBoxesThreshold() {
//        filtering();
//        List<MatOfPoint> coordinates = new ArrayList<MatOfPoint>();
//        Imgproc.findContours(filteredMat, coordinates, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        final List<Float> ratio = new ArrayList<>();
//        double totArea = 0;
//        for (int i = 0; i < coordinates.size(); i++) {
//            if (Imgproc.contourArea(coordinates.get(i)) > 30000 && Imgproc.contourArea(coordinates.get(i)) < 300000) {
////            if (Imgproc.contourArea(coordinates.get(i)) > 20000 && Imgproc.contourArea(coordinates.get(i)) < 700000) {
//                Rect box = Imgproc.boundingRect(coordinates.get(i));
//
//                float ratiod = ((float) box.height / (float) box.width);
//                if (0.4 < ratiod && ratiod < 0.55) {
//                    // Drawing of rectangle
//                    Label newLabel = new Label(box.x, box.y, box.x + box.width, box.y + box.height, Color.GREEN);
//                    if (boxes.contains(newLabel)) {
//                        Log.d("SAME", newLabel.toString());
//                    } else {
//                        totArea += newLabel.getArea();
//                        Log.d("Area", newLabel.getArea() + "");
//                        boxes.add(newLabel);
//                        centroids.add(new Centroid(box.x + box.width / 2, box.y + box.height / 2, 20, Color.BLACK));
//                    }
//                    Log.d("(Drawn) Ratio: ", ratiod + ", Area: " + Imgproc.contourArea(coordinates.get(i)));
//                } else {
//                    Log.d("(Wrong) Ratio: ", ratiod + ", Area: " + Imgproc.contourArea(coordinates.get(i)));
//                }
//            } else {
//                Log.d("Area: ", "" + Imgproc.contourArea(coordinates.get(i)));
//            }
//        }
//        avgArea = totArea / boxes.size();
//    }

    private static void filtering() {
        Core.bitwise_not(ImageMat, filteredMat);
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.threshold(filteredMat, filteredMat, 115, 255, Imgproc.THRESH_BINARY);
        Imgproc.threshold(filteredMat, filteredMat, 90, 255, Imgproc.THRESH_BINARY);
        Bitmap thres = Bitmap.createBitmap(filteredMat.cols(), filteredMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(filteredMat, thres);
        MainActivity.storeImage(thres);

        Size size1 = new Size(9, 5); // 2,2 // the smaller, the smaller the area of the rect
        Size size2 = new Size(1, 1); //1,1

//        Size size1 = new Size(2, 5); // 2,2 // the smaller, the smaller the area of the rect
//        Size size2 = new Size(1, 1); //1,1

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size1);
        Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, size2);
        Imgproc.erode(filteredMat, filteredMat, kernel2);

        Imgproc.dilate(filteredMat, filteredMat, kernel);

        Imgproc.erode(filteredMat, filteredMat, kernel2);
        Core.bitwise_not(filteredMat, filteredMat);
    }

    //Getter
    // Return a sorted eliminated Boxes
    public static List<Label> getEliminatedBoxes(Boundary topBoundary, Boundary bottomBoundary, Boundary rightBoundary, Boundary leftBoundary) {
        List<Label> eliminatedBoxes = new ArrayList<Label>();
        double eps = 100;
        double areaEps = avgArea * 0.6;
        boolean tolerate = false;
        double totLength = 0;
        double totHeight = 0;
        for (Label box : boxes) {
            if (!(box.getArea() < areaEps + avgArea && box.getArea() > avgArea - areaEps)) {
                continue;
            }
            if (topBoundary.getCenter() - eps < box.getTop() && bottomBoundary.getCenter() + eps > box.getBottom()
                    && leftBoundary.getCenter() - eps < box.getLeft() && rightBoundary.getCenter() + eps > box.getRight()) {
                eliminatedBoxes.add(box);
                totHeight += Math.abs(box.getTop() - box.getBottom());
                totLength += Math.abs(box.getTop() - box.getBottom());
            }
        }

        Collections.sort(eliminatedBoxes, new Comparator<Label>() {
            public int compare(Label d1, Label d2) {
                return Double.compare(d1.getLeft(), d2.getLeft());
            }
        });
        for (Label d : eliminatedBoxes) {
            Log.d("Sorted Box", d.toString());
        }
        avgLength = totLength / eliminatedBoxes.size();
        avgHeight = totHeight / eliminatedBoxes.size();
        return eliminatedBoxes;
    }

    public double getAvgEliminatedLength() {
        return avgLength;
    }

    public double getAvgEliminatedHeight() {
        return avgHeight;
    }

    public static List<Centroid> getEliminatedCentroids(List<Label> boxes) {
        List<Centroid> centroids = new ArrayList<Centroid>();
        for (Label box : boxes) {
            double x = (box.getRight() + box.getLeft()) / 2;
            double y = (box.getBottom() + box.getTop()) / 2;
            centroids.add(new Centroid(x, y, 20, Color.BLACK));
        }
        if (centroids.size() != boxes.size()) {
            Log.e("Centroid", "Wrong size");
        }
        return centroids;
    }

    public static List<Label> getBoxes() {
        return boxes;
    }

    public static List<Centroid> getCentroids() {
        return centroids;
    }

    private void clear() {
        boxes = new ArrayList<Label>();
        centroids = new ArrayList<Centroid>();
        filteredMat = new Mat();
        avgArea = 0;
    }
}

enum Algorithm {
    HAAR,
    THRESHOLDING
}