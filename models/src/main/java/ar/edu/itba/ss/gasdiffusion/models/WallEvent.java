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
        Point newPoint = null;

        if(wall == Wall.VERTICAL) {
            newPoint = point.withVx(-1 * point.vx());
        } else if (wall == Wall.HORIZONTAL) {
            newPoint = point.withVy(-1 * point.vy());
        }

        points.add(
                newPoint
        );

        return points;
    }
}
