package ar.edu.itba.ss.gasdiffusion.models;

import java.util.HashSet;
import java.util.Set;

public class WallEvent extends Event {
    private final Point point;
    private Wall wall;

    public WallEvent(final double time, final Point point, final Wall wall) {
        super(time);
        this.point = point;
        this.wall = wall;
    }

    @Override
    public Set<Point> execute() {
        final Set<Point> points = new HashSet<>();

        if(wall == Wall.VERTICAL) {
            points.add(point.updatePoint(time, -1 * point.vx(), point.vy()));
        } else if (wall == Wall.HORIZONTAL) {
            points.add(point.updatePoint(time, point.vx(), -1 * point.vy()));
        }

        return points;
    }
}
