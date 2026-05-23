package entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 2D polygon model exactly as Professor Murphy built it on April 14, 16, and
 * 21. The model is a collection of polygons (each polygon is a closed list of
 * (x, y) vertices in model space). The class loads its geometry from a plain
 * text file, caches cos and sin of the current angle so trig is only
 * recomputed when the model turns, and applies the 2D rotation formula
 * derived from the sum-of-angles identities:
 *
 *     X' = X cos A - Y sin A
 *     Y' = X sin A + Y cos A
 *
 * Each vertex is re-rotated every frame from the ORIGINAL model coordinates
 * (per his April 16 rule: never mutate the source data, always filter it
 * through the current transform).
 *
 * The .model2D file format mirrors the layout from his class:
 *
 *     polyCount
 *
 *     X row 0 comma-separated
 *     X row 1 comma-separated
 *     ...
 *
 *     Y row 0 comma-separated
 *     Y row 1 comma-separated
 *
 * Both rows for the same polygon must have the same number of vertices.
 */
public class PolygonModel2D {

    private int[][] xCoords;
    private int[][] yCoords;

    public double x;
    public double y;
    public double angleDeg;
    private double cosA = 1;
    private double sinA = 0;

    public Color outlineColor = new Color(255, 238, 221);
    public Color fillColor    = null;     // null = wireframe only
    public float strokeWidth  = 2f;

    public PolygonModel2D(double x, double y, double angleDeg) {
        this.x = x;
        this.y = y;
        setAngle(angleDeg);
    }

    public void setAngle(double degrees) {
        this.angleDeg = degrees;
        double r = Math.PI * degrees / 180.0;
        this.cosA = Math.cos(r);
        this.sinA = Math.sin(r);
    }

    public void turnLeft(double deltaDegrees)  { setAngle(angleDeg - deltaDegrees); }
    public void turnRight(double deltaDegrees) { setAngle(angleDeg + deltaDegrees); }

    /** Move forward in the direction the model is facing (April 21 lecture). */
    public void forward(double d)  { x += cosA * d; y += sinA * d; }
    public void backward(double d) { x -= cosA * d; y -= sinA * d; }

    /**
     * Loads a .model2D file. File format:
     *   polyCount
     *   (blank)
     *   X row 0
     *   X row 1
     *   ...
     *   (blank)
     *   Y row 0
     *   Y row 1
     *   ...
     */
    public void load(String filename) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(new File(filename)))) {
            int polyCount = Integer.parseInt(in.readLine().trim());
            xCoords = new int[polyCount][];
            yCoords = new int[polyCount][];

            skipBlankLines(in);
            for (int p = 0; p < polyCount; p++) {
                xCoords[p] = parseRow(in.readLine());
            }
            skipBlankLines(in);
            for (int p = 0; p < polyCount; p++) {
                yCoords[p] = parseRow(in.readLine());
            }
        }
    }

    /** Build the model from in-memory arrays (handy when no file is needed). */
    public void setModel(int[][] xCoords, int[][] yCoords) {
        this.xCoords = xCoords;
        this.yCoords = yCoords;
    }

    private static int[] parseRow(String line) {
        // Trim every token after split, per the April 16 debugging lesson
        // (NumberFormatException came from stray spaces).
        String[] parts = line.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Integer.parseInt(parts[i].trim());
        }
        return out;
    }

    private static void skipBlankLines(BufferedReader in) throws IOException {
        in.mark(8192);
        String line;
        while ((line = in.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                in.reset();
                return;
            }
            in.mark(8192);
        }
    }

    public void draw(Graphics2D g) {
        if (xCoords == null) return;

        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int poly = 0; poly < xCoords.length; poly++) {
            int n = xCoords[poly].length;
            int[] xp = new int[n];
            int[] yp = new int[n];
            for (int v = 0; v < n; v++) {
                double mx = xCoords[poly][v];
                double my = yCoords[poly][v];
                // 2D rotation formula (derived from sum-of-angles, April 14)
                xp[v] = (int) (mx * cosA - my * sinA + x);
                yp[v] = (int) (mx * sinA + my * cosA + y);
            }
            if (fillColor != null) {
                g.setColor(fillColor);
                g.fillPolygon(xp, yp, n);
            }
            g.setColor(outlineColor);
            g.drawPolygon(xp, yp, n);
        }
    }

    public int polygonCount() {
        return xCoords == null ? 0 : xCoords.length;
    }

    /** Returns a list-style view of all vertices for any external math. */
    public List<int[]> allVertices() {
        List<int[]> out = new ArrayList<>();
        if (xCoords == null) return out;
        for (int p = 0; p < xCoords.length; p++) {
            for (int v = 0; v < xCoords[p].length; v++) {
                out.add(new int[] { xCoords[p][v], yCoords[p][v] });
            }
        }
        return out;
    }
}
