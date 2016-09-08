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

        // Components of delta v
        final double deltaVx = p2.vx() - p1.vx();
        final double deltaVy = p2.vy() - p1.vy();

        // Components of delta r
        final double deltaRx = p2.x() - p1.x();
        final double deltaRy = p2.y() - p1.y();
        // Scalar product of delta v and delta r
        final double vr = deltaVx * deltaRx + deltaVy * deltaRy;

        final double radioSum = p1.radio() + p2.radio();

        double J = (2.0 * p1.mass() * p2.mass() * vr) / (radioSum * (p1.mass() + p2.mass()));

        final double Jx = J * deltaRx / radioSum;
        final double Jy = J * deltaRy / radioSum;

        // Newton's second law
        final double nextVx1 = p1.vx() + Jx / p1.mass();
        final double nextVy1 = p1.vy() + Jy / p1.mass();

        final double nextVx2 = p2.vx() - Jx / p2.mass();
        final double nextVy2 = p2.vy() - Jy / p2.mass();

        final Point nextPoint1 = p1.withVx(nextVx1).withVy(nextVy1);
        final Point nextPoint2 = p2.withVx(nextVx2).withVy(nextVy2);

        newPoints.add(nextPoint1);
        newPoints.add(nextPoint2);

        return newPoints;
    }
}
