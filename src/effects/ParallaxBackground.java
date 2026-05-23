package effects;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

import game.Camera;

/**
 * Quiet, low-contrast backdrop. Pure dark gradient with a subtle warm
 * vignette in the lower third. No giant sun, no brush stroke, no decorative
 * shapes. The job of the background is to NOT compete with the gameplay.
 *
 * Camera still drifts to keep parallax math wired (March 10 lesson) but the
 * effect is so subtle it just gives the void a faint sense of breath.
 */
public class ParallaxBackground {

    private double timeSec;

    public void update(double deltaMs) {
        timeSec += deltaMs / 1000.0;
        Camera.x = Math.sin(timeSec * 0.06) * 40;
        Camera.y = Math.sin(timeSec * 0.04) * 14;
    }

    public void draw(Graphics2D g, int w, int h) {
        // Top half: cool deep ink. Bottom half: very slightly warmer black so
        // the playfield reads as "ground" not just floating in space.
        g.setPaint(new GradientPaint(
            0, 0,         new Color(8, 8, 12),
            0, (float) h, new Color(14, 8, 6)));
        g.fillRect(0, 0, w, h);

        // Subtle warm vignette down low, drifting with the camera. Reads as
        // distant ambient light instead of a dominant focal point.
        double ox = -Camera.x * 0.3;
        double oy = -Camera.y * 0.2;
        Point2D center = new Point2D.Double(w * 0.5 + ox, h * 0.95 + oy);
        float radius = (float) (Math.max(w, h) * 0.55);
        float[] dist = { 0f, 0.55f, 1f };
        Color[] cols = {
            new Color(80, 25, 20, 90),
            new Color(40, 14, 10, 30),
            new Color(0, 0, 0, 0),
        };
        g.setPaint(new RadialGradientPaint(center, radius, dist, cols));
        g.fillRect(0, 0, w, h);
    }
}
