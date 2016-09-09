package ar.edu.itba.ss.gasdiffusion.services;

import ar.edu.itba.ss.gasdiffusion.models.Point;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PointFactory {
    private static PointFactory pointFactory;

    private PointFactory() {
    }

    public static PointFactory getInstance() {
        if (pointFactory == null) {
            pointFactory = new PointFactory();
        }
        return pointFactory;
    }


    public Set<Point> randomPoints(final Point leftBottomPoint,
                                   final Point rightTopPoint,
                                   final double[] radios,
                                   final boolean canCollide,
                                   final int maxTries,
                                   final double speed,
                                   final double mass) {
        final double minX, minY, maxX, maxY;
        if (leftBottomPoint != null) {
            minX = leftBottomPoint.x();
            minY = leftBottomPoint.y();
        } else {
            minX = minY = Double.MIN_VALUE;
        }

        if (rightTopPoint != null) {
            maxX = rightTopPoint.x();
            maxY = rightTopPoint.y();
        } else {
            maxX = maxY = Double.MAX_VALUE;
        }

        final int amount = radios.length;

        final Set<Point> generatedPoints = new HashSet<>(amount);

        int tries;
        double pX, pY, pR, pOrientation;

        for (int i = 0 ; i < amount ; i++) {
            Point p;
            tries = 0;
            do {
                pX = RandomInRange.randomDouble(minX, maxX);
                pY = RandomInRange.randomDouble(minY, maxY);
                pR = radios[i] <= -1 ? 0 : radios[i];
                pOrientation = RandomInRange.randomDouble(0, 2 * Math.PI);

                p = Point.builder(pX, pY)
                        .mass(mass)
                        .radio(pR)
                        .vx(speed * Math.cos(pOrientation))
                        .vy(speed * Math.sin(pOrientation))
                        .build();

                tries ++;
                if (tries > maxTries) {
                    return generatedPoints;
                }
            } while (!passCollisionCondition(generatedPoints, p, canCollide));

            // for sure that the point is not at the set; if it were, it would have collied with itself
            generatedPoints.add(p);
        }

        return generatedPoints;
    }

    /**
     * Generates in a pseudo-aleatory manner, but based on the given parameters,
     * the specified amount of points.
     * Collisions are accepted or not depending the given parameter.
     *
     * @param leftBottomPoint the point at that corner of the area to where the points must belong ; null if random
     * @param rightTopPoint the point at that corner of the area to where the points must belong ; null if random
     * @param radio the point's radio ; < 0 if random
     *
     * @param amount the amount of points to be generated - if possible
     * @param canCollide whether the points can collide or not
     * @param maxTries how many times the function will try to generate non-colliding points - consecutively.
     *                 If this limit is reached, the set as is at that moment is returned
     * @param speed points' speed
     * @return a set containing the generated points - could have less than amount points
     */
    public Set<Point> randomPoints(final Point leftBottomPoint,
                                   final Point rightTopPoint,
                                   final double radio,
                                   final int amount,
                                   final boolean canCollide,
                                   final int maxTries,
                                   final double speed,
                                   final double mass) {
        double[] radios = new double[amount];
        Arrays.fill(radios, radio);
        return randomPoints(leftBottomPoint, rightTopPoint, radios, canCollide, maxTries, speed, mass);
    }

    /**
     * Generates in a pseudo-aleatory manner the given amount of points.
     * Collisions are accepted or not depending the given parameter.
     *
     * @param amount the amount of points to be generated - if possible
     * @param canCollide whether the points can collide or not
     * @param maxTries how many times the function will try to generate non-colliding points - consecutively.
     *                 If this limit is reached, the set as is at that moment is returned
     * @return a set containing the generated points - could have less than amount points
     */
    public Set<Point> randomPoints(final int amount, final boolean canCollide, final int maxTries, final double speed,
                                   final double mass) {
        return randomPoints(null, null, -1, amount, canCollide, maxTries, speed, mass);
    }

    /**
     * Checks that the collision condition is passed, comparing the just created point with all the
     * previous already obtained
     * @param generatedPoints -
     * @param p -
     * @param canCollide -
     * @return -
     */
    private boolean passCollisionCondition(final Set<Point> generatedPoints,
                                           final Point p, final boolean canCollide) {
        if (canCollide) {
            return true;
        }

        for (Point each : generatedPoints) {
            if (GeometricEquations.distanceBetween(each, p) < 0) {
                return false;
            }
        }

        return true;
    }
}

