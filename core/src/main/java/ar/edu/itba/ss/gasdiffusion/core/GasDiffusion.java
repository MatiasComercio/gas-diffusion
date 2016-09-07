package ar.edu.itba.ss.gasdiffusion.core;

import ar.edu.itba.ss.gasdiffusion.models.*;
import ar.edu.itba.ss.gasdiffusion.services.GeometricEquations;

import java.util.HashSet;
import java.util.Set;

public class GasDiffusion {
    //private final Queue<Event> pq;
    private double currentTime;
    private final double L;
    private final double W;
    private final double opening;

    public GasDiffusion(final double L, final double W, final double opening) {
        //pq = new PriorityQueue<>();
        currentTime = 0;
        this.L = L;
        this.W = W;
        this.opening = opening;
    }

    public Set<Point> run(Set<Point> points) {

        Set<Point> updatedParticles = new HashSet<>();
        Event currentEvent, minEvent = null;
        for(Point point : points){
            currentEvent = predictCollisions(point, points);
            if(minEvent == null || currentEvent.compareTo(minEvent) < 0 ){
                minEvent = currentEvent;
            }
        }

        ///
        Set<Point> collisionParticles = minEvent.execute();
        //updatedParticles.addAll(collisionParticles);
        ///

        for(Point point : points){
            if(!collisionParticles.contains(point)) {
                updatedParticles.add(GeometricEquations.movePoint(point, minEvent.getTime()));
            }
        }
        return updatedParticles;

    }

    /**
     * Predict the next events (collisions) and add them to a priority queue
     * @param point
     * @param points
     */
    public Event predictCollisions(final Point point, final Set<Point> points) {
        Event pointEvent = null, hWallEvent, vWallEvent, nextEvent;
        double tc, tcmin = Double.MAX_VALUE;

        /**
         * Improve by receiving an array and iterate from the i+1 point to avoid having twice the events in the
         * priority queue
         */
        for(Point currentPoint : points) {
            if(!point.equals(currentPoint)) {
                tc = GeometricEquations.collisionTime(point, currentPoint);
                if(pointEvent == null || tc < pointEvent.getTime()){
                    pointEvent = new PointsEvent(tc, point, currentPoint);
                }
            }
        }

        nextEvent = pointEvent;

        // Insert collision event with one of the horizontal walls
        tc = GeometricEquations.timeToHitWall(point, Wall.HORIZONTAL, 0, L);
        hWallEvent = new WallEvent(tc, point, Wall.HORIZONTAL);

        if(nextEvent == null || hWallEvent.getTime() < nextEvent.getTime()){
            nextEvent = hWallEvent;
        }

        // Insert collision event with one of the vertical walls
        tc = GeometricEquations.timeToHitWall(point, Wall.VERTICAL, 0, W);
        vWallEvent = new WallEvent(tc, point, Wall.VERTICAL);

        if(nextEvent == null || vWallEvent.getTime() < nextEvent.getTime()){
            nextEvent = hWallEvent;
        }

        //TODO: Check collisions with wall in the middle

        return nextEvent;
    }

}
