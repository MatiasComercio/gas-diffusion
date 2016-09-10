package ar.edu.itba.ss.gasdiffusion.services;

import ar.edu.itba.ss.gasdiffusion.models.Point;
import ar.edu.itba.ss.gasdiffusion.models.Wall;

import java.util.HashSet;
import java.util.Set;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public abstract class GeometricEquations {
    public static double distanceBetween(final Point p1, final Point p2) {
        return sqrt(pow(p2.x() - p1.x(), 2) + pow(p2.y() - p1.y(), 2)) - p1.radio() - p2.radio();
    }

    /**
     * Calculate the time of collision between two particles.
     * @param p1 the first particle
     * @param p2 the second particle
     * @return Double.POSITIVE_INFINITY when d < 0 or deltaV * deltaR >= 0;
     *          a positive value in other case;
     */
    public static Double collisionTime(final Point p1, final Point p2) {
        // Components of Delta v
        final double deltaVx = p2.vx() - p1.vx();
        final double deltaVy = p2.vy() - p1.vy();

        // Components of Delta r
        final double deltaRx = p2.x() - p1.x();
        final double deltaRy = p2.y() - p1.y();

        final double vr = deltaVx * deltaRx + deltaVy * deltaRy;

        if(vr >= 0) {
            return Double.POSITIVE_INFINITY;
        }

        final double vv = pow(deltaVx, 2) + pow(deltaVy, 2);
        final double rr = pow(deltaRx, 2) + pow(deltaRy, 2);
        /**
         * The following formula was taken from class lecture and not from the paper
         * The paper specifies that sigma equals the sums of its radios only when they collide.
         * Otherwise, it is calculated as:
         * rxi' = rxi + deltaT vxi,   ryi' = ryi + deltaT vyi
         * rxj' = rxj + deltaT vxj,   ryj' = ryj + deltaT vyj
         * Problem is that we don't have deltaT
         */
        final double σ = p1.radio() + p2.radio();

        //final double d = pow(vr, 2) - pow(vv, 2) * (pow(rr, 2) - pow(σ,2));
        final double d = pow(vr, 2) - vv * (rr - pow(σ,2));

        if(d < 0) {
            return Double.POSITIVE_INFINITY;
        }

        double time = -1 * (vr + sqrt(d)) / (vv);

        return time;
    }

    /**
     * Calculates the time when it collides with a horizontal wall
     * @param point
     * @param wall
     * @param negativeBound
     * @param positiveBound
     * @return
     */
    public static Double timeToHitWall(final Point point, final Wall wall,
                                       final double negativeBound, final double positiveBound) {
        double v = 0.0;
        double r = 0.0;

        if(wall == Wall.HORIZONTAL) {
            v = point.vy();
            r = point.y();
        } else if(wall == Wall.VERTICAL) {
            v = point.vx();
            r = point.x();
        }

        if(v == 0) {
            return Double.POSITIVE_INFINITY;
        }
        if(v < 0) {
            return (negativeBound + point.radio() - r) / v;
        }
        return (positiveBound - point.radio() - r) / v;
    }

    public static Point movePoint(final Point point, final double time) {
        final double newX = point.x() + point.vx() * time;
        final double newY = point.y() + point.vy() * time;

        return point.withX(newX).withY(newY);
    }


}
