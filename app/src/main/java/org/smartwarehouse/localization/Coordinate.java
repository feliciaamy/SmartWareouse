package org.smartwarehouse.localization;

import java.text.DecimalFormat;

/**
 * Created by Amy on 2/6/17.
 */

public class Coordinate {
    Type type;
    double x;
    double y;

    public Coordinate(Type type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String toString() {
        DecimalFormat df = new DecimalFormat("#.00");
        return this.type + ":" + df.format(this.x) + "," + df.format(this.y);
    }
}

enum Type {
    BINLABEL,
    BOX
}