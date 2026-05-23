package entities;

import java.awt.Color;

/**
 * Fruit catalog. Each entry describes a fruit's display name, point value,
 * and the colors used by FruitRenderer when drawing it.
 *
 * Mirrors the FRUIT_TYPES table from the JavaScript prototype so the gameplay
 * feel stays identical between the two versions.
 */
public enum FruitType {
    APPLE     ("apple",      1, c(0xe74c3c), c(0xfadbd8), c(0xff6b6b), c(0xc0392b)),
    ORANGE    ("orange",     1, c(0xf39c12), c(0xfdebd0), c(0xf5b041), c(0xd68910)),
    WATERMELON("watermelon", 3, c(0x27ae60), c(0xff6b6b), c(0x2ecc71), c(0x1e8449)),
    COCONUT   ("coconut",    2, c(0x8b5a2b), c(0xfdfefe), c(0xa0522d), c(0x5d4037)),
    LEMON     ("lemon",      1, c(0xf1c40f), c(0xfcf3cf), c(0xf4d03f), c(0xd4ac0d)),
    GRAPE     ("grape",      1, c(0x8e44ad), c(0xe8daef), c(0x9b59b6), c(0x6c3483)),
    KIWI      ("kiwi",       2, c(0x795548), c(0xa5d610), c(0x8d6e63), c(0x5d4037)),
    PEACH     ("peach",      1, c(0xffab91), c(0xfff3e0), c(0xffccbc), c(0xe64a19));

    public final String name;
    public final int points;
    public final Color color;
    public final Color innerColor;
    public final Color highlight;
    public final Color shadow;

    FruitType(String name, int points, Color color, Color innerColor,
              Color highlight, Color shadow) {
        this.name = name;
        this.points = points;
        this.color = color;
        this.innerColor = innerColor;
        this.highlight = highlight;
        this.shadow = shadow;
    }

    private static Color c(int rgb) { return new Color(rgb); }

    public static FruitType random() {
        FruitType[] all = values();
        return all[(int) (Math.random() * all.length)];
    }
}
