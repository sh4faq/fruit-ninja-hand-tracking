package themes.backgrounds;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.Random;

import themes.Background;
import themes.BackgroundCatalog;

/**
 * Volcano family. 100% procedurally drawn fire / lava scenes. Registers
 * bg_016 .. bg_020.
 *
 * Layers: dark red-to-black sky with drifting ash, jagged mountain
 * silhouettes with glowing crack lines, foreground rocks above a lava
 * pool, and a field of bright embers drifting upward (signature).
 */
public final class BackgroundsVolcano {

    private BackgroundsVolcano() {}

    static {
        BackgroundCatalog.register(new Variant(
            "bg_016", "Lava Field", 350, "Glowing cracks in dark stone.",
            new Color(255, 120, 30),
            new Color(45, 12, 10), new Color(10, 4, 6),
            60, false, false, 1.0f));
        BackgroundCatalog.register(new Variant(
            "bg_017", "Ember Sky", 600, "Sky thick with ash and embers.",
            new Color(230, 50, 30),
            new Color(70, 18, 12), new Color(20, 6, 8),
            110, false, false, 1.05f));
        BackgroundCatalog.register(new Variant(
            "bg_018", "Magma Falls", 900, "Rivers of molten rock.",
            new Color(255, 180, 50),
            new Color(55, 20, 14), new Color(14, 6, 8),
            90, true, false, 1.1f));
        BackgroundCatalog.register(new Variant(
            "bg_019", "Crater Edge", 1400, "Standing on the volcano's lip.",
            new Color(255, 70, 30),
            new Color(85, 22, 14), new Color(25, 8, 10),
            130, false, false, 1.15f));
        BackgroundCatalog.register(new Variant(
            "bg_020", "Inferno Throne", 2100, "The volcano god's domain.",
            new Color(255, 230, 180),
            new Color(120, 35, 18), new Color(35, 10, 10),
            200, false, true, 1.3f));
    }

    static abstract class AbstractVolcano implements Background {

        protected final String id, name, tagline;
        protected final int price;
        protected final Color accent, skyTop, skyBottom;
        protected final int emberCount;
        protected final boolean hasMagmaFall, hasLavaLake;
        protected final float brightness;

        private final Random rng;
        private final float[] eX, eY, eLife, eMax, eSize, ePhase;
        private static final int ASH_N = 50;
        private final float[] aX = new float[ASH_N];
        private final float[] aY = new float[ASH_N];
        private final float[] aSize = new float[ASH_N];
        private final float[] aPhase = new float[ASH_N];
        private final float[] mountainPts = new float[16];
        private final float[][] cracks = new float[8][8];
        private double timeSec;

        AbstractVolcano(String id, String name, int price, String tagline,
                        Color accent, Color skyTop, Color skyBottom,
                        int emberCount, boolean hasMagmaFall,
                        boolean hasLavaLake, float brightness) {
            this.id = id; this.name = name; this.price = price;
            this.tagline = tagline; this.accent = accent;
            this.skyTop = skyTop; this.skyBottom = skyBottom;
            this.emberCount = emberCount;
            this.hasMagmaFall = hasMagmaFall;
            this.hasLavaLake = hasLavaLake;
            this.brightness = brightness;
            this.rng = new Random(id.hashCode() * 9176L + 31L);

            this.eX = new float[emberCount];
            this.eY = new float[emberCount];
            this.eLife = new float[emberCount];
            this.eMax = new float[emberCount];
            this.eSize = new float[emberCount];
            this.ePhase = new float[emberCount];
            for (int i = 0; i < emberCount; i++) resetEmber(i, true);

            for (int i = 0; i < ASH_N; i++) {
                aX[i] = rng.nextFloat();
                aY[i] = rng.nextFloat();
                aSize[i] = 1f + rng.nextFloat() * 2.2f;
                aPhase[i] = rng.nextFloat() * 6.28f;
            }
            for (int i = 0; i < 16; i++) mountainPts[i] = 0.30f + rng.nextFloat() * 0.40f;
            for (int c = 0; c < cracks.length; c++) {
                float sx = rng.nextFloat();
                float sy = 0.55f + rng.nextFloat() * 0.30f;
                for (int p = 0; p < 4; p++) {
                    cracks[c][p * 2]     = sx + (rng.nextFloat() - 0.5f) * 0.06f;
                    cracks[c][p * 2 + 1] = sy + p * 0.04f + rng.nextFloat() * 0.02f;
                }
            }
        }

        private void resetEmber(int i, boolean anywhere) {
            eX[i] = rng.nextFloat();
            eY[i] = anywhere ? rng.nextFloat() : (0.85f + rng.nextFloat() * 0.15f);
            eMax[i] = 2.5f + rng.nextFloat() * 4.0f;
            eLife[i] = anywhere ? rng.nextFloat() * eMax[i] : 0f;
            eSize[i] = 1.2f + rng.nextFloat() * 2.4f;
            ePhase[i] = rng.nextFloat() * 6.28f;
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        @Override
        public void update(double deltaMs) {
            float dt = (float) (deltaMs / 1000.0);
            timeSec += deltaMs / 1000.0;
            for (int i = 0; i < emberCount; i++) {
                eLife[i] += dt;
                eY[i] -= dt * 0.06f;
                if (eLife[i] >= eMax[i] || eY[i] < -0.05f) resetEmber(i, false);
            }
            for (int i = 0; i < ASH_N; i++) {
                aY[i] -= dt * 0.015f;
                aX[i] += dt * 0.008f;
                if (aY[i] < -0.05f) aY[i] = 1.05f;
                if (aX[i] > 1.05f) aX[i] -= 1.1f;
            }
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setPaint(new GradientPaint(0, 0, skyTop, 0, h, skyBottom));
            g.fillRect(0, 0, w, h);

            float glow = 0.18f + (float) (Math.sin(timeSec * 1.5) * 0.04);
            Color glowC = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                                    (int) (glow * 90 * brightness));
            g.setPaint(new GradientPaint(0, h * 0.45f, new Color(0, 0, 0, 0),
                                         0, h * 0.95f, glowC));
            g.fillRect(0, 0, w, h);

            g.setColor(new Color(40, 38, 40, 110));
            for (int i = 0; i < ASH_N; i++) {
                float ax = (aX[i] + (float) Math.sin(timeSec * 0.4 + aPhase[i]) * 0.01f) * w;
                float ay = aY[i] * h;
                g.fillOval((int) ax, (int) ay, (int) aSize[i], (int) aSize[i]);
            }

            if (hasMagmaFall) drawMagmaFall(g, w, h);

            drawMountains(g, w, h);
            drawForeground(g, w, h);

            if (hasLavaLake) drawLavaLake(g, w, h);

            drawEmbers(g, w, h);

            if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }

        private void drawMountains(Graphics2D g, int w, int h) {
            float baseY = 0.55f, scaleY = 0.65f;
            Path2D.Float p = new Path2D.Float();
            p.moveTo(0, h);
            int n = mountainPts.length;
            for (int i = 0; i < n; i++) {
                float fx = i / (float) (n - 1);
                float peak = mountainPts[i];
                float py = h * (baseY + (1f - peak) * scaleY * 0.4f);
                p.lineTo(fx * w, py);
                if (i < n - 1) {
                    float nx = (i + 0.5f) / (float) (n - 1);
                    float valley = (peak + mountainPts[i + 1]) * 0.5f - 0.08f;
                    p.lineTo(nx * w, h * (baseY + (1f - valley) * scaleY * 0.4f + 0.05f));
                }
            }
            p.lineTo(w, h);
            p.closePath();
            g.setColor(new Color(8, 4, 6));
            g.fill(p);

            Shape oldClip = g.getClip();
            g.setClip(p);
            for (int c = 0; c < cracks.length; c++) {
                Path2D.Float cp = new Path2D.Float();
                cp.moveTo(cracks[c][0] * w, cracks[c][1] * h);
                for (int pp = 1; pp < 4; pp++) {
                    cp.lineTo(cracks[c][pp * 2] * w, cracks[c][pp * 2 + 1] * h);
                }
                int a = 180 + (int) (Math.sin(timeSec * 2.2 + c) * 50);
                a = Math.max(80, Math.min(230, a));
                g.setStroke(new BasicStroke(3.5f));
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50));
                g.draw(cp);
                g.setStroke(new BasicStroke(1.6f));
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a));
                g.draw(cp);
            }
            g.setClip(oldClip);
        }

        private void drawForeground(Graphics2D g, int w, int h) {
            float baseY = h * 0.82f;
            g.setPaint(new GradientPaint(
                0, baseY, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220),
                0, h, new Color(120, 30, 10)));
            g.fillRect(0, (int) baseY, w, (int) (h - baseY));

            int rocks = 6;
            for (int i = 0; i < rocks; i++) {
                float cx = (i + 0.5f) / rocks * w;
                float rw = w * (0.12f + (i % 3) * 0.04f);
                float rh = h * (0.16f + (i % 2) * 0.05f);
                Path2D.Float p = new Path2D.Float();
                p.moveTo(cx - rw, h);
                p.lineTo(cx - rw * 0.6f, baseY + (h - baseY) * 0.2f);
                p.lineTo(cx - rw * 0.2f, baseY - rh * 0.5f);
                p.lineTo(cx + rw * 0.1f, baseY - rh);
                p.lineTo(cx + rw * 0.5f, baseY - rh * 0.4f);
                p.lineTo(cx + rw * 0.8f, baseY + (h - baseY) * 0.1f);
                p.lineTo(cx + rw, h);
                p.closePath();
                g.setColor(new Color(6, 3, 5));
                g.fill(p);

                g.setStroke(new BasicStroke(1.2f));
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
                g.drawLine((int) (cx - rw * 0.2f), (int) (baseY - rh * 0.5f),
                           (int) (cx + rw * 0.1f), (int) (baseY - rh));
                g.drawLine((int) (cx + rw * 0.1f), (int) (baseY - rh),
                           (int) (cx + rw * 0.5f), (int) (baseY - rh * 0.4f));
            }
        }

        private void drawMagmaFall(Graphics2D g, int w, int h) {
            float cx = w * 0.72f;
            float sw = w * 0.045f;
            g.setPaint(new GradientPaint(
                cx, 0, new Color(255, 230, 120),
                cx, h * 0.85f, new Color(255, 80, 20)));
            Path2D.Float p = new Path2D.Float();
            p.moveTo(cx - sw * 0.5f, h * 0.30f);
            p.lineTo(cx - sw * 0.7f, h * 0.85f);
            p.lineTo(cx + sw * 0.7f, h * 0.85f);
            p.lineTo(cx + sw * 0.5f, h * 0.30f);
            p.closePath();
            g.fill(p);
            g.setStroke(new BasicStroke(sw * 1.4f));
            g.setColor(new Color(255, 100, 40, 60));
            g.drawLine((int) cx, (int) (h * 0.30f), (int) cx, (int) (h * 0.85f));
        }

        private void drawLavaLake(Graphics2D g, int w, int h) {
            float topY = h * 0.78f;
            g.setPaint(new GradientPaint(
                0, topY, new Color(255, 240, 200),
                0, h, new Color(220, 60, 20)));
            g.fillRect(0, (int) topY, w, (int) (h - topY));
            g.setColor(new Color(255, 255, 220, 110));
            for (int i = 0; i < 5; i++) {
                float yy = topY + i * (h - topY) / 6f
                        + (float) Math.sin(timeSec * 1.2 + i) * 3f;
                g.fillRect(0, (int) yy, w, 1);
            }
        }

        private void drawEmbers(Graphics2D g, int w, int h) {
            for (int i = 0; i < emberCount; i++) {
                float lifeFrac = eLife[i] / eMax[i];
                float alpha = 1f - lifeFrac;
                if (alpha <= 0) continue;
                float wobble = (float) Math.sin(timeSec * 2.0 + ePhase[i]) * 0.012f;
                float px = (eX[i] + wobble) * w;
                float py = eY[i] * h;
                float sz = eSize[i];
                int a = Math.min(255, (int) (alpha * 230 * brightness));
                g.setColor(new Color(255, 120, 30, a / 4));
                g.fillOval((int) (px - sz * 2.2f), (int) (py - sz * 2.2f),
                           (int) (sz * 4.4f), (int) (sz * 4.4f));
                int rr = Math.min(255, accent.getRed() + 30);
                int gg = Math.min(255, accent.getGreen() + 80);
                int bb = accent.getBlue();
                g.setColor(new Color(rr, gg, bb, a));
                g.fillOval((int) (px - sz * 0.5f), (int) (py - sz * 0.5f),
                           (int) sz, (int) sz);
            }
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setPaint(new GradientPaint(x, y, skyTop, x, y + h, skyBottom));
            g.fillRect(x, y, w, h);

            g.setPaint(new GradientPaint(
                x, y + h * 0.45f, new Color(0, 0, 0, 0),
                x, y + h, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110)));
            g.fillRect(x, y, w, h);

            Path2D.Float m = new Path2D.Float();
            m.moveTo(x, y + h);
            m.lineTo(x,             y + h * 0.70f);
            m.lineTo(x + w * 0.25f, y + h * 0.55f);
            m.lineTo(x + w * 0.40f, y + h * 0.72f);
            m.lineTo(x + w * 0.60f, y + h * 0.45f);
            m.lineTo(x + w * 0.78f, y + h * 0.65f);
            m.lineTo(x + w,         y + h * 0.55f);
            m.lineTo(x + w,         y + h);
            m.closePath();
            g.setColor(new Color(8, 4, 6));
            g.fill(m);

            g.setStroke(new BasicStroke(1.4f));
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
            g.drawLine(x + (int) (w * 0.55f), y + (int) (h * 0.55f),
                       x + (int) (w * 0.60f), y + (int) (h * 0.85f));

            g.setPaint(new GradientPaint(
                x, y + h * 0.85f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220),
                x, y + h, new Color(120, 30, 10)));
            g.fillRect(x, y + (int) (h * 0.85f), w, (int) (h * 0.15f));

            int[][] embers = {
                {x + (int) (w * 0.30f), y + (int) (h * 0.35f), 3},
                {x + (int) (w * 0.55f), y + (int) (h * 0.50f), 2},
                {x + (int) (w * 0.70f), y + (int) (h * 0.25f), 2},
            };
            for (int[] e : embers) {
                g.setColor(new Color(255, 140, 40, 70));
                g.fillOval(e[0] - e[2] * 2, e[1] - e[2] * 2, e[2] * 4, e[2] * 4);
                g.setColor(new Color(255, 220, 140, 240));
                g.fillOval(e[0] - e[2] / 2, e[1] - e[2] / 2, e[2], e[2]);
            }

            if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            g.setClip(oldClip);
        }
    }

    private static final class Variant extends AbstractVolcano {
        Variant(String id, String name, int price, String tagline,
                Color accent, Color skyTop, Color skyBottom,
                int emberCount, boolean hasMagmaFall,
                boolean hasLavaLake, float brightness) {
            super(id, name, price, tagline, accent, skyTop, skyBottom,
                  emberCount, hasMagmaFall, hasLavaLake, brightness);
        }
    }
}
