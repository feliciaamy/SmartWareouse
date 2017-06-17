package org.smartwarehouse.localization;

import java.text.DecimalFormat;

/**
 * Created by Amy on 2/6/17.
 */

public class Coordinate {
    Type type;
    double x;
    double y;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coordinate that = (Coordinate) o;

        if (Double.compare(that.x, x) != 0) return false;
        return Double.compare(that.y, y) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public Coordinate(Type type, double x, double y) {
        this.type = type;
        this.x = x;

        this.y = y;
    }

    public String toString() {
        DecimalFormat df = new DecimalFormat("#.00");
        return type + ": " + df.format(this.x) + "," + df.format(this.y);
    }
}
