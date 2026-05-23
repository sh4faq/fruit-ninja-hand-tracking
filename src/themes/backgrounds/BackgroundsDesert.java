package themes.backgrounds;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.Random;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/**
 * Desert family. Five procedurally-drawn desert scenes registered under
 * bg_031 .. bg_035. No bitmap assets.
 */
public final class BackgroundsDesert {

    private BackgroundsDesert() {}

    enum Style { DUNE_SEA, MESA_DUSK, SANDSTORM, OASIS, NIGHT }

    static {
        BackgroundCatalog.register(new Variant("bg_031", "Dune Sea", 350,
            "Endless sand under the sun.",
            Style.DUNE_SEA, new Color(230, 190, 80)));
        BackgroundCatalog.register(new Variant("bg_032", "Mesa Dusk", 550,
            "Red rock at sundown.",
            Style.MESA_DUSK, new Color(180, 70, 50)));
        BackgroundCatalog.register(new Variant("bg_033", "Sandstorm", 850,
            "Wind-whipped fury.",
            Style.SANDSTORM, new Color(220, 200, 110)));
        BackgroundCatalog.register(new Variant("bg_034", "Oasis Mirage", 1300,
            "Water somewhere, surely.",
            Style.OASIS, new Color(60, 140, 80)));
        BackgroundCatalog.register(new Variant("bg_035", "Desert Night", 1900,
            "Cold stars over hot sand.",
            Style.NIGHT, new Color(30, 50, 110)));
    }

    static abstract class AbstractDesert implements Background {

        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Style style;
        protected final Color accent;
        protected double timeSec;

        private final float[] dustX = new float[80];
        private final float[] dustY = new float[80];
        private final float[] dustLen = new float[80];
        private final float[] dustSpeed = new float[80];
        private final float[] starX = new float[60];
        private final float[] starY = new float[60];
        private final float[] starS = new float[60];
        private final float[] starT = new float[60];

        AbstractDesert(String id, String name, int price, String tagline,
                       Style style, Color accent) {
            this.id = id; this.name = name; this.price = price;
            this.tagline = tagline; this.style = style; this.accent = accent;
            Random r = new Random(id.hashCode());
            for (int i = 0; i < dustX.length; i++) {
                dustX[i] = r.nextFloat();
                dustY[i] = r.nextFloat();
                dustLen[i] = 20f + r.nextFloat() * 60f;
                dustSpeed[i] = 0.4f + r.nextFloat() * 0.9f;
            }
            for (int i = 0; i < starX.length; i++) {
                starX[i] = r.nextFloat();
                starY[i] = r.nextFloat() * 0.55f;
                starS[i] = 0.8f + r.nextFloat() * 1.8f;
                starT[i] = r.nextFloat() * 6.28f;
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
            // Monotonic forward scroll. Sandstorm pushes faster.
            double speed = (style == Style.SANDSTORM) ? 18.0 : 8.0;
            Camera.x = timeSec * speed;
            if (style == Style.SANDSTORM) {
                float dt = (float) (deltaMs / 1000.0);
                for (int i = 0; i < dustX.length; i++) {
                    dustX[i] += dustSpeed[i] * dt * 1.4f;
                    if (dustX[i] > 1.1f) {
                        dustX[i] = -0.1f;
                        dustY[i] = (float) Math.random();
                    }
                }
            }
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            drawSky(g, w, h);
            drawSkyFeature(g, w, h);
            drawMidLayer(g, w, h);
            drawForeground(g, w, h);
            drawOverlay(g, w, h);
            if (oldAA != null)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }

        private void drawSky(Graphics2D g, int w, int h) {
            Color top, bot;
            switch (style) {
                case DUNE_SEA:  top = new Color(255,220,140); bot = new Color(255,180,90); break;
                case MESA_DUSK: top = new Color(80,30,60);    bot = new Color(220,90,50);  break;
                case SANDSTORM: top = new Color(200,170,90);  bot = new Color(230,200,130); break;
                case OASIS:     top = new Color(180,220,230); bot = new Color(245,220,170); break;
                case NIGHT: default: top = new Color(8,14,40); bot = new Color(40,35,80);  break;
            }
            g.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g.fillRect(0, 0, w, h);
        }

        private void drawSkyFeature(Graphics2D g, int w, int h) {
            if (style == Style.DUNE_SEA) {
                int cx = w * 3 / 4, cy = h / 3, rr = (int)(h * 0.28);
                for (int k = 4; k >= 1; k--) {
                    g.setColor(new Color(255, 230, 140, 30 + (4 - k) * 20));
                    g.fillOval(cx - rr*k/2, cy - rr*k/2, rr*k, rr*k);
                }
                g.setColor(new Color(255, 245, 200));
                g.fillOval(cx - rr/2, cy - rr/2, rr, rr);
            } else if (style == Style.NIGHT) {
                for (int i = 0; i < starX.length; i++) {
                    float br = 0.6f + 0.4f * (float) Math.sin(timeSec * 1.4 + starT[i]);
                    int a = Math.min(255, (int)(180 * br) + 60);
                    g.setColor(new Color(255, 255, 240, a));
                    g.fillOval((int)(starX[i] * w), (int)(starY[i] * h),
                               (int) starS[i], (int) starS[i]);
                }
                int mx = w * 4 / 5, my = h / 5, mr = (int)(h * 0.09);
                g.setColor(new Color(240, 235, 210));
                g.fillOval(mx - mr, my - mr, mr * 2, mr * 2);
                g.setColor(new Color(8, 14, 40));
                g.fillOval(mx - mr + (int)(mr * 0.4),
                           my - mr - (int)(mr * 0.1), mr * 2, mr * 2);
            } else if (style == Style.MESA_DUSK) {
                int cx = w / 5, cy = h / 2, rr = (int)(h * 0.18);
                g.setColor(new Color(255, 180, 90, 120));
                g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
                g.setColor(new Color(255, 220, 140));
                g.fillOval(cx - rr/2, cy - rr/2, rr, rr);
            }
        }

        private void drawMidLayer(Graphics2D g, int w, int h) {
            double offset = Camera.x * 0.25;
            if (style == Style.MESA_DUSK) {
                drawMesas(g, w, h, offset, h * 0.65, new Color(120, 50, 40), 5);
                return;
            }
            Color c;
            switch (style) {
                case DUNE_SEA:  c = new Color(210,160,70); break;
                case SANDSTORM: c = new Color(180,150,80,220); break;
                case OASIS:     c = new Color(220,190,130); break;
                case NIGHT:     c = new Color(40,30,60); break;
                default:        c = new Color(180,140,80); break;
            }
            drawDuneBand(g, w, h, offset, h * 0.62, 70, 0.012, c);
        }

        private void drawForeground(Graphics2D g, int w, int h) {
            double offset = Camera.x * 0.6;
            if (style == Style.MESA_DUSK) {
                drawMesas(g, w, h, offset, h * 0.78, new Color(70, 25, 25), 3);
            } else {
                Color c;
                switch (style) {
                    case DUNE_SEA:  c = new Color(170,120,50); break;
                    case SANDSTORM: c = new Color(140,110,60); break;
                    case OASIS:     c = new Color(180,150,90); break;
                    case NIGHT:     c = new Color(20,15,35);   break;
                    default:        c = new Color(140,100,60); break;
                }
                drawDuneBand(g, w, h, offset, h * 0.80, 50, 0.018, c);
            }
            switch (style) {
                case DUNE_SEA:
                    drawCactus(g, w * 0.12, h * 0.78, h * 0.14, new Color(60, 90, 50));
                    break;
                case OASIS:
                    drawWaterMirror(g, w / 2, (int)(h * 0.68),
                                    (int)(w * 0.14), (int)(h * 0.025));
                    drawPalm(g, w * 0.18, h * 0.80, h * 0.22);
                    drawPalm(g, w * 0.82, h * 0.78, h * 0.20);
                    break;
                case MESA_DUSK:
                    drawRock(g, w * 0.85, h * 0.86, h * 0.06, new Color(60, 20, 20));
                    break;
                case SANDSTORM:
                    g.setColor(new Color(245, 230, 170, 180));
                    for (int i = 0; i < dustX.length; i++) {
                        g.fillRect((int)(dustX[i] * w), (int)(dustY[i] * h),
                                   (int) dustLen[i], 2);
                    }
                    break;
                case NIGHT:
                    drawCactus(g, w * 0.78, h * 0.82, h * 0.10, new Color(20, 35, 25));
                    break;
            }
        }

        private void drawOverlay(Graphics2D g, int w, int h) {
            if (style == Style.DUNE_SEA) {
                g.setColor(new Color(255, 220, 140, 18));
                for (int i = 0; i < 6; i++) {
                    double y = h * 0.55 + i * 12 + Math.sin(timeSec * 1.2 + i) * 3;
                    g.fillRect(0, (int) y, w, 4);
                }
            } else if (style == Style.SANDSTORM) {
                g.setColor(new Color(220, 200, 140, 70));
                g.fillRect(0, 0, w, h);
            } else if (style == Style.OASIS) {
                g.setColor(new Color(255, 230, 180, 25));
                g.fillRect(0, (int)(h * 0.6), w, (int)(h * 0.4));
            }
        }

        private void drawDuneBand(Graphics2D g, int w, int h, double offset,
                                  double baseY, double amp, double freq, Color c) {
            Path2D.Double p = new Path2D.Double();
            p.moveTo(0, h);
            for (int x = 0; x <= w; x += 6) {
                double y = baseY + Math.sin((x + offset) * freq) * amp
                         + Math.sin((x + offset) * freq * 2.7) * amp * 0.3;
                p.lineTo(x, y);
            }
            p.lineTo(w, h); p.closePath();
            g.setColor(c); g.fill(p);
        }

        private double slotHash(int slot, int salt) {
            long x = ((long) slot * 2654435761L) ^ ((long) salt * 0x9E3779B97F4A7C15L);
            x ^= (x >>> 33);
            x *= 0xff51afd7ed558ccdL;
            x ^= (x >>> 33);
            x *= 0xc4ceb9fe1a85ec53L;
            x ^= (x >>> 33);
            return (x >>> 11) / (double) (1L << 53);
        }

        private void drawMesas(Graphics2D g, int w, int h, double offset,
                               double baseY, Color c, int count) {
            double span = (double) w / count;
            // World-slot mesas: each slot index has unique height/width.
            int firstSlot = (int) Math.floor(offset / span) - 1;
            int slots = count + 3;
            for (int k = 0; k < slots; k++) {
                int idx = firstSlot + k;
                double cx = idx * span + span * 0.5 - offset;
                double top = baseY - (60 + slotHash(idx, 0xD1) * 80.0);
                double width = span * (0.55 + slotHash(idx, 0xD2) * 0.28);
                int x = (int)(cx - width / 2), y = (int) top;
                if (x + width < -10 || x > w + 10) continue;
                g.setColor(c);
                g.fillRect(x, y, (int) width, (int)(h - y));
                g.setColor(c.darker());
                g.fillRect(x, y + (int)((h - y) * 0.18), (int) width, 4);
            }
        }

        private void drawCactus(Graphics2D g, double cx, double by,
                                double height, Color c) {
            g.setColor(c);
            int tW = (int)(height * 0.18), tH = (int) height;
            g.fillRoundRect((int)(cx - tW / 2.0), (int)(by - tH), tW, tH, tW, tW);
            int aW = (int)(height * 0.13), aH = (int)(height * 0.45);
            g.fillRoundRect((int)(cx - tW / 2.0 - aW), (int)(by - tH * 0.7), aW, aH, aW, aW);
            g.fillRoundRect((int)(cx + tW / 2.0), (int)(by - tH * 0.8), aW, aH, aW, aW);
        }

        private void drawPalm(Graphics2D g, double cx, double by, double height) {
            g.setColor(new Color(80, 50, 30));
            int tW = (int)(height * 0.07);
            g.fillRoundRect((int)(cx - tW / 2.0), (int)(by - height),
                            tW, (int) height, tW, tW);
            g.setColor(new Color(40, 110, 60));
            double tx = cx, ty = by - height;
            for (int k = 0; k < 7; k++) {
                double ang = -Math.PI / 2 + (k - 3) * 0.45;
                double fx = tx + Math.cos(ang) * height * 0.45;
                double fy = ty + Math.sin(ang) * height * 0.45;
                Path2D.Double f = new Path2D.Double();
                f.moveTo(tx, ty);
                f.quadTo((tx + fx) / 2, ty - height * 0.1, fx, fy);
                f.quadTo((tx + fx) / 2 + 4, ty + 4, tx + 2, ty + 2);
                f.closePath();
                g.fill(f);
            }
        }

        private void drawWaterMirror(Graphics2D g, int cx, int cy, int rw, int rh) {
            g.setColor(new Color(60, 140, 180, 200));
            g.fillOval(cx - rw, cy - rh, rw * 2, rh * 2);
            g.setColor(new Color(220, 240, 255, 160));
            g.fillOval(cx - rw / 2, cy - rh, rw, rh / 2);
        }

        private void drawRock(Graphics2D g, double cx, double by, double size, Color c) {
            g.setColor(c);
            Path2D.Double p = new Path2D.Double();
            p.moveTo(cx - size, by);
            p.lineTo(cx - size * 0.6, by - size * 0.9);
            p.lineTo(cx + size * 0.4, by - size * 1.0);
            p.lineTo(cx + size, by - size * 0.4);
            p.lineTo(cx + size, by);
            p.closePath();
            g.fill(p);
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);
            Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            drawSky(g, w, h);
            if (style == Style.DUNE_SEA) {
                int rr = (int)(h * 0.32);
                g.setColor(new Color(255, 240, 180));
                g.fillOval(w * 2 / 3, h / 6, rr, rr);
            } else if (style == Style.NIGHT) {
                g.setColor(new Color(255, 255, 230));
                for (int i = 0; i < 12; i++)
                    g.fillOval((i * 53) % w, (i * 17) % (h / 2), 2, 2);
                int mr = (int)(h * 0.16);
                g.setColor(new Color(240, 235, 210));
                g.fillOval(w * 3 / 4, h / 6, mr, mr);
                g.setColor(new Color(8, 14, 40));
                g.fillOval(w * 3 / 4 + mr / 3, h / 6 - 1, mr, mr);
            } else if (style == Style.MESA_DUSK) {
                g.setColor(new Color(255, 200, 110));
                g.fillOval(w / 6, h / 3, (int)(h * 0.22), (int)(h * 0.22));
            }
            if (style == Style.MESA_DUSK) {
                drawMesas(g, w, h, 0,  h * 0.62, new Color(120, 50, 40), 4);
                drawMesas(g, w, h, 20, h * 0.80, new Color(70, 25, 25), 3);
            } else {
                Color m, f;
                switch (style) {
                    case DUNE_SEA:  m=new Color(210,160,70); f=new Color(170,120,50); break;
                    case SANDSTORM: m=new Color(180,150,80); f=new Color(140,110,60); break;
                    case OASIS:     m=new Color(220,190,130); f=new Color(180,150,90); break;
                    case NIGHT:     m=new Color(40,30,60);   f=new Color(20,15,35);   break;
                    default:        m=accent.darker(); f=accent.darker().darker();    break;
                }
                drawDuneBand(g, w, h, 0,  h * 0.60, h * 0.06, 0.04, m);
                drawDuneBand(g, w, h, 18, h * 0.78, h * 0.05, 0.06, f);
            }
            if (style == Style.DUNE_SEA) {
                drawCactus(g, w * 0.18, h * 0.85, h * 0.18, new Color(60, 90, 50));
            } else if (style == Style.OASIS) {
                drawPalm(g, w * 0.25, h * 0.85, h * 0.32);
                drawWaterMirror(g, w / 2, (int)(h * 0.72),
                                (int)(w * 0.12), (int)(h * 0.03));
            } else if (style == Style.SANDSTORM) {
                g.setColor(new Color(245, 230, 170, 180));
                for (int i = 0; i < 14; i++)
                    g.fillRect((i * 23) % w, (i * 11) % h, 18, 2);
                g.setColor(new Color(220, 200, 140, 80));
                g.fillRect(0, 0, w, h);
            } else if (style == Style.NIGHT) {
                drawCactus(g, w * 0.78, h * 0.88, h * 0.14, new Color(20, 35, 25));
            }
            g.translate(-x, -y);
            if (oldAA != null)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
            g.setClip(oldClip);
        }
    }

    private static final class Variant extends AbstractDesert {
        Variant(String id, String name, int price, String tagline,
                Style style, Color accent) {
            super(id, name, price, tagline, style, accent);
        }
    }
}
