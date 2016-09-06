package ar.edu.itba.ss.gasdiffusion.services;

import ar.edu.itba.ss.gasdiffusion.models.Point;
import ar.edu.itba.ss.gasdiffusion.models.Wall;

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
        // Components of velocity of Point 1
        final double vx1 = p1.speed() * Math.cos(p1.orientation());
        final double vy1 = p1.speed() * Math.sin(p1.orientation());

        // Components of velocity of Point 2
        final double vx2 = p2.speed() * Math.cos(p2.orientation());
        final double vy2 = p2.speed() * Math.sin(p2.orientation());

        // Components of Delta v
        final double Δvx = vx2 - vx1;
        final double Δvy = vy2 - vy1;

        // Components of Delta r
        final double Δrx = p2.x() - p1.x();
        final double Δry = p2.y() - p1.y();

        final double vr = Δvx * Δrx + Δvy * Δry;

        if(vr >= 0) {
            return Double.POSITIVE_INFINITY;
        }

        final double vv = pow(Δvx, 2) + pow(Δvy, 2);
        final double rr = pow(Δrx, 2) + pow(Δry, 2);
        /**
         * The following formula was taken from class lecture and not from the paper
         * The paper specifies that sigma equals the sums of its radios only when they collide.
         * Otherwise, it is calculated as:
         * rxi' = rxi + Δt vxi,   ryi' = ryi + Δt vyi
         * rxj' = rxj + Δt vxj,   ryj' = ryj + Δt vyj
         * Problem is that we don't have Δt
         */
        final double σ = p1.radio() + p2.radio();

        final double d = pow(vr, 2) - pow(vv, 2) * (pow(rr, 2) - pow(σ,2));

        if(d < 0) {
            return Double.POSITIVE_INFINITY;
        }

        return -1 * (vr + sqrt(d)) / (vv);
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
            v = point.speed() * Math.sin(point.orientation());
            r = point.y();
        } else if(wall == Wall.VERTICAL) {
            v = point.speed() * Math.cos(point.orientation());
            r = point.x();
        }

        if(v == 0) {
            return Double.POSITIVE_INFINITY;
        }
        if(v < 0) {
            return (negativeBound + point.radio() - r) / -v;
        }
        return (positiveBound - point.radio() - r) / v;
    }

    /**
     * Calculate the new particles configuration based on an elastic collision
     * @param p1
     * @param p2
     * @return an array of two points, containing the points with the updated velocities (orientation)
     */
    public static Point[] solveCollision(final Point p1, final Point p2) {
        // Components of velocity of Point 1
        final double vx1 = p1.speed() * Math.cos(p1.orientation());
        final double vy1 = p1.speed() * Math.sin(p1.orientation());

        // Components of velocity of Point 2
        final double vx2 = p2.speed() * Math.cos(p2.orientation());
        final double vy2 = p2.speed() * Math.sin(p2.orientation());

        // Components of Δv
        final double Δvx = vx2 - vx1;
        final double Δvy = vy2 - vy1;

        // Components of Δr
        final double Δrx = p2.x() - p1.x();
        final double Δry = p2.y() - p1.y();
        // Scalar product of Δv and Δr
        final double vr = Δvx * Δrx + Δvy * Δry;

        final double σ = p1.radio() + p2.radio();

        final double J = (2 * p1.mass() * p2.mass() * vr) / (σ * (p1.mass() + p2.mass()));
        final double Jx = J * Δrx / σ;
        final double Jy = J * Δry / σ;

        // Newton's second law
        final double nextVx1 = vx1 + Jx / p1.mass();
        final double nextVy1 = vy1 + Jy / p1.mass();

        // TODO: I can calculate the next speed components for p2, the problem is that i can't return two new points
        final double nextVx2 = vx2 - Jx / p2.mass();
        final double nextVy2 = vy2 - Jy / p2.mass();

        final Point nextPoint1 = p1.withOrientation(
                Math.acos(nextVx1 / p1.speed())
        );

        final Point nextPoint2 = p2.withOrientation(
                Math.acos(nextVx2 / p2.speed())
        );

        return new Point[]{nextPoint1, nextPoint2};
    }

    public static Point movePoint(final Point point, final double time) {
        final double newX = point.x() + point.speed() * time;
        final double newY = point.y() + point.speed() * time;

        return point.withX(newX).withY(newY);
    }
}
