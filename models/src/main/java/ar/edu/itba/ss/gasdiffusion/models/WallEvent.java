package ar.edu.itba.ss.gasdiffusion.models;

public class WallEvent extends Event {
    private final Point point;
    private Wall wall;

    public WallEvent(final double time, final Point point, final Wall wall) {
        super(time);
        this.point = point;
        this.wall = wall;
    }

    @Override
    public void execute() {
        /**
         * TODO: The idea is that from the main loop it executes the minimum time priority event (wall collision or
         * between two particles) and the loop does not need to know which type of event is it. The only issue is that
         * we have to create a new point each time and there is no way of returning 1 or 2 points in the way the
         * geometricalEquations are made
         */
    }
}
