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

//        // Components of delta v
//        final double deltaVx = p2.vx() - p1.vx();
//        final double deltaVy = p2.vy() - p1.vy();
//
//        // Components of delta r
//        final double deltaRx = p2.x() - p1.x();
//        final double deltaRy = p2.y() - p1.y();
//        // Scalar product of delta v and delta r
//        final double vr = deltaVx * deltaRx + deltaVy * deltaRy;
//
//        final double radioSum = p1.radio() + p2.radio();
//
//        double J = (2.0 * p1.mass() * p2.mass() * vr) / (radioSum * (p1.mass() + p2.mass()));
//
//        final double Jx = J * deltaRx / radioSum;
//        final double Jy = J * deltaRy / radioSum;
//
//        // Newton's second law
//        final double nextVx1 = p1.vx() + Jx / p1.mass();
//        final double nextVy1 = p1.vy() + Jy / p1.mass();
//
//        final double nextVx2 = p2.vx() - Jx / p2.mass();
//        final double nextVy2 = p2.vy() - Jy / p2.mass();
//
//        final Point nextPoint1 = p1.movePoint(time).withVx(nextVx1).withVy(nextVy1);
//        final Point nextPoint2 = p2.movePoint(time).withVx(nextVx2).withVy(nextVy2);


        // New Method for velocity calculation

        //////////////////////
        final double newVelX1 = (p1.vx() * (p1.mass() - p2.mass()) + (2 * p2.mass() * p2.vx())) / (p1.mass() + p2.mass());
        final double newVelY1 = (p1.vy() * (p1.mass() - p2.mass()) + (2 * p2.mass() * p2.vy())) / (p1.mass() + p2.mass());
        final double newVelX2 = (p2.vx() * (p2.mass() - p1.mass()) + (2 * p1.mass() * p1.vx())) / (p1.mass() + p2.mass());
        final double newVelY2 = (p2.vy() * (p2.mass() - p1.mass()) + (2 * p1.mass() * p1.vy())) / (p1.mass() + p2.mass());

        final Point newPoint1 = p1.movePoint(time).withVx(newVelX1).withVy(newVelY1);
        final Point newPoint2 = p2.movePoint(time).withVx(newVelX2).withVy(newVelY2);
        /////////////////////

        newPoints.add(newPoint1);
        newPoints.add(newPoint2);

        return newPoints;
    }
}
