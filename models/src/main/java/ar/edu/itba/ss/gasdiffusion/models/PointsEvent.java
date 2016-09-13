package ar.edu.itba.ss.gasdiffusion.models;

import java.util.HashSet;
import java.util.Set;

public class PointsEvent extends Event {
  private static final int X = 0;
  private static final int Y = 1;

  private final Point p1, p2;

  public PointsEvent(final double time, final Point p1, final Point p2) {
    super(time);
    this.p1 = p1;
    this.p2 = p2;
  }

  /**
   * Calculate the new particles configuration based on an elastic collision
   * @return a set of two points, containing the points with the updated velocities (orientation)
   */
  @Override
  public Set<Point> execute() {
    final Set<Point> newPoints = new HashSet<>();

    final Point updatedP1 = p1.updatePoint(time);
    final Point updatedP2 = p2.updatePoint(time);

    final double sigma = updatedP1.radio() + updatedP2.radio();
    final double deltaR[] = {
            updatedP2.x() - updatedP1.x(),
            updatedP2.y() - updatedP1.y()
    };
    final double deltaV[] = {
            updatedP2.vx() - updatedP1.vx(),
            updatedP2.vy() - updatedP1.vy()
    };

    final double deltaV_deltaR_squareDistance = deltaV[X] * deltaR[X] + deltaV[Y] * deltaR[Y];

    // Thanks Marco Boschetti & Juan Cruz Lepore for the following.
    /*
        Each ji will assume that the mass of particle i is not Infinite, so as we can divide all the term by
        our mass.
        If mass of particle i results to be Infinite, then it is because our new speed is not of our interest
         (due to the fact that the particles with infinite mass should not move),
          and this does not affect the other particles.
     */
    final double j1 = (2 * updatedP1.mass() * deltaV_deltaR_squareDistance)/
            (sigma * (updatedP1.mass() / updatedP2.mass() + 1));
    final double j2 = (2 * updatedP2.mass() * deltaV_deltaR_squareDistance)/
            (sigma * (1 + updatedP2.mass() / updatedP1.mass()));

    final double jx1 = j1 * deltaR[X] / sigma;
    final double jy1 = j1 * deltaR[Y] / sigma;
    final double jx2 = j2 * deltaR[X] / sigma;
    final double jy2 = j2 * deltaR[Y] / sigma;


    final double moduleBefore =
            Math.sqrt(Math.pow(updatedP1.vx() - updatedP2.vx(), 2) + Math.pow(updatedP1.vy() - updatedP2.vy(), 2));

    final double newVx1 = updatedP1.vx() + jx1 / updatedP1.mass();
    final double newVy1 = updatedP1.vy() + jy1 / updatedP1.mass();
    final double newVx2 = updatedP2.vx() - jx2 / updatedP2.mass();
    final double newVy2 = updatedP2.vy() - jy2 / updatedP2.mass();

    final Point newPoint1 = p1.updatePoint(time, newVx1, newVy1);
    final Point newPoint2 = p2.updatePoint(time, newVx2, newVy2);
    newPoints.add(newPoint1);
    newPoints.add(newPoint2);

    final double moduleAfter =
            Math.sqrt(Math.pow(newPoint1.vx() - newPoint2.vx(), 2) + Math.pow(newPoint1.vy() - newPoint2.vy(), 2));

    if (Math.abs(moduleAfter - moduleBefore) >= 1e-6) {
      throw new IllegalStateException("Kinetic energy is being modified on a particle colision.");
    }

    return newPoints;
  }

  @Override
  public double getPressure() {
    return 0; // no pressure at a particle's collision
  }
}
