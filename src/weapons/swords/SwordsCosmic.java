package weapons.swords;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;

import weapons.Sword;
import weapons.SwordCatalog;
import weapons.TrailPoint;

/**
 * Cosmic / Galactic family. The top-tier endgame blades: starfields,
 * nebulae and supernova energy. Each variant gets a cached BufferedImage
 * sword head featuring a deep-space blade body, ornate guard and a glowing
 * pommel gem in the variant accent.
 *
 * <p>Signature flourish: a few twinkling stars drifting near the tip.
 *
 * 5 variants registered under sword_036 .. sword_040.
 */
public final class SwordsCosmic {

    private SwordsCosmic() {}

    static {
        SwordCatalog.register(new Variant(
            "sword_036", "Nebula Veil", 1500,
            "Stardust on every cut.",
            new Color(210, 140, 235), new Color(255, 150, 220), false));
        SwordCatalog.register(new Variant(
            "sword_037", "Supernova Edge", 2200,
            "Born from a dying star.",
            new Color(255, 170, 80), new Color(255, 245, 220), false));
        SwordCatalog.register(new Variant(
            "sword_038", "Void Walker", 3000,
            "The blade that walks between.",
            new Color(95, 40, 140), new Color(25, 10, 45), false));
        SwordCatalog.register(new Variant(
            "sword_039", "Comet Heart", 4000,
            "A frozen comet, sharpened.",
            new Color(140, 230, 255), new Color(220, 250, 255), false));
        SwordCatalog.register(new Variant(
            "sword_040", "Quasar Blade", 6000,
            "Power of a galaxy core.",
            new Color(255, 255, 255), new Color(255, 255, 255), true));
    }

    // ====================================================================
    //  ABSTRACT BASE -- legendary multi-layer cosmic trail rendering.
    // ====================================================================

    static abstract class AbstractCosmic implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color primary;
        protected final Color secondary;
        protected final boolean rainbow;

        AbstractCosmic(String id, String name, int price, String tagline,
                       Color primary, Color secondary, boolean rainbow) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.primary = primary;
            this.secondary = secondary;
            this.rainbow = rainbow;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() {
            if (rainbow) {
                float hue = (float) ((System.currentTimeMillis() % 3000L) / 3000.0);
                return Color.getHSBColor(hue, 0.85f, 1.0f);
            }
            return primary;
        }

        protected Color accentNow(long t) {
            if (rainbow) {
                float hue = (float) ((t % 3000L) / 3000.0);
                return Color.getHSBColor(hue, 0.9f, 1.0f);
            }
            return primary;
        }

        protected Color shiftHue(Color base, float delta) {
            float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
            float hue = (hsb[0] + delta) % 1.0f;
            if (hue < 0) hue += 1.0f;
            return Color.getHSBColor(hue, Math.max(0.55f, hsb[1]), 1.0f);
        }

        /** Lazily build & cache a cosmic blade head (uses primary as accent). */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                // Pale steel blade body, cosmic accent inner glow.
                SwordHeadRenderer.paintBladeBody(g,
                    SwordHeadRenderer.lighter(secondary, 0.6f), primary);
                // Ornate guard in secondary tint.
                SwordHeadRenderer.paintCrossGuard(g,
                    SwordHeadRenderer.darker(secondary, 0.2f));
                // Deep-space grip.
                SwordHeadRenderer.paintGrip(g, new Color(20, 15, 35));
                // Glowing accent gem in the pommel.
                SwordHeadRenderer.paintPommel(g, new Color(35, 25, 55), primary);

                // Tiny pre-baked star sparkles on the blade.
                int tipX = SwordHeadRenderer.TIP_X;
                int tipY = SwordHeadRenderer.TIP_Y;
                int len = SwordHeadRenderer.BLADE_LEN;
                g.setColor(new Color(255, 255, 255, 200));
                for (int i = 0; i < 5; i++) {
                    int sx = tipX + (int)(len * (0.15 + i * 0.18));
                    int sy = tipY + ((i % 2 == 0) ? -2 : 2);
                    g.fillOval(sx, sy, 2, 2);
                    g.drawLine(sx - 2, sy + 1, sx + 3, sy + 1);
                    g.drawLine(sx, sy - 2, sx, sy + 3);
                }
            } finally {
                g.dispose();
            }
            SwordHeadRenderer.putCached(id, cached);
            return cached;
        }

        @Override
        public void drawTrail(Graphics2D g, List<TrailPoint> points) {
            if (points.size() < 2) return;
            long now = System.currentTimeMillis();
            Color acc = accentNow(now);

            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (TrailPoint p : points) {
                if (first) { path.moveTo(p.x, p.y); first = false; }
                else { path.lineTo(p.x, p.y); }
            }

            // Layer 1: super-wide ambient halo (~36px)
            g.setStroke(new BasicStroke(36f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 35));
            g.draw(path);

            // Layer 2: wide halo (~24px) with secondary tint
            Color halo = rainbow ? shiftHue(acc, 0.08f) : secondary;
            g.setStroke(new BasicStroke(24f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(halo.getRed(), halo.getGreen(), halo.getBlue(), 70));
            g.draw(path);

            // Layer 3: mid (~12px) accent
            g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 150));
            g.draw(path);

            // Layer 4: bright white core (3px)
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 235));
            g.draw(path);

            // Shimmer dots along the trail body.
            int i = 0;
            for (TrailPoint p : points) {
                float phase = (float) (i * 0.17);
                float dotHue;
                if (rainbow) {
                    dotHue = (float) (((now * 0.0008) + i * 0.04) % 1.0);
                } else {
                    float[] hsb = Color.RGBtoHSB(acc.getRed(), acc.getGreen(),
                                                  acc.getBlue(), null);
                    dotHue = (hsb[0] + (float) Math.sin(phase) * 0.05f) % 1.0f;
                    if (dotHue < 0) dotHue += 1.0f;
                }
                Color shimmer = Color.getHSBColor(dotHue, 0.7f, 1.0f);
                int alpha = (int) (180 * Math.max(0.0, p.life));
                g.setColor(new Color(shimmer.getRed(), shimmer.getGreen(),
                                      shimmer.getBlue(), alpha));
                int size = 2 + (i % 3);
                g.fillOval((int) p.x - size / 2, (int) p.y - size / 2, size, size);
                i++;
            }

            // Background star particles around the trail (original behaviour).
            i = 0;
            for (TrailPoint p : points) {
                if ((i & 1) == 0) { i++; continue; }
                for (int s = 0; s < 2; s++) {
                    double angle = (i * 0.83) + (s * Math.PI) + (now * 0.0009);
                    double radius = 14.0 + ((i * 7) % 18);
                    int sx = (int) (p.x + Math.cos(angle) * radius);
                    int sy = (int) (p.y + Math.sin(angle) * radius);
                    double pulse = 0.55 + 0.45 * Math.sin(now * 0.01 + i + s * 1.7);
                    int starSize = 3 + (int) (pulse * 4);
                    int sa = (int) (220 * pulse * Math.max(0.0, p.life));
                    Color starCol = rainbow
                        ? Color.getHSBColor((float) ((now * 0.0006 + i * 0.07) % 1.0),
                                            0.5f, 1.0f)
                        : new Color(255, 255, 255);
                    drawStar(g, sx, sy, starSize,
                             new Color(starCol.getRed(), starCol.getGreen(),
                                       starCol.getBlue(), Math.min(255, sa)));
                }
                i++;
            }

            // (Sword sprite intentionally NOT drawn during gameplay --
            // slice-trail only. The cached head appears in drawPreview only.)

            // Signature flourish: drifting twinkling stars near the tip.
            drawDriftingStars(g, points, now);
        }

        /**
         * A few bright twinkling stars drift away from the tip in a slow
         * outward orbit, fading as they age.
         */
        protected void drawDriftingStars(Graphics2D g, List<TrailPoint> points, long now) {
            TrailPoint tip = points.get(0);
            Composite oldComp = g.getComposite();
            int stars = 5;
            for (int s = 0; s < stars; s++) {
                long seed = tip.timeMs + s * 7919L + (now / 200L);
                double a = ((seed >> 4) & 0x3FFL) / 1024.0 * Math.PI * 2.0;
                double age = ((now - tip.timeMs + s * 80L) % 1200L);
                double r = 6.0 + (age / 1200.0) * 24.0;
                double sx = tip.x + Math.cos(a) * r;
                double sy = tip.y + Math.sin(a) * r;
                double pulse = 0.4 + 0.6 * Math.abs(Math.sin(now * 0.01 + s));
                int sz = 2 + (int)(pulse * 3);
                float alpha = (float) Math.max(0.0, 1.0 - age / 1200.0) * 0.9f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                Color starCol;
                if (rainbow) {
                    starCol = Color.getHSBColor(
                        (float) ((seed & 0x3FFL) / 1024.0), 0.65f, 1.0f);
                } else {
                    starCol = SwordHeadRenderer.lighter(primary, 0.4f);
                }
                drawStar(g, (int) sx, (int) sy, sz, starCol);
                // Bright white core dot.
                g.setColor(new Color(255, 255, 255));
                g.fillOval((int) sx - 1, (int) sy - 1, 2, 2);
            }
            g.setComposite(oldComp);
        }

        /** Draws a tiny 4-point star (cross) centered at (cx, cy). */
        protected void drawStar(Graphics2D g, int cx, int cy, int size, Color c) {
            g.setColor(c);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(cx - size, cy, cx + size, cy);
            g.drawLine(cx, cy - size, cx, cy + size);
            int half = Math.max(1, size / 2);
            g.fillOval(cx - half, cy - half, half * 2, half * 2);
        }

        /** Draw the cached head once at a single tip position. */
        protected void drawSwordHead(Graphics2D g, double tipX, double tipY, double angleDeg) {
            BufferedImage head = cachedHead();
            AffineTransform old = g.getTransform();
            AffineTransform t = new AffineTransform();
            t.translate(tipX, tipY);
            t.rotate(Math.toRadians(angleDeg + 180.0));
            t.translate(-SwordHeadRenderer.TIP_X, -SwordHeadRenderer.TIP_Y);
            g.drawImage(head, t, null);
            g.setTransform(old);
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            long now = System.currentTimeMillis();
            Color acc = accentNow(now);
            Color halo = rainbow ? shiftHue(acc, 0.08f) : secondary;

            int x1 = x + 12, y1 = y + h - 16;
            int x2 = x + w - 12, y2 = y + 16;

            g.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 45));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(halo.getRed(), halo.getGreen(), halo.getBlue(), 90));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 170));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 240));
            g.drawLine(x1, y1, x2, y2);

            int steps = 10;
            for (int s = 0; s < steps; s++) {
                double t = s / (double) (steps - 1);
                int sx = (int) (x1 + (x2 - x1) * t);
                int sy = (int) (y1 + (y2 - y1) * t);
                double dx = (x2 - x1);
                double dy = (y2 - y1);
                double len = Math.sqrt(dx * dx + dy * dy);
                double nx = -dy / len;
                double ny = dx / len;
                double off = ((s * 13) % 22) - 11;
                int starX = (int) (sx + nx * off);
                int starY = (int) (sy + ny * off);
                double pulse = 0.55 + 0.45 * Math.sin(now * 0.01 + s);
                int starSize = 2 + (int) (pulse * 3);
                Color starCol;
                if (rainbow) {
                    starCol = Color.getHSBColor(
                        (float) ((now * 0.0007 + s * 0.09) % 1.0), 0.55f, 1.0f);
                } else {
                    starCol = new Color(255, 255, 255);
                }
                int sa = (int) (220 * pulse);
                drawStar(g, starX, starY, starSize,
                         new Color(starCol.getRed(), starCol.getGreen(),
                                   starCol.getBlue(), Math.min(255, sa)));
            }

            // Mini cached cosmic head at the tip.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractCosmic {
        Variant(String id, String name, int price, String tagline,
                Color primary, Color secondary, boolean rainbow) {
            super(id, name, price, tagline, primary, secondary, rainbow);
        }
    }
}
