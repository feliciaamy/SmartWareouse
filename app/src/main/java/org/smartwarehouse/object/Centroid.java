package org.smartwarehouse.object;

/**
 * Created by Amy on 14/7/17.
 */

public class Centroid implements Dimension {
    private int r;
    private double x, y;
    private int color;

    public Centroid(double x, double y, int r, int color) {
        this.r = r;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public double getArea() {
        return (33 / 7 * Math.pow(r, 2));
    }

    @Override
    public Point getCentroid() {
        return new Point(x,y);
    }

    public int getR() {
        return r;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = r;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + color;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Centroid dimension = (Centroid) o;

        if (Math.abs(this.x - ((Centroid) o).x) < r && (Math.abs(this.x - ((Centroid) o).x) < r)) {
            return true;
        } else {
            return false;
        }

    }
}
