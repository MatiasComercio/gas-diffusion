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

	@Value.Check
	protected void checkRadio() {
		if (radio() < 0) {
			throw new IllegalArgumentException("Radio should be >= 0");
		}
	}

	@Value.Default
	@Value.Auxiliary
	public double speed() {
		return 0;
	}

	@Value.Check
	protected void checkSpeed() {
		if (speed() < 0) {
			throw new IllegalArgumentException("Speed (velocity's module) should be >= 0");
		}
	}

	@Value.Default
	@Value.Auxiliary
	public double orientation(){
		return 0;
	}

	@Value.Derived
	//@Value.Default
	@Value.Auxiliary
	public double vx() {
		return speed() * Math.cos(orientation());
	}

	@Value.Derived
	//@Value.Default
	@Value.Auxiliary
	public double vy() {
		return speed() * Math.sin(orientation());
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
						+ ", speed=" + speed()
						+ ", orientation=" + orientation()
						+ "}";
	}
	
	/* for testing purposes only */
	public static void resetIdGen() {
		idGen = 0;
	}
}
