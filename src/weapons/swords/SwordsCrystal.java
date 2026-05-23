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
 * Crystal / Gemstone family. Translucent faceted blades cut from gemstone.
 * 5 variants registered under sword_011 .. sword_015.
 *
 * <p>Each blade head is pre-rendered into a BufferedImage with a faceted
 * crystal silhouette, internal facet shading via linear gradients between
 * the three tints, a sharp white facet highlight, and a gem-inlaid pommel.
 * Motion-blur afterimages drag behind during a slice.
 *
 * <p>Signature flourish: light refraction rays radiating outward from the
 * pommel gem.
 */
public final class SwordsCrystal {

    private SwordsCrystal() {}

    static {
        SwordCatalog.register(new Variant(
            "sword_011", "Ruby Shard", 350,
            "Cut from a king's tomb.",
            new Color(220, 40, 60),
            new Color[]{
                new Color(220, 40, 60),
                new Color(150, 20, 40),
                new Color(255, 140, 160)
            },
            null));
        SwordCatalog.register(new Variant(
            "sword_012", "Sapphire Tooth", 500,
            "Cold deep-sea blue.",
            new Color(40, 90, 220),
            new Color[]{
                new Color(40, 90, 220),
                new Color(15, 40, 130),
                new Color(150, 200, 255)
            },
            null));
        SwordCatalog.register(new Variant(
            "sword_013", "Emerald Fang", 700,
            "Living crystal.",
            new Color(40, 180, 90),
            new Color[]{
                new Color(40, 180, 90),
                new Color(15, 90, 50),
                new Color(160, 240, 180)
            },
            null));
        SwordCatalog.register(new Variant(
            "sword_014", "Amethyst Spike", 1000,
            "Royal violet edge.",
            new Color(150, 70, 210),
            new Color[]{
                new Color(150, 70, 210),
                new Color(80, 30, 130),
                new Color(210, 170, 250)
            },
            null));
        SwordCatalog.register(new Variant(
            "sword_015", "Obsidian Razor", 1500,
            "Volcanic glass, sharper than steel.",
            new Color(25, 25, 35),
            new Color[]{
                new Color(25, 25, 35),
                new Color(10, 10, 18),
                new Color(60, 60, 80)
            },
            new Color(80, 230, 255)));
    }

    // ====================================================================
    //  ABSTRACT BASE -- all visual logic lives here.
    // ====================================================================

    static abstract class AbstractCrystal implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color accent;
        protected final Color[] facetColors;
        protected final Color innerGlow;

        AbstractCrystal(String id, String name, int price, String tagline,
                        Color accent, Color[] facetColors, Color innerGlow) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.accent = accent;
            this.facetColors = facetColors;
            this.innerGlow = innerGlow;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        /** Lazily build & cache a faceted crystal blade head. */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                int tipX = SwordHeadRenderer.TIP_X;
                int tipY = SwordHeadRenderer.TIP_Y;
                int len = SwordHeadRenderer.BLADE_LEN;

                // Faceted shard silhouette: pointed tip, wide mid, taper to base.
                Path2D.Double shard = new Path2D.Double();
                shard.moveTo(tipX, tipY);
                shard.lineTo(tipX + len * 0.4, tipY - 9);
                shard.lineTo(tipX + len, tipY - 4);
                shard.lineTo(tipX + len, tipY + 4);
                shard.lineTo(tipX + len * 0.4, tipY + 9);
                shard.closePath();

                SwordHeadRenderer.paintDropShadow(g, shard);

                // Linear gradient across top of crystal: light tint -> base -> dark.
                float topY = tipY - 9f;
                float botY = tipY + 9f;
                Color light = facetColors[2];
                Color body = facetColors[0];
                Color dark = facetColors[1];
                LinearGradientPaint lg = new LinearGradientPaint(
                    new Point2D.Float(0, topY), new Point2D.Float(0, botY),
                    new float[] { 0f, 0.5f, 1f },
                    new Color[] { light, body, dark });
                Paint oldPaint = g.getPaint();
                g.setPaint(lg);
                g.fill(shard);

                // Inner facet darkening (top triangle).
                Path2D.Double topFacet = new Path2D.Double();
                topFacet.moveTo(tipX, tipY);
                topFacet.lineTo(tipX + len * 0.4, tipY - 9);
                topFacet.lineTo(tipX + len, tipY - 4);
                topFacet.lineTo(tipX + len * 0.5, tipY);
                topFacet.closePath();
                g.setPaint(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), 130));
                g.fill(topFacet);

                // Central facet line.
                g.setPaint(new Color(255, 255, 255, 200));
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(tipX, tipY,
                           (int)(tipX + len * 0.5), tipY);
                // Crossbar facet line.
                g.drawLine((int)(tipX + len * 0.4), tipY - 9,
                           (int)(tipX + len * 0.4), tipY + 9);

                // Bright edge highlight along the upper-front edge.
                g.setPaint(new Color(255, 255, 255, 240));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(tipX, tipY, (int)(tipX + len * 0.4), tipY - 9);

                // Outline.
                g.setPaint(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), 240));
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(shard);

                // Inner glow gem (obsidian etc.).
                if (innerGlow != null) {
                    g.setPaint(new Color(innerGlow.getRed(), innerGlow.getGreen(),
                                         innerGlow.getBlue(), 220));
                    g.fillOval((int)(tipX + len * 0.5) - 4, tipY - 4, 8, 8);
                }

                g.setPaint(oldPaint);

                // Crystal-themed hilt: silver guard, dark grip, gem pommel.
                SwordHeadRenderer.paintCrossGuard(g, SwordHeadRenderer.lighter(accent, 0.3f));
                SwordHeadRenderer.paintGrip(g, new Color(40, 42, 55));
                SwordHeadRenderer.paintPommel(g, new Color(60, 60, 70),
                                              facetColors[facetColors.length - 1]);
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

            // Outer bloom halo.
            Path2D.Double bloom = new Path2D.Double();
            boolean bfirst = true;
            for (TrailPoint p : points) {
                if (bfirst) { bloom.moveTo(p.x, p.y); bfirst = false; }
                else { bloom.lineTo(p.x, p.y); }
            }
            g.setStroke(new BasicStroke(38f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 30));
            g.draw(bloom);

            if (innerGlow != null) {
                Path2D.Double halo = new Path2D.Double();
                boolean first = true;
                for (TrailPoint p : points) {
                    if (first) { halo.moveTo(p.x, p.y); first = false; }
                    else { halo.lineTo(p.x, p.y); }
                }
                g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(innerGlow.getRed(), innerGlow.getGreen(),
                                     innerGlow.getBlue(), 70));
                g.draw(halo);
            }

            // Faceted segments.
            for (int i = 1; i < n; i++) {
                TrailPoint a = points.get(i - 1);
                TrailPoint b = points.get(i);
                Color tint = facetColors[i % facetColors.length];

                double dx = b.x - a.x;
                double dy = b.y - a.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                if (len < 0.0001) continue;
                double nxp = -dy / len;
                double nyp =  dx / len;

                g.setStroke(new BasicStroke(11f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 140));
                g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);

                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                Color dark = facetColors[1];
                g.setColor(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), 180));
                g.drawLine((int) (a.x + nxp * 2.0), (int) (a.y + nyp * 2.0),
                           (int) (b.x + nxp * 2.0), (int) (b.y + nyp * 2.0));

                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                g.setColor(new Color(255, 255, 255, 220));
                g.drawLine((int) (a.x - nxp * 3.0), (int) (a.y - nyp * 3.0),
                           (int) (b.x - nxp * 3.0), (int) (b.y - nyp * 3.0));
            }

            // Shard particles near the freshest points.
            int particleCount = Math.min(5, n);
            for (int i = 0; i < particleCount; i++) {
                TrailPoint p = points.get(n - 1 - i);
                double age = (n - 1 - i) / (double) Math.max(1, n - 1);
                int alpha = (int) (200 * (1.0 - age));
                if (alpha < 20) continue;
                int size = 4 + (i % 3);
                double ox = (((i * 31) % 13) - 6);
                double oy = (((i * 47) % 11) - 5);
                drawShard(g, p.x + ox, p.y + oy, size,
                          new Color(facetColors[2].getRed(),
                                    facetColors[2].getGreen(),
                                    facetColors[2].getBlue(),
                                    Math.max(0, Math.min(255, alpha))));
            }

            drawSparkSpecks(g, points);

            // (Sword sprite intentionally NOT drawn during gameplay --
            // slice-trail only. The cached head appears in drawPreview only.)

            // Signature flourish: refraction rays from the pommel gem.
            drawRefractionRays(g, points);
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
                g.setColor(new Color(255, 255, 255, 235));
                g.fillOval((int) (p.x + ox) - sz / 2,
                           (int) (p.y + oy) - sz / 2, sz, sz);
            }
        }

        /**
         * Refraction rays: 6 thin radial lines emanating from the pommel
         * end of the sword (just past the trail tip), rotated by point age.
         */
        protected void drawRefractionRays(Graphics2D g, List<TrailPoint> points) {
            if (points.size() < 2) return;
            TrailPoint tip = points.get(0);
            TrailPoint next = points.get(1);
            double angleDeg = Math.toDegrees(Math.atan2(tip.y - next.y, tip.x - next.x));

            // Pommel sits at tipX + IMG_W - GUARD_X - etc; approximate at trail
            // tip back-direction by the blade visual length.
            double rad = Math.toRadians(angleDeg);
            double backDist = SwordHeadRenderer.GUARD_X + SwordHeadRenderer.GRIP_W
                              + SwordHeadRenderer.POMMEL_W / 2.0
                              - SwordHeadRenderer.TIP_X;
            double gemX = tip.x - Math.cos(rad) * backDist;
            double gemY = tip.y - Math.sin(rad) * backDist;

            long now = System.currentTimeMillis();
            double spin = (now % 4000L) / 4000.0 * Math.PI * 2.0;
            int rays = 6;
            Composite oldComp = g.getComposite();
            Color rayColor = (innerGlow != null) ? innerGlow : facetColors[2];

            for (int i = 0; i < rays; i++) {
                double a = spin + (i / (double) rays) * Math.PI * 2.0;
                double rLen = 16.0 + Math.sin(now * 0.005 + i) * 4.0;
                double ex = gemX + Math.cos(a) * rLen;
                double ey = gemY + Math.sin(a) * rLen;

                // Thick faint ray.
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(rayColor);
                g.drawLine((int) gemX, (int) gemY, (int) ex, (int) ey);
                // Thin bright ray.
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 255, 255, 230));
                g.drawLine((int) gemX, (int) gemY,
                           (int) (gemX + Math.cos(a) * rLen * 0.85),
                           (int) (gemY + Math.sin(a) * rLen * 0.85));
            }
            g.setComposite(oldComp);
        }

        /**
         * Legacy entry point -- uses cached BufferedImage now.
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

        /** Draw a small diamond shard centered at (cx, cy). */
        private void drawShard(Graphics2D g, double cx, double cy, int size, Color c) {
            Path2D.Double diamond = new Path2D.Double();
            diamond.moveTo(cx,         cy - size);
            diamond.lineTo(cx + size,  cy);
            diamond.lineTo(cx,         cy + size);
            diamond.lineTo(cx - size,  cy);
            diamond.closePath();
            g.setColor(c);
            g.fill(diamond);
            g.setStroke(new BasicStroke(1f));
            g.setColor(new Color(255, 255, 255, Math.min(255, c.getAlpha() + 30)));
            g.draw(diamond);
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            int x1 = x + 12, y1 = y + h - 16;
            int x2 = x + w - 12, y2 = y + 16;

            if (innerGlow != null) {
                g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(innerGlow.getRed(), innerGlow.getGreen(),
                                     innerGlow.getBlue(), 90));
                g.drawLine(x1, y1, x2, y2);
            }

            int diamonds = 6;
            double brightestIdx = 2;
            for (int i = 0; i < diamonds; i++) {
                double t = (i + 0.5) / diamonds;
                double cx = x1 + (x2 - x1) * t;
                double cy = y1 + (y2 - y1) * t;
                int size = (i == 0 || i == diamonds - 1) ? 7 : 11;

                Color body = facetColors[i % facetColors.length];

                Path2D.Double diamond = new Path2D.Double();
                diamond.moveTo(cx,        cy - size);
                diamond.lineTo(cx + size, cy);
                diamond.lineTo(cx,        cy + size);
                diamond.lineTo(cx - size, cy);
                diamond.closePath();

                g.setColor(new Color(body.getRed(), body.getGreen(), body.getBlue(), 200));
                g.fill(diamond);

                g.setStroke(new BasicStroke(1.4f));
                g.setColor(new Color(facetColors[1].getRed(),
                                     facetColors[1].getGreen(),
                                     facetColors[1].getBlue(), 230));
                g.draw(diamond);

                if (i == (int) brightestIdx) {
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(255, 255, 255, 240));
                    g.drawLine((int) (cx - size), (int) cy,
                               (int) cx, (int) (cy - size));
                }
            }

            // Mini crystal sword head at the tip of the slash.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractCrystal {
        Variant(String id, String name, int price, String tagline,
                Color accent, Color[] facetColors, Color innerGlow) {
            super(id, name, price, tagline, accent, facetColors, innerGlow);
        }
    }
}
