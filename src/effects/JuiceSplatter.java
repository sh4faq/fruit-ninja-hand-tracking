package effects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Burst of colored droplets fired outward from a slice point.
 *
 * The splatter is a tiny composition of Particles. It dies when all its
 * particles have decayed.
 */
public class JuiceSplatter {

    private final List<Particle> particles = new ArrayList<>();

    public JuiceSplatter(double x, double y, Color color) {
        for (int i = 0; i < 10; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    public void update(double dtScale) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(dtScale);
            if (p.isDead()) particles.remove(i);
        }
    }

    public boolean isDead() { return particles.isEmpty(); }

    public void draw(Graphics2D g) {
        for (Particle p : particles) p.draw(g);
    }
}
