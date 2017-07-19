package org.smartwarehouse.object;

import android.graphics.Color;

import org.smartwarehouse.localization.Coordinate;

/**
 * Created by Amy on 14/7/17.
 */

public interface Dimension {

    public int getColor();
    public double getArea();
    public Point getCentroid();
}
