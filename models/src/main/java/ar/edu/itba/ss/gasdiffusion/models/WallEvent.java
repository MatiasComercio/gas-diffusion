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
        double orientation = 0.0;

        if(wall == Wall.VERTICAL) {
            //orientation = Math.acos(-1 * point.vx() / point.speed());
            orientation = Math.atan( ((-1) * point.vy()) / point.vx());
        } else if (wall == Wall.HORIZONTAL) {
            //orientation = Math.asin(-1 * point.vy() / point.speed());
            orientation = Math.atan(point.vy() / ( (-1) * point.vx()));
        }

        points.add(
                //point.withSpeed(orientation)
                point.withOrientation(orientation)
        );

        return points;
    }
}
