package org.smartwarehouse.object;

/**
 * Created by Amy on 14/7/17.
 */

public class Label implements Dimension {
    private double left, top, right, bottom;
    private int color;

    public Label(double left, double top, double right, double bottom, int color) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.color = color;
    }

    public double getLeft() {
        return left;
    }

    public double getTop() {
        return top;
    }

    public double getRight() {
        return right;
    }

    public double getBottom() {
        return bottom;
    }

    public int getColor() {
        return color;
    }

    public boolean isInside(Object o) {
        Label rect = (Label) o;
        int score = 0;
        if (rect.getArea() < this.getArea()) {
            if (rect.right < this.right) {
                score++;
            }
            if (rect.left > this.left) {
                score++;
            }
            if (rect.top > this.top) {
                score++;
            }
            if (rect.bottom < this.bottom) {
                score++;
            }

            if (score >= 2) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Label rect = (Label) o;
        double eps = Math.sqrt(Math.pow((this.left - this.right), 2) + Math.pow((this.top - this.bottom), 2)) / 2;
        if (Math.abs(rect.bottom - this.bottom) < eps && Math.abs(rect.top - this.top) < eps &&
                Math.abs(rect.left - this.left) < eps && Math.abs(rect.right - this.right) < eps) {
            return true;
        } else
            return false;
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
        result = 31 * result + color;
        return result;
    }

    @Override
    public String toString() {
        return "Label: " + "(" + left + ", " + top + "), (" + right + ", " + bottom + ")";
    }

    @Override
    public double getArea() {
        return (Math.abs((left - right) * (bottom - top)));
    }

    @Override
    public Point getCentroid() {
        return new Point((left + right) / 2, (top + bottom) / 2);
    }
}
