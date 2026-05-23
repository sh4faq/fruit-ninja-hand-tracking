package effects;

import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import economy.Inventory;
import weapons.Sword;
import weapons.SwordCatalog;
import weapons.TrailPoint;

/**
 * Player's slice trail point buffer. The actual rendering style comes from
 * the currently-equipped {@link Sword}, looked up via
 * {@link SwordCatalog#get(String)} on every draw.
 *
 * Stores the last N points of the pointer's path and ages each one out.
 * When draw() is called, the live points are handed to the equipped sword
 * so the per-skin trail style (color, glow width, particles) takes over.
 */
public class SliceTrail {

    private static final int MAX_POINTS = 12;

    private final Deque<TrailPoint> points = new ArrayDeque<>();

    public void addPoint(double x, double y) {
        points.addFirst(new TrailPoint(x, y));
        while (points.size() > MAX_POINTS) points.removeLast();
    }

    public void update(double dtScale) {
        for (TrailPoint p : points) p.life -= 0.1 * dtScale;
        points.removeIf(p -> p.life <= 0);
    }

    public void clear() { points.clear(); }

    public List<TrailPoint> snapshot() {
        return new ArrayList<>(points);
    }

    public void draw(Graphics2D g) {
        if (points.size() < 2) return;
        Sword sword = SwordCatalog.get(Inventory.equippedSword());
        if (sword == null) return;
        sword.drawTrail(g, snapshot());
    }
}
