package effects;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import game.Camera;

/**
 * Loads pre-rendered background images from assets/images/ and composes them
 * with z-divided parallax exactly like Murphy's March 10 lecture, except the
 * art is real bitmaps instead of procedurally drawn ridges.
 *
 * Layout (back to front):
 *   layer-1-sky.png       deep z, barely moves
 *   layer-2-mountain.png  mid z, drifts gently
 *   layer-3-ground.png    near z, biggest swing
 *
 * Each layer is drawn twice side by side so the camera can scroll across
 * either edge without exposing the void. Images are scaled to fit the panel
 * height while keeping the aspect ratio, then tiled horizontally.
 *
 * If any image is missing the class falls back to a dim solid color so the
 * game still runs, just without the bitmap backdrop.
 */
public class BitmapBackground {

    private BufferedImage sky;
    private BufferedImage mountain;
    private BufferedImage ground;
    private boolean loaded;

    private static final double Z_SKY      = 600;
    private static final double Z_MOUNTAIN = 220;
    private static final double Z_GROUND   = 90;

    public BitmapBackground() {
        loadAll();
    }

    private void loadAll() {
        sky      = tryLoad("assets/images/layer-1-sky.png");
        mountain = tryLoad("assets/images/layer-2-mountain.png");
        ground   = tryLoad("assets/images/layer-3-ground.png");
        loaded = (sky != null && mountain != null && ground != null);
    }

    private static BufferedImage tryLoad(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            return ImageIO.read(f);
        } catch (Exception ex) {
            return null;
        }
    }

    private double timeSec;

    public void update(double deltaMs) {
        timeSec += deltaMs / 1000.0;
        // Slow gentle horizontal drift that drives the parallax layers.
        Camera.x = Math.sin(timeSec * 0.07) * 220;
        Camera.y = Math.sin(timeSec * 0.04) * 30;
    }

    public void draw(Graphics2D g, int w, int h) {
        if (!loaded) {
            // Fallback solid dark fill
            g.setColor(new java.awt.Color(12, 10, 14));
            g.fillRect(0, 0, w, h);
            return;
        }

        // Each layer is scaled to fill the panel height, then tiled
        // horizontally. The horizontal offset is Camera.x divided by the
        // layer's depth (March 10 parallax formula).
        drawLayer(g, sky,      w, h, Camera.x / Z_SKY      * w);
        drawLayer(g, mountain, w, h, Camera.x / Z_MOUNTAIN * w);
        drawLayer(g, ground,   w, h, Camera.x / Z_GROUND   * w);
    }

    private static void drawLayer(Graphics2D g, BufferedImage img,
                                  int panelW, int panelH, double offsetX) {
        // Scale to fit panel height
        double scale = panelH / (double) img.getHeight();
        int drawW = (int) (img.getWidth() * scale);
        if (drawW <= 0) return;

        // Mirror-tile so the wrap is invisible: even-indexed tiles draw normally,
        // odd-indexed tiles draw horizontally flipped. Each tile's right edge
        // therefore matches the next tile's left edge pixel-for-pixel.
        //
        // Track the absolute tile index (not just on-screen index) so the
        // even/odd parity remains stable as the camera drifts past tile
        // boundaries, otherwise the flips would swap and produce a visible jump.
        int firstTile = (int) Math.floor(offsetX / (double) drawW);
        int ox = (int) Math.floor(offsetX) - firstTile * drawW;
        int x = -ox;
        int tileIndex = firstTile;
        while (x < panelW) {
            if ((tileIndex & 1) == 0) {
                g.drawImage(img, x, 0, drawW, panelH, null);
            } else {
                g.drawImage(img, x + drawW, 0, -drawW, panelH, null);
            }
            x += drawW;
            tileIndex++;
        }
    }
}
