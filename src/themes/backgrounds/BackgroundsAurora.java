package themes.backgrounds;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/**
 * Aurora / ice / winter family. 100% procedural premium tier. Renders
 * starfield, multi-band wavy aurora ribbons (Path2D), jagged dark mountain
 * silhouettes with snow caps, rolling snow drifts, and drifting snow
 * particles with sin-wave wobble. 5 variants under bg_036 .. bg_040.
 */
public final class BackgroundsAurora {

    private BackgroundsAurora() {}

    static {
        BackgroundCatalog.register(new Variant("bg_036", "Northern Lights", 1200,
            "Green and pink ribbons.", new Color(80, 255, 160),
            new Color[]{new Color(80, 255, 150, 120), new Color(255, 120, 200, 100),
                        new Color(120, 220, 255, 90)}, Style.NORMAL));
        BackgroundCatalog.register(new Variant("bg_037", "Glacier Cathedral", 1800,
            "Cold blue cathedral of ice.", new Color(150, 220, 255),
            new Color[]{new Color(150, 220, 255, 130), new Color(100, 180, 255, 110),
                        new Color(200, 240, 255, 90)}, Style.NORMAL));
        BackgroundCatalog.register(new Variant("bg_038", "Frozen Lake", 2400,
            "Mirror under starlight.", new Color(220, 230, 240),
            new Color[]{new Color(200, 230, 255, 110), new Color(170, 200, 230, 100),
                        new Color(220, 220, 240, 90)}, Style.REFLECTION));
        BackgroundCatalog.register(new Variant("bg_039", "Blizzard Peak", 3200,
            "Snow rages on the summit.", new Color(255, 255, 255),
            new Color[]{new Color(220, 230, 240, 90), new Color(180, 200, 230, 80),
                        new Color(255, 255, 255, 70)}, Style.BLIZZARD));
        BackgroundCatalog.register(new Variant("bg_040", "Polar Solstice", 4500,
            "All hues at the world's end.", new Color(255, 200, 255),
            new Color[]{new Color(255, 100, 200, 110), new Color(100, 255, 200, 110),
                        new Color(200, 150, 255, 110)}, Style.RAINBOW));
    }

    private enum Style { NORMAL, REFLECTION, BLIZZARD, RAINBOW }

    // ====================================================================
    //  ABSTRACT BASE
    // ====================================================================

    static abstract class AbstractAurora implements Background {

        protected final String id, name, tagline;
        protected final int price;
        protected final Color accent;
        protected final Color[] auroraColors;
        protected final Style style;

        private final List<Star> stars = new ArrayList<>();
        private final List<Snow> flakes = new ArrayList<>();
        private final double[] mtnNear, mtnFar;
        private double timeSec;
        private final Random rng = new Random();
        private int lastW = -1, lastH = -1;

        AbstractAurora(String id, String name, int price, String tagline,
                       Color accent, Color[] auroraColors, Style style) {
            this.id = id; this.name = name; this.price = price;
            this.tagline = tagline; this.accent = accent;
            this.auroraColors = auroraColors; this.style = style;
            Random r = new Random(id.hashCode());
            this.mtnNear = new double[64];
            this.mtnFar = new double[48];
            for (int i = 0; i < mtnNear.length; i++) mtnNear[i] = r.nextDouble();
            for (int i = 0; i < mtnFar.length; i++)  mtnFar[i]  = r.nextDouble();
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        private void ensureParticles(int w, int h) {
            if (w == lastW && h == lastH && !stars.isEmpty()) return;
            lastW = w; lastH = h;
            stars.clear();
            for (int i = 0; i < 180; i++) {
                Star s = new Star();
                s.x = rng.nextDouble() * w;
                s.y = rng.nextDouble() * h * 0.7;
                s.size = 0.5 + rng.nextDouble() * 1.8;
                s.phase = rng.nextDouble() * Math.PI * 2;
                stars.add(s);
            }
            flakes.clear();
            int flakeCount = (style == Style.BLIZZARD) ? 280 : 110;
            for (int i = 0; i < flakeCount; i++) {
                Snow f = new Snow();
                f.x = rng.nextDouble() * w;
                f.y = rng.nextDouble() * h;
                f.size = 1.0 + rng.nextDouble() * 2.5;
                f.vy = 20 + rng.nextDouble() * 40;
                f.wobbleAmp = 8 + rng.nextDouble() * 24;
                f.wobblePhase = rng.nextDouble() * Math.PI * 2;
                f.wobbleSpeed = 0.6 + rng.nextDouble() * 1.4;
                if (style == Style.BLIZZARD) { f.vy *= 2.2; f.wobbleAmp *= 1.6; }
                flakes.add(f);
            }
        }

        @Override
        public void update(double deltaMs) {
            timeSec += deltaMs / 1000.0;
            // Monotonic forward scroll instead of sine oscillation -- the
            // oscillation read as "looping back and forth" to the player.
            Camera.x = timeSec * 8.0;
            double dt = deltaMs / 1000.0;
            double drift = (style == Style.BLIZZARD) ? 180 : 30;
            for (Snow f : flakes) {
                f.y += f.vy * dt;
                f.wobblePhase += f.wobbleSpeed * dt;
                f.x += drift * dt;
                if (lastH > 0 && f.y > lastH) { f.y = -5; f.x = rng.nextDouble() * lastW; }
                if (lastW > 0 && f.x > lastW + 20) f.x = -20;
            }
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            ensureParticles(w, h);
            drawSky(g, w, h);
            drawStars(g, w, h);
            drawAurora(g, w, h);

            if (style == Style.REFLECTION) {
                int horizon = (int) (h * 0.55);
                drawMountains(g, w, h, false);
                Composite saved = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                drawMountains(g, w, h, true);
                g.setComposite(saved);
                GradientPaint lake = new GradientPaint(
                    0, horizon, new Color(20, 30, 55, 220),
                    0, h, new Color(8, 12, 25));
                g.setPaint(lake);
                g.fillRect(0, horizon, w, h - horizon);
                drawIceStreaks(g, w, h, horizon);
            } else {
                drawMountains(g, w, h, false);
                drawSnowDrifts(g, w, h);
            }
            drawSnow(g, w, h);
            if (style == Style.BLIZZARD) {
                g.setColor(new Color(255, 255, 255, 40));
                g.fillRect(0, 0, w, h);
            }
        }

        private void drawSky(Graphics2D g, int w, int h) {
            g.setPaint(new GradientPaint(0, 0, new Color(4, 6, 22),
                                          0, h, new Color(20, 30, 60)));
            g.fillRect(0, 0, w, h);
        }

        private void drawStars(Graphics2D g, int w, int h) {
            for (Star s : stars) {
                double tw = 0.6 + 0.4 * Math.sin(timeSec * 2.5 + s.phase);
                int a = (int) Math.max(0, Math.min(255, 180 * tw));
                g.setColor(new Color(255, 255, 240, a));
                double sz = s.size * (0.8 + 0.3 * tw);
                int isz = (int) Math.max(1, sz);
                g.fillOval((int) (s.x - sz / 2), (int) (s.y - sz / 2), isz, isz);
            }
        }

        private void drawAurora(Graphics2D g, int w, int h) {
            Composite saved = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            int bands = auroraColors.length + 2;
            for (int b = 0; b < bands; b++) {
                Color c = ribbonColor(b);
                double phase = timeSec * (0.25 + 0.07 * b) + b * 1.3;
                double baseY = h * 0.10 + b * (h * 0.06);
                double amp = h * (0.05 + 0.015 * (b % 3));
                double freq = 2.2 + 0.4 * b;
                double thickness = h * (0.10 + 0.015 * b);
                drawRibbon(g, w, baseY, amp, freq, phase, thickness, c);
            }
            g.setComposite(saved);
        }

        private Color ribbonColor(int band) {
            if (style == Style.RAINBOW) {
                float hue = (float) ((timeSec * 0.08 + band * 0.18) % 1.0);
                Color base = Color.getHSBColor(hue, 0.75f, 1.0f);
                return new Color(base.getRed(), base.getGreen(), base.getBlue(), 110);
            }
            return auroraColors[band % auroraColors.length];
        }

        private void drawRibbon(Graphics2D g, int w, double baseY, double amp,
                                double freq, double phase, double thickness, Color c) {
            int steps = 64;
            double parallax = Camera.x * 0.15 * 0.1;
            double[] xs = new double[steps + 1];
            double[] ysT = new double[steps + 1];
            double[] ysB = new double[steps + 1];
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double x = t * w;
                double wob = Math.sin(t * Math.PI * freq + phase) * amp
                           + Math.sin(t * Math.PI * freq * 2.3 + phase * 1.4) * amp * 0.4;
                double y = baseY + wob + parallax;
                xs[i] = x; ysT[i] = y; ysB[i] = y + thickness;
            }
            Path2D.Double poly = new Path2D.Double();
            poly.moveTo(xs[0], ysT[0]);
            for (int i = 1; i <= steps; i++) poly.lineTo(xs[i], ysT[i]);
            for (int i = steps; i >= 0; i--) poly.lineTo(xs[i], ysB[i]);
            poly.closePath();

            float midX = w / 2f;
            double midY = (ysT[steps / 2] + ysB[steps / 2]) / 2;
            g.setPaint(new GradientPaint(
                midX, (float) (midY - thickness * 0.5), c,
                midX, (float) (midY + thickness * 0.5),
                new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)));
            g.fill(poly);

            Stroke s0 = g.getStroke();
            g.setStroke(new BasicStroke(1.6f));
            g.setColor(new Color(
                Math.min(255, c.getRed() + 40),
                Math.min(255, c.getGreen() + 40),
                Math.min(255, c.getBlue() + 40),
                Math.min(255, c.getAlpha() + 60)));
            Path2D.Double top = new Path2D.Double();
            top.moveTo(xs[0], ysT[0]);
            for (int i = 1; i <= steps; i++) top.lineTo(xs[i], ysT[i]);
            g.draw(top);
            g.setStroke(s0);
        }

        // Deterministic hash from world slot index to stable double in [0,1).
        private double slotHash(int slot, int salt) {
            long x = ((long) slot * 2654435761L) ^ ((long) salt * 0x9E3779B97F4A7C15L);
            x ^= (x >>> 33);
            x *= 0xff51afd7ed558ccdL;
            x ^= (x >>> 33);
            x *= 0xc4ceb9fe1a85ec53L;
            x ^= (x >>> 33);
            return (x >>> 11) / (double) (1L << 53);
        }

        private void drawMountains(Graphics2D g, int w, int h, boolean flipped) {
            int base = flipped ? (int) (h * 0.55) : h;
            int horizon = (int) (h * 0.55);

            // Far range: world-slot peaks so the silhouette never repeats.
            double pxFar = Camera.x * 0.15;
            double farStep = w / 10.0;
            int leftFar = (int) Math.floor(pxFar / farStep) - 1;
            int slotsFar = (int) Math.ceil(w / farStep) + 3;
            Path2D.Double far = new Path2D.Double();
            far.moveTo(leftFar * farStep - pxFar, base);
            for (int k = 0; k < slotsFar; k++) {
                int idx = leftFar + k;
                double x = idx * farStep - pxFar;
                double peakH = slotHash(idx, 0xF1) * h * 0.12 + h * 0.08;
                far.lineTo(x, flipped ? (horizon + peakH) : (horizon - peakH + h * 0.04));
            }
            far.lineTo((leftFar + slotsFar - 1) * farStep - pxFar, base);
            far.closePath();
            g.setColor(new Color(18, 24, 42)); g.fill(far);

            // Near range: closer parallax, faster but still unique slots.
            double pxNear = Camera.x * 0.35;
            double nearStep = w / 8.0;
            int leftNear = (int) Math.floor(pxNear / nearStep) - 1;
            int slotsNear = (int) Math.ceil(w / nearStep) + 3;
            Path2D.Double near = new Path2D.Double();
            near.moveTo(leftNear * nearStep - pxNear, base);
            List<double[]> peaks = new ArrayList<>();
            double prevH = -1, curH = slotHash(leftNear, 0xF2);
            for (int k = 0; k < slotsNear; k++) {
                int idx = leftNear + k;
                double nextH = slotHash(idx + 1, 0xF2);
                double x = idx * nearStep - pxNear;
                double peakH = curH * h * 0.22 + h * 0.10;
                double y = flipped ? (horizon + peakH) : (horizon - peakH + h * 0.10);
                near.lineTo(x, y);
                if (!flipped && prevH >= 0 && curH > prevH && curH > nextH && curH > 0.55) {
                    peaks.add(new double[]{x, y});
                }
                prevH = curH;
                curH = nextH;
            }
            near.lineTo((leftNear + slotsNear - 1) * nearStep - pxNear, base);
            near.closePath();
            g.setColor(new Color(8, 10, 18)); g.fill(near);

            if (!flipped) {
                g.setColor(new Color(245, 250, 255));
                for (double[] p : peaks) {
                    double cs = h * 0.05;
                    Path2D.Double cap = new Path2D.Double();
                    cap.moveTo(p[0], p[1]);
                    cap.lineTo(p[0] - cs * 0.55, p[1] + cs);
                    cap.lineTo(p[0] - cs * 0.18, p[1] + cs * 0.55);
                    cap.lineTo(p[0] + cs * 0.10, p[1] + cs * 0.85);
                    cap.lineTo(p[0] + cs * 0.55, p[1] + cs);
                    cap.closePath();
                    g.fill(cap);
                }
            }
        }

        private void drawSnowDrifts(Graphics2D g, int w, int h) {
            Path2D.Double d1 = new Path2D.Double();
            double baseY1 = h * 0.82;
            d1.moveTo(0, h);
            for (int i = 0; i <= 80; i++) {
                double t = i / 80.0;
                double y = baseY1 + Math.sin(t * Math.PI * 2.0 + Camera.x * 0.003) * 14
                                  + Math.sin(t * Math.PI * 5.7) * 5;
                d1.lineTo(t * w, y);
            }
            d1.lineTo(w, h); d1.closePath();
            g.setColor(new Color(230, 238, 250));
            g.fill(d1);

            Path2D.Double d2 = new Path2D.Double();
            double baseY2 = h * 0.91;
            d2.moveTo(0, h);
            for (int i = 0; i <= 80; i++) {
                double t = i / 80.0;
                double y = baseY2 + Math.sin(t * Math.PI * 3.3 + Camera.x * 0.005 + 1.4) * 10;
                d2.lineTo(t * w, y);
            }
            d2.lineTo(w, h); d2.closePath();
            g.setColor(new Color(255, 255, 255));
            g.fill(d2);
        }

        private void drawIceStreaks(Graphics2D g, int w, int h, int horizon) {
            g.setColor(new Color(255, 255, 255, 25));
            Stroke s0 = g.getStroke();
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 14; i++) {
                double t = (i + 0.5) / 14.0;
                int y = horizon + (int) (t * (h - horizon));
                g.drawLine(0, y, w, y);
            }
            g.setStroke(s0);
        }

        private void drawSnow(Graphics2D g, int w, int h) {
            g.setColor(new Color(255, 255, 255, 220));
            for (Snow f : flakes) {
                double wob = Math.sin(f.wobblePhase) * f.wobbleAmp;
                double x = f.x + wob;
                int sz = (int) Math.max(1, f.size);
                g.fillOval((int) (x - sz / 2.0), (int) (f.y - sz / 2.0), sz, sz);
            }
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);
            g.setPaint(new GradientPaint(x, y, new Color(4, 6, 22),
                                          x, y + h, new Color(20, 30, 60)));
            g.fillRect(x, y, w, h);

            Random r = new Random(id.hashCode() ^ 0xBEEF);
            g.setColor(new Color(255, 255, 240, 200));
            for (int i = 0; i < 30; i++)
                g.fillOval(x + r.nextInt(w), y + r.nextInt((int) (h * 0.6)), 1, 1);

            // Single aurora ribbon
            double baseY = y + h * 0.25, amp = h * 0.08, thick = h * 0.18;
            int steps = 32;
            double[] xs = new double[steps + 1], ysT = new double[steps + 1], ysB = new double[steps + 1];
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                xs[i] = x + t * w;
                double wob = Math.sin(t * Math.PI * 2.4) * amp;
                ysT[i] = baseY + wob; ysB[i] = baseY + wob + thick;
            }
            Path2D.Double rib = new Path2D.Double();
            rib.moveTo(xs[0], ysT[0]);
            for (int i = 1; i <= steps; i++) rib.lineTo(xs[i], ysT[i]);
            for (int i = steps; i >= 0; i--) rib.lineTo(xs[i], ysB[i]);
            rib.closePath();
            Color top = (style == Style.RAINBOW) ? new Color(180, 120, 255, 180) : accent;
            g.setPaint(new GradientPaint(x + w / 2f, (float) baseY,
                new Color(top.getRed(), top.getGreen(), top.getBlue(), 200),
                x + w / 2f, (float) (baseY + thick),
                new Color(top.getRed(), top.getGreen(), top.getBlue(), 0)));
            g.fill(rib);

            // Mountains + caps
            int horizon = y + (int) (h * 0.65);
            Path2D.Double m = new Path2D.Double();
            m.moveTo(x, y + h);
            int seg = 8;
            for (int i = 0; i <= seg; i++) {
                double t = i / (double) seg;
                double py = horizon - (mtnNear[i % mtnNear.length] * h * 0.28 + h * 0.05);
                m.lineTo(x + t * w, py);
            }
            m.lineTo(x + w, y + h); m.closePath();
            g.setColor(new Color(8, 10, 18)); g.fill(m);

            g.setColor(new Color(245, 250, 255));
            for (int i = 1; i < seg; i++) {
                double v = mtnNear[i % mtnNear.length];
                if (v <= 0.55) continue;
                double t = i / (double) seg;
                double px = x + t * w, py = horizon - (v * h * 0.28 + h * 0.05);
                double cap = h * 0.07;
                Path2D.Double cp = new Path2D.Double();
                cp.moveTo(px, py);
                cp.lineTo(px - cap * 0.45, py + cap);
                cp.lineTo(px + cap * 0.45, py + cap);
                cp.closePath();
                g.fill(cp);
            }

            // Snowflakes
            g.setColor(new Color(255, 255, 255, 230));
            for (int i = 0; i < 25; i++) {
                int sz = 1 + r.nextInt(2);
                g.fillOval(x + r.nextInt(w), y + r.nextInt(h), sz, sz);
            }

            // Foreground drift
            g.setColor(new Color(255, 255, 255));
            Path2D.Double drift = new Path2D.Double();
            drift.moveTo(x, y + h);
            for (int i = 0; i <= 20; i++) {
                double t = i / 20.0;
                double py = y + h * 0.88 + Math.sin(t * Math.PI * 2.5) * 3;
                drift.lineTo(x + t * w, py);
            }
            drift.lineTo(x + w, y + h); drift.closePath();
            g.fill(drift);
            g.setClip(oldClip);
        }

        private static final class Star { double x, y, size, phase; }
        private static final class Snow {
            double x, y, size, vy, wobbleAmp, wobblePhase, wobbleSpeed;
        }
    }

    private static final class Variant extends AbstractAurora {
        Variant(String id, String name, int price, String tagline,
                Color accent, Color[] auroraColors, Style style) {
            super(id, name, price, tagline, accent, auroraColors, style);
        }
    }
}
