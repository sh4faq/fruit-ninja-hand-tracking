package effects;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * A floating "+10" (or "x2 COMBO") that rises and fades after a successful
 * slice. Combos render larger and gold; regular slices render white.
 */
public class ScorePopup {

    public double x, y;
    public final int score;
    public final boolean isCombo;
    public double life = 1.0;
    public double vy = -3;
    public double scale;
    public double age;

    private final double startScale;
    private final double peakScale;
    private final double restScale;

    public ScorePopup(double x, double y, int score, boolean isCombo) {
        this.x = x;
        this.y = y;
        this.score = score;
        this.isCombo = isCombo;

        // Scale punch: spawn small, overshoot, settle back. Combos overshoot harder.
        this.startScale = isCombo ? 0.4 : 0.6;
        this.peakScale  = isCombo ? 1.8 : 1.3;
        this.restScale  = isCombo ? 1.5 : 1.0;
        this.scale = startScale;
    }

    public void update(double dtScale) {
        age += dtScale;
        y += vy * dtScale;
        vy *= Math.pow(0.95, dtScale);
        life -= 0.025 * dtScale;

        // Animated scale: easeOutBack-ish bounce-in over the first ~12 frames,
        // then settle to restScale.
        double t = Math.min(1.0, age / 12.0);
        if (t < 0.5) {
            // Punch up to peak
            double u = t * 2.0;
            scale = startScale + (peakScale - startScale) * easeOutQuad(u);
        } else {
            // Settle back to rest
            double u = (t - 0.5) * 2.0;
            scale = peakScale + (restScale - peakScale) * easeOutQuad(u);
        }
    }

    private static double easeOutQuad(double t) {
        return t * (2 - t);
    }

    public boolean isDead() { return life <= 0; }

    public void draw(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.scale(scale, scale);

        float alpha = (float) Math.max(0, Math.min(1, life));
        int fontSize = isCombo ? 38 : 28;
        g.setFont(new Font("Arial Black", Font.BOLD, fontSize));

        String text = "+" + score;
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);

        // Drop shadow
        g.setColor(new Color(0, 0, 0, (int) (alpha * 150)));
        g.drawString(text, -tw / 2 + 2, 2);

        // Main text
        if (isCombo) {
            g.setColor(new Color(255, 204, 0, (int) (alpha * 255)));
        } else {
            g.setColor(new Color(255, 255, 255, (int) (alpha * 255)));
        }
        g.drawString(text, -tw / 2, 0);

        g.setTransform(saved);
    }
}
