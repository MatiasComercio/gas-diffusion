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
						+ "}";
	}



	/* for testing purposes only */
	public static void resetIdGen() {
		idGen = 0;
	}

	public Point movePoint(final double time) {
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
}
