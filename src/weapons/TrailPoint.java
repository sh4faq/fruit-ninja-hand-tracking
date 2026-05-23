package weapons;

/**
 * One sample on the player's slice trail. The active Sword reads a list of
 * these each frame and decides how to render them.
 *
 * <ul>
 *   <li>{@code x, y}: screen position</li>
 *   <li>{@code life}: 1.0 when freshly added, fades to 0 over time</li>
 *   <li>{@code timeMs}: wall-clock timestamp when the point was added; lets
 *       swords compute their own per-point age for animation</li>
 * </ul>
 */
public class TrailPoint {

    public final double x;
    public final double y;
    public double life;
    public final long timeMs;

    public TrailPoint(double x, double y) {
        this(x, y, 1.0, System.currentTimeMillis());
    }

    public TrailPoint(double x, double y, double life, long timeMs) {
        this.x = x;
        this.y = y;
        this.life = life;
        this.timeMs = timeMs;
    }
}
