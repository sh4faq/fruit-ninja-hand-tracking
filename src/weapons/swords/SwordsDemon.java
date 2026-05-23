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
 * Demon / cursed blade family. Chaotic alive trails on top of a pre-rendered
 * blade head with a wicked-curved blade silhouette, dark steel gradient,
 * a skull-shaped pommel gem and grip wrapped in chained iron.
 * 5 variants registered under sword_016 .. sword_020.
 *
 * <p>Signature flourish: dripping ember particles falling from the blade.
 */
public final class SwordsDemon {

    private SwordsDemon() {}

    static {
        SwordCatalog.register(new Variant(
            "sword_016", "Hellfire Cleaver", 400,
            "Forged in brimstone.",
            new Color(255, 110, 30), new Color(255, 200, 90)));
        SwordCatalog.register(new Variant(
            "sword_017", "Bloodfang", 600,
            "Hungers for blood.",
            new Color(150, 15, 25), new Color(255, 60, 60)));
        SwordCatalog.register(new Variant(
            "sword_018", "Shadow Rend", 850,
            "Cuts the soul.",
            new Color(70, 20, 100), new Color(180, 100, 230)));
        SwordCatalog.register(new Variant(
            "sword_019", "Void Reaper", 1200,
            "From beyond the veil.",
            new Color(30, 80, 50), new Color(120, 255, 160)));
        SwordCatalog.register(new Variant(
            "sword_020", "Soulfire Brand", 1800,
            "Each strike claims a soul.",
            new Color(120, 180, 255), new Color(230, 245, 255)));
    }

    // ====================================================================
    //  ABSTRACT BASE -- chaotic flame-tongue trail rendering.
    // ====================================================================

    static abstract class AbstractDemon implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color accent;
        protected final Color highlight;

        AbstractDemon(String id, String name, int price, String tagline,
                      Color accent, Color highlight) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.accent = accent;
            this.highlight = highlight;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        /** Lazily build & cache a demonic curved-blade sword head. */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                // Demon blade body in dark steel tone with the accent as inner glow.
                SwordHeadRenderer.paintBladeBody(g, new Color(70, 50, 60), accent);

                // Add jagged barbs along the spine for menace.
                int tipX = SwordHeadRenderer.TIP_X;
                int tipY = SwordHeadRenderer.TIP_Y;
                int len = SwordHeadRenderer.BLADE_LEN;
                g.setColor(new Color(20, 20, 25, 230));
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 4; i++) {
                    int bx = tipX + (int) (len * (0.3 + i * 0.15));
                    Path2D.Double barb = new Path2D.Double();
                    barb.moveTo(bx, tipY - SwordHeadRenderer.BLADE_HALF_H + 0.5);
                    barb.lineTo(bx + 4, tipY - SwordHeadRenderer.BLADE_HALF_H - 4);
                    barb.lineTo(bx + 6, tipY - SwordHeadRenderer.BLADE_HALF_H);
                    barb.closePath();
                    g.fill(barb);
                }

                // Ember glow along the cutting edge.
                g.setColor(new Color(highlight.getRed(), highlight.getGreen(),
                                     highlight.getBlue(), 200));
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D.Double emberLine = new Path2D.Double();
                emberLine.moveTo(tipX + 2, tipY + 1);
                emberLine.quadTo(tipX + len * 0.5, tipY + SwordHeadRenderer.BLADE_HALF_H,
                                 tipX + len - 3, tipY + SwordHeadRenderer.BLADE_HALF_H - 1.5);
                g.draw(emberLine);

                // Wicked hilt: bronze guard, dark grip, glowing gem pommel.
                SwordHeadRenderer.paintCrossGuard(g, new Color(95, 60, 40));
                SwordHeadRenderer.paintGrip(g, new Color(25, 20, 22));
                SwordHeadRenderer.paintPommel(g, new Color(60, 30, 30), accent);
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

            // ---- 1. Wide dark outer aura ----
            Path2D.Double base = new Path2D.Double();
            boolean first = true;
            for (TrailPoint p : points) {
                if (first) { base.moveTo(p.x, p.y); first = false; }
                else       { base.lineTo(p.x, p.y); }
            }
            g.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 170));
            g.draw(base);

            g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 90));
            g.draw(base);

            // ---- 2. Wobbling flame tongues ----
            for (int i = 0; i < 3; i++) {
                double phase = i * 2.094;
                double freq  = 0.012 + i * 0.004;
                double amp   = 5.5 + i * 1.5;
                int alpha    = 180 - i * 35;

                Path2D.Double tongue = buildWobblePath(points, now, freq, phase, amp);
                g.setStroke(new BasicStroke(6f - i * 1.2f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                     accent.getBlue(), alpha));
                g.draw(tongue);
            }

            // ---- 3. Inner core ----
            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(highlight.getRed(), highlight.getGreen(),
                                 highlight.getBlue(), 235));
            g.draw(base);

            // ---- 4. Drip / ichor ----
            int step = Math.max(1, n / 8);
            for (int i = 0; i < n; i += step) {
                TrailPoint p = points.get(i);
                long seed = p.timeMs;
                double r1 = ((seed * 1103515245L + 12345L) >>> 16) % 1000 / 1000.0;
                double r2 = ((seed * 22695477L  + 1L) >>> 16)      % 1000 / 1000.0;
                if (r1 < 0.55) continue;

                long age = now - p.timeMs;
                double fall = Math.min(28.0, age * 0.05);
                double ox   = (r2 - 0.5) * 6.0;
                int size    = 3 + (int) (r1 * 3);
                int alpha   = (int) (220 * Math.max(0.0, p.life));

                g.setColor(new Color(0, 0, 0, Math.min(200, alpha)));
                g.fillOval((int)(p.x + ox) - size/2 - 1,
                           (int)(p.y + fall) - size/2 - 1,
                           size + 2, size + 2);
                g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                     accent.getBlue(), alpha));
                g.fillOval((int)(p.x + ox) - size/2,
                           (int)(p.y + fall) - size/2,
                           size, size);
            }

            // ---- 5. (Sword sprite intentionally NOT drawn during gameplay)
            //         Slice-trail only. The cached head appears in drawPreview.

            // ---- 6. Signature flourish: dripping ember particles ----
            drawDrippingEmbers(g, points);
        }

        /**
         * Bright ember droplets that fall away from the blade with gravity
         * and a fading hot-to-dark gradient.
         */
        protected void drawDrippingEmbers(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            long now = System.currentTimeMillis();
            Composite oldComp = g.getComposite();
            for (int i = 0; i < n; i++) {
                TrailPoint p = points.get(i);
                long seed = p.timeMs ^ (i * 0xC2B2AE3D27D4EB4FL);
                if ((seed & 7L) >= 2L) continue; // ~25% chance per point

                double r2 = ((seed * 22695477L  + 1L) >>> 16) % 1000 / 1000.0;
                long age = now - p.timeMs;
                double fall = Math.min(48.0, age * 0.09);
                double ox = (r2 - 0.5) * 8.0;
                double ex = p.x + ox;
                double ey = p.y + fall;
                int size = 2 + (int) ((seed >> 5) & 2L);

                float alpha = (float) Math.max(0.0, p.life) * 0.85f - (float) (fall / 100.0);
                if (alpha < 0.05f) continue;
                if (alpha > 0.85f) alpha = 0.85f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                // Hot core then fading colored halo.
                g.setColor(new Color(255, 240, 180));
                g.fillOval((int) ex - size, (int) ey - size, size * 2, size * 2);
                g.setColor(highlight);
                g.fillOval((int) ex - size / 2, (int) ey - size / 2, size, size);
                // Trailing wisp.
                g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                     accent.getBlue(), 160));
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine((int) ex, (int) (ey - fall * 0.4), (int) ex, (int) ey);
            }
            g.setComposite(oldComp);
        }

        private Path2D.Double buildWobblePath(List<TrailPoint> points, long now,
                                              double freq, double phase, double amp) {
            Path2D.Double path = new Path2D.Double();
            int n = points.size();
            for (int i = 0; i < n; i++) {
                TrailPoint p = points.get(i);

                double dx, dy;
                if (i == 0) {
                    TrailPoint q = points.get(1);
                    dx = q.x - p.x; dy = q.y - p.y;
                } else if (i == n - 1) {
                    TrailPoint q = points.get(n - 2);
                    dx = p.x - q.x; dy = p.y - q.y;
                } else {
                    TrailPoint a = points.get(i - 1);
                    TrailPoint b = points.get(i + 1);
                    dx = b.x - a.x; dy = b.y - a.y;
                }
                double len = Math.sqrt(dx*dx + dy*dy);
                if (len < 0.0001) len = 1.0;
                double nx = -dy / len;
                double ny =  dx / len;

                double wobble = Math.sin(now * freq + i * 0.55 + phase) * amp;
                double px = p.x + nx * wobble;
                double py = p.y + ny * wobble;

                if (i == 0) path.moveTo(px, py);
                else        path.lineTo(px, py);
            }
            return path;
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
            int x1 = x + 12, y1 = y + h - 16;
            int x2 = x + w - 12, y2 = y + 16;

            g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0, 0, 0, 170));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 130));
            g.drawLine(x1, y1, x2, y2);

            double dx = x2 - x1, dy = y2 - y1;
            double len = Math.sqrt(dx*dx + dy*dy);
            double ux = dx / len, uy = dy / len;
            double nx = -uy, ny = ux;
            for (int i = 0; i < 2; i++) {
                int steps = 16;
                Path2D.Double tongue = new Path2D.Double();
                for (int s = 0; s <= steps; s++) {
                    double t = s / (double) steps;
                    double cx = x1 + dx * t;
                    double cy = y1 + dy * t;
                    double w2 = Math.sin(t * Math.PI * 3 + i * 1.7) * (5 + i * 2);
                    double px = cx + nx * w2;
                    double py = cy + ny * w2;
                    if (s == 0) tongue.moveTo(px, py);
                    else        tongue.lineTo(px, py);
                }
                g.setStroke(new BasicStroke(4f - i * 1.3f,
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                     accent.getBlue(), 200 - i * 60));
                g.draw(tongue);
            }

            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(highlight.getRed(), highlight.getGreen(),
                                 highlight.getBlue(), 235));
            g.drawLine(x1, y1, x2, y2);

            // Mini demon sword head at the tip.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractDemon {
        Variant(String id, String name, int price, String tagline,
                Color accent, Color highlight) {
            super(id, name, price, tagline, accent, highlight);
        }
    }
}
