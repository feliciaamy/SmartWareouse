package org.smartwarehouse.localization;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

import static android.os.Environment.getDataDirectory;

/**
 * Created by Amy on 5/7/17.
 */

public class Haar {
    private static Mat ImageMat;
    private static Mat filteredMat = new Mat();

    // Found
    private static List<Dimension> boxes = new ArrayList<Dimension>();
    private static List<Dimension> centroids = new ArrayList<Dimension>();


    public Haar(Mat img) {
        this.ImageMat = img;
        clear();
        findBoxes();
    }

    private void findBoxes() {
//        InputStream iser = context.getResources().openRawResource(
//                org.opencv.R.raw.cascade);
//        File cascadeDirER = context.getDir("assets", Context. Context.MODE_PRIVATE);
//        File cascadeFileER = new File(cascadeDirER,
//                "cascade.xml");
//        readFile(cascadeFileER);

        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File cascadeDirER = new File(sdcard, "cascade.xml");
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

        for (Rect box : boxesArray) {
            boxes.add(new Dimension(box.x, box.y, box.x + box.width, box.y + box.height, Color.GREEN));
            centroids.add(new Dimension(box.x + box.width / 2, box.y + box.height / 2, 20, Color.BLACK));
        }
        Log.d("Haar", "Boxes #: " + boxes.size());
        Log.d("Haar", "Centroids #: " + centroids.size());
    }

    private void readFile(File file) {
        BufferedReader br = null;
        FileReader fr = null;

        try {

            fr = new FileReader(file.getAbsolutePath());
            br = new BufferedReader(fr);

            String sCurrentLine;

            br = new BufferedReader(new FileReader(file.getAbsolutePath()));

            while ((sCurrentLine = br.readLine()) != null) {
                System.out.println(sCurrentLine);
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
    }


    public static List<Dimension> getEliminatedCentroids(List<Dimension> boxes) {
        List<Dimension> centroids = new ArrayList<Dimension>();
        for (Dimension box : boxes) {
            double x = (box.getRight() + box.getLeft()) / 2;
            double y = (box.getBottom() + box.getTop()) / 2;
            centroids.add(new Dimension(x, y, 20, Color.BLACK));
        }
        if (centroids.size() != boxes.size()) {
            Log.e("Centroid", "Wrong size");
        }
        return centroids;
    }

    public static List<Dimension> getEliminatedBoxes(List<Dimension> boundaries) {
        List<Dimension> eliminatedBoxes = new ArrayList<Dimension>();
        double eps = 5;
        boolean tolerate = false;
        for (Dimension box : boxes) {
            int score = 0;
            double x = (box.getRight() + box.getLeft()) / 2;
            double y = (box.getBottom() + box.getTop()) / 2;
            for (Dimension b : boundaries) {
                if (b.getOrientation() == Orientation.HORIZONTAL) {
                    tolerate = !tolerate;
                    if (y - b.getCenter() < eps) { // Bottom Boundary
                        score -= 5;
                    } else { // Top Boundary
                        score += 5;
                    }
                } else {
                    if (x - b.getCenter() < eps) { // Right Boundary
                        score -= 1;
                    } else { // Left Boundary
                        score += 1;
                    }
                }
            }
            if (score == 0 || tolerate && score == -5) {
                eliminatedBoxes.add(box);
            }
        }

        Collections.sort(eliminatedBoxes, new Comparator<Dimension>() {
            public int compare(Dimension d1, Dimension d2) {
                return Double.compare(d1.getLeft(), d2.getLeft());
            }
        });
        for (Dimension d : eliminatedBoxes) {
            Log.d("Sorted Box", d.toString());
        }
        return eliminatedBoxes;
    }

    private void clear() {
        boxes = new ArrayList<Dimension>();
        centroids = new ArrayList<Dimension>();
        filteredMat = new Mat();
    }
}

//import numpy as np
//import cv2
//face_cascade = cv2.CascadeClassifier('cascade.xml')
//
//img = cv2.imread('t3.jpg')
//gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
//
//faces = face_cascade.detectMultiScale(gray, 1.3, 5)
//for (x,y,w,h) in faces:
//cv2.rectangle(img,(x,y),(x+w,y+h),(255,0,0),10)
//
//print(len(faces))
//cv2.namedWindow('img',cv2.WINDOW_NORMAL)
//cv2.imshow('img',img)
//cv2.waitKey(0)
//cv2.destroyAllWindows()
