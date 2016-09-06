package ar.edu.itba.ss.gasdiffusion.models;

public class PointsEvent extends Event {
    private final Point p1, p2;

    public PointsEvent(final double time, final Point p1, final Point p2) {
        super(time);
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public void execute() {

    }

    // TODO: Not sure if something else should go here
}
