package ar.edu.itba.ss.gasdiffusion.models;

import java.util.HashSet;
import java.util.Set;

public class PointsEvent extends Event {
    private final Point p1, p2;

    public PointsEvent(final double time, final Point p1, final Point p2) {
        super(time);
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * Calculate the new particles configuration based on an elastic collision
     * @return a set of two points, containing the points with the updated velocities (orientation)
     */
    @Override
    public Set<Point> execute() {
        final Set<Point> newPoints = new HashSet<>();

        // New Method for velocity calculation.
        // See: http://gamedevelopment.tutsplus.com/tutorials/when-worlds-collide-simulating-circle-circle-collisions--gamedev-769

        final double newVelX1 = (p1.vx() * (p1.mass() - p2.mass()) + (2 * p2.mass() * p2.vx())) / (p1.mass() + p2.mass());
        final double newVelY1 = (p1.vy() * (p1.mass() - p2.mass()) + (2 * p2.mass() * p2.vy())) / (p1.mass() + p2.mass());
        final double newVelX2 = (p2.vx() * (p2.mass() - p1.mass()) + (2 * p1.mass() * p1.vx())) / (p1.mass() + p2.mass());
        final double newVelY2 = (p2.vy() * (p2.mass() - p1.mass()) + (2 * p1.mass() * p1.vy())) / (p1.mass() + p2.mass());

        newPoints.add(p1.updatePoint(time, newVelX1, newVelY1));
        newPoints.add(p2.updatePoint(time, newVelX2, newVelY2));

        return newPoints;
    }

    @Override
    public double getPressure() {
        return 0; // no pressure at a particle's collision
    }
}
