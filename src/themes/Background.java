package themes;

import java.awt.Graphics2D;

/**
 * One purchasable background skin. Drives both the in-game backdrop and the
 * menu backdrop while equipped.
 *
 * Implementations may use bitmap assets (loaded from assets/images/) or
 * pure procedural drawing. Each implementation owns its own state so the
 * BackgroundCatalog can keep a single instance per id and re-use it.
 */
public interface Background {

    /** Stable unique identifier for persistence (e.g. "bg_dojo_dusk"). */
    String id();

    /** Display name in the shop. */
    String name();

    /** Coin cost. Default background is 0. */
    int price();

    /** One-line tagline shown on the shop card. */
    String tagline();

    /** Primary accent color shown in shop preview. */
    java.awt.Color previewAccent();

    /** Per-frame state advance. May drive parallax, particles, etc. */
    void update(double deltaMs);

    /** Fills the entire (0,0,w,h) viewport. */
    void draw(Graphics2D g, int w, int h);

    /** Draws a small thumbnail into the shop card rectangle. */
    void drawPreview(Graphics2D g, int x, int y, int w, int h);
}
