package weapons;

import java.awt.Graphics2D;
import java.util.List;

/**
 * One purchasable sword skin. Each sword controls how the player's slice
 * trail (and any extra flourish particles) are rendered.
 *
 * Implementations are stateless: the catalog returns the same Sword instance
 * for repeated lookups, and any per-frame state lives on the trail points
 * passed in.
 */
public interface Sword {

    /** Stable unique identifier used for persistence ("sword_001" etc). */
    String id();

    /** Display name in the shop and equipped-info UI. */
    String name();

    /** Coin cost. Starter sword is 0. */
    int price();

    /** Short one-line tagline shown in the shop card. */
    String tagline();

    /** Primary accent color shown in shop preview. */
    java.awt.Color previewAccent();

    /** Render the slice trail along the given point sequence. */
    void drawTrail(Graphics2D g, List<TrailPoint> points);

    /** A simple shop-card preview: draws a sample blade slash at (x, y). */
    void drawPreview(Graphics2D g, int x, int y, int w, int h);
}
