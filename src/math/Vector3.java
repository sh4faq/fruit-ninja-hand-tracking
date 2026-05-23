package math;

/**
 * 3D vector with mutable x, y, z components.
 *
 * Used by the entity physics so each fruit has a real position and velocity
 * in 3D space. Z greater than 0 is "deeper" (away from the viewer); z less
 * than 0 is "closer" (in front of the screen plane). The {@link #project}
 * helper applies a simple perspective division to flatten a 3D point onto
 * 2D screen coordinates for rendering and slice-collision testing.
 *
 * Standard vector ops included so the rest of the game can demonstrate
 * linear algebra (dot, cross, normalize, distance) instead of inlining
 * raw arithmetic everywhere.
 */
public class Vector3 {

    public double x;
    public double y;
    public double z;

    /** Pinhole camera focal length used by {@link #project}. */
    public static final double FOCAL_LENGTH = 800.0;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(Vector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public void add(Vector3 other) {
        x += other.x;
        y += other.y;
        z += other.z;
    }

    public void add(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
    }

    public void scale(double f) {
        x *= f;
        y *= f;
        z *= f;
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /** Cross product, stored into <code>out</code>. */
    public void cross(Vector3 other, Vector3 out) {
        double cx = y * other.z - z * other.y;
        double cy = z * other.x - x * other.z;
        double cz = x * other.y - y * other.x;
        out.set(cx, cy, cz);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public void normalize() {
        double len = length();
        if (len > 1e-9) {
            x /= len;
            y /= len;
            z /= len;
        }
    }

    public double distanceTo(Vector3 other) {
        double dx = other.x - x;
        double dy = other.y - y;
        double dz = other.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Perspective-project a world-space point onto screen-space coordinates,
     * relative to an "anchor" (typically the center of the playfield). The
     * perspective division ratio is also returned in the {@code out} array
     * so callers can scale radii and sprite sizes by the same amount.
     *
     *   scale = FOCAL_LENGTH / (FOCAL_LENGTH + z)
     *   screenX = anchorX + (x - anchorX) * scale
     *   screenY = anchorY + (y - anchorY) * scale
     *
     * The result fills out[0]=screenX, out[1]=screenY, out[2]=scale.
     */
    public void project(double anchorX, double anchorY, double[] out) {
        double scale = FOCAL_LENGTH / (FOCAL_LENGTH + z);
        out[0] = anchorX + (x - anchorX) * scale;
        out[1] = anchorY + (y - anchorY) * scale;
        out[2] = scale;
    }
}
