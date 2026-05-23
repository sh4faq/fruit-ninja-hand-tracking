package input;

/**
 * A short directed line segment representing the motion of the slicing pointer
 * (either the mouse or a tracked fingertip) between two consecutive frames.
 *
 * The collision system treats each slice as a line segment versus a circle
 * (the fruit), and the half-fruit launch direction is taken from this vector.
 */
public class SliceLine {
    public final double x1, y1, x2, y2;

    public SliceLine(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double length() {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double dx() { return x2 - x1; }
    public double dy() { return y2 - y1; }
}
