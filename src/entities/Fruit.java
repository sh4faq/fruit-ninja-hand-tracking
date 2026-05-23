package entities;

import java.awt.Graphics2D;
import java.util.Random;

import math.Collision;
import math.Vector3;

/**
 * A single fruit. Has position, velocity, rotation, and a type.
 *
 * Physics is the classic projectile model: gravity is added to vy each frame,
 * then position is integrated by velocity. Rotation advances by a fixed
 * per-frame rate, scaled by deltaTime so motion stays frame-rate independent.
 *
 * A Fruit can be either "whole" (a sphere) or a "half" (one of the two halves
 * produced by slicing). Halves keep their own velocity and rotation, so they
 * fly apart naturally.
 */
public class Fruit {

    public static final double BASE_RADIUS = 55;
    public static final double MIN_FRUIT_SPEED = 12;
    public static final double MAX_FRUIT_SPEED = 16;

    private static final Random RNG = new Random();

    public final FruitType type;
    public double x, y;
    public double vx, vy;

    // 3D depth. z > 0 means deeper (further behind the screen), z < 0 means
    // closer to the viewer. The renderer applies a perspective division
    // based on this so deeper fruits draw smaller and farther toward the
    // center of the screen. See Vector3#project for the math.
    public double z;
    public double vz;

    public double rotation;
    public double rotationSpeed;
    public double radius;

    public boolean isSliced;
    public boolean isHalf;
    public HalfSide halfSide;

    public enum HalfSide { LEFT, RIGHT }

    public Fruit(int panelWidth, int panelHeight) {
        this(panelWidth, panelHeight, FruitType.random(),
             MIN_FRUIT_SPEED, MAX_FRUIT_SPEED);
    }

    public Fruit(int panelWidth, int panelHeight, FruitType type,
                 double minSpeed, double maxSpeed) {
        this.type = type;
        this.radius = BASE_RADIUS + RNG.nextDouble() * 20 - 10;

        this.x = radius + 100 + RNG.nextDouble() * (panelWidth - 2 * radius - 200);
        this.y = panelHeight + radius;

        double angle = RNG.nextDouble() * 0.6 - 0.3;
        double speed = minSpeed + RNG.nextDouble() * (maxSpeed - minSpeed);
        this.vx = Math.sin(angle) * speed * 0.8;
        this.vy = -speed * 1.1;

        // Spawn at a random depth so each wave has visual layering. Most
        // fruits live in the mid-field; a few come from deeper, a few up
        // close. vz adds a slow drift toward the viewer.
        this.z = -80 + RNG.nextDouble() * 280;
        this.vz = -0.6 + RNG.nextDouble() * 0.4;

        this.rotation = 0;
        this.rotationSpeed = (RNG.nextDouble() - 0.5) * 0.2;
    }

    public void update(double dtScale, double gravity) {
        vy += gravity * dtScale;
        x += vx * dtScale;
        y += vy * dtScale;
        z += vz * dtScale;
        rotation += rotationSpeed * dtScale;
    }

    /** Perspective depth scale: 1.0 at the screen plane, smaller as z grows. */
    public double depthScale() {
        return Vector3.FOCAL_LENGTH / (Vector3.FOCAL_LENGTH + z);
    }

    public boolean isOffScreen(int panelWidth, int panelHeight) {
        double s = depthScale();
        double visualR = radius * s;
        return y > panelHeight + visualR * 2
            || x < -visualR * 4
            || x > panelWidth + visualR * 4
            || z > 600   // too far behind the screen
            || z < -200; // pierced through the viewer
    }

    public boolean checkSlice(double x1, double y1, double x2, double y2) {
        if (isSliced) return false;
        // Collision uses the depth-scaled radius so deeper fruits are harder
        // to hit and closer fruits are easier (matches the visual size).
        double hitR = radius * depthScale();
        return Collision.segmentIntersectsCircle(x1, y1, x2, y2, x, y, hitR);
    }

    /**
     * Cuts the fruit and returns the two halves that should be added to the
     * world. Each half inherits the fruit's velocity but is nudged outward
     * and given its own spin so they visibly fly apart.
     */
    public Fruit[] slice(double sliceDx, double sliceDy) {
        this.isSliced = true;

        // The slice is a 2D vector on the screen plane (z = 0). We compute
        // the "split direction" via a 3D cross product with the screen
        // normal (0, 0, 1), which is the textbook way to get a perpendicular
        // vector in 3D space. The result here ends up purely in the xy
        // plane, but the math demonstrates the same Vector3 used elsewhere.
        Vector3 slice = new Vector3(sliceDx, sliceDy, 0);
        Vector3 screenNormal = new Vector3(0, 0, 1);
        Vector3 split = new Vector3();
        slice.cross(screenNormal, split);
        split.normalize();

        Fruit left = halfClone(HalfSide.LEFT);
        left.vx = this.vx - split.x * 3;
        left.vy = this.vy - split.y * 3 - 1;
        left.vz = this.vz - 0.4;   // halves drift slightly toward the viewer
        left.rotationSpeed = -0.15;

        Fruit right = halfClone(HalfSide.RIGHT);
        right.vx = this.vx + split.x * 3;
        right.vy = this.vy + split.y * 3 - 1;
        right.vz = this.vz - 0.4;
        right.rotationSpeed = 0.15;

        return new Fruit[] { left, right };
    }

    private Fruit halfClone(HalfSide side) {
        Fruit h = new Fruit(0, 0, this.type, 0, 0);
        h.x = this.x;
        h.y = this.y;
        h.z = this.z;
        h.radius = this.radius;
        h.rotation = this.rotation;
        h.isHalf = true;
        h.halfSide = side;
        h.isSliced = true;
        return h;
    }

    public void draw(Graphics2D g) {
        if (isHalf) {
            FruitRenderer.drawHalf(g, this);
        } else {
            FruitRenderer.drawWhole(g, this);
        }
    }
}
