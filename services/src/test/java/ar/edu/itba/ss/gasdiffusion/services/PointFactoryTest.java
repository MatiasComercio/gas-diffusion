package ar.edu.itba.ss.gasdiffusion.services;

import ar.edu.itba.ss.gasdiffusion.models.Point;
import org.junit.Test;

import java.util.Set;

public class PointFactoryTest {
	/* trivial repetition test */

	@Test
	public void testRandomPoints() {
		// trivial repetition
		for (int i = 0 ; i < 10000 ; i++) {
			wrappedTestRandomPoints();
		}
	}

	private void wrappedTestRandomPoints() {
		final PointFactory pF = PointFactory.getInstance();

		final Point leftBottomPoint = Point.builder(0,0).build();
		final Point rightTopPoint = Point.builder(20,20).build();

		final Set<Point> points = pF.randomPoints(leftBottomPoint, rightTopPoint, 2, 5, false, 10, 0, 0);

		for (Point p1 : points) {

			// check bounds
			if (p1.x() < 0 || p1.y() < 0 || p1.x() >= 20 || p1.y() >= 20) {
				// out of bounds
				assert false; // this will throw an assertion
			}

			// check collision
			for (Point p2 : points) {
				if (p1.equals(p2)) {
					continue;
				}

				if (GeometricEquations.distanceBetween(p1, p2) < 0) {
					// collision detected
					assert false;
				}
			}
		}

//		System.out.println("---------------------------");
//		points.forEach(System.out::println);
//		System.out.println("---------------------------");
	}
}
