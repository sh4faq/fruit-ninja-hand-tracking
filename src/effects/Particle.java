package effects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * A single particle. Tiny square that drifts under gravity until it fades out.
 *
 * Decay reduces life to zero; once dead the particle is removed by the manager.
 */
public class Particle {

    private static final Random RNG = new Random();

    public double x, y;
    public double vx, vy;
    public double gravity;
    public double size;
    public double life;
    public double decay;
    public final Color color;

    public Particle(double x, double y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.size = 4 + RNG.nextDouble() * 6;
        this.vx = (RNG.nextDouble() - 0.5) * 14;
        this.vy = -4 + RNG.nextDouble() * 10 - 6;
        this.gravity = 0.4;
        this.life = 1.0;
        this.decay = 0.02 + RNG.nextDouble() * 0.03;
    }

    public void update(double dtScale) {
        vy += gravity * dtScale;
        x += vx * dtScale;
        y += vy * dtScale;
        life -= decay * dtScale;
    }

    public boolean isDead() { return life <= 0; }

    public void draw(Graphics2D g) {
        float a = (float) Math.max(0, Math.min(1, life));
        g.setColor(new Color(color.getRed() / 255f, color.getGreen() / 255f,
                              color.getBlue() / 255f, a));
        int s = (int) size;
        g.fillRect((int) (x - s / 2.0), (int) (y - s / 2.0), s, s);
    }
}
