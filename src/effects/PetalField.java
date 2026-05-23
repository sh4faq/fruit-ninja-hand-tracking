package effects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Subtle ink-dust motes drifting across the void. Sparse, dim, monochrome.
 *
 * Replaces the pink-petal field that fought with the new ink-samurai
 * aesthetic. The motes are tiny white dots at low alpha; only a few of them
 * are visible at once so they read as atmosphere rather than visual clutter.
 *
 * Same class name as before so callers (GamePanel) don't need to change.
 */
public class PetalField {

    private static final Random RNG = new Random();

    private static class Mote {
        double x, y;
        double vx, vy;
        double size;
        double wobble;
        double wobblePhase;
        float alpha;
    }

    private final List<Mote> motes = new ArrayList<>();
    private final int count;

    public PetalField(int count) {
        this.count = count;
    }

    public void seed(int width, int height) {
        motes.clear();
        for (int i = 0; i < count; i++) {
            Mote m = new Mote();
            m.x = RNG.nextDouble() * width;
            m.y = RNG.nextDouble() * height;
            initVelocity(m);
            motes.add(m);
        }
    }

    private void initVelocity(Mote m) {
        m.vx = -0.20 + RNG.nextDouble() * 0.40;
        m.vy = 0.10 + RNG.nextDouble() * 0.35;
        m.size = 1 + RNG.nextDouble() * 2.6;
        m.wobble = 0.15 + RNG.nextDouble() * 0.35;
        m.wobblePhase = RNG.nextDouble() * Math.PI * 2;
        m.alpha = 0.10f + RNG.nextFloat() * 0.20f;
    }

    public void update(double dtScale, int width, int height) {
        if (motes.isEmpty()) seed(width, height);

        for (Mote m : motes) {
            m.wobblePhase += 0.03 * dtScale;
            m.x += (m.vx + Math.sin(m.wobblePhase) * m.wobble) * dtScale;
            m.y += m.vy * dtScale;

            if (m.y > height + 20 || m.x < -40 || m.x > width + 40) {
                m.x = RNG.nextDouble() * width;
                m.y = -10 - RNG.nextDouble() * 60;
                initVelocity(m);
            }
        }
    }

    public void draw(Graphics2D g) {
        for (Mote m : motes) {
            int a = (int) (m.alpha * 255);
            g.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, a))));
            int s = (int) m.size;
            g.fillOval((int) m.x, (int) m.y, s, s);
        }
    }
}
