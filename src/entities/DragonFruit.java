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
 * Dragon Fruit: an ultra-rare 50-point fruit.
 *
 * Spawn chance is gated to about half a percent per regular fruit spawn over
 * in {@link game.GamePanel}, so seeing one is a treat. Single slice destroys
 * it and grants both score and coins.
 */
public class DragonFruit {

    public static final int POINT_VALUE = 50;
    public static final int COIN_VALUE  = 50;

    private static final Random RNG = new Random();

    public double x, y;
    public double vx, vy;
    public double z, vz;
    public double radius = 58;
    public double rotation;
    public double rotationSpeed;
    public double sparklePhase;
    public boolean isSliced;

    public DragonFruit(int panelWidth, int panelHeight) {
        this.x = radius + 100 + RNG.nextDouble() * (panelWidth - 2 * radius - 200);
        this.y = panelHeight + radius;
        double angle = RNG.nextDouble() * 0.5 - 0.25;
        double speed = 13 + RNG.nextDouble() * 3;
        this.vx = Math.sin(angle) * speed * 0.8;
        this.vy = -speed * 1.1;
        this.z = -50 + RNG.nextDouble() * 200;
        this.vz = -0.5 + RNG.nextDouble() * 0.25;
        this.rotationSpeed = (RNG.nextDouble() - 0.5) * 0.14;
        this.sparklePhase = RNG.nextDouble() * Math.PI * 2;
    }

    public void update(double dtScale, double gravity) {
        vy += gravity * dtScale;
        x += vx * dtScale;
        y += vy * dtScale;
        z += vz * dtScale;
        rotation += rotationSpeed * dtScale;
        sparklePhase += 0.18 * dtScale;
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
        if (isSliced) return false;
        double hitR = radius * depthScale();
        return Collision.segmentIntersectsCircle(x1, y1, x2, y2, x, y, hitR);
    }

    public void draw(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        double s = depthScale();
        g.scale(s, s);
        g.rotate(rotation);

        double r = radius;

        // Outer aura
        Color aura = new Color(255, 70, 180, 90);
        Point2D ac = new Point2D.Double(0, 0);
        float[] adist = { 0f, 0.55f, 1f };
        Color[] acolors = {
            aura,
            new Color(255, 70, 180, 35),
            new Color(0, 0, 0, 0)
        };
        g.setPaint(new RadialGradientPaint(ac, (float) (r * 1.7), adist, acolors));
        g.fill(new Ellipse2D.Double(-r * 1.7, -r * 1.7, r * 3.4, r * 3.4));

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new Ellipse2D.Double(-r + 4, -r + 6, r * 2.04, r * 2.04));

        // Pink-magenta scaly body
        Point2D bc = new Point2D.Double(-r * 0.3, -r * 0.3);
        float[] dist = { 0f, 0.5f, 1f };
        Color[] colors = {
            new Color(0xff8fb8),
            new Color(0xe91e63),
            new Color(0x880e4f)
        };
        g.setPaint(new RadialGradientPaint(bc, (float) r, dist, colors));
        g.fill(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        // Scale texture: little triangle-like flicks across the body
        g.setColor(new Color(255, 200, 220, 130));
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < 14; i++) {
            double a = i * (Math.PI * 2 / 14.0);
            double rr = r * 0.6;
            double sx = Math.cos(a) * rr;
            double sy = Math.sin(a) * rr;
            Path2D.Double scale = new Path2D.Double();
            scale.moveTo(sx, sy);
            scale.lineTo(sx + Math.cos(a + 0.6) * r * 0.18,
                         sy + Math.sin(a + 0.6) * r * 0.18);
            scale.lineTo(sx + Math.cos(a - 0.6) * r * 0.18,
                         sy + Math.sin(a - 0.6) * r * 0.18);
            scale.closePath();
            g.fill(scale);
        }

        // Specular highlight
        g.setColor(new Color(255, 240, 250, 180));
        g.fill(new Ellipse2D.Double(-r * 0.55, -r * 0.65, r * 0.55, r * 0.4));

        // Green spike-like leaves at the stem (top)
        g.setColor(new Color(0x66bb6a));
        for (int i = -3; i <= 3; i++) {
            double a = -Math.PI / 2 + i * 0.18;
            double tipX = Math.cos(a) * r * 1.05;
            double tipY = Math.sin(a) * r * 1.05;
            Path2D.Double leaf = new Path2D.Double();
            leaf.moveTo(Math.cos(a - 0.05) * r * 0.55,
                        Math.sin(a - 0.05) * r * 0.55);
            leaf.lineTo(tipX, tipY);
            leaf.lineTo(Math.cos(a + 0.05) * r * 0.55,
                        Math.sin(a + 0.05) * r * 0.55);
            leaf.closePath();
            g.fill(leaf);
            // Highlight along one edge
            g.setColor(new Color(0xa5d6a7));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(new java.awt.geom.Line2D.Double(
                Math.cos(a) * r * 0.55, Math.sin(a) * r * 0.55, tipX, tipY));
            g.setColor(new Color(0x66bb6a));
        }

        // Outline
        g.setColor(new Color(80, 0, 40, 180));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        // Sparkle accents (rare-feel shimmer)
        int sparkles = 4;
        for (int i = 0; i < sparkles; i++) {
            double sa = sparklePhase + i * (Math.PI * 2 / sparkles);
            double sr = r * 0.35;
            double sx = Math.cos(sa) * sr;
            double sy = Math.sin(sa) * sr;
            double size = 2.5 + Math.abs(Math.sin(sparklePhase + i)) * 2.5;
            g.setColor(new Color(255, 255, 255, 220));
            g.fill(new Ellipse2D.Double(sx - size / 2, sy - size / 2, size, size));
        }

        g.setTransform(saved);
    }
}
