package ar.edu.itba.ss.gasdiffusion.services;

import ar.edu.itba.ss.gasdiffusion.models.Point;
import ar.edu.itba.ss.gasdiffusion.models.Wall;

import java.util.Set;

public abstract class StateEquations {
    private static final double BOLTZMANN_CONSTANT = 1.3806503e-23;

    /**
     * TODO: check if this equation is valid to use in this system
     * Calculate the current temperature of a bounded system based on the equipartition theorem.
     * @param points the set of points that are part of the system
     * @param spatialDimension the number of spatial dimensions
     * @return the temperature
     */
    public static double temperature(final Set<Point> points, final int spatialDimension) {
        final double N = points.size();
        double sum = 0;

        for (Point point: points) {
            sum += point.mass() * Math.pow(point.speed(), 2);
        }

        /**
         * the factor N − 1 takes into
         * account that the total momentum of the system is a conserved quantity,
         * which reduces the number of degrees of freedom per particle by
         * the number of spatial dimensions).
         */
//        return sum / (BOLTZMANN_CONSTANT * spatialDimension * (N - 1));

        return BOLTZMANN_CONSTANT * sum;
    }

    public static double pressure(final Point point, final Wall wall, final double tc) {
        return 2 * point.mass() * point.speed() / (tc * wall.getLength());
    }
}
