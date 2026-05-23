package entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Random;

import math.Collision;
import math.Vector3;

/**
 * A "ghost" fruit that actively evades the player's blade using the cross
 * product steering trick from Professor Murphy's April 30 lecture
 * (BadPolygonModel2D's chase / evade routines).
 *
 * Each frame:
 *
 *   1. Build the fruit's facing unit vector U = (cosA, sinA).
 *   2. Build the vector from fruit to threat: T = (threatX - x, threatY - y).
 *   3. Compute the 2D cross product: cross = U.x * T.y - U.y * T.x
 *        cross > 0  =>  threat is on the fruit's LEFT  =>  turn RIGHT to evade.
 *        cross < 0  =>  threat is on the fruit's RIGHT =>  turn LEFT to evade.
 *   4. Move forward in the facing direction by `forwardSpeed`.
 *
 * The "threat" here is the player's pointer position (mouse or fingertip).
 * The fruit only reacts when the pointer is inside an awareness radius,
 * otherwise it drifts gently with normal physics so it doesn't feel
 * supernatural.
 *
 * Slicing this fruit gives a much larger score bonus than a normal fruit.
 */
public class EvaderFruit {

    private static final Random RNG = new Random();

    public double x, y;
    public double vx, vy;
    public double z, vz;
    public double radius = 38;
    public double angleDeg;          // facing direction in degrees
    private double cosA = 1, sinA = 0;

    public boolean isSliced;

    private static final double AWARENESS_RADIUS = 260;
    private static final double TURN_RATE_DEG    = 4.0;
    private static final double FORWARD_SPEED    = 3.4;
    private static final double DRIFT_DAMPING    = 0.92;

    public EvaderFruit(int panelWidth, int panelHeight) {
        // Spawn at a random edge of the playfield rather than from below,
        // so the evader feels like a wandering rogue rather than a thrown fruit.
        int side = RNG.nextInt(4);
        switch (side) {
            case 0: x = -radius;             y = RNG.nextInt(panelHeight); break;
            case 1: x = panelWidth + radius; y = RNG.nextInt(panelHeight); break;
            case 2: x = RNG.nextInt(panelWidth); y = -radius; break;
            default: x = RNG.nextInt(panelWidth); y = panelHeight + radius;
        }
        // Initial facing toward the screen center so it enters the playfield.
        double cx = panelWidth * 0.5, cy = panelHeight * 0.5;
        setAngle(Math.toDegrees(Math.atan2(cy - y, cx - x)));
        this.z = -40 + RNG.nextDouble() * 120;
        this.vz = -0.2 + RNG.nextDouble() * 0.1;
    }

    public void setAngle(double degrees) {
        this.angleDeg = degrees;
        double r = Math.PI * degrees / 180.0;
        this.cosA = Math.cos(r);
        this.sinA = Math.sin(r);
    }

    public double depthScale() {
        return Vector3.FOCAL_LENGTH / (Vector3.FOCAL_LENGTH + z);
    }

    /**
     * Steer away from the threat point (typically the player's pointer) and
     * advance forward. If the threat is outside the awareness radius the
     * fruit drifts on residual velocity instead of steering.
     */
    public void update(double dtScale, double threatX, double threatY, boolean threatActive) {
        double dx = threatX - x;
        double dy = threatY - y;
        double distSq = dx * dx + dy * dy;

        if (threatActive && distSq < AWARENESS_RADIUS * AWARENESS_RADIUS) {
            // 2D cross product of facing-unit U = (cosA, sinA) and
            // fruit-to-threat T = (dx, dy). The sign tells us which side
            // of the fruit the threat lies on.
            double cross = cosA * dy - sinA * dx;
            if (cross > 0)      turnRight(TURN_RATE_DEG * dtScale);
            else if (cross < 0) turnLeft(TURN_RATE_DEG * dtScale);

            // Advance along the facing vector.
            x += cosA * FORWARD_SPEED * dtScale;
            y += sinA * FORWARD_SPEED * dtScale;

            // Add some drift velocity in the evade direction too, so the
            // fruit "kicks" away from the blade rather than just turning.
            vx += cosA * 0.4 * dtScale;
            vy += sinA * 0.4 * dtScale;
        } else {
            // Drift with damping when the player isn't nearby.
            x += vx * dtScale;
            y += vy * dtScale;
            vx *= Math.pow(DRIFT_DAMPING, dtScale);
            vy *= Math.pow(DRIFT_DAMPING, dtScale);
        }

        z += vz * dtScale;
    }

    public void turnLeft(double d)  { setAngle(angleDeg - d); }
    public void turnRight(double d) { setAngle(angleDeg + d); }

    public boolean isOffScreen(int w, int h) {
        double s = depthScale();
        double vr = radius * s;
        // Wide leeway so the evader can leave and return.
        return x < -vr * 8 || x > w + vr * 8
            || y < -vr * 8 || y > h + vr * 8
            || z > 600 || z < -200;
    }

    public boolean checkSlice(double x1, double y1, double x2, double y2) {
        if (isSliced) return false;
        double hitR = radius * depthScale();
        return Collision.segmentIntersectsCircle(x1, y1, x2, y2, x, y, hitR);
    }

    public void draw(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        double s = depthScale();
        g.scale(s, s);
        g.rotate(Math.toRadians(angleDeg));

        double r = radius;

        // Aura (the fruit is "haunted")
        Point2D ac = new Point2D.Double(0, 0);
        float[] adist = { 0f, 0.6f, 1f };
        Color[] acolors = {
            new Color(120, 220, 150, 80),
            new Color(120, 220, 150, 30),
            new Color(120, 220, 150, 0),
        };
        g.setPaint(new RadialGradientPaint(ac, (float) (r * 1.6), adist, acolors));
        g.fill(new Ellipse2D.Double(-r * 1.6, -r * 1.6, r * 3.2, r * 3.2));

        // Body
        Point2D bc = new Point2D.Double(-r * 0.25, -r * 0.25);
        float[] bdist = { 0f, 0.5f, 1f };
        Color[] bcolors = {
            new Color(200, 255, 200),
            new Color(80, 200, 110),
            new Color(20, 80, 30),
        };
        g.setPaint(new RadialGradientPaint(bc, (float) r, bdist, bcolors));
        g.fill(new Ellipse2D.Double(-r, -r, r * 2, r * 2));

        // Eyes (so the evade behavior reads as "scared")
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(r * 0.05, -r * 0.25, r * 0.30, r * 0.40));
        g.fill(new Ellipse2D.Double(r * 0.50, -r * 0.25, r * 0.30, r * 0.40));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(r * 0.16, -r * 0.10, r * 0.10, r * 0.18));
        g.fill(new Ellipse2D.Double(r * 0.62, -r * 0.10, r * 0.10, r * 0.18));

        // Outline
        g.setColor(new Color(20, 60, 25, 220));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(-r, -r, r * 2, r * 2));

        g.setTransform(saved);
    }
}
