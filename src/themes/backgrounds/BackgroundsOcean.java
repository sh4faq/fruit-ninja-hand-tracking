package themes.backgrounds;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.Random;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/**
 * Ocean / aquatic background family. Procedurally drawn surface and
 * underwater scenes. Five variants registered under bg_021 .. bg_025.
 */
public final class BackgroundsOcean {

    private BackgroundsOcean() {}

    // Variant kinds drive the rendering branches.
    private static final int KIND_LAGOON  = 0;
    private static final int KIND_TRENCH  = 1;
    private static final int KIND_STORM   = 2;
    private static final int KIND_REEF    = 3;
    private static final int KIND_MOONLIT = 4;

    static {
        BackgroundCatalog.register(new Variant(
            "bg_021", "Tropic Lagoon", 350,
            "Crystal water and palms.",
            KIND_LAGOON, new Color(64, 224, 208)));
        BackgroundCatalog.register(new Variant(
            "bg_022", "Deep Trench", 600,
            "Dark fathoms below.",
            KIND_TRENCH, new Color(20, 40, 110)));
        BackgroundCatalog.register(new Variant(
            "bg_023", "Storm Sea", 900,
            "Lightning over breakers.",
            KIND_STORM, new Color(120, 110, 150)));
        BackgroundCatalog.register(new Variant(
            "bg_024", "Coral Reef", 1300,
            "Color and life underwater.",
            KIND_REEF, new Color(255, 110, 130)));
        BackgroundCatalog.register(new Variant(
            "bg_025", "Moonlit Tide", 1900,
            "Silver waves under moon.",
            KIND_MOONLIT, new Color(190, 210, 235)));
    }

    // ====================================================================
    //  ABSTRACT BASE
    // ====================================================================

    static abstract class AbstractOcean implements Background {

        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final int kind;
        protected final Color accent;

        protected double timeSec;
        protected double flashTimer;     // for Storm lightning
        protected double nextFlashIn;
        protected final Random rng = new Random(0xC0FFEEL);

        // Particle pool (reused for rain, bubbles, fish, glints, leaves)
        protected final double[] px = new double[60];
        protected final double[] py = new double[60];
        protected final double[] pv = new double[60];   // velocity / phase
        protected final int[]    pk = new int[60];      // sub-kind / color idx

        AbstractOcean(String id, String name, int price, String tagline,
                      int kind, Color accent) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.kind = kind;
            this.accent = accent;
            nextFlashIn = 2.0 + rng.nextDouble() * 4.0;
            for (int i = 0; i < px.length; i++) {
                px[i] = rng.nextDouble() * 1600;
                py[i] = rng.nextDouble() * 900;
                pv[i] = 0.4 + rng.nextDouble() * 1.6;
                pk[i] = rng.nextInt(5);
            }
        }

        @Override public String id()           { return id; }
        @Override public String name()         { return name; }
        @Override public int price()           { return price; }
        @Override public String tagline()      { return tagline; }
        @Override public Color previewAccent() { return accent; }

        @Override
        public void update(double deltaMs) {
            double dt = deltaMs / 1000.0;
            timeSec += dt;
            // Monotonic forward scroll, not sine oscillation.
            Camera.x = timeSec * 8.0;

            if (kind == KIND_STORM) {
                if (flashTimer > 0) flashTimer -= dt;
                nextFlashIn -= dt;
                if (nextFlashIn <= 0) {
                    flashTimer = 0.10 + rng.nextDouble() * 0.18;
                    nextFlashIn = 3.0 + rng.nextDouble() * 5.0;
                }
            }
        }

        // ----------------------------------------------------------------
        //  MAIN DRAW
        // ----------------------------------------------------------------
        @Override
        public void draw(Graphics2D g, int w, int h) {
            drawSkyOrDeep(g, w, h);
            drawMidLayer(g, w, h);
            drawForeground(g, w, h);
            drawParticles(g, w, h);

            if (kind == KIND_STORM && flashTimer > 0) {
                int a = (int) (160 * Math.min(1.0, flashTimer / 0.10));
                g.setColor(new Color(255, 255, 255, a));
                g.fillRect(0, 0, w, h);
            }
        }

        // Layer 1: gradient
        private void drawSkyOrDeep(Graphics2D g, int w, int h) {
            Color top, bot;
            switch (kind) {
                case KIND_LAGOON:
                    top = new Color(150, 220, 255); bot = new Color(70, 200, 200); break;
                case KIND_TRENCH:
                    top = new Color(8, 24, 60);     bot = new Color(2, 6, 18);     break;
                case KIND_STORM:
                    top = new Color(45, 45, 70);    bot = new Color(20, 25, 50);   break;
                case KIND_REEF:
                    top = new Color(40, 130, 180);  bot = new Color(15, 60, 110);  break;
                case KIND_MOONLIT:
                default:
                    top = new Color(20, 35, 70);    bot = new Color(60, 80, 130);  break;
            }
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g.fillRect(0, 0, w, h);

            if (kind == KIND_MOONLIT) {
                int mx = (int) (w * 0.72), my = (int) (h * 0.25), mr = 60;
                g.setColor(new Color(255, 255, 245, 60));
                g.fillOval(mx - mr - 12, my - mr - 12, (mr + 12) * 2, (mr + 12) * 2);
                g.setColor(new Color(245, 245, 230));
                g.fillOval(mx - mr, my - mr, mr * 2, mr * 2);
            }
        }

        // Layer 2: wavy surface or seafloor
        private void drawMidLayer(Graphics2D g, int w, int h) {
            boolean underwater = (kind == KIND_TRENCH || kind == KIND_REEF);
            double horizon = underwater ? h * 0.82 : h * 0.55;
            double cx = Camera.x * 0.15;

            Path2D path = new Path2D.Double();
            path.moveTo(0, h);
            path.lineTo(0, horizon);
            for (int x = 0; x <= w; x += 6) {
                double t = (x + cx) * 0.012;
                double y = horizon
                        + Math.sin(t + timeSec * 0.8) * 14
                        + Math.sin(t * 2.3 + timeSec * 1.1) * 6;
                path.lineTo(x, y);
            }
            path.lineTo(w, h);
            path.closePath();

            Color midTop, midBot;
            switch (kind) {
                case KIND_LAGOON:
                    midTop = new Color(40, 170, 175); midBot = new Color(10, 90, 110); break;
                case KIND_TRENCH:
                    midTop = new Color(4, 12, 30);    midBot = new Color(0, 2, 8);     break;
                case KIND_STORM:
                    midTop = new Color(30, 35, 55);   midBot = new Color(10, 12, 28);  break;
                case KIND_REEF:
                    midTop = new Color(20, 50, 80);   midBot = new Color(8, 25, 50);   break;
                case KIND_MOONLIT:
                default:
                    midTop = new Color(30, 50, 95);   midBot = new Color(10, 20, 50);  break;
            }
            g.setPaint(new GradientPaint(0, (float) horizon, midTop, 0, h, midBot));
            g.fill(path);
        }

        // Layer 3: lapping wave arcs, coral, palms, etc.
        private void drawForeground(Graphics2D g, int w, int h) {
            Stroke saved = g.getStroke();
            switch (kind) {
                case KIND_TRENCH:
                case KIND_REEF:
                    drawCoral(g, w, h);
                    break;
                case KIND_LAGOON:
                    drawWaveArcs(g, w, h, new Color(255, 255, 255, 110));
                    drawPalms(g, w, h);
                    break;
                case KIND_STORM:
                    drawWaveArcs(g, w, h, new Color(200, 210, 230, 130));
                    break;
                case KIND_MOONLIT:
                    drawWaveArcs(g, w, h, new Color(220, 230, 255, 140));
                    break;
            }
            g.setStroke(saved);
        }

        private void drawWaveArcs(Graphics2D g, int w, int h, Color c) {
            double horizon = h * 0.55;
            g.setColor(c);
            g.setStroke(new BasicStroke(2f));
            for (int row = 0; row < 4; row++) {
                double y = horizon + 20 + row * 22;
                double phase = timeSec * 1.2 + row * 0.7 + Camera.x * 0.004;
                for (int x = -20; x < w + 20; x += 36) {
                    double off = Math.sin((x * 0.04) + phase) * 4;
                    g.drawArc(x, (int) (y + off), 24, 8, 0, 180);
                }
            }
        }

        private void drawPalms(Graphics2D g, int w, int h) {
            drawPalm(g, 30 - (int)(Camera.x * 0.05), h, 1);
            drawPalm(g, w - 30 - (int)(Camera.x * 0.05), h, -1);
        }

        private void drawPalm(Graphics2D g, int baseX, int h, int dir) {
            g.setColor(new Color(15, 25, 25));
            g.setStroke(new BasicStroke(6f));
            int trunkH = (int)(h * 0.55);
            int topY = h - trunkH;
            Path2D trunk = new Path2D.Double();
            trunk.moveTo(baseX, h);
            trunk.quadTo(baseX + dir * 18, h - trunkH * 0.5, baseX + dir * 8, topY);
            g.draw(trunk);
            g.setStroke(new BasicStroke(3f));
            for (int i = 0; i < 6; i++) {
                double ang = Math.PI * (0.15 + i * 0.13) * (dir > 0 ? 1 : -1);
                int lx = baseX + dir * 8;
                int ex = lx + (int)(Math.cos(ang) * 55 * dir);
                int ey = topY + (int)(Math.sin(ang) * 35) - 5;
                Path2D leaf = new Path2D.Double();
                leaf.moveTo(lx, topY);
                leaf.quadTo((lx + ex) / 2.0, topY - 20, ex, ey);
                g.draw(leaf);
            }
        }

        // Deterministic hash maps a world slot to stable double in [0,1).
        private double slotHash(int slot, int salt) {
            long x = ((long) slot * 2654435761L) ^ ((long) salt * 0x9E3779B97F4A7C15L);
            x ^= (x >>> 33);
            x *= 0xff51afd7ed558ccdL;
            x ^= (x >>> 33);
            x *= 0xc4ceb9fe1a85ec53L;
            x ^= (x >>> 33);
            return (x >>> 11) / (double) (1L << 53);
        }

        private void drawCoral(Graphics2D g, int w, int h) {
            Color[] corals = (kind == KIND_REEF)
                ? new Color[] { new Color(255, 110, 130), new Color(255, 170, 80),
                                new Color(150, 90, 200),  new Color(80, 200, 180) }
                : new Color[] { new Color(20, 50, 70),    new Color(15, 30, 50),
                                new Color(25, 40, 60),    new Color(10, 25, 40) };
            // World-slot scatter so coral never visibly repeats.
            double offset = Camera.x * 0.2;
            double cellW = 150.0;
            int leftSlot = (int) Math.floor(offset / cellW) - 1;
            int slots = (int) Math.ceil(w / cellW) + 3;
            int baseY = (int)(h * 0.85);
            for (int k = 0; k < slots; k++) {
                int idx = leftSlot + k;
                double localX = slotHash(idx, 0x10) * cellW;
                int x = (int) (idx * cellW - offset + localX);
                if (x < -60 || x > w + 60) continue;
                int colorIdx = (int) (slotHash(idx, 0x20) * corals.length);
                if (colorIdx >= corals.length) colorIdx = corals.length - 1;
                g.setColor(corals[colorIdx]);
                for (int b = 0; b < 5; b++) {
                    int bx = x + (b - 2) * 8;
                    int bh = 30 + (int) (slotHash(idx, 0x30 + b) * 50.0);
                    g.fillOval(bx - 6, baseY - bh, 12, bh + 12);
                }
            }
        }

        // ----------------------------------------------------------------
        //  PARTICLES
        // ----------------------------------------------------------------
        private void drawParticles(Graphics2D g, int w, int h) {
            switch (kind) {
                case KIND_STORM:   drawRain(g, w, h);     break;
                case KIND_MOONLIT: drawGlints(g, w, h);   break;
                case KIND_TRENCH:  drawBubbles(g, w, h);  break;
                case KIND_REEF:    drawFish(g, w, h);     break;
                case KIND_LAGOON:  /* palms are in fg */  break;
            }
        }

        private void drawRain(Graphics2D g, int w, int h) {
            g.setColor(new Color(200, 210, 235, 170));
            g.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i < px.length; i++) {
                px[i] += pv[i] * 5;
                py[i] += pv[i] * 18;
                if (py[i] > h) { py[i] = -10; px[i] = rng.nextDouble() * (w + 100) - 50; }
                if (px[i] > w + 20) px[i] -= w + 40;
                g.drawLine((int) px[i], (int) py[i],
                           (int) (px[i] - 6), (int) (py[i] - 16));
            }
        }

        private void drawGlints(Graphics2D g, int w, int h) {
            double horizon = h * 0.55;
            g.setColor(new Color(230, 240, 255, 220));
            // Per-slot moonlight glints so they don't visibly loop.
            double offset = Camera.x * 0.3;
            double cellW = 32.0;
            int leftSlot = (int) Math.floor(offset / cellW) - 1;
            int slots = (int) Math.ceil(w / cellW) + 3;
            for (int k = 0; k < slots; k++) {
                int idx = leftSlot + k;
                double localX = slotHash(idx, 0x40) * cellW;
                int x = (int) (idx * cellW - offset + localX);
                if (x < -4 || x > w + 4) continue;
                double t = timeSec * 0.6 + slotHash(idx, 0x50) * 6.28;
                int y = (int) (horizon + 25 + Math.sin(t) * 18
                               + slotHash(idx, 0x60) * 48.0);
                double s = 0.5 + 0.5 * Math.sin(timeSec * 3 + idx);
                int r = (int) (1 + s * 2);
                g.fillOval(x, y, r * 2, r);
            }
        }

        private void drawBubbles(Graphics2D g, int w, int h) {
            for (int i = 0; i < px.length; i++) {
                py[i] -= pv[i] * 0.6;
                px[i] += Math.sin(timeSec + i) * 0.4;
                if (py[i] < -10) { py[i] = h + 10; px[i] = rng.nextDouble() * w; }
                int r = 2 + (pk[i] % 4);
                g.setColor(new Color(180, 220, 240, 90));
                g.fillOval((int) px[i], (int) py[i], r * 2, r * 2);
                g.setColor(new Color(230, 245, 255, 160));
                g.drawOval((int) px[i], (int) py[i], r * 2, r * 2);
            }
        }

        private void drawFish(Graphics2D g, int w, int h) {
            Color[] cols = { new Color(255, 180, 80), new Color(255, 110, 130),
                             new Color(120, 220, 255), new Color(255, 240, 120),
                             new Color(180, 120, 220) };
            for (int i = 0; i < 18; i++) {
                px[i] += pv[i] * 0.9;
                if (px[i] > w + 30) { px[i] = -30; py[i] = rng.nextDouble() * h * 0.75; }
                double bob = Math.sin(timeSec * 2 + i) * 3;
                int x = (int) px[i], y = (int) (py[i] + bob);
                g.setColor(cols[pk[i] % cols.length]);
                g.fillOval(x, y, 14, 7);
                int[] tx = { x, x - 5, x - 5 };
                int[] ty = { y + 3, y, y + 7 };
                g.fillPolygon(tx, ty, 3);
            }
        }

        // ----------------------------------------------------------------
        //  PREVIEW THUMBNAIL
        // ----------------------------------------------------------------
        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);

            Color top, bot;
            switch (kind) {
                case KIND_LAGOON:
                    top = new Color(150, 220, 255); bot = new Color(40, 170, 175); break;
                case KIND_TRENCH:
                    top = new Color(8, 24, 60);     bot = new Color(0, 2, 8);      break;
                case KIND_STORM:
                    top = new Color(45, 45, 70);    bot = new Color(20, 25, 50);   break;
                case KIND_REEF:
                    top = new Color(40, 130, 180);  bot = new Color(15, 60, 110);  break;
                case KIND_MOONLIT:
                default:
                    top = new Color(20, 35, 70);    bot = new Color(60, 80, 130);  break;
            }
            g.setPaint(new GradientPaint(x, y, top, x, y + h, bot));
            g.fillRect(x, y, w, h);

            // Wave line
            double horizon = y + h * 0.6;
            g.setColor(new Color(255, 255, 255, 140));
            g.setStroke(new BasicStroke(1.5f));
            Path2D wave = new Path2D.Double();
            wave.moveTo(x, horizon);
            for (int i = 0; i <= w; i += 4) {
                double yy = horizon + Math.sin(i * 0.18 + timeSec * 2) * 3;
                wave.lineTo(x + i, yy);
            }
            g.draw(wave);

            // Particle hints
            g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 220));
            if (kind == KIND_STORM) {
                for (int i = 0; i < 5; i++)
                    g.drawLine(x + 5 + i * 9, y + 4 + i * 3,
                               x + 2 + i * 9, y + 14 + i * 3);
            } else if (kind == KIND_MOONLIT) {
                g.fillOval(x + w - 18, y + 6, 10, 10);
                for (int i = 0; i < 4; i++)
                    g.fillOval(x + 6 + i * 11, (int) horizon + 4, 3, 2);
            } else if (kind == KIND_TRENCH) {
                for (int i = 0; i < 4; i++)
                    g.fillOval(x + 8 + i * 12, y + h - 10 - i * 4, 4, 4);
            } else if (kind == KIND_REEF) {
                for (int i = 0; i < 3; i++)
                    g.fillOval(x + 6 + i * 14, y + h - 14, 8, 4);
            } else { // lagoon
                g.setColor(new Color(20, 30, 30));
                g.fillRect(x + 4, y + h - 18, 2, 14);
                g.fillRect(x + w - 6, y + h - 18, 2, 14);
            }

            g.setClip(oldClip);
        }
    }

    private static final class Variant extends AbstractOcean {
        Variant(String id, String name, int price, String tagline,
                int kind, Color accent) {
            super(id, name, price, tagline, kind, accent);
        }
    }
}
