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
    private double fraction;


    public GasDiffusion(final double L, final double W, final double opening) {
        //pq = new PriorityQueue<>();
        currentTime = 0;
        this.L = L;
        this.W = W;
        this.opening = opening;
        this.fraction = 0;
    }

    public Set<Point> run(Set<Point> points) {

        Set<Point> updatedParticles = new HashSet<>();
        Event currentEvent, minEvent = null;
        fraction = 0;
        currentTime = 0;

        // Calculate the closest event
        for(Point point : points){
            currentEvent = predictCollisions(point, points);
            if(minEvent == null || currentEvent.compareTo(minEvent) < 0 ){
                minEvent = currentEvent;
            }
        }

        // Update the position of all particles (Including the ones that collided)
        for(Point point : points){
            updatedParticles.add(GeometricEquations.movePoint(point, minEvent.getTime()));
            if(point.x() <= this.W/2){ // Calculates the fraction before moving particles
                fraction++;
            }
        }

        // Remove the particles that collided, and add them again with updated velocity
        Set<Point> collisionParticles = minEvent.execute();
        updatedParticles.removeAll(collisionParticles); // This is done, because the set does not replace elements
        for(Point  point: collisionParticles){
            updatedParticles.add(GeometricEquations.movePoint(point, minEvent.getTime()));
        }

        fraction /= points.size();
        currentTime = minEvent.getTime();

        return updatedParticles;

    }

    /**
     * Predict the next events (collisions) and add them to a priority queue
     * @param point
     * @param points
     */
    public Event predictCollisions(final Point point, final Set<Point> points) {
        Event pointEvent = null, hWallEvent, vWallEvent, minEvent;
        double tc;

        /**
         * Improve by receiving an array and iterate from the i+1 point to avoid having twice the events in the
         * priority queue
         */
        // Calculate the collision between the given point and all the remaining points
        for(Point currentPoint : points) {
            if(!point.equals(currentPoint)) {
                tc = GeometricEquations.collisionTime(point, currentPoint);
                if(pointEvent == null || tc < pointEvent.getTime()){
                    pointEvent = new PointsEvent(tc, point, currentPoint);
                }
            }
        }

        minEvent = pointEvent;

        // Calculate the collision between the given point and one of the horizontal walls
        tc = GeometricEquations.timeToHitWall(point, Wall.HORIZONTAL, 0, L);
        hWallEvent = new WallEvent(tc, point, Wall.HORIZONTAL);

        if(minEvent == null || hWallEvent.getTime() < minEvent.getTime()){
            minEvent = hWallEvent;
        }

        // Calculate the collision between the given point and one of the vertical walls
        tc = GeometricEquations.timeToHitWall(point, Wall.VERTICAL, 0, W);
        vWallEvent = new WallEvent(tc, point, Wall.VERTICAL);

        if(minEvent == null || vWallEvent.getTime() < minEvent.getTime()){
            minEvent = hWallEvent;
        }

        //TODO: Check collisions with wall in the middle

        //TODO: Calculate Temperature and Pressure

        return minEvent;
    }

    public double getFraction() {
        return fraction;
    }

    public double getCurrentTime() {
        return currentTime;
    }
}
