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

    public Dimension(double left, double top, double right, double bottom, int color) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.color = color;
        this.shape = Shape.RECTANGLE;
    }

    public Dimension(double x, double y, int r, int color) {
        this.r = r;
        this.shape = Shape.CIRCLE;
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public Dimension(double start, double end, double center, Orientation orientation, int color) {
        this.start = start;
        this.end = end;
        this.center = center;
        this.color = color;
        this.orientation = orientation;
        this.shape = Shape.LINE;
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
