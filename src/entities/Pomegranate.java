package entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Random;

import math.Collision;
import math.Vector3;

/**
 * Pomegranate: an oversized multi-hit fruit. Takes {@link #MAX_HEALTH} slices
 * to destroy. Each individual slice grants a small bonus and shakes the
 * screen; the final slice grants the finale bonus.
 *
 * Spawning is gated to Arcade mode by {@link game.GamePanel}.
 */
public class Pomegranate {

    public static final int MAX_HEALTH = 8;
    public static final int HIT_POINTS = 5;
    public static final int FINALE_BONUS = 40;

    private static final Random RNG = new Random();

    public double x, y;
    public double vx, vy;
    public double z, vz;
    public double radius = 90;
    public double rotation;
    public double rotationSpeed;
    public int health = MAX_HEALTH;
    public boolean dead;
    public double hitFlash; // briefly white when hit, fades in update()

    public Pomegranate(int panelWidth, int panelHeight) {
        this.x = radius + 100 + RNG.nextDouble() * (panelWidth - 2 * radius - 200);
        this.y = panelHeight + radius;

        // Pomegranate is heavy: gentle arc, slow tumble.
        double angle = RNG.nextDouble() * 0.3 - 0.15;
        double speed = 13 + RNG.nextDouble() * 2.0;
        this.vx = Math.sin(angle) * speed * 0.7;
        this.vy = -speed * 1.05;
        this.z = -30 + RNG.nextDouble() * 120;
        this.vz = -0.4 + RNG.nextDouble() * 0.2;
        this.rotationSpeed = (RNG.nextDouble() - 0.5) * 0.08;
    }

    public void update(double dtScale, double gravity) {
        // A bit lighter gravity than a regular fruit so it hangs in view longer.
        vy += gravity * 0.85 * dtScale;
        x += vx * dtScale;
        y += vy * dtScale;
        z += vz * dtScale;
        rotation += rotationSpeed * dtScale;
        if (hitFlash > 0) hitFlash = Math.max(0, hitFlash - 0.08 * dtScale);
    }

    public double depthScale() {
        return Vector3.FOCAL_LENGTH / (Vector3.FOCAL_LENGTH + z);
    }

    public boolean isOffScreen(int w, int h) {
        double visualR = radius * depthScale();
        return y > h + visualR * 2 || x < -visualR * 4 || x > w + visualR * 4
            || z > 600 || z < -200;
    }

    public boolean checkSlice(double x1, double y1, double x2, double y2) {
        if (dead) return false;
        double hitR = radius * depthScale();
        return Collision.segmentIntersectsCircle(x1, y1, x2, y2, x, y, hitR);
    }

    /** Returns true when this slice destroyed the pomegranate. */
    public boolean registerHit() {
        health--;
        hitFlash = 1.0;
        if (health <= 0) {
            dead = true;
            return true;
        }
        return false;
    }

    public void draw(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        double s = depthScale();
        g.scale(s, s);
        g.rotate(rotation);

        double r = radius;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 110));
        g.fill(new Ellipse2D.Double(-r + 6, -r + 8, r * 2.05, r * 2.05));

        // Deep red gradient body
        Point2D bc = new Point2D.Double(-r * 0.3, -r * 0.3);
        float[] dist = { 0f, 0.45f, 1f };
        Color[] colors = {
            new Color(0xd14848),
            new Color(0x8b1a1a),
            new Color(0x2a0606)
        };
        g.setPaint(new RadialGradientPaint(bc, (float) r, dist, colors));
        g.fill(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        // Skin variegation: subtle darker blotches around the lower half
        g.setColor(new Color(60, 12, 12, 110));
        for (int i = 0; i < 7; i++) {
            double a = i * (Math.PI * 2 / 7.0) + rotation * 0.1;
            double bx = Math.cos(a) * r * 0.55;
            double by = Math.sin(a) * r * 0.55;
            double br = r * 0.18;
            g.fill(new Ellipse2D.Double(bx - br, by - br, br * 2, br * 2));
        }

        // Seed pips: bright red specks scattered across the surface
        g.setColor(new Color(255, 90, 90, 220));
        for (int i = 0; i < 16; i++) {
            double a = i * 0.95 + rotation * 0.3;
            double rr = (i % 3) * 0.18 + 0.15;
            double px = Math.cos(a) * r * rr;
            double py = Math.sin(a) * r * rr - r * 0.05;
            double pr = 3.0 + (i % 2);
            g.fill(new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2));
        }
        // Highlights on a few seeds
        g.setColor(new Color(255, 220, 220, 200));
        for (int i = 0; i < 5; i++) {
            double a = i * 1.7 + rotation * 0.3;
            double px = Math.cos(a) * r * 0.35;
            double py = Math.sin(a) * r * 0.35 - r * 0.05;
            g.fill(new Ellipse2D.Double(px - 1.2, py - 1.2, 2.4, 2.4));
        }

        // Specular highlight on the upper-left
        g.setColor(new Color(255, 200, 200, 150));
        g.fill(new Ellipse2D.Double(-r * 0.55, -r * 0.65, r * 0.55, r * 0.4));

        // Crown stem at top: 5-pointed dried calyx
        g.setColor(new Color(70, 30, 12));
        Path2D.Double crown = new Path2D.Double();
        double cr = r * 0.28;
        double cy = -r * 0.92;
        for (int i = 0; i < 10; i++) {
            double a = (-Math.PI / 2) + i * (Math.PI / 5);
            double rad = (i % 2 == 0) ? cr : cr * 0.45;
            double cx = Math.cos(a) * rad;
            double cyp = cy + Math.sin(a) * rad;
            if (i == 0) crown.moveTo(cx, cyp);
            else crown.lineTo(cx, cyp);
        }
        crown.closePath();
        g.fill(crown);
        g.setColor(new Color(40, 18, 6));
        g.setStroke(new BasicStroke(2f));
        g.draw(crown);

        // Outline ring
        g.setColor(new Color(0, 0, 0, 140));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        // Health pip ring: small dots under the crown showing remaining hits
        int pips = MAX_HEALTH;
        for (int i = 0; i < pips; i++) {
            double a = -Math.PI / 2 + (i - (pips - 1) / 2.0) * 0.15;
            double px = Math.cos(a) * (r * 1.18);
            double py = Math.sin(a) * (r * 1.18);
            if (i < health) {
                g.setColor(new Color(255, 220, 120, 230));
            } else {
                g.setColor(new Color(60, 60, 60, 160));
            }
            g.fill(new Ellipse2D.Double(px - 3.5, py - 3.5, 7, 7));
        }

        // Hit flash overlay
        if (hitFlash > 0) {
            int fa = (int) Math.min(180, hitFlash * 180);
            g.setColor(new Color(255, 255, 255, fa));
            g.fill(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));
        }

        g.setTransform(saved);
    }
}
