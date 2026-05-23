package themes.backgrounds;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/**
 * Forest family. 100% procedural - no bitmap assets. Each variant renders
 * three parallax bands (sky gradient, mid silhouette tree-line, foreground
 * trees) plus drifting accent-colored particles. 5 variants under
 * bg_006 .. bg_010.
 */
public final class BackgroundsForest {

    private BackgroundsForest() {}

    static {
        BackgroundCatalog.register(new Variant(
            "bg_006", "Bamboo Grove", 300,
            "Tall stalks at dawn.",
            new Color(255, 220, 170), new Color(180, 210, 150),
            new Color(40, 80, 50),    new Color(20, 50, 30),
            new Color(110, 200, 90),  ParticleKind.BAMBOO_LEAF));
        BackgroundCatalog.register(new Variant(
            "bg_007", "Autumn Maple", 500,
            "Red and gold leaves drift.",
            new Color(255, 180, 120), new Color(200, 90, 60),
            new Color(110, 50, 30),   new Color(70, 25, 15),
            new Color(220, 70, 40),   ParticleKind.MAPLE_LEAF));
        BackgroundCatalog.register(new Variant(
            "bg_008", "Sakura Path", 800,
            "Cherry petals falling.",
            new Color(255, 220, 230), new Color(255, 170, 200),
            new Color(140, 80, 110),  new Color(80, 40, 70),
            new Color(255, 180, 210), ParticleKind.PETAL));
        BackgroundCatalog.register(new Variant(
            "bg_009", "Pine Twilight", 1100,
            "Deep evergreen at dusk.",
            new Color(60, 70, 110),   new Color(30, 50, 70),
            new Color(15, 30, 40),    new Color(5, 15, 25),
            new Color(20, 90, 100),   ParticleKind.NEEDLE));
        BackgroundCatalog.register(new Variant(
            "bg_010", "Jungle Mist", 1700,
            "Tropical, humid, alive.",
            new Color(180, 220, 180), new Color(110, 180, 140),
            new Color(20, 70, 50),    new Color(10, 40, 25),
            new Color(40, 180, 110),  ParticleKind.VINE));
    }

    // ====================================================================
    //  PARTICLE KINDS
    // ====================================================================

    enum ParticleKind { BAMBOO_LEAF, MAPLE_LEAF, PETAL, NEEDLE, VINE }

    private static final class Particle {
        double x, y, baseX, vy, phase, size;
    }

    // ====================================================================
    //  ABSTRACT BASE
    // ====================================================================

    static abstract class AbstractForest implements Background {

        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color skyTop;
        protected final Color skyBottom;
        protected final Color midSilhouette;
        protected final Color fgSilhouette;
        protected final Color accent;
        protected final ParticleKind kind;

        // Per-instance state (NOT static)
        private double timeSec;
        private final List<Particle> particles = new ArrayList<Particle>();
        private final Random rng = new Random();
        private int lastW = 1280, lastH = 720;
        private boolean particlesSeeded;

        // Cached silhouette path layout
        private final double[] midPeaks   = new double[24];
        private final double[] fgTrunkX   = new double[10];
        private final double[] fgTrunkH   = new double[10];
        private boolean layoutSeeded;

        AbstractForest(String id, String name, int price, String tagline,
                       Color skyTop, Color skyBottom,
                       Color midSilhouette, Color fgSilhouette,
                       Color accent, ParticleKind kind) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.skyTop = skyTop;
            this.skyBottom = skyBottom;
            this.midSilhouette = midSilhouette;
            this.fgSilhouette = fgSilhouette;
            this.accent = accent;
            this.kind = kind;
        }

        @Override public String id()           { return id; }
        @Override public String name()         { return name; }
        @Override public int price()           { return price; }
        @Override public String tagline()      { return tagline; }
        @Override public Color previewAccent() { return accent; }

        private void seedLayout() {
            if (layoutSeeded) return;
            layoutSeeded = true;
            Random r = new Random(id.hashCode());
            for (int i = 0; i < midPeaks.length; i++) {
                midPeaks[i] = 0.35 + r.nextDouble() * 0.35;
            }
            for (int i = 0; i < fgTrunkX.length; i++) {
                fgTrunkX[i] = (i + r.nextDouble() * 0.6) / fgTrunkX.length;
                fgTrunkH[i] = 0.30 + r.nextDouble() * 0.25;
            }
        }

        private void seedParticles(int w, int h) {
            particles.clear();
            int count = 40;
            for (int i = 0; i < count; i++) {
                Particle p = new Particle();
                p.baseX = rng.nextDouble() * w;
                p.x = p.baseX;
                p.y = rng.nextDouble() * h;
                p.vy = 20 + rng.nextDouble() * 40;
                p.phase = rng.nextDouble() * Math.PI * 2;
                p.size = 4 + rng.nextDouble() * 6;
                particles.add(p);
            }
            particlesSeeded = true;
        }

        @Override
        public void update(double deltaMs) {
            double dt = deltaMs / 1000.0;
            timeSec += dt;
            // Monotonic forward scroll, not sine oscillation.
            Camera.x = timeSec * 10.0;

            if (!particlesSeeded) seedParticles(lastW, lastH);
            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                p.y += p.vy * dt;
                p.x = p.baseX + Math.sin(timeSec * 1.3 + p.phase + i * 0.21) * 25;
                if (p.y > lastH + 20) {
                    p.y = -20;
                    p.baseX = rng.nextDouble() * lastW;
                }
            }
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            lastW = w;
            lastH = h;
            seedLayout();
            if (!particlesSeeded) seedParticles(w, h);

            // ---- Layer 1: sky (z = 500, no parallax basically) ----
            GradientPaint sky = new GradientPaint(
                0, 0, skyTop, 0, h, skyBottom);
            g.setPaint(sky);
            g.fillRect(0, 0, w, h);

            // Soft sun/moon disk for atmosphere
            int sunX = (int) (w * 0.78 - Camera.x / 500.0 * w * 0.05);
            int sunY = (int) (h * 0.25);
            int sunR = (int) (h * 0.10);
            g.setColor(new Color(
                Math.min(255, accent.getRed() + 60),
                Math.min(255, accent.getGreen() + 60),
                Math.min(255, accent.getBlue() + 60), 90));
            g.fillOval(sunX - sunR, sunY - sunR, sunR * 2, sunR * 2);

            // ---- Layer 2: mid silhouette tree-line (z = 200) ----
            double midOffset = Camera.x / 200.0 * w * 0.15;
            drawMidLine(g, w, h, midOffset);

            // ---- Layer 3: foreground trees (z = 80) ----
            double fgOffset = Camera.x / 80.0 * w * 0.35;
            drawForeground(g, w, h, fgOffset);

            // ---- Particles ----
            drawParticles(g, w, h);
        }

        // Deterministic hash: maps slot index to a stable double in [0,1).
        // Avoids modulo wrap so slots scrolling past have unique parameters.
        private double slotHash(int slot, int salt) {
            long x = ((long) slot * 2654435761L) ^ ((long) salt * 0x9E3779B97F4A7C15L);
            x ^= (x >>> 33);
            x *= 0xff51afd7ed558ccdL;
            x ^= (x >>> 33);
            x *= 0xc4ceb9fe1a85ec53L;
            x ^= (x >>> 33);
            return (x >>> 11) / (double) (1L << 53);
        }

        private void drawMidLine(Graphics2D g, int w, int h, double offset) {
            Path2D path = new Path2D.Double();
            double baseY = h * 0.62;
            double step = w / 8.0;
            int leftSlot = (int) Math.floor(offset / step) - 1;
            int slots = (int) Math.ceil(w / step) + 3;

            double startX = leftSlot * step - offset;
            double startPeak = baseY - slotHash(leftSlot, 0xA) * h * 0.18;
            path.moveTo(startX, h);
            path.lineTo(startX, startPeak);
            for (int k = 1; k < slots; k++) {
                int idx = leftSlot + k;
                double px = idx * step - offset;
                double peakY = baseY - slotHash(idx, 0xA) * h * 0.18;
                double cx = px - step * 0.5;
                double cy = baseY - slotHash(idx, 0xB) * h * 0.25;
                path.quadTo(cx, cy, px, peakY);
            }
            path.lineTo((leftSlot + slots - 1) * step - offset, h);
            path.closePath();
            g.setColor(midSilhouette);
            g.fill(path);
        }

        private void drawForeground(Graphics2D g, int w, int h, double offset) {
            // Ground band
            g.setColor(fgSilhouette);
            int groundY = (int) (h * 0.82);
            g.fillRect(0, groundY, w, h - groundY);

            // Trees scattered across deterministic world slots.
            // No fixed array: each slot is hashed for unique trunk + height,
            // so as offset grows trees scroll past forever without repeating.
            double step = w / 6.0;
            int leftSlot = (int) Math.floor(offset / step) - 1;
            int slots = (int) Math.ceil(w / step) + 3;
            int trunkW = Math.max(6, (int) (w * 0.012));

            for (int k = 0; k < slots; k++) {
                int idx = leftSlot + k;
                double slotX = idx * step - offset;
                double jitter = (slotHash(idx, 0x1) - 0.5) * step * 0.7;
                double cx = slotX + step * 0.5 + jitter;
                double trunkH = h * (0.30 + slotHash(idx, 0x2) * 0.25);
                int trunkX = (int) cx - trunkW / 2;
                int trunkY = (int) (groundY - trunkH);
                g.setColor(fgSilhouette.darker());
                g.fillRect(trunkX, trunkY, trunkW, (int) trunkH);
                drawFoliage(g, (int) cx, trunkY, (int) (trunkH * 0.55));
            }
        }

        private void drawFoliage(Graphics2D g, int cx, int topY, int radius) {
            Color leafA = new Color(
                accent.getRed(), accent.getGreen(), accent.getBlue(), 220);
            Color leafB = new Color(
                Math.max(0, accent.getRed() - 30),
                Math.max(0, accent.getGreen() - 30),
                Math.max(0, accent.getBlue() - 30), 200);
            g.setColor(leafB);
            g.fillOval(cx - radius, topY - radius, radius * 2, radius * 2);
            g.setColor(leafA);
            int r2 = (int) (radius * 0.75);
            g.fillOval(cx - r2 - radius / 3, topY - r2, r2 * 2, r2 * 2);
            g.fillOval(cx - r2 + radius / 3, topY - r2 + radius / 4,
                r2 * 2, r2 * 2);
        }

        private void drawParticles(Graphics2D g, int w, int h) {
            Color pc = new Color(
                accent.getRed(), accent.getGreen(), accent.getBlue(), 140);
            g.setColor(pc);
            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                drawParticleShape(g, p);
            }
        }

        private void drawParticleShape(Graphics2D g, Particle p) {
            int s = (int) p.size;
            int px = (int) p.x;
            int py = (int) p.y;
            switch (kind) {
                case BAMBOO_LEAF:
                    g.fillOval(px - s, py - s / 3, s * 2, Math.max(2, s / 2));
                    break;
                case MAPLE_LEAF:
                    Path2D leaf = new Path2D.Double();
                    leaf.moveTo(px, py - s);
                    leaf.lineTo(px + s, py);
                    leaf.lineTo(px + s / 2, py + s / 2);
                    leaf.lineTo(px, py + s);
                    leaf.lineTo(px - s / 2, py + s / 2);
                    leaf.lineTo(px - s, py);
                    leaf.closePath();
                    g.fill(leaf);
                    break;
                case PETAL:
                    g.fillOval(px - s / 2, py - s, s, s * 2);
                    break;
                case NEEDLE:
                    g.fillRect(px, py, 1, s);
                    g.fillRect(px - 1, py, 2, Math.max(2, s - 2));
                    break;
                case VINE:
                    g.fillOval(px - s, py - 1, s * 2, 2);
                    g.fillOval(px - 1, py - s / 2, 2, s);
                    break;
            }
        }

        // ====================================================================
        //  PREVIEW
        // ====================================================================

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);

            // Sky band
            GradientPaint sky = new GradientPaint(
                x, y, skyTop, x, y + h, skyBottom);
            g.setPaint(sky);
            g.fillRect(x, y, w, h);

            // Mid mountain band
            Path2D mid = new Path2D.Double();
            double baseY = y + h * 0.62;
            mid.moveTo(x, y + h);
            int n = 8;
            for (int i = 0; i <= n; i++) {
                double px = x + (double) i * w / n;
                double hh = 0.30 + ((i * 37) % 17) / 17.0 * 0.30;
                double py = baseY - hh * h * 0.20;
                mid.lineTo(px, py);
            }
            mid.lineTo(x + w, y + h);
            mid.closePath();
            g.setColor(midSilhouette);
            g.fill(mid);

            // Ground + trees
            g.setColor(fgSilhouette);
            int groundY = (int) (y + h * 0.82);
            g.fillRect(x, groundY, w, (y + h) - groundY);

            int trees = 4;
            for (int i = 0; i < trees; i++) {
                int cx = x + (int) ((i + 0.5) * w / trees);
                int trunkH = (int) (h * 0.30);
                int trunkW = Math.max(2, w / 30);
                g.setColor(fgSilhouette.darker());
                g.fillRect(cx - trunkW / 2, groundY - trunkH, trunkW, trunkH);
                int r = Math.max(4, w / 12);
                g.setColor(new Color(
                    accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
                g.fillOval(cx - r, groundY - trunkH - r, r * 2, r * 2);
            }

            // A couple of particles
            g.setColor(new Color(
                accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
            Particle p1 = new Particle();
            p1.x = x + w * 0.25; p1.y = y + h * 0.30; p1.size = Math.max(3, w / 30.0);
            Particle p2 = new Particle();
            p2.x = x + w * 0.65; p2.y = y + h * 0.45; p2.size = Math.max(3, w / 30.0);
            drawParticleShape(g, p1);
            drawParticleShape(g, p2);

            g.setClip(oldClip);
        }
    }

    // ====================================================================
    //  VARIANT
    // ====================================================================

    private static final class Variant extends AbstractForest {
        Variant(String id, String name, int price, String tagline,
                Color skyTop, Color skyBottom,
                Color midSilhouette, Color fgSilhouette,
                Color accent, ParticleKind kind) {
            super(id, name, price, tagline, skyTop, skyBottom,
                midSilhouette, fgSilhouette, accent, kind);
        }
    }
}
