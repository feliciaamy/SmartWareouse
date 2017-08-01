package org.smartwarehouse.localization;

import android.util.Log;

import com.scandit.recognition.Barcode;

import org.opencv.core.Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Amy on 14/6/17.
 */

public class Bin {
    private Coordinate binLabel = null;
    private List<Coordinate> boxes = null;
    private Map<Coordinate, Barcodes> boxesBarcodes = new HashMap<>();
    private Barcodes binLabelBarcode = null;
    private int level = 0;

    private double binWidth;
    private double binHeight;
    private double occupancyLevel = 0;
    private String error = "";

    private final double BOX_WIDTH_CM = 13.5;
    private final double BOX_HEIGHT_CM = 8;
    private final double HEIGHT_TO_CM = 71.5;
    private final double WIDTH_TO_CM = 71.5;

    private int counter = 0;

    public Bin(Coordinate binLabel, List<Coordinate> boxes, double binWidth, double binHeight, int level) {
        if (binLabel == null) {
            error = "[ERROR] Missing bin label";
        }
        this.level = level;
        this.binLabel = binLabel;
        this.boxes = boxes;
        this.binHeight = binHeight;
        this.binWidth = binWidth;
    }

    public double getOccupancyLevel() {
        // Note that in the image the top left corner is (0, 0)
        // Assuming that the box is sorted from left to right bottom to top

        Log.d("Occupancy: Height", binHeight + "");
        Log.d("Occupancy: Width", binWidth + "");

        List<Double> stackAreas = new ArrayList<>();
        int count = 0;
        double x = -1;
        for (Coordinate box : boxes) {
            // New Stack
            if (x == -1) {
                x = box.x;
            }
            if (Math.abs(box.x - x) > 400) {
                stackAreas.add(count * BOX_HEIGHT_CM * BOX_WIDTH_CM);
                count = 0;
                x = box.x;
            }

            count++;
        }

        // For the last stack
        if (!boxes.isEmpty()) {
            stackAreas.add(count * BOX_HEIGHT_CM * BOX_WIDTH_CM);
        }

        Log.d("Occupancy", stackAreas.size() + "");
        double occupiedArea = 0;
        for (Double area : stackAreas) {
            occupiedArea += area;
        }
        if (stackAreas.size() != 0) {
            double heightCm = binHeight / HEIGHT_TO_CM;
            double widthCm = binWidth / WIDTH_TO_CM;
            if (binHeight == -1){
                heightCm = 32;
            }
            occupancyLevel = Math.round((occupiedArea / (heightCm * widthCm)) * 100);
        }
        return occupancyLevel;
    }

    public void setBinLabelBarcode(Barcodes barcode) {
        this.binLabelBarcode = barcode;
    }

    public void mapBoxes(Coordinate coordinate, Barcodes barcode) {
        boxesBarcodes.put(coordinate, barcode);
    }

    public Barcodes getBinLabelBarcode() {
        return binLabelBarcode;
    }

    public Coordinate getBinLabelCoordinate() {
        return binLabel;
    }

    public boolean hasNext() {
        if (counter < boxes.size()) {
            return true;
        }
        return false;
    }

    public Coordinate nextBox() {
        if (hasNext()) {
            counter++;
            return boxes.get(counter - 1);
        }
        return null;
    }

    public Coordinate popBox() {
        Coordinate box = boxes.get(0);
        boxes.remove(0);
        return box;
    }

    public String toString() {
        String data = "";
        if (binLabel != null) {
            data = "Bin Label: " + binLabel.x + ", " + binLabel.y + "\n";
        }
        for (Coordinate box : boxes) {
            data = data + box.x + ", " + box.y + "\n";
        }
        return data;
    }

    public Map<Coordinate, Barcodes> getBoxesBarcodes() {
        return boxesBarcodes;
    }

    public String getError() {
        return error;
    }

    public void addError(String newError) {
        if (!error.equals("")) {
            error = error + "; ";
        }
        error = error + newError;
    }

    public int getLevel(){
        return level;
    }
}
