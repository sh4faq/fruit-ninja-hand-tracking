package themes.backgrounds;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Random;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/** Cosmic / Space background family. Procedural, bg_011..bg_015. */
public final class BackgroundsCosmic {

    private BackgroundsCosmic() {}

    // Style identifiers for variant-specific extras
    private static final int STYLE_PLAIN   = 0;
    private static final int STYLE_NEBULA  = 1;
    private static final int STYLE_SPIRAL  = 2;
    private static final int STYLE_HOLE    = 3;
    private static final int STYLE_AURORA  = 4;

    static {
        BackgroundCatalog.register(new Variant(
            "bg_011", "Starfield", 400,
            "A million silent stars",
            new Color(255, 255, 255),
            new Color(255, 255, 255, 40),
            STYLE_PLAIN));
        BackgroundCatalog.register(new Variant(
            "bg_012", "Nebula Drift", 700,
            "Pink and purple cloud",
            new Color(230, 70, 200),
            new Color(230, 70, 200, 70),
            STYLE_NEBULA));
        BackgroundCatalog.register(new Variant(
            "bg_013", "Galaxy Spiral", 1100,
            "Whole arms of stars",
            new Color(90, 150, 255),
            new Color(90, 150, 255, 60),
            STYLE_SPIRAL));
        BackgroundCatalog.register(new Variant(
            "bg_014", "Black Hole", 1700,
            "Light bends here",
            new Color(120, 60, 200),
            new Color(120, 60, 200, 80),
            STYLE_HOLE));
        BackgroundCatalog.register(new Variant(
            "bg_015", "Aurora Cosmos", 2500,
            "Northern lights in space",
            new Color(80, 230, 180),
            new Color(80, 230, 180, 70),
            STYLE_AURORA));
    }

    // ====================================================================
    //  ABSTRACT BASE
    // ====================================================================

    static abstract class AbstractCosmic implements Background {

        private static final int DEEP_STAR_COUNT  = 220;
        private static final int NEAR_STAR_COUNT  = 11;
        private static final int NEBULA_BLOB_COUNT = 5;

        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color accent;
        protected final Color nebulaColor;
        protected final int style;

        // Deep stars: parallel arrays for cache friendliness
        private final float[] dStarX  = new float[DEEP_STAR_COUNT];
        private final float[] dStarY  = new float[DEEP_STAR_COUNT];
        private final float[] dStarR  = new float[DEEP_STAR_COUNT];
        private final float[] dStarSp = new float[DEEP_STAR_COUNT];
        private final float[] dStarPh = new float[DEEP_STAR_COUNT];

        // Near stars
        private final float[] nStarX  = new float[NEAR_STAR_COUNT];
        private final float[] nStarY  = new float[NEAR_STAR_COUNT];
        private final float[] nStarR  = new float[NEAR_STAR_COUNT];
        private final float[] nStarSp = new float[NEAR_STAR_COUNT];
        private final float[] nStarPh = new float[NEAR_STAR_COUNT];

        // Nebula blobs (normalized 0..1 in x,y)
        private final float[] blobX = new float[NEBULA_BLOB_COUNT];
        private final float[] blobY = new float[NEBULA_BLOB_COUNT];
        private final float[] blobR = new float[NEBULA_BLOB_COUNT];

        protected double timeSec;

        AbstractCosmic(String id, String name, int price, String tagline,
                       Color accent, Color nebulaColor, int style) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.accent = accent;
            this.nebulaColor = nebulaColor;
            this.style = style;

            // Deterministic per-id seed
            Random r = new Random(id.hashCode() * 9176L + 31L);
            for (int i = 0; i < DEEP_STAR_COUNT; i++) {
                dStarX[i]  = r.nextFloat();
                dStarY[i]  = r.nextFloat();
                dStarR[i]  = 0.5f + r.nextFloat() * 1.4f;
                dStarSp[i] = 0.6f + r.nextFloat() * 3.2f;
                dStarPh[i] = r.nextFloat() * 6.283f;
            }
            for (int i = 0; i < NEAR_STAR_COUNT; i++) {
                nStarX[i]  = r.nextFloat();
                nStarY[i]  = r.nextFloat();
                nStarR[i]  = 1.6f + r.nextFloat() * 2.4f;
                nStarSp[i] = 0.4f + r.nextFloat() * 2.0f;
                nStarPh[i] = r.nextFloat() * 6.283f;
            }
            for (int i = 0; i < NEBULA_BLOB_COUNT; i++) {
                blobX[i] = 0.1f + r.nextFloat() * 0.8f;
                blobY[i] = 0.2f + r.nextFloat() * 0.6f;
                blobR[i] = 0.18f + r.nextFloat() * 0.22f;
            }
        }

        @Override public String id()           { return id; }
        @Override public String name()         { return name; }
        @Override public int price()           { return price; }
        @Override public String tagline()      { return tagline; }
        @Override public Color previewAccent() { return accent; }

        @Override
        public void update(double deltaMs) {
            timeSec += deltaMs / 1000.0;
            // Monotonic forward scroll, not sine oscillation.
            Camera.x = timeSec * 6.0;
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            // Base: deep space
            g.setColor(new Color(4, 4, 12));
            g.fillRect(0, 0, w, h);

            drawDeepStars(g, w, h);
            drawNebula(g, w, h);

            // Variant-specific mid layer
            switch (style) {
                case STYLE_SPIRAL: drawSpiral(g, w, h); break;
                case STYLE_HOLE:   drawBlackHole(g, w, h); break;
                case STYLE_AURORA: drawAurora(g, w, h); break;
                default: break;
            }

            drawNearStars(g, w, h);

            if (oldAA != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            }
        }

        // Deterministic hash from world slot index to stable double in [0,1).
        private double slotHash(int slot, int salt) {
            long x = ((long) slot * 2654435761L) ^ ((long) salt * 0x9E3779B97F4A7C15L);
            x ^= (x >>> 33);
            x *= 0xff51afd7ed558ccdL;
            x ^= (x >>> 33);
            x *= 0xc4ceb9fe1a85ec53L;
            x ^= (x >>> 33);
            return (x >>> 11) / (double) (1L << 53);
        }

        private void drawDeepStars(Graphics2D g, int w, int h) {
            // Hash-per-slot scatter: as Camera.x grows, new slots scroll in
            // from the right with unique star positions. No repeat cycle.
            double offset = Camera.x * 0.05;
            double cellW = 24.0;
            int leftSlot = (int) Math.floor(offset / cellW) - 1;
            int slots = (int) Math.ceil(w / cellW) + 3;
            for (int k = 0; k < slots; k++) {
                int idx = leftSlot + k;
                int starsHere = 1 + (int) (slotHash(idx, 0x10) * 2.0);
                for (int s = 0; s < starsHere; s++) {
                    double localX = slotHash(idx, 0x20 + s) * cellW;
                    double x = idx * cellW - offset + localX;
                    if (x < -4 || x > w + 4) continue;
                    double y = slotHash(idx, 0x30 + s) * h;
                    double sp = 0.4 + slotHash(idx, 0x40 + s) * 0.8;
                    double ph = slotHash(idx, 0x50 + s) * Math.PI * 2.0;
                    double tw = Math.sin(timeSec * sp + ph);
                    int alpha = (int) (140 + 110 * tw);
                    if (alpha < 30) alpha = 30;
                    if (alpha > 255) alpha = 255;
                    g.setColor(new Color(255, 255, 255, alpha));
                    float r = (float) (0.7 + slotHash(idx, 0x60 + s) * 1.6);
                    g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                }
            }
        }

        private void drawNebula(Graphics2D g, int w, int h) {
            double drift = Camera.x / 600.0;
            Color outer = new Color(nebulaColor.getRed(),
                nebulaColor.getGreen(), nebulaColor.getBlue(), 0);
            for (int i = 0; i < NEBULA_BLOB_COUNT; i++) {
                float cx = (blobX[i] + (float) (drift * 0.03f)) * w;
                float cy = blobY[i] * h;
                float radius = blobR[i] * Math.max(w, h);
                if (radius < 1) continue;
                try {
                    g.setPaint(new RadialGradientPaint(cx, cy, radius,
                        new float[] { 0f, 1f },
                        new Color[] { nebulaColor, outer }));
                    g.fill(new Ellipse2D.Float(
                        cx - radius, cy - radius, radius * 2, radius * 2));
                } catch (Exception ignore) { /* radius==0 */ }
            }
        }

        private void drawSpiral(Graphics2D g, int w, int h) {
            double cx = w * 0.5;
            double cy = h * 0.5;
            double rot = timeSec * 0.08;
            for (int arm = 0; arm < 3; arm++) {
                double armOff = arm * (2 * Math.PI / 3);
                for (int i = 0; i < 70; i++) {
                    double t = i / 70.0;
                    double rad = t * Math.min(w, h) * 0.55;
                    double ang = armOff + rot + t * 6.0;
                    double sx = cx + Math.cos(ang) * rad;
                    double sy = cy + Math.sin(ang) * rad * 0.7;
                    int alpha = (int) (200 * (1 - t));
                    g.setColor(new Color(
                        accent.getRed(), accent.getGreen(), accent.getBlue(),
                        Math.max(20, alpha)));
                    double sr = 1.2 + (1 - t) * 2.0;
                    g.fill(new Ellipse2D.Double(sx - sr, sy - sr, sr * 2, sr * 2));
                }
            }
            // bright core
            RadialGradientPaint core = new RadialGradientPaint(
                (float) cx, (float) cy, (float) (Math.min(w, h) * 0.12),
                new float[] { 0f, 1f },
                new Color[] {
                    new Color(255, 240, 220, 220),
                    new Color(255, 240, 220, 0)
                });
            g.setPaint(core);
            double cr = Math.min(w, h) * 0.12;
            g.fill(new Ellipse2D.Double(cx - cr, cy - cr, cr * 2, cr * 2));
        }

        private void drawBlackHole(Graphics2D g, int w, int h) {
            double cx = w * 0.5;
            double cy = h * 0.5;
            double rOuter = Math.min(w, h) * 0.22;
            double rInner = rOuter * 0.55;

            // Glowing accretion ring (outer halo)
            Color a0 = new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 0);
            Color a1 = new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 180);
            double hr = rOuter * 1.6;
            g.setPaint(new RadialGradientPaint(
                (float) cx, (float) cy, (float) hr,
                new float[] { 0f, 0.55f, 1f },
                new Color[] { a0, a1, a0 }));
            g.fill(new Ellipse2D.Double(cx - hr, cy - hr, hr * 2, hr * 2));

            // Bright thin ring outline
            g.setColor(new Color(255, 220, 255, 200));
            g.setStroke(new java.awt.BasicStroke(2f));
            g.draw(new Ellipse2D.Double(
                cx - rOuter, cy - rOuter, rOuter * 2, rOuter * 2));

            // Inner black void
            g.setColor(new Color(0, 0, 0, 255));
            g.fill(new Ellipse2D.Double(
                cx - rInner, cy - rInner, rInner * 2, rInner * 2));
        }

        private void drawAurora(Graphics2D g, int w, int h) {
            int bandTop = (int) (h * 0.05);
            int bandBot = (int) (h * 0.55);
            for (int band = 0; band < 3; band++) {
                Path2D.Double p = new Path2D.Double();
                double yMid = bandTop + (band + 0.5) * ((bandBot - bandTop) / 3.0);
                double amp = 22 + band * 8;
                double speed = 0.4 + band * 0.15;
                p.moveTo(0, yMid);
                for (int x = 0; x <= w; x += 12) {
                    double y = yMid + Math.sin(x * 0.012 + timeSec * speed + band)
                                      * amp;
                    p.lineTo(x, y);
                }
                for (int x = w; x >= 0; x -= 12) {
                    double y = yMid + 40 + Math.sin(x * 0.009 + timeSec * speed
                                                    + band + 1.3) * amp;
                    p.lineTo(x, y);
                }
                p.closePath();
                Color c1 = new Color(
                    accent.getRed(), accent.getGreen(), accent.getBlue(), 70);
                Color c2 = new Color(
                    160, 90, 220, 50);
                g.setPaint(band % 2 == 0 ? c1 : c2);
                g.fill(p);
            }
        }

        private void drawNearStars(Graphics2D g, int w, int h) {
            // Near-star slots scroll faster (closer parallax) but still unique.
            double offset = Camera.x * 0.2;
            double cellW = 80.0;
            int leftSlot = (int) Math.floor(offset / cellW) - 1;
            int slots = (int) Math.ceil(w / cellW) + 3;
            for (int k = 0; k < slots; k++) {
                int idx = leftSlot + k;
                double localX = slotHash(idx, 0x70) * cellW;
                double x = idx * cellW - offset + localX;
                if (x < -16 || x > w + 16) continue;
                double y = slotHash(idx, 0x80) * h;
                float r = (float) (1.4 + slotHash(idx, 0x90) * 2.4);
                double sp = 0.6 + slotHash(idx, 0xA0) * 1.0;
                double ph = slotHash(idx, 0xB0) * Math.PI * 2.0;
                double tw = Math.sin(timeSec * sp + ph);
                int alpha = (int) (200 + 55 * tw);
                if (alpha < 80) alpha = 80;
                if (alpha > 255) alpha = 255;

                // soft halo
                try {
                    RadialGradientPaint hp = new RadialGradientPaint(
                        (float) x, (float) y, r * 4f,
                        new float[] { 0f, 1f },
                        new Color[] {
                            new Color(accent.getRed(), accent.getGreen(),
                                      accent.getBlue(), alpha / 2),
                            new Color(accent.getRed(), accent.getGreen(),
                                      accent.getBlue(), 0)
                        });
                    g.setPaint(hp);
                    g.fill(new Ellipse2D.Double(
                        x - r * 4, y - r * 4, r * 8, r * 8));
                } catch (Exception ignore) {}

                g.setColor(new Color(
                    accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
                g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
            }
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            java.awt.Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);

            // Deep space
            g.setColor(new Color(4, 4, 12));
            g.fillRect(x, y, w, h);

            // Nebula glow
            float cx = x + w * 0.55f;
            float cy = y + h * 0.5f;
            float rad = Math.max(w, h) * 0.55f;
            try {
                g.setPaint(new RadialGradientPaint(cx, cy, rad,
                    new float[] { 0f, 1f },
                    new Color[] {
                        new Color(nebulaColor.getRed(), nebulaColor.getGreen(),
                                  nebulaColor.getBlue(), 130),
                        new Color(nebulaColor.getRed(), nebulaColor.getGreen(),
                                  nebulaColor.getBlue(), 0)
                    }));
                g.fill(new Ellipse2D.Float(cx - rad, cy - rad, rad * 2, rad * 2));
            } catch (Exception ignore) {}

            // Scattered white stars (deterministic, based on id)
            Random r = new Random(id.hashCode() * 7L + 3L);
            for (int i = 0; i < 40; i++) {
                int sx = x + r.nextInt(Math.max(1, w));
                int sy = y + r.nextInt(Math.max(1, h));
                int a = 120 + r.nextInt(135);
                g.setColor(new Color(255, 255, 255, a));
                int sr = r.nextInt(2) + 1;
                g.fillOval(sx - sr, sy - sr, sr * 2, sr * 2);
            }

            // A few accent-colored brighter stars
            for (int i = 0; i < 6; i++) {
                int sx = x + r.nextInt(Math.max(1, w));
                int sy = y + r.nextInt(Math.max(1, h));
                g.setColor(new Color(
                    accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
                g.fillOval(sx - 2, sy - 2, 4, 4);
            }

            g.setClip(oldClip);
            if (oldAA != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            }
        }
    }

    private static final class Variant extends AbstractCosmic {
        Variant(String id, String name, int price, String tagline,
                Color accent, Color nebulaColor, int style) {
            super(id, name, price, tagline, accent, nebulaColor, style);
        }
    }
}
