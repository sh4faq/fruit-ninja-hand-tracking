package effects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns every visual effect on screen.
 *
 * The game panel calls into this with deltaMs per frame, and the manager
 * advances all active effects together. Effects that finish (life <= 0)
 * are dropped automatically.
 *
 * One SliceTrail per finger is kept so that the 5-finger blade mode can show
 * five independent ribbons. For the mouse / single-finger case we just use
 * index 1.
 */
public class EffectsManager {

    private static final int TRAIL_COUNT = 5;

    private final List<JuiceSplatter> splatters = new ArrayList<>();
    private final List<ScorePopup> popups = new ArrayList<>();
    private final List<ComboBanner> banners = new ArrayList<>();
    private final SliceTrail[] trails = new SliceTrail[TRAIL_COUNT];

    public EffectsManager() {
        for (int i = 0; i < TRAIL_COUNT; i++) trails[i] = new SliceTrail();
    }

    public void addSplatter(double x, double y, Color color) {
        if (splatters.size() >= 6) splatters.remove(0);
        splatters.add(new JuiceSplatter(x, y, color));
    }

    public void addScorePopup(double x, double y, int score, boolean isCombo) {
        if (popups.size() >= 8) popups.remove(0);
        popups.add(new ScorePopup(x, y, score, isCombo));
    }

    /**
     * Adds a screen-centered multi-slice combo banner. Drawn separately from
     * the world-space effects via {@link #drawOverlay(Graphics2D, int, int)}
     * so it sits at the same position regardless of camera shake.
     */
    public void addComboBanner(String text, int bonus) {
        if (banners.size() >= 3) banners.remove(0);
        banners.add(new ComboBanner(text, bonus));
    }

    public void addTrailPoint(int fingerIndex, double x, double y) {
        if (fingerIndex < 0 || fingerIndex >= TRAIL_COUNT) return;
        trails[fingerIndex].addPoint(x, y);
    }

    public void clearTrails() {
        for (SliceTrail t : trails) t.clear();
    }

    public void update(double deltaMs) {
        double dtScale = deltaMs / 16.67;

        for (int i = splatters.size() - 1; i >= 0; i--) {
            JuiceSplatter s = splatters.get(i);
            s.update(dtScale);
            if (s.isDead()) splatters.remove(i);
        }
        for (int i = popups.size() - 1; i >= 0; i--) {
            ScorePopup p = popups.get(i);
            p.update(dtScale);
            if (p.isDead()) popups.remove(i);
        }
        for (int i = banners.size() - 1; i >= 0; i--) {
            ComboBanner b = banners.get(i);
            b.update(dtScale);
            if (b.isDead()) banners.remove(i);
        }
        for (SliceTrail t : trails) t.update(dtScale);
    }

    public void draw(Graphics2D g) {
        for (SliceTrail t : trails) t.draw(g);
        for (JuiceSplatter s : splatters) s.draw(g);
        for (ScorePopup p : popups) p.draw(g);
    }

    /** Draws screen-space overlays such as the multi-slice combo banner. */
    public void drawOverlay(Graphics2D g, int w, int h) {
        for (ComboBanner b : banners) b.draw(g, w, h);
    }
}
