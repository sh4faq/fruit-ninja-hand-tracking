package themes.backgrounds;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import game.Camera;
import themes.Background;
import themes.BackgroundCatalog;

/**
 * Dojo family. Reuses the three parallax PNG layers from Bevouliin's
 * Japanese mountain pack (assets/images/layer-1-sky.png etc.) with a
 * per-variant color tint and parallax depth. 5 variants registered under
 * bg_001 .. bg_005.
 *
 * Reference template for the other background-family agents. The pattern:
 *
 *   1. {@code BackgroundsXxxx.java} contains a static block that registers
 *      five {@link Variant} instances with id "bg_NNN".
 *   2. The {@code Variant} subclass stores per-instance config (tint color,
 *      tagline, price, accent) and the abstract base owns rendering.
 */
public final class BackgroundsDojo {

    private BackgroundsDojo() {}

    static {
        BackgroundCatalog.register(new Variant(
            "bg_001", "Sakura Dojo", 0,
            "The training grounds.",
            new Color(255, 255, 255, 0), new Color(255, 180, 200)));
        BackgroundCatalog.register(new Variant(
            "bg_002", "Dojo at Dawn", 120,
            "First light through the bamboo.",
            new Color(255, 200, 140, 70), new Color(255, 180, 100)));
        BackgroundCatalog.register(new Variant(
            "bg_003", "Dojo at Midnight", 220,
            "Moonlit blade work.",
            new Color(20, 40, 90, 130), new Color(80, 120, 220)));
        BackgroundCatalog.register(new Variant(
            "bg_004", "Bloodmoon Dojo", 400,
            "When the red moon rises.",
            new Color(160, 30, 30, 100), new Color(220, 50, 50)));
        BackgroundCatalog.register(new Variant(
            "bg_005", "Ash Dojo", 650,
            "After the fire.",
            new Color(0, 0, 0, 120), new Color(200, 200, 200)));
    }

    // ====================================================================
    //  ABSTRACT BASE
    // ====================================================================

    static abstract class AbstractDojo implements Background {

        private static BufferedImage sky;
        private static BufferedImage mountain;
        private static BufferedImage ground;
        private static boolean assetsTried;

        protected final String id;
        protected final String name;
        protected final int price;
        protected final String tagline;
        protected final Color tint;     // overlay tint, can have alpha 0
        protected final Color accent;   // shown in the shop card preview

        private double timeSec;

        AbstractDojo(String id, String name, int price, String tagline,
                     Color tint, Color accent) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.tagline = tagline;
            this.tint = tint;
            this.accent = accent;
            loadAssetsOnce();
        }

        @Override public String id()       { return id; }
        @Override public String name()     { return name; }
        @Override public int price()       { return price; }
        @Override public String tagline()  { return tagline; }
        @Override public Color previewAccent() { return accent; }

        private static void loadAssetsOnce() {
            if (assetsTried) return;
            assetsTried = true;
            sky      = tryLoad("assets/images/layer-1-sky.png");
            mountain = tryLoad("assets/images/layer-2-mountain.png");
            ground   = tryLoad("assets/images/layer-3-ground.png");
        }

        private static BufferedImage tryLoad(String path) {
            try {
                File f = new File(path);
                if (!f.exists()) return null;
                return ImageIO.read(f);
            } catch (Exception ex) { return null; }
        }

        @Override
        public void update(double deltaMs) {
            timeSec += deltaMs / 1000.0;
            // Slow monotonic horizontal drift. With drawLayer's mirror-tile
            // (even-indexed tiles normal, odd-indexed tiles horizontally
            // flipped), each tile's right edge matches the next tile's left
            // edge pixel-for-pixel so the wrap is invisible. At ~10 px/sec
            // a full image cycle takes about 2.5 minutes -- longer than a
            // typical run, so the content doesn't visibly repeat.
            Camera.x = timeSec * 10.0;
            Camera.y = 0;
        }

        @Override
        public void draw(Graphics2D g, int w, int h) {
            if (sky == null || mountain == null || ground == null) {
                // Fallback
                g.setColor(new Color(12, 10, 14));
                g.fillRect(0, 0, w, h);
                return;
            }
            drawLayer(g, sky,      w, h, Camera.x / 600 * w);
            drawLayer(g, mountain, w, h, Camera.x / 220 * w);
            drawLayer(g, ground,   w, h, Camera.x /  90 * w);

            // Apply the variant's tint overlay
            if (tint.getAlpha() > 0) {
                g.setColor(tint);
                g.fillRect(0, 0, w, h);
            }
        }

        @Override
        public void drawPreview(Graphics2D g, int x, int y, int w, int h) {
            if (sky == null) {
                g.setColor(accent.darker());
                g.fillRect(x, y, w, h);
                return;
            }
            // Draw all three layers fitted into the preview rect
            Composite saved = g.getComposite();
            java.awt.Shape oldClip = g.getClip();
            g.setClip(x, y, w, h);

            double scale = (double) h / sky.getHeight();
            int drawW = (int) (sky.getWidth() * scale);
            g.drawImage(sky, x, y, drawW, h, null);
            g.drawImage(mountain, x, y, drawW, h, null);
            g.drawImage(ground, x, y, drawW, h, null);
            if (tint.getAlpha() > 0) {
                g.setColor(tint);
                g.fillRect(x, y, w, h);
            }

            g.setClip(oldClip);
            g.setComposite(saved);
        }

        private static void drawLayer(Graphics2D g, BufferedImage img,
                                      int panelW, int panelH, double offsetX) {
            double scale = panelH / (double) img.getHeight();
            int drawW = (int) (img.getWidth() * scale);
            if (drawW <= 0) return;

            // Mirror-tile: tile index 0 normal, 1 mirrored, 2 normal, 3 mirrored, ...
            // Each tile's right edge then matches the next tile's left edge because
            // the next tile is a horizontal flip, so the wrap is seamless.
            //
            // We need to know the *absolute* tile index (not just the on-screen
            // index) so the parity stays consistent as the camera drifts past
            // tile boundaries. Otherwise the mirrored/normal pairs would
            // suddenly swap when ox crosses zero.
            int firstTile = (int) Math.floor(offsetX / (double) drawW);
            int ox = (int) Math.floor(offsetX) - firstTile * drawW;
            // After the floor, ox is in [0, drawW); shift left so the leftmost
            // visible tile starts off-screen at xx = -ox.
            int xx = -ox;
            int tileIndex = firstTile;
            while (xx < panelW) {
                if ((tileIndex & 1) == 0) {
                    g.drawImage(img, xx, 0, drawW, panelH, null);
                } else {
                    // Horizontal flip: destination x runs from xx+drawW back to xx.
                    g.drawImage(img, xx + drawW, 0, -drawW, panelH, null);
                }
                xx += drawW;
                tileIndex++;
            }
        }
    }

    private static final class Variant extends AbstractDojo {
        Variant(String id, String name, int price, String tagline,
                Color tint, Color accent) {
            super(id, name, price, tagline, tint, accent);
        }
    }
}
