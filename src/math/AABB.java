package math;

/**
 * Axis-aligned bounding box (x, y, w, h). The overlap test is the De Morgan
 * derivation from Professor Murphy's February 10 lecture:
 *
 *   Two AABBs do NOT overlap iff one is entirely
 *     right of the other:   r2.x + r2.w < r1.x
 *     OR left of:           r1.x + r1.w < r2.x
 *     OR above:             r2.y + r2.h < r1.y
 *     OR below:             r1.y + r1.h < r2.y
 *
 *   Apply De Morgan to negate the OR chain (and flip < to >=) and you get
 *   the overlap test as a single AND of four inequalities.
 *
 * Used in this project for menu hit-testing and powerup pickup detection
 * (anywhere a quick rectangle-vs-point or rectangle-vs-rectangle check is
 * more natural than a circle-vs-line slice test).
 */
public class AABB {

    public double x, y, w, h;

    public AABB(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** Point-in-rect: the special case w = h = 0 of overlap. */
    public boolean contains(double px, double py) {
        return px >= x && px <= x + w
            && py >= y && py <= y + h;
    }

    /** De Morgan overlap test from the Feb 10 lecture. */
    public boolean overlaps(AABB other) {
        return x + w >= other.x
            && other.x + other.w >= x
            && y + h >= other.y
            && other.y + other.h >= y;
    }

    /**
     * Penetration depth on the X axis (positive means this box pushes the
     * other box to the right; negative pushes to the left). Used for the
     * push-out collision pattern from the March 5 lecture.
     */
    public double penetrationX(AABB other) {
        double left  = (x + w) - other.x;
        double right = (other.x + other.w) - x;
        return Math.abs(left) < Math.abs(right) ? -left : right;
    }

    public double penetrationY(AABB other) {
        double top    = (y + h) - other.y;
        double bottom = (other.y + other.h) - y;
        return Math.abs(top) < Math.abs(bottom) ? -top : bottom;
    }
}
