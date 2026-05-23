package entities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tile map exactly as Professor Murphy built it on March 12 and refactored on
 * March 17. The map is a row-by-row array of strings; each character indexes
 * into a tile image array using the ASCII trick:
 *
 *     image = tile[c - 'A'];
 *
 * Map files live on disk and are loaded at runtime so non-programmers can
 * edit levels in a text editor ("mods are exactly this", as he put it).
 *
 * File format:
 *
 *     # optional comment lines starting with #
 *     tileWidth tileHeight
 *     row1
 *     row2
 *     ...
 *
 * '.' renders no tile (blank). Any other letter from A onward indexes the
 * supplied tile array. If the index is out of range the tile is skipped.
 */
public class TileMap {

    private int tileW;
    private int tileH;
    private String[] map;
    private BufferedImage[] tile;

    public TileMap(BufferedImage[] tile) {
        this.tile = tile;
    }

    public void load(String filename) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(new File(filename)))) {
            List<String> rows = new ArrayList<>();
            String line;
            boolean firstDataLine = true;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                if (firstDataLine) {
                    String[] dims = line.trim().split("\\s+");
                    tileW = Integer.parseInt(dims[0]);
                    tileH = Integer.parseInt(dims[1]);
                    firstDataLine = false;
                } else {
                    rows.add(line);
                }
            }
            this.map = rows.toArray(new String[0]);
        }
    }

    public int width()  { return map == null ? 0 : (map[0].length() * tileW); }
    public int height() { return map == null ? 0 : (map.length * tileH); }
    public int tileWidth()  { return tileW; }
    public int tileHeight() { return tileH; }

    public void draw(Graphics2D g, int offsetX, int offsetY) {
        if (map == null || tile == null) return;

        for (int row = 0; row < map.length; row++) {
            String line = map[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                if (c == '.' || c == ' ') continue;
                int idx = c - 'A';
                if (idx < 0 || idx >= tile.length || tile[idx] == null) continue;
                int x = col * tileW + offsetX;
                int y = row * tileH + offsetY;
                g.drawImage(tile[idx], x, y, tileW, tileH, null);
            }
        }
    }

    // ====================================================================
    //  PROCEDURAL TILE GENERATOR
    // ====================================================================
    //
    // Because we don't ship PNG tile assets, we generate the tile palette on
    // the fly. Each tile is a small BufferedImage drawn with Graphics2D.
    // Tile A = wooden dojo floor, B = paper screen, C = stone wall,
    // D = paper-lantern.

    public static BufferedImage[] buildDojoTiles(int size) {
        return new BufferedImage[] {
            buildFloor(size),
            buildScreen(size),
            buildStone(size),
            buildLantern(size),
        };
    }

    private static BufferedImage buildFloor(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(70, 45, 22));
        g.fillRect(0, 0, s, s);
        // Plank texture
        g.setColor(new Color(55, 35, 18));
        for (int y = 0; y < s; y += s / 4) {
            g.drawLine(0, y, s, y);
        }
        g.setColor(new Color(40, 25, 14, 160));
        g.drawRect(0, 0, s - 1, s - 1);
        g.dispose();
        return img;
    }

    private static BufferedImage buildScreen(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(230, 215, 180));
        g.fillRect(0, 0, s, s);
        // Shoji-style grid
        g.setColor(new Color(60, 40, 25));
        int cells = 4;
        for (int i = 0; i <= cells; i++) {
            int p = i * (s / cells);
            g.drawLine(p, 0, p, s);
            g.drawLine(0, p, s, p);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage buildStone(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 80, 78));
        g.fillRect(0, 0, s, s);
        // Brick pattern
        g.setColor(new Color(55, 55, 52));
        g.drawLine(0, s / 2, s, s / 2);
        g.drawLine(s / 2, 0, s / 2, s / 2);
        g.drawLine(s / 4, s / 2, s / 4, s);
        g.drawLine(3 * s / 4, s / 2, 3 * s / 4, s);
        g.dispose();
        return img;
    }

    private static BufferedImage buildLantern(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Transparent backdrop
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, s, s);
        // Lantern body
        g.setColor(new Color(230, 90, 60));
        g.fillOval(s / 6, s / 6, 2 * s / 3, 2 * s / 3);
        // Inner glow
        g.setColor(new Color(255, 220, 120, 180));
        g.fillOval(s / 4, s / 4, s / 2, s / 2);
        // Top + bottom caps
        g.setColor(new Color(60, 40, 25));
        g.fillRect(s / 3, 0, s / 3, s / 8);
        g.fillRect(s / 3, 7 * s / 8, s / 3, s / 8);
        g.dispose();
        return img;
    }
}
