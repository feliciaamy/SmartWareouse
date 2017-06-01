package org.boofcv.android.localization;

/**
 * Created by Amy on 27/5/17.
 */

public class Dimension {
    // shape = RECTANGLE
    double left, top, right, bottom;
    // shape = CIRCLE
    int r;
    double x, y;
    // shape = LINE
    double start, end, center;

    int color;
    Orientation orientation;
    Shape shape;

    // Rectangle
    public Dimension(double left, double top, double right, double bottom, int color) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.color = color;
        this.shape = Shape.RECTANGLE;
    }

    // Circle
    public Dimension(double x, double y, int r, int color) {
        this.r = r;
        this.x = x;
        this.y = y;
        this.color = color;
        this.shape = Shape.CIRCLE;
    }

    // Line
    public Dimension(double start, double end, double center, Orientation orientation, int color) {
        this.start = start;
        this.end = end;
        this.center = center;
        this.color = color;
        this.orientation = orientation;
        this.shape = Shape.LINE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dimension dimension = (Dimension) o;

        if (this.shape != ((Dimension) o).shape) {
            return false;
        }
        double eps = 0;
        switch (this.shape) {
            case LINE:
                eps = Math.abs(this.start - this.end) * 0.2;
                if (this.orientation != ((Dimension) o).orientation) {
                    return false;
                }

                if (Math.abs(this.start - ((Dimension) o).start) < eps &&
                        Math.abs(this.end - ((Dimension) o).end) < eps &&
                        Math.abs(Math.abs(this.start - this.end) - Math.abs(((Dimension) o).start - ((Dimension) o).end)) < eps &&
                        Math.abs(this.center - ((Dimension) o).center) < eps) {
                    return true;
                } else {
                    return false;
                }
            case CIRCLE:
                if (Math.abs(this.x - ((Dimension) o).x) < r && (Math.abs(this.x - ((Dimension) o).x) < r)) {
                    return true;
                } else {
                    return false;
                }
            case RECTANGLE:
                eps = Math.sqrt(Math.pow((this.left - this.right), 2) + Math.pow((this.top - this.bottom), 2)) / 2;
                if (Math.sqrt(Math.pow((this.left - this.right), 2) + Math.pow((this.top - this.bottom), 2)) < eps) {
                    return true;
                } else
                    return false;
            default:
                return false;
        }
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(left);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(top);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(right);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(bottom);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + r;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(start);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(end);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(center);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + color;
        result = 31 * result + (orientation != null ? orientation.hashCode() : 0);
        result = 31 * result + (shape != null ? shape.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        switch (shape) {
            case LINE:
                return orientation + " Line: (" + start + ", " + end + "), " + center;
            case CIRCLE:
                return "Circle: " + "(" + x + ", " + y + "), radius = " + r;
            case RECTANGLE:

                return "Rectangle: " + "(" + left + ", " + top + "), (" + right + ", " + bottom + ")";
        }
        return "";
    }
}

enum Orientation {
    HORIZONTAL,
    VERTICAL
}