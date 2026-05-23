package ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * Loads custom TrueType fonts from assets/fonts/ and registers them with the
 * AWT graphics environment so they are available to the rest of the game.
 *
 * If a font file is missing we fall back to the closest built-in equivalent
 * so the game still runs cleanly.
 *
 * Drop the .ttf files into assets/fonts/ to enable the custom look:
 *   - Bangers-Regular.ttf       (comic impact, used for titles)
 *   - RussoOne-Regular.ttf      (clean bold, used for HUD numbers)
 *
 * Both fonts are free on Google Fonts.
 */
public final class FontLoader {

    private FontLoader() {}

    public static final String TITLE_FAMILY;
    public static final String HUD_FAMILY;
    public static final String BODY_FAMILY = "SansSerif";

    static {
        TITLE_FAMILY = tryLoad("assets/fonts/Bangers-Regular.ttf", "Arial Black");
        HUD_FAMILY   = tryLoad("assets/fonts/RussoOne-Regular.ttf", "Arial Black");
    }

    public static Font title(int style, int size) {
        return new Font(TITLE_FAMILY, style, size);
    }

    public static Font hud(int style, int size) {
        return new Font(HUD_FAMILY, style, size);
    }

    public static Font body(int style, int size) {
        return new Font(BODY_FAMILY, style, size);
    }

    private static String tryLoad(String path, String fallback) {
        try {
            File f = new File(path);
            if (!f.exists()) return fallback;
            Font font = Font.createFont(Font.TRUETYPE_FONT, f);
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .registerFont(font);
            return font.getFamily();
        } catch (Exception e) {
            return fallback;
        }
    }
}
