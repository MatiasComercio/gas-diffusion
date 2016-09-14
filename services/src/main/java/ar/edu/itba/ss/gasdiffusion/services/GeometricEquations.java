package ar.edu.itba.ss.gasdiffusion.services;

import ar.edu.itba.ss.gasdiffusion.models.Point;
import ar.edu.itba.ss.gasdiffusion.models.Wall;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public abstract class GeometricEquations {
    static double distanceBetween(final Point p1, final Point p2) {
        return sqrt(pow(p2.x() - p1.x(), 2) + pow(p2.y() - p1.y(), 2)) - p1.radio() - p2.radio();
    }

    /**
     * Calculate the time of collision between two particles.
     * @param p1 the first particle
     * @param p2 the second particle
     * @return Double.POSITIVE_INFINITY when d < 0 or deltaV * deltaR >= 0;
     *          a positive value in other case;
     */
    public static double collisionTime(final Point p1, final Point p2) {
        // Components of Delta v
        final double deltaVx = p2.vx() - p1.vx();
        final double deltaVy = p2.vy() - p1.vy();

        // Components of Delta r
        final double deltaRx = p2.x() - p1.x();
        final double deltaRy = p2.y() - p1.y();

        // deltaV * deltaR
        final double vr = deltaVx * deltaRx + deltaVy * deltaRy;

        if(vr >= 0) {
            return Double.POSITIVE_INFINITY;
        }

        final double vv = pow(deltaVx, 2) + pow(deltaVy, 2);
        final double rr = pow(deltaRx, 2) + pow(deltaRy, 2);
        /*
         * The following formula was taken from class lecture and not from the paper
         * The paper specifies that sigma equals the sums of its radios only when they collide.
         * Otherwise, it is calculated as:
         * rxi' = rxi + deltaT vxi,   ryi' = ryi + deltaT vyi
         * rxj' = rxj + deltaT vxj,   ryj' = ryj + deltaT vyj
         * Problem is that we don't have deltaT
         */
        final double sigma = p1.radio() + p2.radio();

        //final double d = pow(vr, 2) - pow(vv, 2) * (pow(rr, 2) - pow(σ,2));
        final double d = pow(vr, 2) - vv * (rr - pow(sigma,2));

        if(d < 0) {
            return Double.POSITIVE_INFINITY;
        }

        return -1 * (vr + sqrt(d)) / (vv);
    }

    /**
     * Calculates the time when it collides with a horizontal wall
     * @param point a given point
     * @param wall The direction of the wall
     * @param negativeBound the position of the wall in case velocity is negative
     * @param positiveBound the position of the wall in case velocity is positive
     * @return the time to reach the wall
     */
    public static double timeToHitWall(final Point point, final Wall wall,
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

    public static double timeToHitMiddleWall(final Point point, final double xPosition, final double wallHeight, final double opening) {
        double tc, newY, openingUpperLimit, openingLowerLimit;
        final double v = point.vx();
        final double r = point.x();

        if(v == 0) {
            return Double.POSITIVE_INFINITY;
        }

        /*
          The idea is:

          If vx < 0:
            calculate tc ONLY IF particle is on the right side (crash the middle wall from the right)
          If vx > 0
            calculate tc ONLY IF particle is on the left side (crash the middle wall from the left)
          if any of this conditions is match, return Double.POSITIVE_INFINITY to represent that, with the current
          system's conditions, it is not possible that the particle hits a middle wall.

          Supposing we got a non infinite tc, we proceed to check the new y position.
          Notice that we got the tc that matches the condition where the center of the particle/point is at distance
          R from the middle wall.

          For simplification purpose, we will consider the particle's MBB (Minimum Bounding Box) instead of the
          particle itself. This simplification is due to the fact that, if not, we should resolve a quadratic
          equation with the time as the unknown variable, and the complexity of this is out of our scope.
          For more information about how to get the exact distance of the particle to a certain figure, check
          the following link: http://www.learnopengl.com/#!In-Practice/2D-Game/Collisions/Collision-Detection

          OK, back on our argument, we should check if the newY +- the particle's radio will collide with a
          middle wall or it is able to pass across the opening. And that's exactly what we do.
          If collision is detected, the tc return is the previously found; if not, tc is set to POSITIVE_INFINITE, as
          there won't be any collision for this particle with any middle wall.

          With all this in mind, we go on to implement the above's logic.
         */

        // Check that if particle is travelling left, it's currently positioned to the right of the wall. (Same if its going left)
        if(v < 0) {
            if( (r-point.radio()) > xPosition){ // r+radio is used to make sure the particle isn't in contact with the wall
                tc = (xPosition + point.radio() - r) / v;
            } else{
                return Double.POSITIVE_INFINITY;
            }
        } else{
            if( (r+point.radio()) < xPosition){
                tc = (xPosition - point.radio() - r) / v;
            } else{
                return Double.POSITIVE_INFINITY;
            }
        }

        // check what happens at the new y position
        newY = point.y() + point.vy() * tc;
        openingUpperLimit = (wallHeight/2) + opening/2;
        openingLowerLimit = (wallHeight/2) - opening/2;

        if (     newY-point.radio() > openingLowerLimit
                && newY+point.radio() < openingUpperLimit){
            // If the MBB of the particle is travelling towards the opening, then there's no collision,
            // and Infinity is returned
            tc = Double.POSITIVE_INFINITY;
        }

        return tc;
    }


}
