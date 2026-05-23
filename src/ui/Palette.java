package ui;

import java.awt.Color;

/**
 * Named colors used across the game. Centralizing them here means we can
 * retune the whole dojo theme by editing one file instead of hunting for
 * scattered RGB literals.
 */
public final class Palette {

    private Palette() {}

    // Backgrounds
    public static final Color INK       = new Color(10, 8, 6);
    public static final Color INK_DEEP  = new Color(5, 3, 2);
    public static final Color DOJO      = new Color(26, 18, 10);
    public static final Color DOJO_DARK = new Color(20, 12, 5);

    // Accents
    public static final Color GOLD      = new Color(245, 200, 66);
    public static final Color GOLD_DARK = new Color(184, 134, 11);
    public static final Color BLOOD     = new Color(231, 76, 60);
    public static final Color BLOOD_DARK= new Color(169, 50, 38);
    public static final Color BAMBOO    = new Color(109, 186, 94);
    public static final Color JADE      = new Color(46, 139, 87);
    public static final Color PLUM      = new Color(138, 94, 207);
    public static final Color SAKURA    = new Color(255, 200, 215);

    // Text
    public static final Color CREAM         = new Color(255, 238, 221);
    public static final Color CREAM_DIM     = new Color(200, 170, 130, 200);
    public static final Color CREAM_DIMMER  = new Color(180, 150, 110, 160);

    // Blade
    public static final Color BLADE_CORE = new Color(255, 255, 255, 230);
    public static final Color BLADE_MID  = new Color(180, 210, 255, 130);
    public static final Color BLADE_GLOW = new Color(120, 180, 255, 60);

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
