package entities;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Random;

/**
 * Photoreal fruit rendering. Each fruit is built from layered Graphics2D
 * paint passes: a radial-gradient base, ambient-occlusion ring, diffuse and
 * specular highlights, plus per-type texture (stripes, peel bumps, fuzz)
 * and stems / leaves / seeds.
 *
 * Designed to read well against a real bitmap background. The strong outlines
 * and bright highlights make each fruit pop without needing a flat void
 * behind it.
 *
 * Drawn under an AffineTransform set up by the caller. We never touch the
 * Graphics2D transform here; we draw at (0, 0) and rely on the caller's
 * translate/rotate/scale.
 */
final class FruitArt {

    private FruitArt() {}

    // ====================================================================
    //  PUBLIC DISPATCH
    // ====================================================================

    static void drawWhole(Graphics2D g, double r, FruitType type) {
        switch (type) {
            case APPLE:      drawApple(g, r); break;
            case ORANGE:     drawOrange(g, r); break;
            case WATERMELON: drawWatermelon(g, r); break;
            case COCONUT:    drawCoconut(g, r); break;
            case LEMON:      drawLemon(g, r); break;
            case GRAPE:      drawGrape(g, r); break;
            case KIWI:       drawKiwi(g, r); break;
            case PEACH:      drawPeach(g, r); break;
        }
    }

    static void drawHalf(Graphics2D g, double r, FruitType type, boolean isLeft) {
        drawHalfShared(g, r, type, isLeft);
    }

    static void enableQualityHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                           RenderingHints.VALUE_STROKE_PURE);
    }

    static Composite normalComposite() {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    }

    // ====================================================================
    //  HELPERS
    // ====================================================================

    private static Color a(int rgb, int alpha) {
        return new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff, alpha);
    }
    private static Color rgb(int rgb) { return new Color(rgb); }

    private static void shadow(Graphics2D g, double r) { shadow(g, r, r); }
    private static void shadow(Graphics2D g, double rx, double ry) {
        g.setColor(new Color(0, 0, 0, 110));
        g.fill(new Ellipse2D.Double(-rx * 1.02 + 5, -ry * 1.02 + 6,
                                    rx * 2.04, ry * 2.04));
    }

    private static void specular(Graphics2D g, double x, double y, double size) {
        Point2D p = new Point2D.Double(x, y);
        float[] dist = { 0f, 0.3f, 0.7f, 1f };
        Color[] cols = {
            new Color(255, 255, 255, 240),
            new Color(255, 255, 255, 150),
            new Color(255, 255, 255, 25),
            new Color(255, 255, 255, 0),
        };
        g.setPaint(new RadialGradientPaint(p, (float) size, dist, cols));
        g.fill(new Ellipse2D.Double(x - size, y - size, size * 2, size * 2));
    }

    private static void diffuseHighlight(Graphics2D g, double x, double y,
                                         double rx, double ry) {
        Point2D p = new Point2D.Double(x, y);
        float[] dist = { 0f, 0.5f, 1f };
        Color[] cols = {
            new Color(255, 255, 255, 115),
            new Color(255, 255, 255, 38),
            new Color(255, 255, 255, 0),
        };
        g.setPaint(new RadialGradientPaint(p, (float) Math.max(rx, ry),
                                            dist, cols));
        g.fill(new Ellipse2D.Double(x - rx, y - ry, rx * 2, ry * 2));
    }

    private static void ao(Graphics2D g, Shape clip, double radius, int alpha) {
        Shape oldClip = g.getClip();
        g.setClip(clip);
        Point2D p = new Point2D.Double(0, 0);
        float[] dist = { 0.7f, 1f };
        Color[] cols = { new Color(0, 0, 0, 0), new Color(0, 0, 0, alpha) };
        g.setPaint(new RadialGradientPaint(p, (float) radius, dist, cols));
        g.fill(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));
        g.setClip(oldClip);
    }

    private static void inkRim(Graphics2D g, Shape shape, float width) {
        g.setColor(new Color(0, 0, 0, 170));
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    // ====================================================================
    //  APPLE
    // ====================================================================

    private static void drawApple(Graphics2D g, double r) {
        shadow(g, r);

        Path2D.Double body = new Path2D.Double();
        body.moveTo(0, -r * 0.82);
        body.curveTo( r * 0.85, -r * 0.88,  r * 1.10, -r * 0.25,  r * 0.92,  r * 0.42);
        body.curveTo( r * 0.78,  r * 1.02,  r * 0.28,  r * 1.06,  0,        r * 0.95);
        body.curveTo(-r * 0.28,  r * 1.06, -r * 0.78,  r * 1.02, -r * 0.92,  r * 0.42);
        body.curveTo(-r * 1.10, -r * 0.25, -r * 0.85, -r * 0.88,  0,        -r * 0.82);
        body.closePath();

        Point2D bc = new Point2D.Double(-r * 0.15, -r * 0.10);
        float[] bd = { 0f, 0.25f, 0.5f, 0.75f, 1f };
        Color[] bcols = { rgb(0xff4040), rgb(0xe63535), rgb(0xcc2929),
                          rgb(0xa31e1e), rgb(0x7a1515) };
        g.setPaint(new RadialGradientPaint(bc, (float) (r * 1.3), bd, bcols));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        Point2D gp = new Point2D.Double(r * 0.1, -r * 0.6);
        g.setPaint(new RadialGradientPaint(gp, (float) (r * 0.5),
            new float[] { 0f, 0.6f, 1f },
            new Color[] { a(0x50A032, 90), a(0x50A032, 30), a(0x50A032, 0) }));
        g.fill(new Ellipse2D.Double(-r * 1.2, -r * 1.2, r * 2.4, r * 2.4));
        g.setClip(oldClip);

        ao(g, body, r * 1.1, 70);
        inkRim(g, body, 2.5f);
        diffuseHighlight(g, -r * 0.3, -r * 0.35, r * 0.4, r * 0.28);
        specular(g, -r * 0.32, -r * 0.42, r * 0.18);
        specular(g, -r * 0.15, -r * 0.55, r * 0.07);

        Path2D.Double stem = new Path2D.Double();
        stem.moveTo(-1, -r * 0.78);
        stem.curveTo(0, -r * 1.0, 3, -r * 1.15, 6, -r * 1.22);
        g.setColor(rgb(0x4a3520));
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(stem);

        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(6, -r * 1.18);
        leaf.quadTo(r * 0.55, -r * 1.42, r * 0.45, -r * 1.02);
        leaf.quadTo(r * 0.25, -r * 1.22, 6, -r * 1.18);
        leaf.closePath();
        LinearGradientPaint leafGrad = new LinearGradientPaint(
            new Point2D.Double(6, -r * 1.4),
            new Point2D.Double(r * 0.45, -r * 1.0),
            new float[] { 0f, 0.5f, 1f },
            new Color[] { rgb(0x2d8a3e), rgb(0x3cb44b), rgb(0x228833) });
        g.setPaint(leafGrad);
        g.fill(leaf);
        inkRim(g, leaf, 1.5f);
    }

    // ====================================================================
    //  ORANGE
    // ====================================================================

    private static void drawOrange(Graphics2D g, double r) {
        shadow(g, r);

        Ellipse2D.Double body = new Ellipse2D.Double(-r, -r, r * 2, r * 2);
        Point2D bc = new Point2D.Double(-r * 0.25, -r * 0.25);
        g.setPaint(new RadialGradientPaint(bc, (float) (r * 1.1),
            new float[] { 0f, 0.3f, 0.6f, 0.85f, 1f },
            new Color[] { rgb(0xffb74d), rgb(0xff9800), rgb(0xf57c00),
                          rgb(0xe65100), rgb(0xbf360c) }));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        Random rng = new Random((long) (r * 100) + 17);
        for (int i = 0; i < 80; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * r * 0.92;
            double bx = Math.cos(angle) * dist;
            double by = Math.sin(angle) * dist;
            double bumpR = 1.5 + rng.nextDouble() * 2.5;
            g.setColor(rng.nextBoolean()
                ? new Color(255, 200, 120, 46)
                : new Color(200, 100, 0, 30));
            g.fill(new Ellipse2D.Double(bx - bumpR, by - bumpR, bumpR * 2, bumpR * 2));
        }
        g.setClip(oldClip);

        ao(g, body, r * 1.0, 55);
        inkRim(g, body, 2.5f);
        diffuseHighlight(g, -r * 0.3, -r * 0.3, r * 0.38, r * 0.25);
        specular(g, -r * 0.3, -r * 0.35, r * 0.16);
        specular(g, -r * 0.12, -r * 0.48, r * 0.06);

        // Tiny stem leaf
        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(0, -r * 0.90);
        leaf.quadTo(r * 0.4, -r * 1.18, r * 0.35, -r * 0.80);
        leaf.quadTo(r * 0.15, -r * 0.98, 0, -r * 0.90);
        leaf.closePath();
        g.setColor(rgb(0x3cb44b));
        g.fill(leaf);
        inkRim(g, leaf, 1.4f);

        // Navel dimple
        g.setColor(new Color(100, 60, 20, 130));
        g.fill(new Ellipse2D.Double(-r * 0.08, r * 0.62, r * 0.16, r * 0.18));
    }

    // ====================================================================
    //  WATERMELON
    // ====================================================================

    private static void drawWatermelon(Graphics2D g, double r) {
        double rx = r * 1.2;
        double ry = r * 0.9;
        shadow(g, rx, ry);

        Ellipse2D.Double body = new Ellipse2D.Double(-rx, -ry, rx * 2, ry * 2);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(-rx * 0.2, -ry * 0.2), (float) rx,
            new float[] { 0f, 0.4f, 0.7f, 1f },
            new Color[] { rgb(0x4caf50), rgb(0x388e3c), rgb(0x2e7d32), rgb(0x1b5e20) }));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        Random rng = new Random((long) (r * 100) + 31);
        for (int i = -3; i <= 3; i++) {
            Path2D.Double stripe = new Path2D.Double();
            double x0 = i * r * 0.32;
            stripe.moveTo(x0, -ry * 1.1);
            stripe.curveTo(x0 + r * 0.05, -ry * 0.3, x0 - r * 0.05, ry * 0.3,
                           x0, ry * 1.1);
            float w = (float) (10 + rng.nextDouble() * 6);
            g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(27, 80, 20, 160));
            g.draw(stripe);
        }
        g.setClip(oldClip);

        ao(g, body, rx, 55);
        inkRim(g, body, 2.5f);
        diffuseHighlight(g, -rx * 0.3, -ry * 0.3, rx * 0.35, ry * 0.22);
        specular(g, -rx * 0.28, -ry * 0.35, r * 0.14);
    }

    // ====================================================================
    //  COCONUT
    // ====================================================================

    private static void drawCoconut(Graphics2D g, double r) {
        shadow(g, r);

        Ellipse2D.Double body = new Ellipse2D.Double(-r, -r, r * 2, r * 2);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(-r * 0.25, -r * 0.25), (float) r,
            new float[] { 0f, 0.3f, 0.6f, 1f },
            new Color[] { rgb(0xa07050), rgb(0x8b5e3c), rgb(0x6d4228), rgb(0x3e2415) }));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        Random rng = new Random((long) (r * 100) + 47);
        for (int i = 0; i < 60; i++) {
            double sa = rng.nextDouble() * Math.PI * 2;
            double len = r * 0.25 + rng.nextDouble() * r * 0.6;
            double startR = r * 0.15 + rng.nextDouble() * r * 0.3;
            double x0 = Math.cos(sa) * startR;
            double y0 = Math.sin(sa) * startR;
            double cx = Math.cos(sa + 0.2) * len;
            double cy = Math.sin(sa + 0.2) * len;
            double x1 = Math.cos(sa + 0.4) * len * 0.9;
            double y1 = Math.sin(sa + 0.4) * len * 0.9;
            Path2D.Double hair = new Path2D.Double();
            hair.moveTo(x0, y0);
            hair.quadTo(cx, cy, x1, y1);
            g.setColor(new Color(60 + rng.nextInt(40), 30 + rng.nextInt(30),
                                 15 + rng.nextInt(15), 50 + rng.nextInt(60)));
            g.setStroke(new BasicStroke((float) (0.8 + rng.nextDouble() * 1.2)));
            g.draw(hair);
        }
        g.setClip(oldClip);

        ao(g, body, r, 80);
        inkRim(g, body, 2.5f);

        double[][] eyes = { { -r * 0.22, -r * 0.08 }, { r * 0.22, -r * 0.08 }, { 0, r * 0.22 } };
        for (double[] eye : eyes) {
            g.setPaint(new RadialGradientPaint(
                new Point2D.Double(eye[0], eye[1]), (float) (r * 0.13),
                new float[] { 0f, 0.7f, 1f },
                new Color[] { rgb(0x1a0e05), rgb(0x2d1a0c), rgb(0x3e2415) }));
            g.fill(new Ellipse2D.Double(eye[0] - r * 0.1, eye[1] - r * 0.13,
                                        r * 0.2, r * 0.26));
        }
        diffuseHighlight(g, -r * 0.35, -r * 0.35, r * 0.3, r * 0.2);
        specular(g, -r * 0.35, -r * 0.4, r * 0.12);
    }

    // ====================================================================
    //  LEMON
    // ====================================================================

    private static void drawLemon(Graphics2D g, double r) {
        double rx = r * 1.2;
        double ry = r * 0.82;
        shadow(g, rx, ry);

        Path2D.Double body = new Path2D.Double();
        body.moveTo(-rx * 1.05, 0);
        body.curveTo(-rx * 0.95, -ry * 0.85, -rx * 0.4, -ry * 1.05, 0, -ry * 0.98);
        body.curveTo(rx * 0.4, -ry * 1.05, rx * 0.95, -ry * 0.85, rx * 1.05, 0);
        body.curveTo(rx * 0.95, ry * 0.85, rx * 0.4, ry * 1.05, 0, ry * 0.98);
        body.curveTo(-rx * 0.4, ry * 1.05, -rx * 0.95, ry * 0.85, -rx * 1.05, 0);
        body.closePath();

        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(-r * 0.2, -r * 0.15), (float) (rx * 1.2),
            new float[] { 0f, 0.3f, 0.55f, 0.8f, 1f },
            new Color[] { rgb(0xfff59d), rgb(0xffee58), rgb(0xfdd835),
                          rgb(0xf9a825), rgb(0xf57f17) }));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        Random rng = new Random((long) (r * 100) + 59);
        for (int i = 0; i < 40; i++) {
            double px = (rng.nextDouble() - 0.5) * rx * 2.2;
            double py = (rng.nextDouble() - 0.5) * ry * 2.2;
            if ((px * px) / (rx * rx) + (py * py) / (ry * ry) >= 1) continue;
            double pr = 1 + rng.nextDouble() * 1.5;
            g.setColor(rng.nextBoolean()
                ? new Color(255, 245, 100, 80)
                : new Color(200, 160, 20, 38));
            g.fill(new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2));
        }
        g.setClip(oldClip);

        ao(g, body, rx, 55);
        inkRim(g, body, 2.5f);
        diffuseHighlight(g, -r * 0.35, -r * 0.25, r * 0.4, r * 0.22);
        specular(g, -r * 0.35, -r * 0.32, r * 0.15);
    }

    // ====================================================================
    //  GRAPE BUNCH
    // ====================================================================

    private static void drawGrape(Graphics2D g, double r) {
        shadow(g, r);

        double[][] grapes = {
            { 0,         -r * 0.48, r * 0.40 },
            { -r * 0.36, -r * 0.12, r * 0.38 },
            {  r * 0.36, -r * 0.12, r * 0.38 },
            { -r * 0.18,  r * 0.28, r * 0.36 },
            {  r * 0.18,  r * 0.28, r * 0.36 },
            { 0,          r * 0.58, r * 0.33 },
        };

        for (double[] grape : grapes) {
            double gx = grape[0], gy = grape[1], gs = grape[2];
            Ellipse2D.Double e = new Ellipse2D.Double(gx - gs, gy - gs, gs * 2, gs * 2);
            g.setPaint(new RadialGradientPaint(
                new Point2D.Double(gx - gs * 0.25, gy - gs * 0.25), (float) gs,
                new float[] { 0f, 0.3f, 0.65f, 1f },
                new Color[] { rgb(0xc39bd3), rgb(0x9b59b6),
                              rgb(0x7d3c98), rgb(0x512e5f) }));
            g.fill(e);
            inkRim(g, e, 1.5f);
            specular(g, gx - gs * 0.25, gy - gs * 0.3, gs * 0.22);
        }

        Path2D.Double stem = new Path2D.Double();
        stem.moveTo(0, -r * 0.85);
        stem.quadTo(2, -r * 1.1, 0, -r * 1.25);
        g.setColor(rgb(0x5d4037));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(stem);
    }

    // ====================================================================
    //  KIWI
    // ====================================================================

    private static void drawKiwi(Graphics2D g, double r) {
        double rx = r * 1.1;
        double ry = r * 0.85;
        shadow(g, rx, ry);

        Ellipse2D.Double body = new Ellipse2D.Double(-rx, -ry, rx * 2, ry * 2);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(-rx * 0.2, -ry * 0.2), (float) rx,
            new float[] { 0f, 0.3f, 0.6f, 1f },
            new Color[] { rgb(0xa59275), rgb(0x8d7557),
                          rgb(0x7a6245), rgb(0x5c4530) }));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        Random rng = new Random((long) (r * 100) + 73);
        for (int i = 0; i < 80; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = rng.nextDouble() * rx * 0.95;
            double hx = Math.cos(angle) * dist;
            double hy = Math.sin(angle) * dist * (ry / rx);
            double hairLen = 2 + rng.nextDouble() * 4;
            double hairAngle = angle + (rng.nextDouble() - 0.5) * 0.8;
            g.setColor(new Color(100 + rng.nextInt(60), 75 + rng.nextInt(40),
                                 50 + rng.nextInt(30), 60 + rng.nextInt(60)));
            g.setStroke(new BasicStroke((float) (0.5 + rng.nextDouble() * 0.8)));
            g.draw(new Line2D.Double(hx, hy,
                hx + Math.cos(hairAngle) * hairLen,
                hy + Math.sin(hairAngle) * hairLen));
        }
        g.setClip(oldClip);

        inkRim(g, body, 2f);
        diffuseHighlight(g, -rx * 0.3, -ry * 0.25, rx * 0.3, ry * 0.2);
        specular(g, -rx * 0.3, -ry * 0.3, r * 0.1);
    }

    // ====================================================================
    //  PEACH
    // ====================================================================

    private static void drawPeach(Graphics2D g, double r) {
        shadow(g, r);

        Ellipse2D.Double body = new Ellipse2D.Double(-r, -r, r * 2, r * 2);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(-r * 0.15, -r * 0.20), (float) r,
            new float[] { 0f, 0.3f, 0.5f, 0.75f, 1f },
            new Color[] { rgb(0xffd9c0), rgb(0xffb89e), rgb(0xff9a76),
                          rgb(0xf47850), rgb(0xd4542a) }));
        g.fill(body);

        Shape oldClip = g.getClip();
        g.setClip(body);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(r * 0.35, -r * 0.1), (float) (r * 0.65),
            new float[] { 0f, 0.5f, 1f },
            new Color[] { new Color(220, 60, 30, 100),
                          new Color(220, 60, 30, 40),
                          new Color(220, 60, 30, 0) }));
        g.fill(new Ellipse2D.Double(-r * 1.2, -r * 1.2, r * 2.4, r * 2.4));
        g.setClip(oldClip);

        Path2D.Double crease = new Path2D.Double();
        crease.moveTo(r * 0.02, -r * 0.88);
        crease.curveTo(r * 0.12, -r * 0.3, r * 0.08, r * 0.3, r * 0.02, r * 0.88);
        g.setColor(new Color(180, 60, 25, 100));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(crease);

        ao(g, body, r, 50);
        inkRim(g, body, 2.5f);
        diffuseHighlight(g, -r * 0.32, -r * 0.38, r * 0.32, r * 0.22);
        specular(g, -r * 0.33, -r * 0.42, r * 0.16);

        Path2D.Double leaf = new Path2D.Double();
        leaf.moveTo(5, -r * 1.06);
        leaf.quadTo(r * 0.45, -r * 1.28, r * 0.38, -r * 0.95);
        leaf.quadTo(r * 0.20, -r * 1.12, 5, -r * 1.06);
        leaf.closePath();
        g.setColor(rgb(0x388e3c));
        g.fill(leaf);
        inkRim(g, leaf, 1.4f);
    }

    // ====================================================================
    //  HALVES (cut faces)
    // ====================================================================

    private static void drawHalfShared(Graphics2D g, double r, FruitType type, boolean isLeft) {
        double startDeg = isLeft ? 90 : -90;
        double extentDeg = 180;

        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Arc2D.Double(-r + 3, -r + 3, 2 * r, 2 * r,
                                startDeg, extentDeg, Arc2D.PIE));

        Arc2D.Double skinArc = new Arc2D.Double(-r, -r, 2 * r, 2 * r,
                                                 startDeg, extentDeg, Arc2D.PIE);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(-r * 0.2, -r * 0.2), (float) r,
            new float[] { 0f, 0.6f, 1f },
            new Color[] { type.highlight, type.color, type.shadow }));
        g.fill(skinArc);

        double fr = r * 0.85;
        Arc2D.Double flesh = new Arc2D.Double(-fr, -fr, 2 * fr, 2 * fr,
                                               startDeg, extentDeg, Arc2D.PIE);
        g.setPaint(new RadialGradientPaint(
            new Point2D.Double(0, 0), (float) fr,
            new float[] { 0f, 0.15f, 0.4f, 0.8f, 1f },
            new Color[] { new Color(255, 254, 245), new Color(255, 248, 232),
                          type.innerColor, type.innerColor, type.shadow }));
        g.fill(flesh);

        g.setColor(new Color(255, 255, 255, 110));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Arc2D.Double(-r * 0.88, -r * 0.88, r * 1.76, r * 1.76,
                                startDeg, extentDeg, Arc2D.OPEN));

        drawHalfInner(g, r, type, isLeft);

        g.setColor(new Color(0, 0, 0, 90));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Arc2D.Double(-r, -r, 2 * r, 2 * r, startDeg, extentDeg, Arc2D.OPEN));
    }

    private static void drawHalfInner(Graphics2D g, double r, FruitType type, boolean isLeft) {
        double sign = isLeft ? -1 : 1;
        double cx = sign * r * 0.05;

        switch (type) {
            case WATERMELON: {
                g.setColor(new Color(20, 20, 20));
                Random rng = new Random(5);
                double startAngle = isLeft ? Math.PI * 0.5 : -Math.PI * 0.5;
                double endAngle = isLeft ? Math.PI * 1.5 : Math.PI * 0.5;
                for (int i = 0; i < 8; i++) {
                    double sa = startAngle + (endAngle - startAngle) * (0.15 + 0.7 * (i / 8.0));
                    double sd = r * (0.35 + rng.nextDouble() * 0.25);
                    double sxc = Math.cos(sa) * sd;
                    double syc = Math.sin(sa) * sd;
                    java.awt.geom.AffineTransform saved = g.getTransform();
                    g.translate(sxc, syc);
                    g.rotate(sa + Math.PI / 2);
                    g.fill(new Ellipse2D.Double(-5, -2.5, 10, 5));
                    g.setTransform(saved);
                }
                break;
            }
            case KIWI: {
                g.setColor(new Color(255, 255, 240, 160));
                g.fill(new Ellipse2D.Double(cx - r * 0.12, -r * 0.12, r * 0.24, r * 0.24));
                g.setColor(new Color(255, 255, 230, 60));
                g.setStroke(new BasicStroke(1.5f));
                for (int i = 0; i < 10; i++) {
                    double a = (i / 10.0) * Math.PI * 2;
                    g.draw(new Line2D.Double(cx + Math.cos(a) * r * 0.1,
                                              Math.sin(a) * r * 0.1,
                                              cx + Math.cos(a) * r * 0.55,
                                              Math.sin(a) * r * 0.55));
                }
                Random rng = new Random(11);
                g.setColor(new Color(20, 20, 20));
                for (int i = 0; i < 18; i++) {
                    double a = (i / 18.0) * Math.PI * 2;
                    double sd = r * (0.35 + rng.nextDouble() * 0.15);
                    java.awt.geom.AffineTransform saved = g.getTransform();
                    g.translate(cx + Math.cos(a) * sd, Math.sin(a) * sd);
                    g.rotate(a);
                    g.fill(new Ellipse2D.Double(-2.5, -1.2, 5, 2.4));
                    g.setTransform(saved);
                }
                break;
            }
            case ORANGE:
            case LEMON: {
                Color seg = type == FruitType.ORANGE
                    ? new Color(255, 255, 255, 75)
                    : new Color(255, 255, 220, 90);
                g.setColor(seg);
                g.setStroke(new BasicStroke(1.5f));
                double ringR = type == FruitType.ORANGE ? r * 0.7 : r * 0.65;
                for (int i = 0; i < 8; i++) {
                    double a = (i / 8.0) * Math.PI * 2;
                    g.draw(new Line2D.Double(cx, 0,
                        cx + Math.cos(a) * ringR, Math.sin(a) * ringR));
                }
                break;
            }
            case APPLE: {
                g.setColor(new Color(200, 180, 140, 110));
                g.fill(new Ellipse2D.Double(cx - r * 0.08, -r * 0.2, r * 0.16, r * 0.4));
                g.setColor(rgb(0x3e2723));
                g.fill(new Ellipse2D.Double(cx - 3, (int) (-r * 0.08) - 5, 6, 10));
                g.fill(new Ellipse2D.Double(cx - 3, (int) ( r * 0.08) - 5, 6, 10));
                break;
            }
            default: break;
        }

        g.setColor(new Color(type.innerColor.getRed(),
                              type.innerColor.getGreen(),
                              type.innerColor.getBlue(), 180));
        g.fill(new Ellipse2D.Double(sign * 2 - 3, r * 0.35 - 5, 6, 10));
    }
}
