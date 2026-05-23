package math;

/**
 * 2D line segment with pre-computed unit normal and "C" constant for fast
 * signed shortest distance queries.
 *
 * Math, exactly as Professor Murphy derived it in the April 23 and April 30
 * lectures:
 *
 *   Given a line segment from A = (Ax, Ay) to B = (Bx, By):
 *     V = (Bx - Ax, By - Ay)            direction along the line
 *     N = (-Vy, Vx) / |V|                unit normal (90 degree CCW of V)
 *     C = -(Nx*Ax + Ny*Ay)               pre-computed constant
 *
 *   For any point P, the signed shortest distance from P to the infinite line
 *   through A and B is:
 *
 *     D = Nx*Px + Ny*Py + C  =  N . P - N . A
 *
 *   Positive D means P sits on the "outside" (normal-pointing) side, negative
 *   means the opposite side, and zero means on the line.
 *
 * Pre-computing N and C means each per-frame distance check is just a dot
 * product plus an add: 4 multiplies, 2 adds. The derivation uses
 * |u||v|cos(theta) = u . v with |N| = 1.
 *
 * The vector |V| is also kept around so we can clamp distance queries to the
 * segment (not just the infinite line) when we want segment-vs-point.
 */
public class Line {

    public final double ax, ay;
    public final double bx, by;
    public final double vx, vy;
    public final double length;
    public final double nx, ny;   // unit normal
    public final double c;        // pre-computed -(nx*ax + ny*ay)

    public Line(double ax, double ay, double bx, double by) {
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
        this.vx = bx - ax;
        this.vy = by - ay;
        this.length = Math.sqrt(vx * vx + vy * vy);
        if (length > 1e-9) {
            this.nx = -vy / length;
            this.ny =  vx / length;
        } else {
            this.nx = 0;
            this.ny = 0;
        }
        this.c = -(nx * ax + ny * ay);
    }

    /**
     * Signed shortest distance from the point (px, py) to the infinite line
     * through A and B. Sign indicates which side of the line the point sits on.
     */
    public double distanceTo(double px, double py) {
        return nx * px + ny * py + c;
    }

    /**
     * Absolute distance from the point (px, py) to the line segment (clamped
     * so endpoints behave correctly).
     */
    public double segmentDistanceTo(double px, double py) {
        if (length < 1e-9) {
            double dx = px - ax, dy = py - ay;
            return Math.sqrt(dx * dx + dy * dy);
        }
        double t = ((px - ax) * vx + (py - ay) * vy) / (length * length);
        t = Math.max(0, Math.min(1, t));
        double cx = ax + t * vx;
        double cy = ay + t * vy;
        double dx = px - cx, dy = py - cy;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
