package ar.edu.itba.ss.gasdiffusion.models;

import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abs",
        typeImmutable = "*",
        get = ""
)
public abstract class PointAbs {

  private static long idGen = 1;

  @Value.Default
  public long id() {
    return idGen ++;
  }

  @Value.Default
  @Value.Auxiliary
  public boolean isColliding() { return false; }

  @Builder.Parameter
  @Value.Auxiliary
  public abstract double x();

  @Builder.Parameter
  @Value.Auxiliary
  public abstract double y();

  @Value.Default
  @Value.Auxiliary
  public double mass() {
    return 0;
  }

  @Value.Default
  @Value.Auxiliary
  public double radio() {
    return 0;
  }

  @Value.Default
  @Value.Auxiliary
  public double vx() {
    return 0;
  }

  @Value.Default
  @Value.Auxiliary
  public double vy() {
    return 0;
  }



  @Value.Check
  protected void checkRadio() {
    if (radio() < 0) {
      throw new IllegalArgumentException("Radio should be >= 0");
    }
  }

  @Value.Derived
  @Value.Auxiliary
  public double speed() {
    return Math.sqrt(Math.pow(vx(), 2) + Math.pow(vy(), 2));
  }

  @Value.Derived
  @Value.Auxiliary
  public double kineticEnergy() {
    return 1/2.0d * mass() * Math.pow(speed(), 2);
  }

  @Value.Check
  protected void checkSpeed() {
    if (speed() < 0) {
      throw new IllegalArgumentException("Speed (velocity's module) should be >= 0");
    }
  }


  /**
   * Prints the immutable value {@code Point} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "Point{"
            + "id=" + id()
            + ", x=" + x()
            + ", y=" + y()
            + ", radio=" + radio()
            + ", vx=" + vx()
            + ", vy=" + vy()
            + ", speed=" + speed()
            + ", mass=" + mass()
            + ", kinetic energy=" + kineticEnergy()
            + "}";
  }

  public Point updatePoint(final double time) {
    final double newX = x() + vx() * time;
    final double newY = y() + vy() * time;

    return Point.builder(newX, newY)
            .id(this.id())
            .vx(this.vx())
            .vy(this.vy())
            .mass(this.mass())
            .radio(this.radio())
            .build();
  }

  /**
   * Assumes that the point is COLLIDING
   */
  public Point updatePoint(final double time, final double newVx, final double newVy) {
    final double newX = x() + vx() * time;
    final double newY = y() + vy() * time;

    return Point.builder(newX, newY)
            .id(this.id())
            .vx(newVx)
            .vy(newVy)
            .mass(this.mass())
            .radio(this.radio())
            .isColliding(true)
            .build();
  }

  /* for testing purposes only */
  public static void resetIdGen() {
    idGen = 0;
  }
}
