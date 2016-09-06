package ar.edu.itba.ss.gasdiffusion.models;

public class WallEvent extends Event {
    private final Point point;
    private Wall wall;

    public WallEvent(final double time, final Point point, final Wall wall) {
        super(time);
        this.point = point;
        this.wall = wall;
    }
}
