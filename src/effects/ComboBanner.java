package effects;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * A big screen-centered banner that announces a multi-slice combo,
 * e.g. "FRUIT FRENZY +30!". Fades out and slides upward over about a second.
 */
public class ComboBanner {

    public final String text;
    public final int bonus;
    public double life = 1.0;
    public double age;
    public double scale = 0.4;

    public ComboBanner(String text, int bonus) {
        this.text = text;
        this.bonus = bonus;
    }

    public void update(double dtScale) {
        age += dtScale;
        // Lifetime ~ 60 frames @ 60 fps = 1 second.
        life -= 0.017 * dtScale;

        // Scale punch in then ease out
        double t = Math.min(1.0, age / 14.0);
        if (t < 0.4) {
            double u = t / 0.4;
            scale = 0.4 + (1.45 - 0.4) * (u * (2 - u));
        } else {
            double u = (t - 0.4) / 0.6;
            scale = 1.45 + (1.15 - 1.45) * (u * (2 - u));
        }
    }

    public boolean isDead() { return life <= 0; }

    /** Draws centered horizontally about 38% from the top of the screen. */
    public void draw(Graphics2D g, int w, int h) {
        AffineTransform saved = g.getTransform();

        float alpha = (float) Math.max(0f, Math.min(1f, life));
        int cx = w / 2;
        int cy = (int) (h * 0.38);

        g.translate(cx, cy);
        g.scale(scale, scale);

        Font big = new Font("Arial Black", Font.BOLD, 56);
        g.setFont(big);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);

        // Heavy ink shadow
        g.setColor(new Color(0, 0, 0, (int) (alpha * 220)));
        g.drawString(text, -tw / 2 + 5, 5);
        // Golden fill
        g.setColor(new Color(255, 210, 70, (int) (alpha * 255)));
        g.drawString(text, -tw / 2, 0);

        // "+bonus" subtitle in white below
        Font sub = new Font("Arial Black", Font.BOLD, 36);
        g.setFont(sub);
        java.awt.FontMetrics fm2 = g.getFontMetrics();
        String b = "+" + bonus;
        int bw = fm2.stringWidth(b);
        g.setColor(new Color(0, 0, 0, (int) (alpha * 200)));
        g.drawString(b, -bw / 2 + 3, 48 + 3);
        g.setColor(new Color(255, 255, 255, (int) (alpha * 255)));
        g.drawString(b, -bw / 2, 48);

        g.setTransform(saved);
    }
}
