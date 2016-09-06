package ar.edu.itba.ss.gasdiffusion.core;

import ar.edu.itba.ss.gasdiffusion.models.*;
import ar.edu.itba.ss.gasdiffusion.services.GeometricEquations;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class GasDiffusion {
    private final Queue<Event> pq;
    private double currentTime;
    private final double L;
    private final double W;
    private final double opening;

    public GasDiffusion(final double L, final double W, final double opening) {
        pq = new PriorityQueue<>();
        currentTime = 0;
        this.L = L;
        this.W = W;
        this.opening = opening;
    }

    /**
     * Predict the next events (collisions) and add them to a priority queue
     * @param point
     * @param points
     */
    public void predictCollisions(final Point point, final Set<Point> points) {
        double tc;

        /**
         * Improve by receiving an array and iterate from the i+1 point to avoid having twice the events in the
         * priority queue
         */
        for(Point currentPoint : points) {
            if(!point.equals(currentPoint)) {
                tc = GeometricEquations.collisionTime(point, currentPoint);
                pq.offer(new PointsEvent(tc, point, currentPoint));
            }
        }

        // Insert collision event with one of the horizontal walls
        tc = GeometricEquations.timeToHitWall(point, Wall.HORIZONTAL, 0, L);
        pq.offer(new WallEvent(tc, point, Wall.HORIZONTAL));

        // Insert collision event with one of the vertical walls
        tc = GeometricEquations.timeToHitWall(point, Wall.VERTICAL, 0, W);
        pq.offer(new WallEvent(tc, point, Wall.VERTICAL));

        //TODO: Check collisions with wall in the middle
    }
}
