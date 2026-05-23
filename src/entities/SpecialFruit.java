package entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Random;

import math.Collision;
import math.Vector3;

/**
 * Arcade-only special fruit. Slicing one activates a powerup.
 *
 *   FREEZE  - slows all falling fruits and pauses the timer
 *   FRENZY  - increases spawn rate
 *   DOUBLE  - all points doubled
 */
public class SpecialFruit {

    private static final Random RNG = new Random();

    public enum Kind { FREEZE, FRENZY, DOUBLE, BERRY_BLAST, PEACHY_TIME, BOMB_DEFLECT }

    public final Kind kind;
    public double x, y;
    public double vx, vy;
    public double z, vz;
    public double radius = 50;
    public double rotation;
    public double rotationSpeed;
    public double pulsePhase;
    public boolean isSliced;

    public SpecialFruit(int panelWidth, int panelHeight, Kind kind) {
        this.kind = kind;
        this.x = radius + 100 + RNG.nextDouble() * (panelWidth - 2 * radius - 200);
        this.y = panelHeight + radius;
        double angle = RNG.nextDouble() * 0.4 - 0.2;
        double speed = 12 + RNG.nextDouble() * 3;
        this.vx = Math.sin(angle) * speed * 0.8;
        this.vy = -speed * 1.1;
        this.z = -40 + RNG.nextDouble() * 180;
        this.vz = -0.4 + RNG.nextDouble() * 0.2;
        this.rotationSpeed = (RNG.nextDouble() - 0.5) * 0.12;
        this.pulsePhase = RNG.nextDouble() * Math.PI * 2;
    }

    public void update(double dtScale, double gravity) {
        vy += gravity * dtScale;
        x += vx * dtScale;
        y += vy * dtScale;
        z += vz * dtScale;
        rotation += rotationSpeed * dtScale;
        pulsePhase += 0.08 * dtScale;
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

        double pulse = 1.0 + Math.sin(pulsePhase) * 0.06;
        g.scale(pulse, pulse);

        double r = radius;
        Color base = baseColor();
        Color light = baseColor().brighter();
        Color dark = baseColor().darker();

        // Aura halo
        Point2D ac = new Point2D.Double(0, 0);
        float[] adist = { 0f, 0.6f, 1f };
        Color[] acolors = {
            withAlpha(base, 60),
            withAlpha(base, 20),
            new Color(0, 0, 0, 0)
        };
        g.setPaint(new RadialGradientPaint(ac, (float) (r * 1.8), adist, acolors));
        g.fill(new Ellipse2D.Double(-r * 1.8, -r * 1.8, r * 3.6, r * 3.6));

        // Body
        Point2D bc = new Point2D.Double(-r * 0.25, -r * 0.25);
        float[] bdist = { 0f, 0.5f, 1f };
        Color[] bcolors = { light, base, dark };
        g.setPaint(new RadialGradientPaint(bc, (float) r, bdist, bcolors));
        g.fill(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        // Inner symbol
        switch (kind) {
            case FREEZE:       drawSnowflake(g, r); break;
            case FRENZY:       drawLightning(g, r); break;
            case DOUBLE:       drawTwoX(g, r);      break;
            case BERRY_BLAST:  drawStrawberry(g, r); break;
            case PEACHY_TIME:  drawPeachClock(g, r); break;
            case BOMB_DEFLECT: drawShield(g, r);    break;
        }

        // Spec highlight
        g.setColor(new Color(255, 255, 255, 180));
        g.fill(new Ellipse2D.Double(-r * 0.55, -r * 0.6, r * 0.5, r * 0.4));

        // Ring
        g.setColor(withAlpha(light, 200));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Ellipse2D.Double(-r, -r, 2 * r, 2 * r));

        g.setTransform(saved);
    }

    private Color baseColor() {
        switch (kind) {
            case FREEZE:       return new Color(0x00BCD4);
            case FRENZY:       return new Color(0xFF1744);
            case DOUBLE:       return new Color(0xFFD600);
            case BERRY_BLAST:  return new Color(0xD81B60);
            case PEACHY_TIME:  return new Color(0xFFAB91);
            case BOMB_DEFLECT: return new Color(0x546E7A);
            default: return Color.WHITE;
        }
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private void drawSnowflake(Graphics2D g, double r) {
        g.setColor(new Color(255, 255, 255, 180));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            double a = (i / 6.0) * Math.PI * 2;
            double ex = Math.cos(a) * r * 0.55;
            double ey = Math.sin(a) * r * 0.55;
            g.draw(new Line2D.Double(0, 0, ex, ey));
            for (int side = -1; side <= 1; side += 2) {
                double ba = a + side * 0.5;
                double bLen = r * 0.2;
                g.draw(new Line2D.Double(ex * 0.6, ey * 0.6,
                                          ex * 0.6 + Math.cos(ba) * bLen,
                                          ey * 0.6 + Math.sin(ba) * bLen));
            }
        }
    }

    private void drawLightning(Graphics2D g, double r) {
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        java.awt.geom.Path2D.Double bolt = new java.awt.geom.Path2D.Double();
        bolt.moveTo(-r * 0.15, -r * 0.5);
        bolt.lineTo( r * 0.05, -r * 0.1);
        bolt.lineTo(-r * 0.08, -r * 0.05);
        bolt.lineTo( r * 0.18,  r * 0.5);
        g.draw(bolt);
    }

    private void drawTwoX(Graphics2D g, double r) {
        g.setColor(new Color(255, 255, 255, 220));
        g.setFont(new Font("Arial Black", Font.BOLD, (int) (r * 0.7)));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth("x2");
        int th = fm.getAscent();
        g.drawString("x2", -tw / 2, th / 2 - 4);
    }

    /** Red strawberry shape inside the pill. */
    private void drawStrawberry(Graphics2D g, double r) {
        // Berry body: rounded teardrop
        java.awt.geom.Path2D.Double body = new java.awt.geom.Path2D.Double();
        body.moveTo(0, -r * 0.4);
        body.curveTo(r * 0.55, -r * 0.4, r * 0.6, r * 0.2, 0, r * 0.55);
        body.curveTo(-r * 0.6, r * 0.2, -r * 0.55, -r * 0.4, 0, -r * 0.4);
        body.closePath();
        g.setColor(new Color(231, 39, 60, 230));
        g.fill(body);
        g.setColor(new Color(140, 10, 30, 200));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(body);

        // Seed dots
        g.setColor(new Color(255, 240, 200, 230));
        for (int i = 0; i < 7; i++) {
            double a = i * 0.9;
            double px = Math.cos(a) * r * 0.22;
            double py = Math.sin(a) * r * 0.22;
            g.fill(new Ellipse2D.Double(px - 1.4, py - 1.4, 2.8, 2.8));
        }
        // Green leaf top
        g.setColor(new Color(46, 160, 67, 240));
        java.awt.geom.Path2D.Double leaf = new java.awt.geom.Path2D.Double();
        leaf.moveTo(-r * 0.32, -r * 0.4);
        leaf.lineTo(0, -r * 0.6);
        leaf.lineTo(r * 0.32, -r * 0.4);
        leaf.lineTo(r * 0.15, -r * 0.32);
        leaf.lineTo(0, -r * 0.45);
        leaf.lineTo(-r * 0.15, -r * 0.32);
        leaf.closePath();
        g.fill(leaf);
    }

    /** Peach silhouette with a small clock face overlay. */
    private void drawPeachClock(Graphics2D g, double r) {
        // Peach body (circle with a subtle cleft)
        g.setColor(new Color(255, 183, 153, 235));
        g.fill(new Ellipse2D.Double(-r * 0.5, -r * 0.45, r, r * 0.95));
        g.setColor(new Color(200, 90, 60, 200));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new Line2D.Double(0, -r * 0.42, 0, r * 0.45));
        // Tiny leaf
        g.setColor(new Color(80, 160, 70, 230));
        java.awt.geom.Path2D.Double leaf = new java.awt.geom.Path2D.Double();
        leaf.moveTo(-r * 0.05, -r * 0.45);
        leaf.curveTo(-r * 0.25, -r * 0.6, r * 0.05, -r * 0.65, r * 0.18, -r * 0.45);
        leaf.closePath();
        g.fill(leaf);
        // Clock face overlay (white circle + hands)
        double cr = r * 0.32;
        g.setColor(new Color(255, 255, 255, 230));
        g.fill(new Ellipse2D.Double(-cr, -cr + r * 0.05, cr * 2, cr * 2));
        g.setColor(new Color(60, 30, 20, 230));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(-cr, -cr + r * 0.05, cr * 2, cr * 2));
        // Hour + minute hands
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0, r * 0.05, 0, r * 0.05 - cr * 0.7));
        g.draw(new Line2D.Double(0, r * 0.05, cr * 0.55, r * 0.05));
    }

    /** Shield silhouette suggesting bomb deflection. */
    private void drawShield(Graphics2D g, double r) {
        java.awt.geom.Path2D.Double shield = new java.awt.geom.Path2D.Double();
        shield.moveTo(0, -r * 0.55);
        shield.lineTo(r * 0.5, -r * 0.4);
        shield.lineTo(r * 0.5, r * 0.1);
        shield.curveTo(r * 0.5, r * 0.5, 0, r * 0.6, 0, r * 0.6);
        shield.curveTo(0, r * 0.6, -r * 0.5, r * 0.5, -r * 0.5, r * 0.1);
        shield.lineTo(-r * 0.5, -r * 0.4);
        shield.closePath();
        g.setColor(new Color(220, 230, 240, 230));
        g.fill(shield);
        g.setColor(new Color(40, 60, 80, 230));
        g.setStroke(new BasicStroke(2f));
        g.draw(shield);
        // Center cross / chevron
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0, -r * 0.25, 0, r * 0.3));
        g.draw(new Line2D.Double(-r * 0.22, r * 0.0, r * 0.22, r * 0.0));
    }
}
