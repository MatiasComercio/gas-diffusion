package ar.edu.itba.ss.gasdiffusion.models;

public enum Wall {
    HORIZONTAL,
    VERTICAL;

    private double length;
    private double pressure = 0.0;

    public void setLength(final double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }

    /**
     * Add pressure to the wall
     * @param pressure the pressure to be added to the wall
     */
    public void increasePressure(final double pressure) {
        this.pressure += pressure;
    }
}
