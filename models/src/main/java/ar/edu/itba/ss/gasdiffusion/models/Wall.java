package ar.edu.itba.ss.gasdiffusion.models;

public enum Wall {
    HORIZONTAL,
    VERTICAL;

    private double length;

    public void setLength(final double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }
}
