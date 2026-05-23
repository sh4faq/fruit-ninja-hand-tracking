package math;

/**
 * 2D vector with mutable x and y components.
 *
 * Used everywhere positions, velocities, or directions are needed.
 * Kept mutable on purpose so per-frame physics updates do not allocate new
 * objects in the game loop, which keeps garbage collection out of the hot path.
 */
public class Vector2 {

    public double x;
    public double y;

    public Vector2() {
        this(0, 0);
    }

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void set(Vector2 other) {
        this.x = other.x;
        this.y = other.y;
    }

    public void add(double dx, double dy) {
        this.x += dx;
        this.y += dy;
    }

    public void add(Vector2 other) {
        this.x += other.x;
        this.y += other.y;
    }

    public void scale(double factor) {
        this.x *= factor;
        this.y *= factor;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public double distanceTo(Vector2 other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
