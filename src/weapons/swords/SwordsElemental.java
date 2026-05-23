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
 * Elemental family. Blades that channel a classic fantasy element
 * (fire / ice / lightning / wind / earth). Each variant gets its own cached
 * BufferedImage sword head whose hilt and pommel reflect its element.
 *
 * 5 variants registered under sword_021 .. sword_025.
 *
 * <p>Per-element signature flourish layered on top of the trail.
 */
public final class SwordsElemental {

    private SwordsElemental() {}

    enum ElementKind { FIRE, ICE, LIGHTNING, WIND, EARTH }

    static {
        SwordCatalog.register(new Variant("sword_021", "Pyre Katana", 400,
            "Fire flickers along the edge", ElementKind.FIRE,
            new Color(255, 140, 40)));
        SwordCatalog.register(new Variant("sword_022", "Glacier Edge", 500,
            "Frozen mist trails behind", ElementKind.ICE,
            new Color(160, 220, 255)));
        SwordCatalog.register(new Variant("sword_023", "Storm Caller", 750,
            "Lightning chases the blade", ElementKind.LIGHTNING,
            new Color(255, 230, 80)));
        SwordCatalog.register(new Variant("sword_024", "Tempest Blade", 1000,
            "Carves wind into walls", ElementKind.WIND,
            new Color(70, 200, 200)));
        SwordCatalog.register(new Variant("sword_025", "Stoneheart Cleaver", 1600,
            "Rock and root, unmoving", ElementKind.EARTH,
            new Color(130, 90, 55)));
    }

    // ====================================================================
    //  ABSTRACT BASE -- all visual logic lives here, branching on element.
    // ====================================================================

    static abstract class AbstractElemental implements Sword {
        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final ElementKind kind;
        protected final Color accent;

        AbstractElemental(String id, String name, int price, String tagline,
                          ElementKind kind, Color accent) {
            this.id = id; this.name = name; this.price = price;
            this.tagline = tagline; this.kind = kind; this.accent = accent;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        /** Lazily build & cache the elemental sword head. */
        private BufferedImage cachedHead() {
            BufferedImage cached = SwordHeadRenderer.getCached(id);
            if (cached != null) return cached;
            cached = SwordHeadRenderer.newCanvas();
            Graphics2D g = SwordHeadRenderer.openGraphics(cached);
            try {
                // Pick a steel color per element.
                Color steel;
                Color guardCol;
                Color gripCol;
                Color pommelCap;
                switch (kind) {
                    case FIRE:
                        steel = new Color(220, 180, 120);
                        guardCol = new Color(90, 50, 30);
                        gripCol = new Color(80, 25, 15);
                        pommelCap = new Color(110, 50, 30);
                        break;
                    case ICE:
                        steel = new Color(220, 240, 255);
                        guardCol = new Color(100, 140, 180);
                        gripCol = new Color(50, 80, 110);
                        pommelCap = new Color(70, 110, 150);
                        break;
                    case LIGHTNING:
                        steel = new Color(240, 240, 220);
                        guardCol = new Color(160, 140, 50);
                        gripCol = new Color(60, 50, 20);
                        pommelCap = new Color(120, 100, 30);
                        break;
                    case WIND:
                        steel = new Color(220, 235, 235);
                        guardCol = new Color(80, 130, 130);
                        gripCol = new Color(50, 90, 90);
                        pommelCap = new Color(60, 110, 110);
                        break;
                    case EARTH:
                    default:
                        steel = new Color(180, 150, 110);
                        guardCol = new Color(90, 65, 35);
                        gripCol = new Color(60, 40, 20);
                        pommelCap = new Color(80, 55, 30);
                        break;
                }
                SwordHeadRenderer.paintBladeBody(g, steel, accent);
                SwordHeadRenderer.paintCrossGuard(g, guardCol);
                SwordHeadRenderer.paintGrip(g, gripCol);
                SwordHeadRenderer.paintPommel(g, pommelCap, accent);

                // Element-specific touches on top of the blade.
                int tipX = SwordHeadRenderer.TIP_X;
                int tipY = SwordHeadRenderer.TIP_Y;
                int len = SwordHeadRenderer.BLADE_LEN;
                switch (kind) {
                    case FIRE:
                        // Glowing accent line along the edge.
                        g.setColor(new Color(255, 180, 80, 200));
                        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        for (int i = 0; i < 4; i++) {
                            int sx = tipX + (int)(len * (0.2 + i * 0.18));
                            g.drawLine(sx, tipY + SwordHeadRenderer.BLADE_HALF_H,
                                       sx + 5, tipY + SwordHeadRenderer.BLADE_HALF_H + 3);
                        }
                        break;
                    case ICE:
                        // Frost crystals on the spine.
                        g.setColor(new Color(220, 240, 255, 220));
                        for (int i = 0; i < 4; i++) {
                            int sx = tipX + (int)(len * (0.25 + i * 0.2));
                            int sy = tipY - SwordHeadRenderer.BLADE_HALF_H;
                            int s = 3;
                            int[] xs = { sx, sx + s, sx, sx - s };
                            int[] ys = { sy - s, sy, sy + s, sy };
                            g.fillPolygon(xs, ys, 4);
                        }
                        break;
                    case LIGHTNING:
                        // Jagged accent zigzag down the blade.
                        g.setColor(new Color(255, 240, 80, 220));
                        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        Path2D.Double zig = new Path2D.Double();
                        zig.moveTo(tipX + 3, tipY);
                        for (int i = 1; i <= 6; i++) {
                            double t = i / 6.0;
                            double zx = tipX + len * t;
                            double zy = tipY + ((i % 2 == 0) ? -2 : 2);
                            zig.lineTo(zx, zy);
                        }
                        g.draw(zig);
                        break;
                    case WIND:
                        // Faint swirl curves.
                        g.setColor(new Color(180, 240, 240, 180));
                        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        for (int i = 0; i < 3; i++) {
                            double cx = tipX + len * (0.3 + i * 0.2);
                            g.drawArc((int)cx - 4, tipY - 4, 8, 8, 30, 220);
                        }
                        break;
                    case EARTH:
                        // Rocky stipple along blade.
                        g.setColor(new Color(60, 40, 25, 220));
                        for (int i = 0; i < 8; i++) {
                            int sx = tipX + (int)(len * (0.15 + i * 0.1));
                            int sy = tipY + ((i % 2 == 0) ? -2 : 1);
                            g.fillOval(sx, sy, 2, 2);
                        }
                        break;
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
            switch (kind) {
                case FIRE:      drawFireTrail(g, points); break;
                case ICE:       drawIceTrail(g, points); break;
                case LIGHTNING: drawLightningTrail(g, points); break;
                case WIND:      drawWindTrail(g, points); break;
                case EARTH:     drawEarthTrail(g, points); break;
            }
            // (Sword sprite intentionally NOT drawn during gameplay --
            // slice-trail only. The cached head appears in drawPreview only.)

            // Per-element signature flourish on top.
            drawElementalFlourish(g, points);
        }

        // ---- FIRE ---------------------------------------------------------
        private void drawFireTrail(Graphics2D g, List<TrailPoint> points) {
            Path2D.Double path = buildPath(points);
            stroke(g, path, 20f, new Color(255, 90, 20, 70));
            stroke(g, path, 9f, new Color(255, 180, 60, 180));
            stroke(g, path, 3f, new Color(255, 240, 200, 240));
            long now = System.currentTimeMillis();
            for (int i = 0; i < points.size(); i += 3) {
                TrailPoint p = points.get(i);
                double age = (now - p.timeMs) / 1000.0;
                int ex = (int) (p.x + Math.sin(i * 0.7) * 4);
                int ey = (int) (p.y - age * 40.0);
                int sz = (int) (6 - age * 6);
                if (sz < 2) continue;
                g.setColor(new Color(255, 160, 40, (int) (200 * p.life)));
                g.fillOval(ex - sz / 2, ey - sz / 2, sz, sz);
                g.setColor(new Color(255, 230, 120, (int) (220 * p.life)));
                g.fillOval(ex - sz / 4, ey - sz / 4, Math.max(1, sz / 2), Math.max(1, sz / 2));
            }
        }

        // ---- ICE ----------------------------------------------------------
        private void drawIceTrail(Graphics2D g, List<TrailPoint> points) {
            Path2D.Double path = buildPath(points);
            stroke(g, path, 18f, new Color(120, 200, 255, 70));
            stroke(g, path, 8f, new Color(220, 240, 255, 180));
            stroke(g, path, 2f, new Color(255, 255, 255, 240));
            for (int i = 0; i < points.size(); i += 4) {
                TrailPoint p = points.get(i);
                int cx = (int) p.x, cy = (int) p.y, s = 5;
                int[] xs = { cx, cx + s, cx, cx - s };
                int[] ys = { cy - s, cy, cy + s, cy };
                g.setColor(new Color(180, 230, 255, (int) (200 * p.life)));
                g.fillPolygon(xs, ys, 4);
                g.setColor(new Color(255, 255, 255, (int) (230 * p.life)));
                g.setStroke(new BasicStroke(1f));
                g.drawPolygon(xs, ys, 4);
            }
        }

        // ---- LIGHTNING ----------------------------------------------------
        private void drawLightningTrail(Graphics2D g, List<TrailPoint> points) {
            stroke(g, buildPath(points), 22f, new Color(255, 240, 120, 70));
            Path2D.Double zig = new Path2D.Double();
            boolean first = true;
            for (int i = 0; i < points.size(); i++) {
                TrailPoint p = points.get(i);
                double[] perp = perpendicular(points, i);
                double off = Math.sin(i * 1.9) * 7.0 + Math.cos(i * 3.1) * 3.0;
                double zx = p.x + perp[0] * off, zy = p.y + perp[1] * off;
                if (first) { zig.moveTo(zx, zy); first = false; }
                else { zig.lineTo(zx, zy); }
            }
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.setColor(new Color(255, 230, 80, 220));
            g.draw(zig);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
            g.setColor(new Color(255, 255, 230, 240));
            g.draw(zig);
        }

        // ---- WIND ---------------------------------------------------------
        private void drawWindTrail(Graphics2D g, List<TrailPoint> points) {
            double t = (System.currentTimeMillis() % 100000) / 200.0;
            for (int line = 0; line < 3; line++) {
                Path2D.Double path = new Path2D.Double();
                boolean first = true;
                double phase = line * 2.094;
                for (int i = 0; i < points.size(); i++) {
                    TrailPoint p = points.get(i);
                    double[] perp = perpendicular(points, i);
                    double off = Math.sin(t + i * 0.4 + phase) * 8.0;
                    double wx = p.x + perp[0] * off, wy = p.y + perp[1] * off;
                    if (first) { path.moveTo(wx, wy); first = false; }
                    else { path.lineTo(wx, wy); }
                }
                g.setStroke(new BasicStroke(line == 1 ? 4f : 2.5f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(180, 240, 240, line == 1 ? 220 : 140));
                g.draw(path);
            }
        }

        // ---- EARTH --------------------------------------------------------
        private void drawEarthTrail(Graphics2D g, List<TrailPoint> points) {
            Path2D.Double path = buildPath(points);
            stroke(g, path, 24f, new Color(90, 60, 30, 160));
            stroke(g, path, 14f, new Color(150, 100, 60, 220));
            stroke(g, path, 5f, new Color(210, 180, 140, 240));
            int mid = points.size() / 2;
            int span = Math.min(points.size(), 8);
            for (int k = -span / 2; k < span / 2; k += 2) {
                int idx = mid + k;
                if (idx < 0 || idx >= points.size()) continue;
                TrailPoint p = points.get(idx);
                int cx = (int) p.x, cy = (int) p.y;
                int r = 5 + ((idx * 7) % 4);
                int[] bx = { cx - r, cx + r - 1, cx + r, cx + 1, cx - r + 2 };
                int[] by = { cy - 1, cy - r, cy + 2, cy + r - 1, cy + r - 3 };
                g.setColor(new Color(110, 75, 45, (int) (230 * p.life)));
                g.fillPolygon(bx, by, 5);
                g.setColor(new Color(60, 35, 20, (int) (220 * p.life)));
                g.setStroke(new BasicStroke(1.3f));
                g.drawPolygon(bx, by, 5);
            }
        }

        /**
         * Per-element signature flourish layered on top of the trail+head.
         */
        private void drawElementalFlourish(Graphics2D g, List<TrailPoint> points) {
            int n = points.size();
            long now = System.currentTimeMillis();
            Composite oldComp = g.getComposite();
            try {
                switch (kind) {
                    case FIRE: {
                        // Bright spark bursts at the tip with embers shooting upward.
                        TrailPoint tip = points.get(0);
                        for (int i = 0; i < 8; i++) {
                            long seed = tip.timeMs + i * 9176L + (now / 80L);
                            double a = (((seed >> 4) & 0x3FFL) / 1024.0) * Math.PI * 2.0;
                            double r = 6.0 + ((seed >> 7) & 0xFL);
                            double ex = tip.x + Math.cos(a) * r;
                            double ey = tip.y + Math.sin(a) * r - 4;
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                            g.setColor(new Color(255, 200 - i * 5, 60));
                            g.fillOval((int) ex - 2, (int) ey - 2, 4, 4);
                            g.setColor(new Color(255, 240, 180));
                            g.fillOval((int) ex - 1, (int) ey - 1, 2, 2);
                        }
                        break;
                    }
                    case ICE: {
                        // Drifting snowflakes around recent points.
                        for (int i = 0; i < Math.min(5, n); i++) {
                            TrailPoint p = points.get(i);
                            long seed = p.timeMs ^ (i * 0xC2B2AE3D27D4EB4FL);
                            double ox = ((seed >> 3) & 0x1FL) - 16;
                            double oy = ((seed >> 9) & 0x1FL) - 16;
                            double rot = ((seed >> 13) & 0xFFL) / 255.0 * Math.PI;
                            int alpha = (int) (200 * Math.max(0, p.life));
                            g.setComposite(AlphaComposite.getInstance(
                                AlphaComposite.SRC_OVER, alpha / 255f));
                            drawSnowflake(g, p.x + ox, p.y + oy, 5, rot);
                        }
                        break;
                    }
                    case LIGHTNING: {
                        // Multiple bright forked bolts from the tip.
                        TrailPoint tip = points.get(0);
                        TrailPoint next = points.get(1);
                        double dx = tip.x - next.x;
                        double dy = tip.y - next.y;
                        double len = Math.sqrt(dx * dx + dy * dy);
                        if (len < 0.001) break;
                        double nx = -dy / len;
                        double ny =  dx / len;
                        for (int b = 0; b < 3; b++) {
                            long seed = tip.timeMs + b * 7919L + (now / 40L);
                            double sign = ((seed >> 4) & 1L) == 0 ? 1 : -1;
                            Path2D.Double bolt = new Path2D.Double();
                            bolt.moveTo(tip.x, tip.y);
                            double bx = tip.x, by = tip.y;
                            for (int s = 1; s <= 5; s++) {
                                bx += nx * sign * 5 + (-ny) * sign * (((seed >> s) & 3L) - 1.5);
                                by += ny * sign * 5 + ( nx) * sign * (((seed >> s) & 3L) - 1.5);
                                bolt.lineTo(bx, by);
                            }
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                            g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                            g.setColor(new Color(255, 230, 80));
                            g.draw(bolt);
                            g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                            g.setColor(Color.WHITE);
                            g.draw(bolt);
                        }
                        break;
                    }
                    case WIND: {
                        // Faint swirl curls peeling off the tip.
                        TrailPoint tip = points.get(0);
                        for (int i = 0; i < 3; i++) {
                            double phase = (now * 0.005 + i * 1.3) % (Math.PI * 2);
                            double cx = tip.x + Math.cos(phase) * 14;
                            double cy = tip.y + Math.sin(phase) * 14;
                            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g.setColor(new Color(200, 240, 240));
                            g.drawArc((int)(cx - 6), (int)(cy - 6), 12, 12, 30, 240);
                        }
                        break;
                    }
                    case EARTH: {
                        // Falling pebbles below recent trail points.
                        for (int i = 0; i < Math.min(5, n); i++) {
                            TrailPoint p = points.get(i);
                            long seed = p.timeMs ^ (i * 0xC2B2AE3D27D4EB4FL);
                            if ((seed & 3L) != 0) continue;
                            long age = now - p.timeMs;
                            double fall = Math.min(34.0, age * 0.07);
                            double ox = ((seed >> 5) & 7L) - 3.5;
                            int sz = 3 + (int)((seed >> 9) & 1L);
                            g.setComposite(AlphaComposite.getInstance(
                                AlphaComposite.SRC_OVER,
                                (float)(Math.max(0, p.life) * 0.85)));
                            g.setColor(new Color(100, 70, 40));
                            g.fillRoundRect((int)(p.x + ox) - sz / 2,
                                            (int)(p.y + fall) - sz / 2, sz, sz, 1, 1);
                            g.setColor(new Color(160, 130, 90));
                            g.fillRect((int)(p.x + ox) - sz / 2 + 1,
                                       (int)(p.y + fall) - sz / 2 + 1, 1, 1);
                        }
                        break;
                    }
                }
            } finally {
                g.setComposite(oldComp);
            }
        }

        /** Tiny 6-arm snowflake. */
        private void drawSnowflake(Graphics2D g, double cx, double cy, double r, double rot) {
            AffineTransform old = g.getTransform();
            g.translate(cx, cy);
            g.rotate(rot);
            g.setColor(new Color(220, 240, 255));
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int arm = 0; arm < 6; arm++) {
                AffineTransform t = g.getTransform();
                g.rotate(arm * Math.PI / 3.0);
                g.drawLine(0, 0, (int) r, 0);
                g.drawLine((int)(r * 0.6), 0, (int)(r * 0.6 + 2), -2);
                g.drawLine((int)(r * 0.6), 0, (int)(r * 0.6 + 2),  2);
                g.setTransform(t);
            }
            g.setTransform(old);
        }

        // ---- helpers ------------------------------------------------------
        private static void stroke(Graphics2D g, Path2D.Double path, float w, Color c) {
            g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(c);
            g.draw(path);
        }

        private static double[] perpendicular(List<TrailPoint> pts, int i) {
            double dx, dy;
            if (i + 1 < pts.size()) {
                TrailPoint n = pts.get(i + 1), p = pts.get(i);
                dx = n.x - p.x; dy = n.y - p.y;
            } else if (i > 0) {
                TrailPoint pr = pts.get(i - 1), p = pts.get(i);
                dx = p.x - pr.x; dy = p.y - pr.y;
            } else { dx = 1; dy = 0; }
            double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy));
            return new double[] { -dy / len, dx / len };
        }

        private static Path2D.Double buildPath(List<TrailPoint> points) {
            Path2D.Double path = new Path2D.Double();
            boolean first = true;
            for (TrailPoint p : points) {
                if (first) { path.moveTo(p.x, p.y); first = false; }
                else { path.lineTo(p.x, p.y); }
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
            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
            g.drawLine(x1, y1, x2, y2);

            double dx = x2 - x1, dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            double px = -dy / len, py = dx / len;

            switch (kind) {
                case FIRE:
                    g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(255, 170, 60));
                    g.drawLine(x1, y1, x2, y2);
                    for (int i = 0; i < 5; i++) {
                        double t = (i + 1) / 6.0;
                        int ex = (int) (x1 + dx * t);
                        int ey = (int) (y1 + dy * t) - 6 - i * 2;
                        g.setColor(new Color(255, 140, 40, 220));
                        g.fillOval(ex - 2, ey - 2, 4, 4);
                    }
                    break;
                case ICE:
                    g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(200, 235, 255));
                    g.drawLine(x1, y1, x2, y2);
                    for (int i = 0; i < 4; i++) {
                        double t = (i + 1) / 5.0;
                        int cx = (int) (x1 + dx * t), cy = (int) (y1 + dy * t), s = 4;
                        int[] xs = { cx, cx + s, cx, cx - s };
                        int[] ys = { cy - s, cy, cy + s, cy };
                        g.setColor(new Color(190, 230, 255, 230));
                        g.fillPolygon(xs, ys, 4);
                        g.setColor(Color.WHITE);
                        g.setStroke(new BasicStroke(1f));
                        g.drawPolygon(xs, ys, 4);
                    }
                    break;
                case LIGHTNING: {
                    Path2D.Double zig = new Path2D.Double();
                    int steps = 10;
                    zig.moveTo(x1, y1);
                    for (int i = 1; i <= steps; i++) {
                        double bx = x1 + dx * i / steps, by = y1 + dy * i / steps;
                        double off = (i % 2 == 0) ? 6.0 : -6.0;
                        zig.lineTo(bx + px * off, by + py * off);
                    }
                    g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                    g.setColor(new Color(255, 230, 80, 220));
                    g.draw(zig);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
                    g.setColor(new Color(255, 255, 230, 240));
                    g.draw(zig);
                    break;
                }
                case WIND: {
                    int steps = 24;
                    for (int line = 0; line < 3; line++) {
                        double phase = line * 2.094;
                        Path2D.Double path = new Path2D.Double();
                        for (int i = 0; i <= steps; i++) {
                            double t = i / (double) steps;
                            double bx = x1 + dx * t, by = y1 + dy * t;
                            double off = Math.sin(t * 6.0 + phase) * 8.0;
                            if (i == 0) path.moveTo(bx + px * off, by + py * off);
                            else path.lineTo(bx + px * off, by + py * off);
                        }
                        g.setStroke(new BasicStroke(line == 1 ? 4f : 2.5f,
                                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.setColor(new Color(180, 240, 240, line == 1 ? 230 : 150));
                        g.draw(path);
                    }
                    break;
                }
                case EARTH:
                    g.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(150, 100, 60));
                    g.drawLine(x1, y1, x2, y2);
                    for (int i = 0; i < 3; i++) {
                        double t = (i + 1) / 4.0;
                        int cx = (int) (x1 + dx * t), cy = (int) (y1 + dy * t), r = 5;
                        int[] bxs = { cx - r, cx + r - 1, cx + r, cx + 1, cx - r + 2 };
                        int[] bys = { cy - 1, cy - r, cy + 2, cy + r - 1, cy + r - 3 };
                        g.setColor(new Color(110, 75, 45, 230));
                        g.fillPolygon(bxs, bys, 5);
                        g.setColor(new Color(50, 30, 15));
                        g.setStroke(new BasicStroke(1.2f));
                        g.drawPolygon(bxs, bys, 5);
                    }
                    break;
            }

            if (kind != ElementKind.LIGHTNING && kind != ElementKind.WIND) {
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 255, 255, 230));
                g.drawLine(x1, y1, x2, y2);
            }

            // Mini cached head at tip.
            double ang = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            drawSwordHead(g, x2, y2, ang);
        }
    }

    private static final class Variant extends AbstractElemental {
        Variant(String id, String name, int price, String tagline,
                ElementKind kind, Color accent) {
            super(id, name, price, tagline, kind, accent);
        }
    }
}
