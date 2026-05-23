package entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Random;

import math.Collision;
import math.Vector3;

/**
 * A bomb. Same kind of projectile as a fruit but drawn as a dark metallic
 * sphere with a fuse and a flickering spark. Slicing one ends the game in
 * Classic mode, or deducts points in Arcade.
 */
public class Bomb {

    private static final Random RNG = new Random();

    public double x, y;
    public double vx, vy;
    public double z, vz;
    public double radius = 45;
    public double rotation;
    public double rotationSpeed;
    public boolean isSliced;
    public double sparkPhase;

    public Bomb(int panelWidth, int panelHeight) {
        this.x = radius + 100 + RNG.nextDouble() * (panelWidth - 2 * radius - 200);
        this.y = panelHeight + radius;

        double angle = RNG.nextDouble() * 0.4 - 0.2;
        double speed = 10 + RNG.nextDouble() * 4;
        this.vx = Math.sin(angle) * speed * 0.8;
        this.vy = -speed * 1.1;
        this.z = -60 + RNG.nextDouble() * 200;
        this.vz = -0.5 + RNG.nextDouble() * 0.3;
        this.rotationSpeed = (RNG.nextDouble() - 0.5) * 0.06;
    }

    public void update(double dtScale, double gravity) {
        vy += gravity * dtScale;
        x += vx * dtScale;
        y += vy * dtScale;
        z += vz * dtScale;
        rotation += rotationSpeed * dtScale;
        sparkPhase += 0.3 * dtScale;
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

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(-r + 4, -r + 5, r * 2.04, r * 2.04));

        // Metallic body
        Point2D center = new Point2D.Double(-r * 0.3, -r * 0.3);
        float[] dist = { 0f, 0.3f, 0.6f, 1f };
        Color[] colors = {
            new Color(85, 85, 85),
            new Color(58, 58, 58),
            new Color(34, 34, 34),
            new Color(10, 10, 10)
        };
        g.setPaint(new RadialGradientPaint(center, (float) r, dist, colors));
        g.fill(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        // Bright X
        g.setColor(new Color(231, 76, 60));
        g.setFont(new Font("Arial Black", Font.BOLD, (int) (r * 0.55)));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth("X");
        int th = fm.getAscent();
        g.drawString("X", -tw / 2, th / 2 - 4);

        // Fuse
        g.setColor(new Color(141, 110, 99));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(0, (int) -r, 8, (int) (-r - 32));

        // Spark glow
        double sx = 8;
        double sy = -r - 32;
        double size = 7 + Math.sin(sparkPhase) * 3;
        Point2D sc = new Point2D.Double(sx, sy);
        float[] sdist = { 0f, 0.4f, 1f };
        Color[] sc2 = {
            new Color(255, 200, 50, 180),
            new Color(255, 120, 0, 80),
            new Color(255, 0, 0, 0)
        };
        g.setPaint(new RadialGradientPaint(sc, (float) (size * 2.5), sdist, sc2));
        g.fill(new Ellipse2D.Double(sx - size * 2.5, sy - size * 2.5,
                                    size * 5, size * 5));

        // Spark core
        g.setColor(new Color(255, 102, 0));
        g.fill(new Ellipse2D.Double(sx - size, sy - size, size * 2, size * 2));
        g.setColor(new Color(255, 221, 0));
        g.fill(new Ellipse2D.Double(sx - size * 0.5, sy - size * 0.5, size, size));

        // Outline
        g.setColor(new Color(0, 0, 0, 120));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        g.setTransform(saved);
    }
}
