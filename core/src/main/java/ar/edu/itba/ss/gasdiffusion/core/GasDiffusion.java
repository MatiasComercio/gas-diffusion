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
  private final SystemData systemData;

  GasDiffusion(final double L, final double W, final double opening) {
    systemData = new SystemData(L, W, opening);
  }

  /**
   * <p>
   *  Runs the next event on the system that is determined by the next collision, and updates all system's
   *  particles and data. These information is returned on the SystemData structure.
   * <p>
   *  Consider that the SystemData structure is part of this class,
   *  and can only be accessed when executing the run method
   * @param points the points over to which run the current iteration
   * @return the SystemData structure
   */
  SystemData run(final List<Point> points) {
    final List<Event> minEvent;
  /*
   * During run method, it saves the number of particles at the right side of the box.
   * For doing that, it starts run method with value 0.0, and it is increased each time a particle is found to be
   * on the right side of the box.
   * This was done in this way so as to get a consistent result when no particles are present on the system.
   */
    systemData.leftSideFraction = 0;
    systemData.collisionTime = 0;

    if (points == null || points.size() == 0) { // nothing to process; avoid future division by zero (1)
      systemData.resetParticles(0);
      return systemData;
    }

    systemData.resetParticles(points.size());

    minEvent = predictCollisions(points);

    if (minEvent.isEmpty()) {
      LOGGER.debug("There does not exist a collision. Skipping particle update...");
      return systemData;
    }

    // There existed at least a collision and that collision has the min. tc
    // Update the position of all particles (Including the ones that collided)
    final double tc = minEvent.get(0).getTime();
    for(final Point point : points) {
      systemData.particles.add(point.updatePoint(tc));
      // If particle is on the left side => add it to the current leftSideFraction counter
      if (point.x() <= systemData.W/2) { // calculate leftSideFraction after update
        systemData.leftSideFraction++;
      }
    }

    Set<Point> collisionParticles;
    for(Event event : minEvent){
      collisionParticles = event.execute();

      // Remove the particles that take part in an event, and add them again with updated velocity and position
      systemData.particles.removeAll(collisionParticles);
      systemData.particles.addAll(collisionParticles);

      // Get the event's resulting currentPressure
      systemData.currentPressure += event.getPressure();
    }

    final long N = points.size();
    systemData.leftSideFraction /= N; // (1) division by zero avoided as noticed at that reference
    systemData.collisionTime = tc;

    return systemData;
  }

  /**
   * Predict the next events (collisions)
   * @param points the collection of points to be checked against the given point
   */
  private List<Event> predictCollisions(final List<Point> points) {
    List<Event> eventList = new ArrayList<>();
    Event minEvent, hWallEvent, vWallEvent, middleWallEvent;
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

      // +++x improve: create event inside the if condition so as not to overhead with object creation

      // Calculate the collision between the given point and one of the horizontal walls
      tc = GeometricEquations.timeToHitWall(point, Wall.HORIZONTAL, 0, systemData.L);
      hWallEvent = new WallEvent(tc, point, Wall.HORIZONTAL);

      if(minEvent == null || hWallEvent.getTime() < minEvent.getTime()){
        minEvent = hWallEvent;
      }

      // Calculate the collision between the given point and one of the vertical walls
      tc = GeometricEquations.timeToHitWall(point, Wall.VERTICAL, 0, systemData.W);
      vWallEvent = new WallEvent(tc, point, Wall.VERTICAL);

      if(vWallEvent.getTime() < minEvent.getTime()) {
        minEvent = vWallEvent;
      }

      // Calculate the collision between the given point and the middle wall
      tc = GeometricEquations.timeToHitMiddleWall(
              point, systemData.W/2, systemData.L, systemData.opening);
      middleWallEvent = new WallEvent(tc, point, Wall.MIDDLE_VERTICAL);

      if(middleWallEvent.getTime() < minEvent.getTime()) {
        minEvent = middleWallEvent;
      }

      // In case the collision happens at the same time than the current events on the list, the event is added.
      // In case this events happens before, the list is replaced for a new one with lower collision time.
      // +++x improve: make the comparision only once and save the result
      if(eventList.isEmpty() ||  minEvent.compareTo(eventList.get(0)) == 0){
        eventList.add(minEvent);
      } else if(minEvent.compareTo(eventList.get(0)) < 0){
        eventList = new ArrayList<>();
        eventList.add(minEvent);
      }

    }

    return eventList;
  }

  /* package-private */ static class SystemData {
    // static system data
    private final double L;
    private final double W;
    private final double opening;

    // dynamic system data
    private double leftSideFraction;
    private double collisionTime;
    private double currentPressure;
    private double totalPressure;
    private List<Point> particles;

    private SystemData(final double L, final double W, final double opening) {
      this.L = L;
      this.W = W;
      this.opening = opening;
      this.leftSideFraction = 1.0d;
      this.collisionTime = 0;
      this.currentPressure = 0;
      this.totalPressure = 0;
      resetParticles(0);
    }

    /**
     * Clears and resets the system's particles list
     * @param N the size of the new list of particles
     * @return the new empty list
     */
    private List<Point> resetParticles(final int N) {
      particles = new ArrayList<>(N);
      return particles;
    }

    /* package-private */ void resetCurrentPressure() {
      this.totalPressure += this.getCurrentPressure();
      this.currentPressure = 0;
    }

    /* package-private */ void resetTotalPressure() {
      this.totalPressure = 0;
    }

    /* package-private */ double getW() {
      return W;
    }

    /**
     *
     * @return the fraction of particles that resides on the left side of the box
     */
    /* package-private */ double getLeftSideFraction() {
      return leftSideFraction;
    }

    /* package-private */ double getCollisionTime() {
      return collisionTime;
    }

    /* package-private */ double getCurrentPressure() {
      return currentPressure;
    }

    /* package-private */ double getTotalPressure() {
      return totalPressure;
    }

    /* package-private */ List<Point> getParticles() {
      return particles;
    }
  }
}
