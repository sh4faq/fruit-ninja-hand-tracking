package entities;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Thin wrapper around FruitArt. Sets up the transform (translate + rotate to
 * the fruit's position and orientation) then delegates the actual drawing to
 * the per-type code in FruitArt.
 */
public final class FruitRenderer {

    private FruitRenderer() {}

    public static void drawWhole(Graphics2D g, Fruit f) {
        AffineTransform saved = g.getTransform();
        g.translate(f.x, f.y);
        double s = f.depthScale();
        g.scale(s, s);
        g.rotate(f.rotation);
        FruitArt.enableQualityHints(g);
        FruitArt.drawWhole(g, f.radius, f.type);
        g.setTransform(saved);
    }

    public static void drawHalf(Graphics2D g, Fruit f) {
        AffineTransform saved = g.getTransform();
        g.translate(f.x, f.y);
        double s = f.depthScale();
        g.scale(s, s);
        g.rotate(f.rotation);
        FruitArt.enableQualityHints(g);
        FruitArt.drawHalf(g, f.radius, f.type, f.halfSide == Fruit.HalfSide.LEFT);
        g.setTransform(saved);
    }
}
