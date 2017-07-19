package org.smartwarehouse.object;


/**
 * Created by Amy on 14/7/17.
 */

public class Boundary implements Dimension {
    private int color;
    private double start, end, center;
    private Orientation orientation;

    public Boundary(double start, double end, double center, Orientation orientation, int color) {
        this.start = start;
        this.end = end;
        this.center = center;
        this.color = color;
        this.orientation = orientation;
    }

    @Override
    public int getColor() {
        return color;
    }

    @Override
    public double getArea() {
        return 0;
    }

    @Override
    public Point getCentroid() {
        return null;
    }

    @Override
    public String toString() {
        return orientation + " Boundary: (" + start + ", " + end + "), " + center;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = color;
        temp = Double.doubleToLongBits(start);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(end);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(center);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + orientation.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Boundary dimension = (Boundary) o;

        double eps = Math.abs(this.start - this.end) * 0.2;
        if (this.orientation != ((Boundary) o).orientation) {
            return false;
        }

        if (Math.abs(this.start - ((Boundary) o).start) < eps && Math.abs(this.end - ((Boundary) o).end) < eps) {
            if (Math.abs(Math.abs(this.start - this.end) - Math.abs(((Boundary) o).start - ((Boundary) o).end)) < eps) {
                if (Math.abs(this.center - ((Boundary) o).center) < eps) {
                    return true;
                }
            }
        }
        return false;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public double getCenter() {
        return center;
    }
}

