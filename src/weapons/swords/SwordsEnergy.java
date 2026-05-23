package weapons.swords;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import weapons.Sword;
import weapons.SwordCatalog;
import weapons.TrailPoint;

/**
 * Energy / Plasma blade family. Sci-fi glowing saber aesthetic.
 * 5 variants registered under sword_006 .. sword_010.
 *
 * <p>Cached BufferedImage sword head: a sleek emitter hilt with grip/guard/
 * pommel detail PLUS a pre-rendered plasma core beam glowing with a radial
 * gradient. Motion blur afterimages give the saber a real arcing feel.
 *
 * <p>Signature flourish: branching lightning arcs leaping off the blade.
 */
public final class SwordsEnergy {

    private SwordsEnergy() {}

    static {
        SwordCatalog.register(new Variant(
            "sword_006", "Blue Plasma", 300,
            "Standard issue saber",
            new Color(80, 160, 255)));
        SwordCatalog.register(new Variant(
            "sword_007", "Crimson Saber", 450,
            "Sith-style red core",
            new Color(255, 50, 50)));
        SwordCatalog.register(new Variant(
            "sword_008", "Verdant Lance", 600,
            "Forest plasma",
            new Color(70, 230, 110)));
        SwordCatalog.register(new Variant(
            "sword_009", "Void Edge", 900,
            "Plasma rends spacetime",
            new Color(170, 70, 230)));
        SwordCatalog.register(new Variant(
            "sword_010", "White Star Blade", 1400,
            "Pure energy, blinding bright",
            new Color(245, 245, 255)));
    }

    // =====================================================================
    //  ABSTRACT BASE -- shared plasma rendering.
    // =====================================================================

    static abstract class AbstractEnergy implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color accent;

        AbstractEnergy(String id, String name, int price, String tagline,
                       Color accent) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.accent = accent;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        private Color withAlpha(Color c, int a) {
            int aa = a < 0 ? 0 : (a > 255 ? 255 : a);
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), aa);
        }

        private Color lighten(Color c, float t) {
            int r = (int)(c.getRed()   + (255 - c.getRed())   * t);
            int gg = (int)(c.getGreen() + (255 - c.getGreen()) * t);
            int b = (int)(c.getBlue()  + (255 - c.getBlue())  * t);
            return new Color(r, gg, b);
        }

        /** Lazily build & cache the plasma sword head image. */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                // 1. Pre-render the plasma beam (not a steel blade) along the blade slot.
                int tipX = SwordHeadRenderer.TIP_X;
                int tipY = SwordHeadRenderer.TIP_Y;
                int beamLen = SwordHeadRenderer.BLADE_LEN;

                // Wide outer halo of the beam.
                for (int r = 14; r >= 1; r--) {
                    int alpha = 6 + (15 - r) * 3;
                    int hh = r * 2 + 4;
                    g.setColor(withAlpha(accent, alpha));
                    g.fillRoundRect(tipX - r, tipY - hh / 2,
                                    beamLen + r * 2, hh, hh, hh);
                }
                // Bright accent body.
                g.setColor(withAlpha(lighten(accent, 0.5f), 235));
                g.fillRoundRect(tipX, tipY - 5, beamLen, 10, 8, 8);
                // White-hot core.
                g.setColor(new Color(255, 255, 255, 250));
                g.fillRoundRect(tipX + 2, tipY - 2, beamLen - 4, 4, 4, 4);
                // Tip flare.
                Point2D center = new Point2D.Float(tipX, tipY);
                Color[] flareCols = {
                    new Color(255, 255, 255, 255),
                    withAlpha(lighten(accent, 0.7f), 200),
                    withAlpha(accent, 0)
                };
                RadialGradientPaint flare = new RadialGradientPaint(
                    center, 16f, new float[] { 0f, 0.45f, 1f }, flareCols);
                g.setPaint(flare);
                g.fill(new Ellipse2D.Double(tipX - 16, tipY - 16, 32, 32));

                // 2. Emitter hilt: dark steel guard, grip, pommel (with accent gem).
                SwordHeadRenderer.paintCrossGuard(g, new Color(70, 75, 90));
                SwordHeadRenderer.paintGrip(g, new Color(35, 38, 48));
                SwordHeadRenderer.paintPommel(g, new Color(45, 48, 60), accent);

                // 3. Emitter ring at the front of the guard glowing with the accent.
                int eX = SwordHeadRenderer.GUARD_X;
                int eY = tipY;
                g.setColor(withAlpha(accent, 220));
                g.fillRoundRect(eX - 3, eY - 4, 4, 8, 2, 2);
                g.setColor(new Color(255, 255, 255, 230));
                g.fillRoundRect(eX - 2, eY - 2, 2, 4, 1, 1);
            } finally {
                g.dispose();
            }
            SwordHeadRenderer.putCached(id, cached);
            return cached;
        }

        @Override
        public void drawTrail(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            if (n < 2) return;

            long now = System.currentTimeMillis();

            // Build a path along all points.
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (TrailPoint p : points) {
                if (first) { path.moveTo(p.x, p.y); first = false; }
                else       { path.lineTo(p.x, p.y); }
            }

            // Use mid-point age for a global pulse intensity.
            TrailPoint mid = points.get(n / 2);
            long midAge = now - mid.timeMs;
            float pulse = (float)(0.55 + 0.45 * Math.sin(midAge * 0.028));
            if (pulse < 0f) pulse = 0f;
            if (pulse > 1f) pulse = 1f;

            // 0) Outer bloom layer, very low alpha, wide.
            float bloomW = 42f + 6f * pulse;
            g.setStroke(new BasicStroke(bloomW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(accent, 30));
            g.draw(path);

            // 1) Wide outer halo, accent color, very low alpha.
            float haloW = 32f + 8f * pulse;
            g.setStroke(new BasicStroke(haloW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(accent, 55));
            g.draw(path);

            // 2) Mid bloom, lighter tint, half alpha.
            float midW = 14f + 4f * pulse;
            g.setStroke(new BasicStroke(midW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(lighten(accent, 0.35f), 140));
            g.draw(path);

            // 3) White-hot bright core.
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 240));
            g.draw(path);

            // 4) Energy sparks -- short perpendicular segments at random points.
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < n - 1; i++) {
                TrailPoint p = points.get(i);
                long seed = p.timeMs ^ (i * 2654435761L);
                if ((seed & 7L) >= 3) continue;

                TrailPoint prev = points.get(i - 1);
                double dx = p.x - prev.x;
                double dy = p.y - prev.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len < 0.001) continue;
                double px = -dy / len;
                double py =  dx / len;

                double sign = ((seed >> 3) & 1L) == 0 ? 1.0 : -1.0;
                double sparkLen = 6.0 + ((seed >> 4) & 0xF);

                long age = now - p.timeMs;
                int alpha = (int)(220 - age * 0.6);
                if (alpha < 40) alpha = 40;
                if (alpha > 220) alpha = 220;

                double sx = p.x;
                double sy = p.y;
                double ex = sx + px * sparkLen * sign;
                double ey = sy + py * sparkLen * sign;

                g.setColor(withAlpha(accent, alpha));
                g.drawLine((int)sx, (int)sy, (int)ex, (int)ey);

                g.setColor(new Color(255, 255, 255, Math.min(255, alpha + 20)));
                g.drawLine((int)(sx + px * sparkLen * sign * 0.7),
                           (int)(sy + py * sparkLen * sign * 0.7),
                           (int)ex, (int)ey);
            }

            // 5) Brightness specks for sparkle.
            drawSparkSpecks(g, points);

            // (Sword sprite intentionally NOT drawn during gameplay --
            // slice-trail only. The cached head appears in drawPreview only.)

            // 7) Signature flourish: branching lightning arcs.
            drawBranchingLightning(g, points);
        }

        protected void drawSparkSpecks(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            int specks = Math.min(5, n);
            for (int i = 0; i < specks; i++) {
                long seed = points.get(0).timeMs ^ (i * 0x9E3779B97F4A7C15L);
                int idx = (int) Math.abs(seed % n);
                TrailPoint p = points.get(idx);
                double ox = ((seed >> 8) & 7L) - 3.5;
                double oy = ((seed >> 12) & 7L) - 3.5;
                int sz = 2 + (int) ((seed >> 16) & 1L);
                g.setColor(new Color(255, 255, 255, 240));
                g.fillOval((int) (p.x + ox) - sz / 2,
                           (int) (p.y + oy) - sz / 2, sz, sz);
            }
        }

        /** Branching lightning leaping off the blade in the accent color. */
        protected void drawBranchingLightning(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            long now = System.currentTimeMillis();
            Composite oldComp = g.getComposite();

            // 2-3 arcs from points near the tip.
            int arcs = Math.min(3, n - 1);
            for (int a = 0; a < arcs; a++) {
                int i = a * 2;
                if (i >= n) break;
                TrailPoint p = points.get(i);
                long seed = p.timeMs ^ (a * 0xC2B2AE3D27D4EB4FL);
                if (((seed >> 4) & 3L) == 0L) continue; // ~75% chance to draw

                // Arc length and base direction perpendicular to the trail.
                TrailPoint q = points.get(Math.min(n - 1, i + 1));
                double dx = q.x - p.x;
                double dy = q.y - p.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len < 0.001) continue;
                double nx = -dy / len;
                double ny =  dx / len;
                double sign = ((seed >> 9) & 1L) == 0 ? 1.0 : -1.0;
                double arcLen = 18.0 + ((seed >> 11) & 0x1F);

                int segments = 5;
                Path2D.Double arc = new Path2D.Double();
                double cx = p.x;
                double cy = p.y;
                arc.moveTo(cx, cy);
                for (int s = 1; s <= segments; s++) {
                    double t = s / (double) segments;
                    double zigSeed = ((seed >> (s * 2)) & 0x7L) - 3.5;
                    double zx = cx + nx * sign * arcLen * t + (-ny) * sign * zigSeed * 1.4;
                    double zy = cy + ny * sign * arcLen * t + ( nx) * sign * zigSeed * 1.4;
                    arc.lineTo(zx, zy);
                    // Smaller branch fork on the 3rd vertex.
                    if (s == 3) {
                        double bx = zx + nx * sign * 6 + (-ny) * sign * 3;
                        double by = zy + ny * sign * 6 + ( nx) * sign * 3;
                        Path2D.Double fork = new Path2D.Double();
                        fork.moveTo(zx, zy);
                        fork.lineTo(bx, by);
                        g.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.7f * (float) Math.max(0, p.life)));
                        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.setColor(new Color(255, 255, 255, 220));
                        g.draw(fork);
                    }
                }
                float alpha = (float) Math.max(0.0, p.life) * 0.85f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(withAlpha(accent, 220));
                g.draw(arc);
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 255, 255, 240));
                g.draw(arc);
            }
            g.setComposite(oldComp);
            // Suppress unused warning if any pulse path adds more later.
            if (now < 0) return;
        }

        /**
         * Legacy entry point. Uses the cached BufferedImage now.
         */
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
            int x1 = x + 12, y1 = y + h - 16;
            int x2 = x + w - 12, y2 = y + 16;

            // Wide halo
            g.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(accent, 70));
            g.drawLine(x1, y1, x2, y2);

            // Mid bloom
            g.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(lighten(accent, 0.35f), 170));
            g.drawLine(x1, y1, x2, y2);

            // Bright core
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 240));
            g.drawLine(x1, y1, x2, y2);

            // A few sparks along the diagonal
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len > 0.001) {
                double ux = dx / len;
                double uy = dy / len;
                double px = -uy;
                double py =  ux;

                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int sparks = 5;
                for (int i = 1; i <= sparks; i++) {
                    double t = i / (double)(sparks + 1);
                    double sx = x1 + dx * t;
                    double sy = y1 + dy * t;
                    double sign = (i % 2 == 0) ? 1.0 : -1.0;
                    double sLen = 8.0 + (i * 2);
                    double ex = sx + px * sLen * sign;
                    double ey = sy + py * sLen * sign;
                    g.setColor(withAlpha(accent, 200));
                    g.drawLine((int)sx, (int)sy, (int)ex, (int)ey);
                    g.setColor(new Color(255, 255, 255, 230));
                    g.drawLine((int)(sx + px * sLen * sign * 0.65),
                               (int)(sy + py * sLen * sign * 0.65),
                               (int)ex, (int)ey);
                    if (ux < 0) { /* keep ux/uy live */ }
                }
            }

            // Mini plasma sword head (cached image) at the tip of the slash.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractEnergy {
        Variant(String id, String name, int price, String tagline,
                Color accent) {
            super(id, name, price, tagline, accent);
        }
    }
}
