package org.smartwarehouse.localization;

import com.scandit.recognition.Barcode;

import org.opencv.core.Core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Amy on 14/6/17.
 */

public class Bin {
    private Coordinate binLabel;
    private List<Coordinate> boxes;
    private Map<Coordinate, Barcodes> boxesBarcodes = new HashMap<>();
    private Barcodes binLabelBarcode;

    private double binHeight;
    private double occupancyLevel = 0;

    private int counter = 0;

    public Bin(Coordinate binLabel, List<Coordinate> boxes, double binHeight) {
        this.binLabel = binLabel;
        this.boxes = boxes;
        this.binHeight = binHeight;
    }

    public double getOccupancyLevel() {
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
        String data = "Bin Label: " + binLabel.x + ", " + binLabel.y + "\n";
        for (Coordinate box : boxes) {
            data = data + box.x + ", " + box.y + "\n";
        }
        return data;
    }

    public Map<Coordinate, Barcodes> getBoxesBarcodes(){
        return boxesBarcodes;
    }
}
