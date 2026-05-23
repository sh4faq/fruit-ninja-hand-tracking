package weapons.swords;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weapons.TrailPoint;

/**
 * Shared sword-head rendering utilities.
 *
 * <p>The big idea: each family renders its blade ONCE into a 160x60
 * {@link BufferedImage} (cached statically per variant id), with proper
 * gradients, drop shadow, hilt-grip detail and an accent gem. Drawing the
 * trail tip then becomes a single {@code drawImage} with an
 * {@link AffineTransform} -- much faster than rebuilding paths every frame,
 * and the gradients give the blades a photographic look instead of a flat
 * vector look.
 *
 * <p>Also provides:
 * <ul>
 *   <li>{@link #drawSwordHeadCached} -- draw with motion-blur afterimages
 *       at recent trail points.</li>
 *   <li>Painter helpers used by every family to compose the cached
 *       BufferedImage: drop shadow, blade body, fuller groove, hilt grip,
 *       cross-guard, pommel gem.</li>
 * </ul>
 *
 * <p>Image layout (in image coords): the sword's tip is at
 * {@code (TIP_X, IMG_H / 2)} and the pommel end is at the far right of the
 * image. When drawn at a trail point, we translate so the tip lands at the
 * trail point and rotate so the blade points along the slice direction.
 */
final class SwordHeadRenderer {

    private SwordHeadRenderer() {}

    /** Cached image canvas size. */
    static final int IMG_W = 160;
    static final int IMG_H = 60;

    /** Tip position inside the image. The blade extends to the right toward the hilt. */
    static final int TIP_X = 6;
    static final int TIP_Y = IMG_H / 2;

    /** Blade body geometry (image-space pixels). */
    static final int BLADE_LEN = 96;       // from TIP_X to start of guard
    static final int BLADE_HALF_H = 6;     // blade half-thickness at base

    /** Hilt geometry. */
    static final int GUARD_X = TIP_X + BLADE_LEN;
    static final int GUARD_W = 8;
    static final int GUARD_H = 22;
    static final int GRIP_X = GUARD_X + GUARD_W;
    static final int GRIP_W = 32;
    static final int GRIP_H = 11;
    static final int POMMEL_X = GRIP_X + GRIP_W;
    static final int POMMEL_W = 14;
    static final int POMMEL_H = 16;

    /** Cache: variant id -> rendered sword head. */
    private static final Map<String, BufferedImage> CACHE = new HashMap<String, BufferedImage>();

    static BufferedImage getCached(String key) {
        return CACHE.get(key);
    }

    static void putCached(String key, BufferedImage img) {
        CACHE.put(key, img);
    }

    /** Create a fresh ARGB image with high-quality rendering hints applied. */
    static BufferedImage newCanvas() {
        BufferedImage img = new BufferedImage(IMG_W, IMG_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_PURE);
        g.dispose();
        return img;
    }

    /** Get a Graphics2D on the image with all the right rendering hints. */
    static Graphics2D openGraphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    // ====================================================================
    //  HIGH-LEVEL RENDER / DRAW
    // ====================================================================

    /**
     * Draw the cached sword-head image at (tipX, tipY) rotated to angleDeg.
     * Also draws 2-3 motion-blur afterimages at recent trail points.
     */
    static void drawSwordHeadCached(Graphics2D g, BufferedImage head,
                                    List<TrailPoint> points) {
        int n = points.size();
        if (n < 2 || head == null) return;

        // Motion blur afterimages: draw the head at recent trail points,
        // back from the tip, with decreasing alpha.
        Composite oldComp = g.getComposite();
        int[] sampleIdx = motionBlurSamples(n);
        for (int s = sampleIdx.length - 1; s >= 0; s--) {
            int i = sampleIdx[s];
            if (i <= 0 || i >= n) continue;
            TrailPoint p = points.get(i);
            TrailPoint a = points.get(Math.max(0, i - 1));
            TrailPoint b = points.get(Math.min(n - 1, i + 1));
            double angleDeg = Math.toDegrees(Math.atan2(a.y - b.y, a.x - b.x));
            float alpha = 0.18f + 0.12f * (sampleIdx.length - s); // older = dimmer
            if (alpha > 0.45f) alpha = 0.45f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            drawHeadAt(g, head, p.x, p.y, angleDeg);
        }
        g.setComposite(oldComp);

        // Main head at the freshest point.
        TrailPoint tip = points.get(0);
        TrailPoint next = points.get(1);
        double angle = Math.toDegrees(Math.atan2(tip.y - next.y, tip.x - next.x));
        drawHeadAt(g, head, tip.x, tip.y, angle);
    }

    /** Pick 2-3 indices into the trail to use for motion-blur afterimages. */
    private static int[] motionBlurSamples(int n) {
        if (n >= 7) return new int[] { 2, 4, 6 };
        if (n >= 5) return new int[] { 2, 4 };
        if (n >= 3) return new int[] { 2 };
        return new int[0];
    }

    /** Internal: render the cached head at a single trail point. */
    private static void drawHeadAt(Graphics2D g, BufferedImage head,
                                   double tipX, double tipY, double angleDeg) {
        AffineTransform old = g.getTransform();
        AffineTransform t = new AffineTransform();
        t.translate(tipX, tipY);
        t.rotate(Math.toRadians(angleDeg + 180.0)); // image points right; flip so blade points OUT toward motion
        t.translate(-TIP_X, -TIP_Y);
        g.drawImage(head, t, null);
        g.setTransform(old);
    }

    // ====================================================================
    //  COMPONENT PAINTERS -- composed by each family's renderer.
    // ====================================================================

    /** Multi-pass soft drop shadow for the whole sword silhouette. */
    static void paintDropShadow(Graphics2D g, Path2D.Double silhouette) {
        Composite old = g.getComposite();
        AffineTransform oldT = g.getTransform();
        for (int pass = 3; pass >= 1; pass--) {
            g.setTransform(oldT);
            g.translate(pass, pass + 1);
            float alpha = 0.08f * pass;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(new Color(0, 0, 0));
            g.setStroke(new BasicStroke(pass * 1.6f));
            g.draw(silhouette);
            g.fill(silhouette);
        }
        g.setTransform(oldT);
        g.setComposite(old);
    }

    /**
     * Paint a realistic single-edge blade body with a linear gradient
     * (edge highlight -> mid steel -> dark fuller -> back) plus an inner-glow
     * stroke in the accent color along the cutting edge.
     */
    static void paintBladeBody(Graphics2D g, Color steel, Color accent) {
        // Blade silhouette: pointed tip on the left, rectangular base on the right.
        Path2D.Double blade = new Path2D.Double();
        blade.moveTo(TIP_X, TIP_Y);                                   // sharp tip
        blade.quadTo(TIP_X + BLADE_LEN * 0.25, TIP_Y - BLADE_HALF_H - 1.0,
                     TIP_X + BLADE_LEN * 0.85, TIP_Y - BLADE_HALF_H);  // back curve
        blade.lineTo(GUARD_X, TIP_Y - BLADE_HALF_H);                  // back at base
        blade.lineTo(GUARD_X, TIP_Y + BLADE_HALF_H);                  // bottom at base
        blade.quadTo(TIP_X + BLADE_LEN * 0.55, TIP_Y + BLADE_HALF_H + 0.5,
                     TIP_X, TIP_Y);                                   // edge curve back to tip
        blade.closePath();

        // Drop shadow under the blade.
        paintDropShadow(g, blade);

        // Vertical gradient: top (back) = dark fuller, mid = bright steel highlight,
        // bottom (cutting edge) = bright white edge highlight.
        float topY = TIP_Y - BLADE_HALF_H - 1f;
        float botY = TIP_Y + BLADE_HALF_H + 1f;
        Color dark = darker(steel, 0.45f);
        Color mid  = steel;
        Color hi   = lighter(steel, 0.55f);
        Color edge = new Color(255, 255, 255);
        float[] dist = { 0.0f, 0.45f, 0.75f, 1.0f };
        Color[] cols = { dark, mid, hi, edge };
        LinearGradientPaint lg = new LinearGradientPaint(
            new Point2D.Float(0, topY), new Point2D.Float(0, botY), dist, cols);
        Paint oldPaint = g.getPaint();
        g.setPaint(lg);
        g.fill(blade);

        // Dark fuller groove running along the spine.
        g.setPaint(new Color(20, 20, 28, 170));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double fuller = new Path2D.Double();
        fuller.moveTo(TIP_X + BLADE_LEN * 0.12, TIP_Y - BLADE_HALF_H * 0.25);
        fuller.quadTo(TIP_X + BLADE_LEN * 0.55, TIP_Y - BLADE_HALF_H * 0.55,
                      GUARD_X - 2, TIP_Y - BLADE_HALF_H * 0.30);
        g.draw(fuller);

        // Bright edge highlight along the cutting edge.
        g.setPaint(new Color(255, 255, 255, 235));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double edgeLine = new Path2D.Double();
        edgeLine.moveTo(TIP_X + 1, TIP_Y + 0.5);
        edgeLine.quadTo(TIP_X + BLADE_LEN * 0.55, TIP_Y + BLADE_HALF_H + 0.5,
                        GUARD_X - 2, TIP_Y + BLADE_HALF_H - 1.0);
        g.draw(edgeLine);

        // 1-pixel inner glow along the cutting edge in the accent color.
        g.setPaint(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
        g.setStroke(new BasicStroke(1.0f));
        Path2D.Double inner = new Path2D.Double();
        inner.moveTo(TIP_X + 2, TIP_Y + 1.5);
        inner.quadTo(TIP_X + BLADE_LEN * 0.55, TIP_Y + BLADE_HALF_H - 0.8,
                     GUARD_X - 3, TIP_Y + BLADE_HALF_H - 2.0);
        g.draw(inner);

        g.setPaint(oldPaint);
    }

    /**
     * Paint a cross-guard / tsuba at the base of the blade with rivet detail.
     */
    static void paintCrossGuard(Graphics2D g, Color guardColor) {
        int gx = GUARD_X;
        int gy = TIP_Y - GUARD_H / 2;
        Path2D.Double guard = new Path2D.Double();
        guard.append(new java.awt.geom.RoundRectangle2D.Double(gx, gy, GUARD_W, GUARD_H, 3, 3), false);

        paintDropShadow(g, silhouetteFrom(guard));

        // Body gradient: dark on top, bright middle, dark on bottom (rounded metal).
        Color dark = darker(guardColor, 0.45f);
        Color mid  = guardColor;
        Color hi   = lighter(guardColor, 0.45f);
        float[] dist = { 0.0f, 0.35f, 0.65f, 1.0f };
        Color[] cols = { dark, mid, hi, dark };
        LinearGradientPaint lg = new LinearGradientPaint(
            new Point2D.Float(gx, gy),
            new Point2D.Float(gx, gy + GUARD_H), dist, cols);
        Paint oldPaint = g.getPaint();
        g.setPaint(lg);
        g.fill(guard);

        // Dark outline.
        g.setPaint(new Color(10, 10, 14, 220));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(guard);

        // Three rivets down the middle.
        for (int i = 0; i < 3; i++) {
            int ry = gy + 5 + i * 6;
            int rx = gx + GUARD_W / 2;
            g.setPaint(new Color(40, 40, 50));
            g.fillOval(rx - 2, ry - 2, 4, 4);
            g.setPaint(new Color(220, 220, 230, 220));
            g.fillOval(rx - 1, ry - 2, 2, 2);
        }

        g.setPaint(oldPaint);
    }

    /**
     * Paint the wrapped grip with diagonal binding lines.
     */
    static void paintGrip(Graphics2D g, Color wrapColor) {
        int gx = GRIP_X;
        int gy = TIP_Y - GRIP_H / 2;
        Path2D.Double grip = new Path2D.Double();
        grip.append(new java.awt.geom.RoundRectangle2D.Double(gx, gy, GRIP_W, GRIP_H, 3, 3), false);

        paintDropShadow(g, silhouetteFrom(grip));

        // Grip gradient: dark base, slight highlight in the middle.
        Color dark = darker(wrapColor, 0.55f);
        Color hi   = lighter(wrapColor, 0.30f);
        float[] dist = { 0.0f, 0.5f, 1.0f };
        Color[] cols = { dark, hi, dark };
        LinearGradientPaint lg = new LinearGradientPaint(
            new Point2D.Float(gx, gy),
            new Point2D.Float(gx, gy + GRIP_H), dist, cols);
        Paint oldPaint = g.getPaint();
        g.setPaint(lg);
        g.fill(grip);

        // Diagonal binding lines (wrapping ridges).
        g.setPaint(new Color(10, 10, 14, 200));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        int stripes = 7;
        for (int i = 0; i <= stripes; i++) {
            int sx = gx + (GRIP_W * i) / stripes;
            // Diagonal: top-left to bottom-right.
            g.drawLine(sx, gy, sx + 3, gy + GRIP_H);
        }
        // Faint highlight stripes.
        g.setPaint(new Color(255, 255, 255, 60));
        for (int i = 0; i <= stripes; i++) {
            int sx = gx + (GRIP_W * i) / stripes + 1;
            g.drawLine(sx, gy + 1, sx + 3, gy + GRIP_H - 1);
        }
        // Grip outline.
        g.setPaint(new Color(10, 10, 14, 220));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(grip);

        g.setPaint(oldPaint);
    }

    /**
     * Paint the pommel cap with a gem inlay in the accent color.
     */
    static void paintPommel(Graphics2D g, Color capColor, Color gemColor) {
        int px = POMMEL_X;
        int py = TIP_Y - POMMEL_H / 2;
        Path2D.Double pommel = new Path2D.Double();
        pommel.append(new java.awt.geom.RoundRectangle2D.Double(px, py, POMMEL_W, POMMEL_H, 6, 6), false);

        paintDropShadow(g, silhouetteFrom(pommel));

        // Radial gradient for a 3D-ish metal cap.
        Color dark = darker(capColor, 0.50f);
        Color hi   = lighter(capColor, 0.55f);
        Paint oldPaint = g.getPaint();
        RadialGradientPaint rg = new RadialGradientPaint(
            new Point2D.Float(px + POMMEL_W * 0.4f, py + POMMEL_H * 0.35f),
            POMMEL_W,
            new float[] { 0f, 0.7f, 1f },
            new Color[] { hi, capColor, dark });
        g.setPaint(rg);
        g.fill(pommel);
        g.setPaint(new Color(10, 10, 14, 220));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(pommel);

        // Gem inlay in the center.
        int gemCx = px + POMMEL_W / 2;
        int gemCy = TIP_Y;
        int gemR = 4;
        // Gem body with radial highlight.
        Color gemDark = darker(gemColor, 0.50f);
        Color gemHi   = lighter(gemColor, 0.65f);
        RadialGradientPaint gemP = new RadialGradientPaint(
            new Point2D.Float(gemCx - 1, gemCy - 1), gemR + 1,
            new float[] { 0f, 0.5f, 1f },
            new Color[] { gemHi, gemColor, gemDark });
        g.setPaint(gemP);
        g.fill(new Ellipse2D.Double(gemCx - gemR, gemCy - gemR, gemR * 2, gemR * 2));
        // Specular dot.
        g.setPaint(new Color(255, 255, 255, 235));
        g.fillOval(gemCx - 2, gemCy - 2, 2, 2);
        // Gem rim.
        g.setPaint(new Color(10, 10, 14, 220));
        g.setStroke(new BasicStroke(0.8f));
        g.drawOval(gemCx - gemR, gemCy - gemR, gemR * 2, gemR * 2);

        g.setPaint(oldPaint);
    }

    // ====================================================================
    //  PIECEWISE COLOR HELPERS
    // ====================================================================

    static Color lighter(Color c, float t) {
        if (t < 0f) t = 0f; if (t > 1f) t = 1f;
        int r = (int) (c.getRed()   + (255 - c.getRed())   * t);
        int gg = (int) (c.getGreen() + (255 - c.getGreen()) * t);
        int b = (int) (c.getBlue()  + (255 - c.getBlue())  * t);
        return new Color(r, gg, b);
    }

    static Color darker(Color c, float t) {
        if (t < 0f) t = 0f; if (t > 1f) t = 1f;
        int r = (int) (c.getRed()   * (1f - t));
        int gg = (int) (c.getGreen() * (1f - t));
        int b = (int) (c.getBlue()  * (1f - t));
        return new Color(r, gg, b);
    }

    static Color withAlpha(Color c, int a) {
        if (a < 0) a = 0; if (a > 255) a = 255;
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Convert any shape's bounds into a quick silhouette path for shadow use. */
    private static Path2D.Double silhouetteFrom(Path2D.Double src) {
        Path2D.Double sil = new Path2D.Double();
        sil.append(src.getPathIterator(null), false);
        return sil;
    }
}
