package ar.edu.itba.ss.gasdiffusion.core;

import ar.edu.itba.ss.gasdiffusion.models.*;
import ar.edu.itba.ss.gasdiffusion.services.GeometricEquations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class GasDiffusion {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final double L;
    private final double W;
    private final double opening;
    private double fraction;
    private double collisionTime;


    GasDiffusion(final double L, final double W, final double opening) {
        this.L = L;
        this.W = W;
        this.opening = opening;
        this.fraction = 0;
        collisionTime = 0;
    }

    List<Point> run(final List<Point> points) {
        final List<Point> updatedParticles = new ArrayList<>();
        final List<Event> minEvent;

        fraction = 0;
        collisionTime = 0; //TODO: This should not be reset

        minEvent = predictCollisions(points);

        // If there existed at least a collision and that collision has the min. tc
        if (!minEvent.isEmpty()) {
            // Update the position of all particles (Including the ones that collided)
            for(final Point point : points) {
                updatedParticles.add(GeometricEquations.movePoint(point, minEvent.get(0).getTime()));
                if(point.x() <= this.W/2){ // Calculates the fraction before moving particles
                    fraction++;
                }
            }

            Set<Point> collisionParticles;
            for(Event event : minEvent){
                collisionParticles = event.execute();

                // Remove the particles that take part in an event, and add them again with updated velocity and position
                updatedParticles.removeAll(collisionParticles);
                updatedParticles.addAll(collisionParticles);

            }

            fraction /= points.size();
            collisionTime = minEvent.get(0).getTime();
        } else {
            LOGGER.debug("There does not exist a collision. Skipping particle update...");
        }

        return updatedParticles;

    }

    /**
     * Predict the next events (collisions)
     * @param points the collection of points to be checked against the given point
     */
    private List<Event> predictCollisions(final List<Point> points) {
        List<Event> eventList = new ArrayList<>();
        Event minEvent = null, hWallEvent, vWallEvent;
        double tc;

        for(int i = 0; i < points.size(); i++) {


            minEvent = null;
            final Point point = points.get(i);

            // Calculate the closest collision between the current particle and all the others
            for(int j = i + 1; j < points.size(); j++) {
                final Point pointToCompare = points.get(j);

                tc = GeometricEquations.collisionTime(point, pointToCompare);
                if(minEvent == null || tc < minEvent.getTime()) {
                    minEvent = new PointsEvent(tc, point, pointToCompare);
                }
            }

            // Calculate the collision between the given point and one of the horizontal walls
            tc = GeometricEquations.timeToHitWall(point, Wall.HORIZONTAL, 0, L);
            hWallEvent = new WallEvent(tc, point, Wall.HORIZONTAL);

            if(minEvent == null || hWallEvent.getTime() < minEvent.getTime()){
                minEvent = hWallEvent;
            }

            // Calculate the collision between the given point and one of the vertical walls
            tc = GeometricEquations.timeToHitWall(point, Wall.VERTICAL, 0, W);
            vWallEvent = new WallEvent(tc, point, Wall.VERTICAL);

            if(vWallEvent.getTime() < minEvent.getTime()) {
                minEvent = vWallEvent;
            }

            // In case the collision happens at the same time than the current events on the list, the event is added.
            // In case this events happens before, the list is replaced for a new one with lower collision time.
            if(eventList.isEmpty() ||  minEvent.compareTo(eventList.get(0)) == 0){
                eventList.add(minEvent);
            } else if(minEvent.compareTo(eventList.get(0)) < 0){
                eventList = new ArrayList<>();
                eventList.add(minEvent);
            }

        }

        //TODO: Check collisions with wall in the middle

        //TODO: Calculate Temperature and Pressure

        return eventList;
    }

    double getFraction() {
        return fraction;
    }

    double getCollisionTime() {
        return collisionTime;
    }
}
