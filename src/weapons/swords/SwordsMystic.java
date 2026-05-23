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
 * Mystic / Spirit family. Druid, fae, celestial, shamanic, and oracular
 * blades. Each variant has a cached BufferedImage sword head with a soft
 * pale blade, gradient hilt and accented gem pommel. The trail still
 * breathes with a spirit haze; floating rune symbols orbit around the hilt.
 *
 * 5 variants registered under sword_031 .. sword_035.
 */
public final class SwordsMystic {

    private SwordsMystic() {}

    static {
        SwordCatalog.register(new Variant(
            "sword_031", "Druid's Branch", 380,
            "Living wood, soft green light.",
            new Color(120, 220, 130), new Color(70, 140, 80)));
        SwordCatalog.register(new Variant(
            "sword_032", "Faerie Sliver", 550,
            "Forged in the hollow hill.",
            new Color(230, 130, 220), new Color(170, 80, 200)));
        SwordCatalog.register(new Variant(
            "sword_033", "Celestial Edge", 850,
            "A piece of the night sky.",
            new Color(255, 230, 150), new Color(255, 250, 220)));
        SwordCatalog.register(new Variant(
            "sword_034", "Shaman's Bone", 1100,
            "Whispered to by ancestors.",
            new Color(245, 240, 220), new Color(180, 160, 130)));
        SwordCatalog.register(new Variant(
            "sword_035", "Oracle's Mirror", 1700,
            "Cuts past, present, and future.",
            new Color(210, 225, 240), new Color(90, 150, 220)));
    }

    // ====================================================================
    //  ABSTRACT BASE -- all visual logic lives here.
    // ====================================================================

    static abstract class AbstractMystic implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color accentColor;
        protected final Color secondaryColor;

        AbstractMystic(String id, String name, int price, String tagline,
                       Color accentColor, Color secondaryColor) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.accentColor = accentColor;
            this.secondaryColor = secondaryColor;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accentColor; }

        /** Lazily build & cache the spirit blade. */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                // Pale ethereal blade body with accent inner glow.
                SwordHeadRenderer.paintBladeBody(g,
                    SwordHeadRenderer.lighter(accentColor, 0.7f),
                    accentColor);
                // Hilt: secondary color guard, dark grip, accent gem pommel.
                SwordHeadRenderer.paintCrossGuard(g, secondaryColor);
                SwordHeadRenderer.paintGrip(g,
                    SwordHeadRenderer.darker(secondaryColor, 0.4f));
                SwordHeadRenderer.paintPommel(g,
                    SwordHeadRenderer.darker(secondaryColor, 0.2f),
                    accentColor);

                // Etched mystic glyph on the blade.
                int tipX = SwordHeadRenderer.TIP_X;
                int tipY = SwordHeadRenderer.TIP_Y;
                int len = SwordHeadRenderer.BLADE_LEN;
                g.setColor(new Color(255, 255, 255, 180));
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int gx = tipX + (int)(len * 0.55);
                g.drawOval(gx - 3, tipY - 3, 6, 6);
                g.drawLine(gx - 5, tipY, gx + 5, tipY);
                g.drawLine(gx, tipY - 5, gx, tipY + 5);
            } finally {
                g.dispose();
            }
            SwordHeadRenderer.putCached(id, cached);
            return cached;
        }

        @Override
        public void drawTrail(Graphics2D g, List<TrailPoint> points) {
            if (points.size() < 2) return;

            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (TrailPoint p : points) {
                if (first) { path.moveTo(p.x, p.y); first = false; }
                else { path.lineTo(p.x, p.y); }
            }

            // 1) Cloudy spirit haze
            Color haze = new Color(secondaryColor.getRed(),
                                   secondaryColor.getGreen(),
                                   secondaryColor.getBlue(), 32);
            g.setColor(haze);
            int idx = 0;
            for (TrailPoint p : points) {
                double life = clamp01(p.life);
                if (life <= 0.0) { idx++; continue; }
                int r = 12;
                double phase = ((p.timeMs % 1200L) / 1200.0) * Math.PI * 2.0;
                int wobble = (int) Math.round(Math.sin(phase + idx * 0.4) * 2.0);
                int rr = r + wobble;
                g.fillOval((int) Math.round(p.x - rr),
                           (int) Math.round(p.y - rr),
                           rr * 2, rr * 2);
                idx++;
            }

            // 2) Mid accent glow
            g.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND,
                                              BasicStroke.JOIN_ROUND));
            g.setColor(new Color(accentColor.getRed(),
                                 accentColor.getGreen(),
                                 accentColor.getBlue(), 150));
            g.draw(path);

            // 3) Bright thin core
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND,
                                               BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 235));
            g.draw(path);

            // 4) Floating rune particles along the trail (original behaviour kept).
            int step = 0;
            for (TrailPoint p : points) {
                if ((step % 4) == 0 && p.life > 0.15) {
                    drawRune(g, p, step);
                }
                step++;
            }

            // (Sword sprite intentionally NOT drawn during gameplay --
            // slice-trail only. The cached head appears in drawPreview only.)

            // 6) Signature flourish: runes ORBITING the hilt.
            drawOrbitingRunes(g, points);
        }

        /**
         * Orbiting runes around the hilt of the sword: 4 rune symbols
         * positioned in a ring around the grip/pommel of the trail tip, all
         * rotating together by a global clock.
         */
        protected void drawOrbitingRunes(Graphics2D g, List<TrailPoint> points) {
            if (points.size() < 2) return;
            TrailPoint tip = points.get(0);
            TrailPoint next = points.get(1);
            double angleDeg = Math.toDegrees(Math.atan2(tip.y - next.y, tip.x - next.x));
            double rad = Math.toRadians(angleDeg);
            // Hilt center: from tip, go back the length of blade + half grip.
            double backDist = SwordHeadRenderer.GUARD_X + SwordHeadRenderer.GRIP_W / 2.0
                              - SwordHeadRenderer.TIP_X;
            double cx = tip.x - Math.cos(rad) * backDist;
            double cy = tip.y - Math.sin(rad) * backDist;

            long now = System.currentTimeMillis();
            double spin = (now % 5000L) / 5000.0 * Math.PI * 2.0;

            Composite oldComp = g.getComposite();
            int runes = 4;
            for (int i = 0; i < runes; i++) {
                double a = spin + (i / (double) runes) * Math.PI * 2.0;
                double r = 18.0;
                double rx = cx + Math.cos(a) * r;
                double ry = cy + Math.sin(a) * r;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                drawRuneSymbol(g, rx, ry, a + spin * 2.0, 5, i,
                               new Color(255, 255, 255, 230));
            }
            g.setComposite(oldComp);
        }

        /** One tiny rotating rune-shape near a trail point (original look). */
        private void drawRune(Graphics2D g, TrailPoint p, int seed) {
            double angle = ((p.timeMs % 3600L) / 3600.0) * Math.PI * 2.0
                           + seed * 0.7;
            int alpha = (int) Math.round(220 * clamp01(p.life));
            int shape = (seed / 4) & 3;
            drawRuneSymbol(g, p.x, p.y, angle, 4, shape,
                           new Color(255, 255, 255, alpha));
        }

        /** Rune symbol primitive used by both the in-trail runes and orbiters. */
        private void drawRuneSymbol(Graphics2D g, double cx, double cy,
                                    double rot, int s, int shape, Color col) {
            AffineTransform old = g.getTransform();
            g.translate(cx, cy);
            g.rotate(rot);
            g.setColor(col);
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (shape & 3) {
                case 0:
                    g.drawOval(-s, -s, s * 2, s * 2);
                    break;
                case 1:
                    Path2D.Double tri = new Path2D.Double();
                    tri.moveTo(0, -s);
                    tri.lineTo(s, s);
                    tri.lineTo(-s, s);
                    tri.closePath();
                    g.draw(tri);
                    break;
                case 2:
                    g.drawRect(-s, -s, s * 2, s * 2);
                    break;
                default:
                    g.drawLine(-s, 0, s, 0);
                    g.drawLine(0, -s, 0, s);
                    break;
            }
            g.setTransform(old);
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

            g.setColor(new Color(secondaryColor.getRed(),
                                 secondaryColor.getGreen(),
                                 secondaryColor.getBlue(), 70));
            for (int i = 0; i <= 6; i++) {
                double t = i / 6.0;
                int cx = (int) Math.round(x1 + (x2 - x1) * t);
                int cy = (int) Math.round(y1 + (y2 - y1) * t);
                int r = 11;
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
            }

            g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND,
                                              BasicStroke.JOIN_ROUND));
            g.setColor(new Color(accentColor.getRed(),
                                 accentColor.getGreen(),
                                 accentColor.getBlue(), 180));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                                               BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 235));
            g.drawLine(x1, y1, x2, y2);

            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND,
                                               BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 230));

            int midX = (x1 + x2) / 2;
            int midY = (y1 + y2) / 2;

            drawPreviewRune(g, midX - 22, midY - 14, 5, 0);
            drawPreviewRune(g, midX + 20, midY + 12, 4, 1);
            drawPreviewRune(g, x2 - 14, y2 + 14, 4, 2);
            drawPreviewRune(g, x1 + 14, y1 - 14, 5, 3);

            // Mini cached head at tip.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }

        private void drawPreviewRune(Graphics2D g, int cx, int cy,
                                     int s, int shape) {
            switch (shape) {
                case 0:
                    Path2D.Double tri = new Path2D.Double();
                    tri.moveTo(cx, cy - s);
                    tri.lineTo(cx + s, cy + s);
                    tri.lineTo(cx - s, cy + s);
                    tri.closePath();
                    g.draw(tri);
                    break;
                case 1:
                    g.drawOval(cx - s, cy - s, s * 2, s * 2);
                    break;
                case 2:
                    g.drawRect(cx - s, cy - s, s * 2, s * 2);
                    break;
                default:
                    g.drawLine(cx - s, cy, cx + s, cy);
                    g.drawLine(cx, cy - s, cx, cy + s);
                    break;
            }
        }

        private static double clamp01(double v) {
            if (v < 0.0) return 0.0;
            if (v > 1.0) return 1.0;
            return v;
        }
    }

    private static final class Variant extends AbstractMystic {
        Variant(String id, String name, int price, String tagline,
                Color accentColor, Color secondaryColor) {
            super(id, name, price, tagline, accentColor, secondaryColor);
        }
    }
}
