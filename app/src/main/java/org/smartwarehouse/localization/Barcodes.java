package org.smartwarehouse.localization;

/**
 * Created by Amy on 2/6/17.
 */

class Barcodes {
    // Box
    private String T = "";
    private String P = "";
    private String D9 = "";
    private String Q = "";
    private String X = "";

    // Bin label
    private String barcode = "";

    private Type type;

    public Barcodes(Type type) {
        this.type = type;
    }

    public void setT(String T) {
        if (type != Type.BOX) {
            return;
        }
        this.T = T;
    }

    public void setP(String P) {
        if (type != Type.BOX) {
            return;
        }

        this.P = P;
    }

    public void setD9(String D9) {
        if (type != Type.BOX) {
            return;
        }
        this.D9 = D9;
    }

    public void setQ(String Q) {
        if (type != Type.BOX) {
            return;
        }
        this.Q = Q;
    }

    public void setX(String X) {
        if (type != Type.BOX) {
            return;
        }
        this.X = X;
    }

    public String getT() {
        return T;
    }

    public String getP() {
        return P;
    }

    public String getD9() {
        return D9;
    }

    public String getQ() {
        return Q;
    }

    public String getX() {
        return X;
    }

    public String getBarcode() {
        return barcode;
    }

    public Type getType() {
        return type;
    }


    public void setBarcode(String barcode) {
        if (type != Type.BINLABEL) {
            return;
        }
        this.barcode = barcode;
    }

    public String toString() {
        if (this.type == Type.BINLABEL) {
            return barcode;
        } else {
            return X + "; " + T + "; " + D9 + "; " + Q + "; " + P;
        }
    }
}
