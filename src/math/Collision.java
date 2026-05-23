package math;

/**
 * Collision detection helpers.
 *
 * The core method here is segmentIntersectsCircle, which is exactly the
 * "line versus circle" test discussed in class. The slice gesture is a short
 * line segment (from the previous fingertip position to the current one) and
 * each fruit is a circle of some radius. If the segment passes through the
 * circle the fruit was sliced.
 *
 * Math (quadratic form):
 *
 *   Parametrize the segment as P(t) = A + t * (B - A) for t in [0, 1].
 *   The point is inside the circle when |P(t) - C|^2 <= r^2.
 *   Expanding gives a quadratic in t:
 *
 *       a*t^2 + b*t + c = 0
 *
 *   where
 *       d  = B - A
 *       f  = A - C
 *       a  = d . d
 *       b  = 2 * (f . d)
 *       c  = f . f - r^2
 *
 *   The discriminant b*b - 4*a*c tells us whether the infinite line crosses
 *   the circle. If it does, the two roots are the parameter values where the
 *   line enters and exits. We only count a hit when at least one root lies in
 *   the [0, 1] range (meaning the actual segment, not its extension, crosses).
 */
public final class Collision {

    private Collision() {}

    public static boolean pointInCircle(double px, double py,
                                        double cx, double cy,
                                        double radius) {
        double dx = px - cx;
        double dy = py - cy;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    /**
     * Returns true if the line segment from (x1, y1) to (x2, y2) intersects
     * the circle centered at (cx, cy) with the given radius.
     *
     * This is the canonical "line versus circle" test from class.
     */
    public static boolean segmentIntersectsCircle(double x1, double y1,
                                                  double x2, double y2,
                                                  double cx, double cy,
                                                  double radius) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double fx = x1 - cx;
        double fy = y1 - cy;

        double a = dx * dx + dy * dy;
        double b = 2 * (fx * dx + fy * dy);
        double c = (fx * fx + fy * fy) - (radius * radius);

        // Degenerate segment (the two endpoints are the same).
        // Fall back to a point-in-circle test.
        if (a == 0) {
            return c <= 0;
        }

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return false;
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double t1 = (-b - sqrtDisc) / (2 * a);
        double t2 = (-b + sqrtDisc) / (2 * a);

        // The actual segment lives on t in [0, 1]. Either entry or exit point
        // landing in that range means the segment touches or crosses the circle.
        return (t1 >= 0 && t1 <= 1) || (t2 >= 0 && t2 <= 1);
    }
}
