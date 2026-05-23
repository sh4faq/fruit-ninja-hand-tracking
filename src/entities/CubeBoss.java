package entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Random;

import math.Collision;
import math.Vector3;

/**
 * A rotating 3D cube boss enemy. The single-feature showcase of the entire
 * May curriculum from Professor Murphy:
 *
 *   April 28: building a Cube class with hard-coded vertex tables per face,
 *             winding order, the 3D perspective transform.
 *   May 5:    rotation around X / Y / Z axes with cached cos and sin.
 *   May 7:    back-face removal via the cross product of two edge vectors
 *             plus a dot product with the camera-to-surface vector.
 *   May 12:   directional lighting via the dot product of the unit normal
 *             with a fixed "sun" vector, plus ambient light.
 *
 * The boss takes BOSS_MAX_HITS slice hits before exploding. Each hit shaves
 * off some health; when health hits zero it counts as a kill and is removed
 * from the world.
 *
 * Vertex layout (counter-clockwise from outside looking in, so the cross
 * product points outward):
 *
 *           Y
 *           |
 *           |  4----5
 *           | /|   /|
 *           |/ |  / |
 *           7----6  |
 *           |  0-|--1
 *           | /  | /
 *           |/   |/
 *           3----2
 *                  X
 *  Z points OUT of the page.
 */
public class CubeBoss {

    private static final Random RNG = new Random();

    // Cube vertices in model space, centered on origin.
    private static final double S = 60;
    private static final double[][] VERTICES = {
        { -S, -S, -S },  // 0
        {  S, -S, -S },  // 1
        {  S, -S,  S },  // 2
        { -S, -S,  S },  // 3
        { -S,  S, -S },  // 4
        {  S,  S, -S },  // 5
        {  S,  S,  S },  // 6
        { -S,  S,  S },  // 7
    };

    // Each face lists vertex indices in CCW order from the OUTSIDE.
    private static final int[][] FACES = {
        { 0, 1, 2, 3 },  // bottom (Y = -S)
        { 4, 7, 6, 5 },  // top    (Y = +S)
        { 0, 4, 5, 1 },  // back   (Z = -S)
        { 2, 6, 7, 3 },  // front  (Z = +S)
        { 0, 3, 7, 4 },  // left   (X = -S)
        { 1, 5, 6, 2 },  // right  (X = +S)
    };

    // Per-face base colors (matches face order above).
    private static final Color[] BASE_COLOR = {
        new Color(170,  60,  60),  // bottom: red
        new Color( 70, 170,  90),  // top:    green
        new Color( 80,  90, 200),  // back:   blue
        new Color(220, 180,  50),  // front:  gold
        new Color(200, 100, 200),  // left:   pink
        new Color( 60, 200, 200),  // right:  cyan
    };

    // Directional "sun" light direction (must be a unit vector).
    private static final double LIGHT_X, LIGHT_Y, LIGHT_Z;
    private static final double AMBIENT = 0.30;
    static {
        double lx = -0.4, ly = -0.6, lz = 0.7;
        double mag = Math.sqrt(lx * lx + ly * ly + lz * lz);
        LIGHT_X = lx / mag;
        LIGHT_Y = ly / mag;
        LIGHT_Z = lz / mag;
    }

    public double x, y, z, vx, vy, vz;
    public double angleX, angleY, angleZ;

    public static final int BOSS_MAX_HITS = 4;
    public int health = BOSS_MAX_HITS;
    public boolean isSliced;

    public CubeBoss(int panelWidth, int panelHeight) {
        // Spawn from the top, drift slowly through the middle area.
        this.x = panelWidth * 0.5 + RNG.nextDouble() * 80 - 40;
        this.y = -120;
        this.z = 60 + RNG.nextDouble() * 60;
        this.vx = -0.4 + RNG.nextDouble() * 0.8;
        this.vy = 1.2 + RNG.nextDouble() * 0.4;
        this.vz = -0.2 + RNG.nextDouble() * 0.2;
        this.angleX = RNG.nextDouble() * 360;
        this.angleY = RNG.nextDouble() * 360;
        this.angleZ = RNG.nextDouble() * 360;
    }

    public double depthScale() {
        return Vector3.FOCAL_LENGTH / (Vector3.FOCAL_LENGTH + z);
    }

    public void update(double dtScale, double gravity) {
        vy += gravity * 0.25 * dtScale;     // lighter gravity than fruit
        x += vx * dtScale;
        y += vy * dtScale;
        z += vz * dtScale;
        // Tumble: rotate around all three axes at different rates.
        angleX += 1.2 * dtScale;
        angleY += 0.9 * dtScale;
        angleZ += 0.6 * dtScale;
    }

    public boolean isOffScreen(int w, int h) {
        return y > h + 200 || x < -200 || x > w + 200 || z > 600 || z < -200;
    }

    /**
     * Slice test against a 2D screen-space radius approximating the cube's
     * footprint. We use the depth-scaled half-diagonal as a generous hit
     * radius so any slice through the visible silhouette counts.
     */
    public boolean checkSlice(double x1, double y1, double x2, double y2) {
        if (isSliced) return false;
        double hitR = S * 1.4 * depthScale();
        return Collision.segmentIntersectsCircle(x1, y1, x2, y2, x, y, hitR);
    }

    /** Records a hit; returns true if the boss is now defeated. */
    public boolean registerHit() {
        health--;
        return health <= 0;
    }

    public void draw(Graphics2D g, int originX, int originY) {
        // Pre-compute trig for the three rotations once per frame.
        double cx = Math.cos(Math.toRadians(angleX));
        double sx = Math.sin(Math.toRadians(angleX));
        double cy = Math.cos(Math.toRadians(angleY));
        double sy = Math.sin(Math.toRadians(angleY));
        double cz = Math.cos(Math.toRadians(angleZ));
        double sz = Math.sin(Math.toRadians(angleZ));

        // Rotate every vertex (model -> world space).
        double[][] worldVerts = new double[VERTICES.length][3];
        for (int i = 0; i < VERTICES.length; i++) {
            double vx = VERTICES[i][0];
            double vy = VERTICES[i][1];
            double vz = VERTICES[i][2];

            // Rotate around X (y/z plane)
            double y1 = vy * cx - vz * sx;
            double z1 = vy * sx + vz * cx;
            // Rotate around Y (x/z plane)
            double x2 = vx * cy + z1 * sy;
            double z2 = -vx * sy + z1 * cy;
            // Rotate around Z (x/y plane)
            double x3 = x2 * cz - y1 * sz;
            double y3 = x2 * sz + y1 * cz;

            worldVerts[i][0] = x3 + x;
            worldVerts[i][1] = y3 + y;
            worldVerts[i][2] = z2 + z;
        }

        // Sort faces by average z (descending) so the painter's algorithm
        // draws far faces before near ones.
        Integer[] faceOrder = { 0, 1, 2, 3, 4, 5 };
        java.util.Arrays.sort(faceOrder, (a, b) -> {
            double za = avgZ(worldVerts, FACES[a]);
            double zb = avgZ(worldVerts, FACES[b]);
            return Double.compare(zb, za);
        });

        // Camera sits at the origin looking down +Z. Project each face if
        // the back-face test passes.
        for (int faceIdx : faceOrder) {
            int[] face = FACES[faceIdx];
            // Compute two edge vectors and the surface normal via the cross
            // product. (Murphy's May 7 lesson.)
            double[] v0 = worldVerts[face[0]];
            double[] v1 = worldVerts[face[1]];
            double[] v2 = worldVerts[face[2]];
            double e1x = v1[0] - v0[0];
            double e1y = v1[1] - v0[1];
            double e1z = v1[2] - v0[2];
            double e2x = v2[0] - v0[0];
            double e2y = v2[1] - v0[1];
            double e2z = v2[2] - v0[2];
            double nx = e1y * e2z - e1z * e2y;
            double ny = e1z * e2x - e1x * e2z;
            double nz = e1x * e2y - e1y * e2x;

            // Camera-to-surface vector (camera at origin so this is just v0).
            // Back-face removal: only draw when the dot product is negative
            // (the surface faces toward the camera). Equivalent to "dot > 0
            // when the camera-to-surface points INTO the surface" in his
            // class; we use the opposite convention because our normal is
            // computed from the outward winding.
            double dotView = nx * v0[0] + ny * v0[1] + nz * v0[2];
            if (dotView >= 0) continue;

            // Normalize the normal for lighting.
            double nmag = Math.sqrt(nx * nx + ny * ny + nz * nz);
            double unx = nx / nmag, uny = ny / nmag, unz = nz / nmag;

            // Lambert diffuse: brightness = max(0, N . L). Plus ambient.
            double diffuse = -(unx * LIGHT_X + uny * LIGHT_Y + unz * LIGHT_Z);
            if (diffuse < 0) diffuse = 0;
            double brightness = Math.min(1.0, AMBIENT + (1.0 - AMBIENT) * diffuse);

            // Apply brightness to the base color.
            Color base = BASE_COLOR[faceIdx];
            int r = (int) (base.getRed()   * brightness);
            int gr = (int) (base.getGreen() * brightness);
            int b = (int) (base.getBlue()  * brightness);
            Color shaded = new Color(clamp(r), clamp(gr), clamp(b));

            // Project the four vertices to screen space.
            int[] xp = new int[4];
            int[] yp = new int[4];
            for (int k = 0; k < 4; k++) {
                double[] v = worldVerts[face[k]];
                double zEye = v[2] + Vector3.FOCAL_LENGTH * 0.6; // pull camera back a bit
                if (zEye < 1) zEye = 1;
                xp[k] = (int) (Vector3.FOCAL_LENGTH * v[0] / zEye) + originX;
                yp[k] = (int) (Vector3.FOCAL_LENGTH * v[1] / zEye) + originY;
            }

            Polygon poly = new Polygon(xp, yp, 4);
            g.setColor(shaded);
            g.fillPolygon(poly);
            g.setColor(new Color(0, 0, 0, 140));
            g.setStroke(new BasicStroke(2f));
            g.drawPolygon(poly);
        }

        // Health pip ring above the cube
        int pipR = 5;
        int pipY = (int) (y - S - 28);
        int totalW = BOSS_MAX_HITS * (pipR * 2 + 4);
        int startX = (int) (x - totalW / 2);
        for (int i = 0; i < BOSS_MAX_HITS; i++) {
            int px = startX + i * (pipR * 2 + 4);
            g.setColor(i < health ? new Color(245, 200, 66) : new Color(80, 60, 30, 160));
            g.fillOval(px, pipY, pipR * 2, pipR * 2);
        }
    }

    private static double avgZ(double[][] worldVerts, int[] face) {
        double s = 0;
        for (int idx : face) s += worldVerts[idx][2];
        return s / face.length;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
