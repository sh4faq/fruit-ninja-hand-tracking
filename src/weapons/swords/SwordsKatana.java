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
 * Katana family. Clean steel-blade slash with a colored hilt-wrap accent.
 * 5 variants registered under sword_001 .. sword_005.
 *
 * <p>This family renders its sword head into a cached {@link BufferedImage}
 * (composed via {@link SwordHeadRenderer}'s gradient blade body, grip,
 * cross-guard, pommel, and drop shadow). The trail tip is drawn with the
 * cached image plus 2-3 motion-blur afterimages at recent trail points so
 * the blade looks like it's arcing through the air.
 *
 * <p>Signature flourish: a cherry-blossom petal drifting off the tip.
 */
public final class SwordsKatana {

    private SwordsKatana() {}

    static {
        SwordCatalog.register(new Variant(
            "sword_001", "Bamboo Bokken", 0,
            "Trainee's wooden blade.",
            new Color(220, 220, 230), new Color(140, 80, 40)));
        SwordCatalog.register(new Variant(
            "sword_002", "Steel Katana", 80,
            "The forge classic.",
            new Color(230, 235, 250), new Color(20, 80, 140)));
        SwordCatalog.register(new Variant(
            "sword_003", "Crimson Edge", 200,
            "Wrapped in red silk.",
            new Color(255, 240, 240), new Color(190, 30, 30)));
        SwordCatalog.register(new Variant(
            "sword_004", "Moonlit Tachi", 450,
            "Cuts on the new moon.",
            new Color(200, 230, 255), new Color(80, 130, 220)));
        SwordCatalog.register(new Variant(
            "sword_005", "Black Lotus Wakizashi", 800,
            "Forbidden temple blade.",
            new Color(140, 130, 200), new Color(60, 30, 70)));
    }

    // ====================================================================
    //  ABSTRACT BASE -- all visual logic lives here.
    // ====================================================================

    static abstract class AbstractKatana implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color bladeColor;
        protected final Color hiltColor;

        AbstractKatana(String id, String name, int price, String tagline,
                       Color bladeColor, Color hiltColor) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.bladeColor = bladeColor;
            this.hiltColor = hiltColor;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return hiltColor; }

        /** Lazily build and cache the rendered sword head image for this variant. */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                // Drop shadow + blade body with gradient + fuller + edge inner glow.
                SwordHeadRenderer.paintBladeBody(g, bladeColor, hiltColor);
                // Tsuba style cross-guard in dark steel.
                SwordHeadRenderer.paintCrossGuard(g, new Color(60, 60, 75));
                // Grip wrap uses the variant hilt color.
                SwordHeadRenderer.paintGrip(g, hiltColor);
                // Pommel cap dark steel with a gem inlay in the variant accent.
                SwordHeadRenderer.paintPommel(g, new Color(45, 45, 55), hiltColor);
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

            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (TrailPoint p : points) {
                if (first) { path.moveTo(p.x, p.y); first = false; }
                else { path.lineTo(p.x, p.y); }
            }

            // Outermost bloom halo (very low alpha, wide) for genuine glow.
            g.setStroke(new BasicStroke(40f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(hiltColor.getRed(), hiltColor.getGreen(),
                                  hiltColor.getBlue(), 30));
            g.draw(path);

            // Outer glow tinted by the hilt color (the "personality" color)
            g.setStroke(new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(hiltColor.getRed(), hiltColor.getGreen(),
                                  hiltColor.getBlue(), 65));
            g.draw(path);

            // Mid layer in steel blade color, half opacity
            g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(bladeColor.getRed(), bladeColor.getGreen(),
                                  bladeColor.getBlue(), 130));
            g.draw(path);

            // Bright white core (the blade's polished edge)
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 230));
            g.draw(path);

            // Sparkle specks along recent points.
            drawSparkSpecks(g, points);

            // (Sword sprite intentionally NOT drawn during gameplay -- real
            // Fruit Ninja only shows the slice trail, never the hand-held
            // weapon. The cached head sprite still appears in drawPreview so
            // shop browsers can see what they're buying.)

            // Signature flourish: cherry-blossom petals drifting off the tip.
            drawCherryBlossomPetals(g, points);
        }

        /** Draw 3-5 small bright specks at random recent trail points. */
        protected void drawSparkSpecks(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            int specks = Math.min(5, n);
            for (int i = 0; i < specks; i++) {
                int idx = (int) (((i * 53L) ^ (points.get(0).timeMs >>> 4)) % n);
                if (idx < 0) idx = -idx;
                if (idx >= n) idx = idx % n;
                TrailPoint p = points.get(idx);
                long seed = p.timeMs ^ (i * 2654435761L);
                double ox = ((seed >> 3) & 7L) - 3.5;
                double oy = ((seed >> 7) & 7L) - 3.5;
                int sz = 2 + (int) ((seed >> 11) & 1L);
                g.setColor(new Color(255, 255, 255, 230));
                g.fillOval((int) (p.x + ox) - sz / 2,
                           (int) (p.y + oy) - sz / 2, sz, sz);
            }
        }

        /**
         * Signature: tiny cherry-blossom petals drifting off the tip and
         * tumbling away with a slow rotation.
         */
        protected void drawCherryBlossomPetals(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            long now = System.currentTimeMillis();
            Composite oldComp = g.getComposite();
            int petals = Math.min(6, n);
            for (int i = 0; i < petals; i++) {
                TrailPoint p = points.get(i);
                long seed = p.timeMs ^ (i * 0x9E3779B97F4A7C15L);
                if ((seed & 3L) != 0L) continue; // ~25% chance per point

                double drift = (now - p.timeMs) * 0.04;
                double ox = ((seed >> 5) & 31L) - 16.0;
                double oy = -((seed >> 11) & 15L) - drift * 0.6;
                double rot = ((seed >> 17) & 0xFF) / 255.0 * Math.PI * 2.0
                           + (now - p.timeMs) * 0.004;

                double cx = p.x + ox;
                double cy = p.y + oy;
                int alpha = (int) (200 * Math.max(0.0, p.life) - drift * 1.5);
                if (alpha < 30) continue;
                if (alpha > 230) alpha = 230;

                g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha / 255f));
                drawPetal(g, cx, cy, rot, 5.0, petalColor());
            }
            g.setComposite(oldComp);
        }

        /** Petal color: pale pink for steel/red blades, soft blue for moonlit. */
        protected Color petalColor() {
            // Mix hilt color toward white for a soft sakura tint.
            return SwordHeadRenderer.lighter(hiltColor, 0.65f);
        }

        /** Draw one little 5-lobed petal centered at (cx, cy). */
        private void drawPetal(Graphics2D g, double cx, double cy,
                               double rot, double size, Color petal) {
            AffineTransform old = g.getTransform();
            g.translate(cx, cy);
            g.rotate(rot);

            // 5 small ovals arranged around the center make a flower silhouette.
            int lobes = 5;
            for (int i = 0; i < lobes; i++) {
                double a = (i / (double) lobes) * Math.PI * 2.0;
                double lx = Math.cos(a) * size * 0.7;
                double ly = Math.sin(a) * size * 0.7;
                AffineTransform lobeT = g.getTransform();
                g.translate(lx, ly);
                g.rotate(a);
                g.setColor(petal);
                g.fillOval((int) -size, (int) (-size * 0.4),
                           (int) (size * 2), (int) (size * 0.8));
                g.setColor(new Color(255, 255, 255, 200));
                g.fillOval((int) -size + 1, (int) (-size * 0.4) + 1,
                           (int) (size * 1.2), (int) (size * 0.4));
                g.setTransform(lobeT);
            }
            // Tiny gold center.
            g.setColor(new Color(255, 220, 120, 240));
            g.fillOval((int) -size / 4, (int) -size / 4,
                       (int) (size / 2 + 1), (int) (size / 2 + 1));

            g.setTransform(old);
        }

        /**
         * Legacy entry point kept for parity. The cached BufferedImage path
         * is now used by drawTrail; this method falls back to drawing the
         * cached head once at (tipX, tipY).
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
            // Diagonal slash across the card
            int x1 = x + 12, y1 = y + h - 16;
            int x2 = x + w - 12, y2 = y + 16;

            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(hiltColor.getRed(), hiltColor.getGreen(),
                                  hiltColor.getBlue(), 90));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(bladeColor);
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 230));
            g.drawLine(x1, y1, x2, y2);

            // Mini sword head (cached image) at the tip of the slash.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractKatana {
        Variant(String id, String name, int price, String tagline,
                Color bladeColor, Color hiltColor) {
            super(id, name, price, tagline, bladeColor, hiltColor);
        }
    }
}
