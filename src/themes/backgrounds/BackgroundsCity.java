package themes.backgrounds;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Random;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/**
 * City / urban skyline family. 100% procedural: a vertical sky gradient with
 * two parallax layers of rectangular building silhouettes lit by tiny windows.
 * Each variant has its own personality (neon signs, rain, spires, warm
 * windows, sunset disc). 5 variants registered under bg_026 .. bg_030.
 */
public final class BackgroundsCity {

    private BackgroundsCity() {}

    static {
        BackgroundCatalog.register(new Variant(
            "bg_026", "Tokyo Neon", 500,
            "Pink and cyan night",
            new Color(60, 18, 70), new Color(8, 4, 14),
            new Color(255, 60, 200), Style.NEON));
        BackgroundCatalog.register(new Variant(
            "bg_027", "Rooftop Sunset", 700,
            "Golden hour skyline",
            new Color(255, 150, 60), new Color(60, 20, 40),
            new Color(255, 160, 60), Style.SUNSET));
        BackgroundCatalog.register(new Variant(
            "bg_028", "Rain District", 1000,
            "Neon reflections in puddles",
            new Color(20, 35, 70), new Color(4, 6, 14),
            new Color(80, 160, 255), Style.RAIN));
        BackgroundCatalog.register(new Variant(
            "bg_029", "Cyber Spire", 1500,
            "Endless arcology towers",
            new Color(15, 40, 70), new Color(2, 4, 12),
            new Color(60, 230, 230), Style.SPIRE));
        BackgroundCatalog.register(new Variant(
            "bg_030", "Skyline at Midnight", 2200,
            "A city that never sleeps",
            new Color(20, 18, 50), new Color(2, 2, 8),
            new Color(180, 110, 230), Style.MIDNIGHT));
    }

    enum Style { NEON, SUNSET, RAIN, SPIRE, MIDNIGHT }

    // ====================================================================
    //  ABSTRACT BASE
    // ====================================================================

    static abstract class AbstractCity implements Background {

        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color skyTop;
        protected final Color skyBottom;
        protected final Color accent;
        protected final Style style;

        // Two parallax layers of buildings. Each layer is a deterministic
        // list of (relativeX, width, height) tuples generated from a seeded
        // Random so they never shimmer.
        private final int[] backX, backW, backH;
        private final int[] frontX, frontW, frontH;
        private final int[] spireX, spireH;     // for SPIRE style
        private final int backTileW, frontTileW;

        private double timeSec;
        private double timeMs;

        AbstractCity(String id, String name, int price, String tagline,
                     Color skyTop, Color skyBottom, Color accent, Style style) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.skyTop = skyTop;
            this.skyBottom = skyBottom;
            this.accent = accent;
            this.style = style;

            Random rng = new Random(id.hashCode() * 1315423911L);

            int backCount = 22;
            backTileW = 2400;
            backX = new int[backCount];
            backW = new int[backCount];
            backH = new int[backCount];
            int cursor = 0;
            for (int i = 0; i < backCount; i++) {
                int w = 80 + rng.nextInt(60);
                int h = 120 + rng.nextInt(160);
                backX[i] = cursor;
                backW[i] = w;
                backH[i] = h;
                cursor += w + 4 + rng.nextInt(14);
            }

            int frontCount = 16;
            frontTileW = 2000;
            frontX = new int[frontCount];
            frontW = new int[frontCount];
            frontH = new int[frontCount];
            cursor = 0;
            for (int i = 0; i < frontCount; i++) {
                int w = 110 + rng.nextInt(80);
                int h = 200 + rng.nextInt(220);
                frontX[i] = cursor;
                frontW[i] = w;
                frontH[i] = h;
                cursor += w + 2 + rng.nextInt(20);
            }

            // Extra-tall spires for the cyber arcology variant.
            int spireCount = 6;
            spireX = new int[spireCount];
            spireH = new int[spireCount];
            for (int i = 0; i < spireCount; i++) {
                spireX[i] = 200 + i * 380 + rng.nextInt(120);
                spireH[i] = 480 + rng.nextInt(180);
            }
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        @Override
        public void update(double deltaMs) {
            timeSec += deltaMs / 1000.0;
            timeMs += deltaMs;
            // Monotonic forward scroll, not sine oscillation.
            Camera.x = timeSec * 12.0;
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Sky gradient (variant top -> dim near-black bottom).
            g.setPaint(new GradientPaint(0, 0, skyTop, 0, h, skyBottom));
            g.fillRect(0, 0, w, h);

            // 2. Sunset disk for SUNSET variant.
            if (style == Style.SUNSET) {
                int sunR = Math.max(60, h / 5);
                int sx = (int) (w * 0.72) - sunR;
                int sy = (int) (h * 0.45) - sunR;
                g.setColor(new Color(255, 200, 90, 220));
                g.fillOval(sx, sy, sunR * 2, sunR * 2);
                g.setColor(new Color(255, 230, 140, 80));
                g.fillOval(sx - 12, sy - 12, sunR * 2 + 24, sunR * 2 + 24);
            }

            // 3. Cyber spires: per-world-slot scatter so they never repeat.
            if (style == Style.SPIRE) {
                double off = Camera.x * (w / 500.0);
                double cellW = 380.0;
                int firstSlot = (int) Math.floor(off / cellW) - 1;
                int slots = (int) Math.ceil(w / cellW) + 3;
                for (int k = 0; k < slots; k++) {
                    int idx = firstSlot + k;
                    long sh64 = tileHash(idx ^ 0x57E5);
                    int localX = (int) ((sh64 & 0xFFFF) % (long) cellW);
                    int sx = (int) (idx * cellW - off + localX);
                    if (sx < -20 || sx > w + 60) continue;
                    int shh = 480 + (int) (((sh64 >>> 16) & 0xFF) * 180 / 255);
                    int sy = h - shh - 80;
                    g.setColor(new Color(8, 14, 30));
                    g.fillRect(sx, sy, 14, shh);
                    boolean lit = ((int) (timeMs / 500) + idx) % 2 == 0;
                    g.setColor(lit ? new Color(255, 70, 70)
                                   : new Color(80, 20, 20));
                    g.fillOval(sx + 1, sy - 6, 12, 12);
                }
            }

            // 4. Back skyline (z = 400).
            drawSkyline(g, w, h, backX, backW, backH, backTileW,
                        Camera.x / 400.0, h - h / 4, new Color(10, 12, 22),
                        true);

            // 5. Front skyline (z = 150).
            drawSkyline(g, w, h, frontX, frontW, frontH, frontTileW,
                        Camera.x / 150.0, h - 30, new Color(4, 4, 10),
                        false);

            // 6. Rain streaks.
            if (style == Style.RAIN) {
                drawRain(g, w, h);
            }

            // 7. Soft ambient glow along the horizon in accent tone.
            int glowH = h / 5;
            g.setPaint(new GradientPaint(
                0, h - glowH, new Color(0, 0, 0, 0),
                0, h - glowH / 2,
                new Color(accent.getRed(), accent.getGreen(),
                          accent.getBlue(), 40)));
            g.fillRect(0, h - glowH, w, glowH);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               oldAA != null ? oldAA
                                             : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
        }

        // Deterministic hash from world tile index.
        private long tileHash(int tileIdx) {
            long h = ((long) tileIdx * 2654435761L) ^ 0xC2B2AE3DL;
            h ^= (h >>> 33);
            h *= 0xff51afd7ed558ccdL;
            h ^= (h >>> 33);
            return h;
        }

        private void drawSkyline(Graphics2D g, int w, int h,
                                 int[] xs, int[] ws, int[] hs,
                                 int tileW, double parallax,
                                 int baselineY, Color silhouette,
                                 boolean back) {
            double off = parallax * w;
            // World-indexed tiles: each tile occurrence has its own seed so
            // building widths/heights/windows vary, eliminating visible repeat.
            int firstTile = (int) Math.floor(off / tileW);
            for (int t = 0; t < 4; t++) {
                int tileIdx = firstTile + t;
                double tileScreenX = tileIdx * tileW - off;
                if (tileScreenX > w) break;
                if (tileScreenX + tileW < 0) continue;
                long th = tileHash(tileIdx);

                for (int i = 0; i < xs.length; i++) {
                    long bh64 = th ^ ((long) i * 0x9E3779B97F4A7C15L);
                    bh64 ^= bh64 >>> 33;
                    int hMix = (int) (bh64 & 0xFF) - 128;
                    int wMix = (int) ((bh64 >>> 8) & 0x3F) - 32;
                    int bx = (int) (tileScreenX + xs[i]);
                    int bw = Math.max(60, ws[i] + wMix);
                    int bh = Math.max(80, hs[i] + hMix);
                    if (bx + bw < 0 || bx > w) continue;
                    int by = baselineY - bh;

                    // Building body.
                    g.setColor(silhouette);
                    g.fillRect(bx, by, bw, bh);

                    // Edge highlight on the front layer only.
                    if (!back) {
                        g.setColor(new Color(silhouette.getRed() + 12,
                                             silhouette.getGreen() + 14,
                                             silhouette.getBlue() + 22));
                        g.fillRect(bx, by, 2, bh);
                    }

                    // Windows, seeded by global tile so they change per cycle.
                    int wSeed = (int) (bh64 & 0x7FFFFFFF);
                    drawWindows(g, bx, by, bw, bh, wSeed, back);

                    // Neon side signs (Tokyo only) on front layer.
                    if (style == Style.NEON && !back && bw > 130) {
                        int signX = bx + bw - 8;
                        int signY = by + 30 + ((i * 17) % Math.max(1, bh - 120));
                        Color neon = (i % 2 == 0)
                            ? new Color(255, 70, 200)
                            : new Color(80, 230, 255);
                        boolean on = ((int) (timeMs / 350) + i) % 2 == 0;
                        if (on) {
                            g.setColor(neon);
                            g.fillRect(signX, signY, 6, 50);
                            g.setColor(new Color(neon.getRed(),
                                                 neon.getGreen(),
                                                 neon.getBlue(), 90));
                            g.fillRect(signX - 3, signY - 3, 12, 56);
                        }
                    }
                }
            }
        }

        private void drawWindows(Graphics2D g, int bx, int by, int bw, int bh,
                                 int seed, boolean back) {
            int cellW = back ? 8 : 10;
            int cellH = back ? 10 : 12;
            int cols = Math.max(1, (bw - 6) / cellW);
            int rows = Math.max(1, (bh - 14) / cellH);
            int winW = cellW - 3;
            int winH = cellH - 4;

            // Pick warm/accent palette for this building.
            Color warm = new Color(255, 220, 130);
            Color accentLit = new Color(accent.getRed(), accent.getGreen(),
                                        accent.getBlue());

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = seed * 31 + r * 7 + c * 13;
                    // ~55% of the windows even exist (rest stay dark).
                    int presence = Math.floorMod(idx * 2654435761L >> 13
                                                     & 0xFFFFFFFFL, 100L) > 45 ? 1 : 0;
                    // Cast back to int via simple modulo for determinism.
                    int p2 = Math.floorMod(idx, 100);
                    boolean exists = p2 > 40;
                    if (style == Style.MIDNIGHT) exists = p2 > 25;
                    if (!exists) continue;
                    // Flicker on a per-window phase.
                    boolean lit = ((int) (timeMs / 500)
                                   + (c + 1) * (r + 1) + seed) % 2 == 0;
                    if (style == Style.MIDNIGHT) {
                        // Most windows just stay lit warm.
                        lit = p2 % 7 != 0 ? true : lit;
                    }
                    if (!lit) continue;

                    Color c1;
                    if (style == Style.MIDNIGHT) {
                        c1 = warm;
                    } else if (style == Style.SUNSET) {
                        c1 = (p2 % 5 == 0) ? accentLit : warm;
                    } else {
                        c1 = (p2 % 3 == 0) ? accentLit : warm;
                    }
                    g.setColor(c1);
                    int wx = bx + 3 + c * cellW;
                    int wy = by + 7 + r * cellH;
                    g.fillRect(wx, wy, winW, winH);
                    // ignore unused variable warning
                    if (presence < 0) presence = 0;
                }
            }
        }

        private void drawRain(Graphics2D g, int w, int h) {
            Stroke saved = g.getStroke();
            g.setStroke(new BasicStroke(1.4f));
            int streaks = 220;
            Random rng = new Random(91173);
            double t = timeMs * 0.6;
            for (int i = 0; i < streaks; i++) {
                int baseX = rng.nextInt(w + 200);
                int baseY = rng.nextInt(h);
                double drop = (t + i * 53) % (h + 80);
                int x = (int) (baseX - drop * 0.25) % (w + 200);
                int y = (int) (baseY + drop) % h;
                if (x < -10) x += w + 200;
                g.setColor(new Color(170, 200, 255, 110));
                g.drawLine(x, y, x - 5, y + 14);
            }
            g.setStroke(saved);
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);

            // Mini sky.
            g.setPaint(new GradientPaint(x, y, skyTop, x, y + h, skyBottom));
            g.fillRect(x, y, w, h);

            // Sun for sunset preview.
            if (style == Style.SUNSET) {
                int r = h / 3;
                g.setColor(new Color(255, 210, 110));
                g.fillOval(x + w - r - 6, y + h / 3, r, r);
            }

            // Mini skyline of 9 deterministic buildings.
            Random rng = new Random(id.hashCode() * 7919L);
            int baseY = y + h - 4;
            int cursor = x + 2;
            int idx = 0;
            while (cursor < x + w) {
                int bw = 8 + rng.nextInt(10);
                int bh = 18 + rng.nextInt(h / 2);
                int by = baseY - bh;
                g.setColor(new Color(6, 8, 16));
                g.fillRect(cursor, by, bw, bh);

                // A few windows.
                int rows = Math.max(1, bh / 6);
                int cols = Math.max(1, bw / 4);
                for (int r2 = 0; r2 < rows; r2++) {
                    for (int c2 = 0; c2 < cols; c2++) {
                        int seed = idx * 17 + r2 * 5 + c2 * 3;
                        if (Math.floorMod(seed, 5) < 2) {
                            Color wc = (Math.floorMod(seed, 7) == 0)
                                ? accent : new Color(255, 220, 130);
                            g.setColor(wc);
                            g.fillRect(cursor + 1 + c2 * 3,
                                       by + 1 + r2 * 5, 2, 2);
                        }
                    }
                }
                cursor += bw + 1;
                idx++;
            }

            // Accent line along the bottom horizon.
            g.setColor(new Color(accent.getRed(), accent.getGreen(),
                                 accent.getBlue(), 160));
            g.fillRect(x, baseY, w, 2);

            // Rain hint or spire hint.
            if (style == Style.RAIN) {
                g.setColor(new Color(180, 210, 255, 140));
                for (int i = 0; i < 14; i++) {
                    int rx = x + (i * 11) % w;
                    int ry = y + (i * 7) % h;
                    g.drawLine(rx, ry, rx - 2, ry + 5);
                }
            } else if (style == Style.SPIRE) {
                g.setColor(new Color(8, 14, 30));
                g.fillRect(x + w / 2, y + h / 5, 3, h - h / 5 - 4);
                g.setColor(accent);
                g.fillOval(x + w / 2 - 1, y + h / 5 - 3, 5, 5);
            }

            g.setClip(oldClip);
        }
    }

    private static final class Variant extends AbstractCity {
        Variant(String id, String name, int price, String tagline,
                Color skyTop, Color skyBottom, Color accent, Style style) {
            super(id, name, price, tagline, skyTop, skyBottom, accent, style);
        }
    }
}
