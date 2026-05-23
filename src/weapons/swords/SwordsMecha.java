package weapons.swords;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import weapons.Sword;
import weapons.SwordCatalog;
import weapons.TrailPoint;

/**
 * Mecha / tech blade family. Chrome, neon, cyberpunk industrial weapons.
 * 5 variants registered under sword_026 .. sword_030.
 *
 * <p>The cached BufferedImage sword head is a chrome-plated blade with a
 * vertical chrome-to-accent gradient, exposed mechanical guard with rivets,
 * a hard-edged angular grip, and a glowing accent pommel core.
 *
 * <p>Signature flourish: a holographic scan line moving down the blade.
 */
public final class SwordsMecha {

    private SwordsMecha() {}

    private static final Color CHROME = new Color(220, 228, 240);

    static {
        SwordCatalog.register(new Variant(
            "sword_026", "Chrome Edge", 350,
            "Buffed steel, neon trim.",
            new Color(80, 220, 255)));
        SwordCatalog.register(new Variant(
            "sword_027", "Gold Titanium", 550,
            "Luxury cyberblade.",
            new Color(255, 200, 60)));
        SwordCatalog.register(new Variant(
            "sword_028", "Neon Pulse", 800,
            "Pulses with the city beat.",
            new Color(255, 60, 220)));
        SwordCatalog.register(new Variant(
            "sword_029", "Mecha Prime", 1300,
            "Original mass-production warblade.",
            new Color(230, 50, 50)));
        SwordCatalog.register(new HoloPhantom(
            "sword_030", "Holo Phantom", 2000,
            "Hardlight projection, deadly real."));
    }

    // ====================================================================
    //  ABSTRACT BASE -- structured double-edged tech trail.
    // ====================================================================

    static abstract class AbstractMecha implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color accent;

        AbstractMecha(String id, String name, int price, String tagline, Color accent) {
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

        /** Subclasses can override to animate accent color per-point. */
        protected Color accentFor(TrailPoint p) {
            return accent;
        }

        /**
         * For HoloPhantom we don't cache by id (the head would also need to
         * cycle hue). Subclasses can override cacheKey() to add the hue bucket.
         */
        protected String cacheKey() { return id; }

        /** Lazily build & cache a chrome mecha sword head. */
        protected BufferedImage cachedHead() {
            String key = cacheKey();
            BufferedImage cached = SwordHeadRenderer.getCached(key);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                paintMechaBlade(g, accent);
                SwordHeadRenderer.paintCrossGuard(g, new Color(120, 130, 150));
                paintMechaGrip(g, accent);
                SwordHeadRenderer.paintPommel(g, new Color(80, 90, 110), accent);
            } finally {
                g.dispose();
            }
            SwordHeadRenderer.putCached(key, cached);
            return cached;
        }

        /** Angular chrome blade with one accent-colored edge stripe. */
        private void paintMechaBlade(Graphics2D g, Color accentNow) {
            int tipX = SwordHeadRenderer.TIP_X;
            int tipY = SwordHeadRenderer.TIP_Y;
            int len = SwordHeadRenderer.BLADE_LEN;
            int half = SwordHeadRenderer.BLADE_HALF_H;

            // Sleek angular silhouette: chisel tip, straight back, flat base.
            Path2D.Double blade = new Path2D.Double();
            blade.moveTo(tipX, tipY);
            blade.lineTo(tipX + len * 0.18, tipY - half - 1);
            blade.lineTo(tipX + len, tipY - half);
            blade.lineTo(tipX + len, tipY + half);
            blade.lineTo(tipX + len * 0.18, tipY + half + 1);
            blade.closePath();

            SwordHeadRenderer.paintDropShadow(g, blade);

            // Vertical gradient: dark steel top -> chrome middle -> accent stripe bottom.
            LinearGradientPaint lg = new LinearGradientPaint(
                new Point2D.Float(0, tipY - half - 1),
                new Point2D.Float(0, tipY + half + 1),
                new float[] { 0f, 0.4f, 0.7f, 1f },
                new Color[] {
                    new Color(60, 70, 90),
                    CHROME,
                    SwordHeadRenderer.lighter(CHROME, 0.2f),
                    SwordHeadRenderer.lighter(accentNow, 0.2f)
                });
            Paint oldPaint = g.getPaint();
            g.setPaint(lg);
            g.fill(blade);

            // Center seam.
            g.setPaint(new Color(30, 35, 45, 180));
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(tipX + (int)(len * 0.12), tipY,
                       tipX + len, tipY);

            // Accent edge strip on the cutting edge.
            g.setPaint(new Color(accentNow.getRed(), accentNow.getGreen(),
                                 accentNow.getBlue(), 230));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(tipX + 1, tipY + half - 1,
                       tipX + len - 2, tipY + half - 1);

            // Bright edge highlight.
            g.setPaint(new Color(255, 255, 255, 230));
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(tipX, tipY, tipX + (int)(len * 0.18), tipY - half - 1);

            // Outline.
            g.setPaint(new Color(20, 25, 35, 230));
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(blade);

            g.setPaint(oldPaint);
        }

        /** Industrial grip: dark with accent piping and visible bolt rivets. */
        private void paintMechaGrip(Graphics2D g, Color accentNow) {
            int gx = SwordHeadRenderer.GRIP_X;
            int gy = SwordHeadRenderer.TIP_Y - SwordHeadRenderer.GRIP_H / 2;
            int w = SwordHeadRenderer.GRIP_W;
            int h = SwordHeadRenderer.GRIP_H;

            Path2D.Double grip = new Path2D.Double();
            grip.append(new java.awt.geom.RoundRectangle2D.Double(gx, gy, w, h, 2, 2), false);

            SwordHeadRenderer.paintDropShadow(g, grip);

            Paint oldPaint = g.getPaint();
            LinearGradientPaint lg = new LinearGradientPaint(
                new Point2D.Float(gx, gy), new Point2D.Float(gx, gy + h),
                new float[] { 0f, 0.5f, 1f },
                new Color[] {
                    new Color(40, 45, 60),
                    new Color(95, 105, 130),
                    new Color(40, 45, 60)
                });
            g.setPaint(lg);
            g.fill(grip);

            // Accent piping along the bottom edge of the grip.
            g.setPaint(new Color(accentNow.getRed(), accentNow.getGreen(),
                                 accentNow.getBlue(), 220));
            g.fillRect(gx, gy + h - 2, w, 2);

            // Rivets across the grip.
            for (int i = 0; i < 4; i++) {
                int rx = gx + 4 + i * 8;
                g.setPaint(new Color(20, 25, 35));
                g.fillOval(rx - 2, SwordHeadRenderer.TIP_Y - 2, 4, 4);
                g.setPaint(new Color(200, 210, 230));
                g.fillOval(rx - 1, SwordHeadRenderer.TIP_Y - 2, 2, 2);
            }

            g.setPaint(new Color(10, 12, 18, 230));
            g.setStroke(new BasicStroke(1f));
            g.draw(grip);
            g.setPaint(oldPaint);
        }

        @Override
        public void drawTrail(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            if (n < 2) return;

            // Build the two offset paths plus the center path.
            Path2D.Double leftPath = new Path2D.Double();
            Path2D.Double rightPath = new Path2D.Double();
            Path2D.Double corePath = new Path2D.Double();

            double offset = 5.0;
            boolean first = true;
            for (int i = 0; i < n; i++) {
                TrailPoint p = points.get(i);
                TrailPoint prev = points.get(Math.max(0, i - 1));
                TrailPoint next = points.get(Math.min(n - 1, i + 1));
                double dx = next.x - prev.x;
                double dy = next.y - prev.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len < 0.0001) len = 1.0;
                double nx = -dy / len;
                double ny = dx / len;

                double lx = p.x + nx * offset;
                double ly = p.y + ny * offset;
                double rx = p.x - nx * offset;
                double ry = p.y - ny * offset;

                if (first) {
                    leftPath.moveTo(lx, ly);
                    rightPath.moveTo(rx, ry);
                    corePath.moveTo(p.x, p.y);
                    first = false;
                } else {
                    leftPath.lineTo(lx, ly);
                    rightPath.lineTo(rx, ry);
                    corePath.lineTo(p.x, p.y);
                }
            }

            Color midAccent = accentFor(points.get(n / 2));
            g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(midAccent.getRed(), midAccent.getGreen(),
                                  midAccent.getBlue(), 55));
            g.draw(corePath);

            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(CHROME.getRed(), CHROME.getGreen(),
                                  CHROME.getBlue(), 230));
            g.draw(leftPath);

            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(midAccent.getRed(), midAccent.getGreen(),
                                  midAccent.getBlue(), 235));
            g.draw(rightPath);

            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 245));
            g.draw(corePath);

            // Data pixels along recent points.
            long now = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                TrailPoint p = points.get(i);
                long age = now - p.timeMs;
                if (age > 400) continue;
                long phase = (p.timeMs + i * 37L) % 90L;
                if (phase >= 30L) continue;

                int pxSize = 3 + (int) (phase % 3);
                Color a = accentFor(p);
                int alpha = (int) Math.max(60, 235 - age / 2);
                g.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(),
                                      Math.min(255, alpha)));
                int sx = (int) Math.round(p.x) - pxSize / 2;
                int sy = (int) Math.round(p.y) - pxSize / 2;
                g.fillRect(sx, sy, pxSize, pxSize);
                if (pxSize >= 5) {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fillRect(sx + 1, sy + 1, 1, 1);
                }
            }

            // (Sword sprite intentionally NOT drawn during gameplay --
            // slice-trail only. The cached head appears in drawPreview only.)

            // Signature flourish: holo scan-line on the blade.
            drawHoloScanLine(g, points);
        }

        /**
         * A bright horizontal scan-line that travels down the blade. We project
         * the line onto the rotated blade by transforming graphics to the trail
         * tip's local frame, then drawing a translucent line at the scan offset.
         */
        protected void drawHoloScanLine(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            if (n < 2) return;
            TrailPoint tip = points.get(0);
            TrailPoint next = points.get(1);
            double angle = Math.atan2(tip.y - next.y, tip.x - next.x);
            long now = System.currentTimeMillis();
            double progress = ((now % 1100L) / 1100.0); // 0..1 sweep

            // Position along the blade from tip toward hilt.
            double blade = SwordHeadRenderer.BLADE_LEN * 0.9;
            double distOnBlade = progress * blade;
            // Blade points OUT toward motion = backward into trail.
            // So scan-line position is at tip - direction * dist (toward hilt is opposite to motion).
            // Since the sword head graphic is drawn with the blade trailing BACK from tip,
            // we move in the OPPOSITE direction from motion.
            double rad = angle + Math.PI;
            double sx = tip.x + Math.cos(rad) * distOnBlade;
            double sy = tip.y + Math.sin(rad) * distOnBlade;

            // Perpendicular extent for the scan-line.
            double nx = -Math.sin(rad);
            double ny =  Math.cos(rad);
            double halfH = 8.0;
            int x1 = (int)(sx + nx * halfH);
            int y1 = (int)(sy + ny * halfH);
            int x2 = (int)(sx - nx * halfH);
            int y2 = (int)(sy - ny * halfH);

            Composite oldComp = g.getComposite();
            float scanAlpha = (float)(0.4 + 0.4 * Math.sin(progress * Math.PI));
            Color scanColor = accentFor(tip);
            // Bloom layer.
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scanAlpha * 0.4f));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(scanColor);
            g.drawLine(x1, y1, x2, y2);
            // Bright core.
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scanAlpha));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255));
            g.drawLine(x1, y1, x2, y2);
            g.setComposite(oldComp);
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

            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 0.0001) len = 1.0;
            double nx = -dy / len;
            double ny = dx / len;
            double off = 5.0;

            Color a = accent;

            g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 75));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(CHROME);
            g.drawLine(
                (int) Math.round(x1 + nx * off), (int) Math.round(y1 + ny * off),
                (int) Math.round(x2 + nx * off), (int) Math.round(y2 + ny * off));

            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(a);
            g.drawLine(
                (int) Math.round(x1 - nx * off), (int) Math.round(y1 - ny * off),
                (int) Math.round(x2 - nx * off), (int) Math.round(y2 - ny * off));

            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 240));
            g.drawLine(x1, y1, x2, y2);

            int steps = 7;
            for (int i = 1; i < steps; i++) {
                double t = i / (double) steps;
                int px = (int) Math.round(x1 + dx * t);
                int py = (int) Math.round(y1 + dy * t);
                int pxSize = 3 + (i % 3);
                int jitterX = ((i * 17) % 7) - 3;
                int jitterY = ((i * 23) % 7) - 3;
                g.setColor(a);
                g.fillRect(px + jitterX - pxSize / 2,
                           py + jitterY - pxSize / 2,
                           pxSize, pxSize);
            }

            // Mini cached head at tip.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractMecha {
        Variant(String id, String name, int price, String tagline, Color accent) {
            super(id, name, price, tagline, accent);
        }
    }

    /**
     * Holo Phantom: hardlight projection. Accent color shifts through the hue
     * wheel based on each point's age. Cached head uses the canonical violet
     * tone (head doesn't cycle, but the trail and scan-line do).
     */
    private static final class HoloPhantom extends AbstractMecha {
        HoloPhantom(String id, String name, int price, String tagline) {
            super(id, name, price, tagline, Color.getHSBColor(0.78f, 0.8f, 1.0f));
        }

        @Override
        protected Color accentFor(TrailPoint p) {
            float hue = ((p.timeMs / 6L) % 360L) / 360.0f;
            return Color.getHSBColor(hue, 0.85f, 1.0f);
        }
    }
}
