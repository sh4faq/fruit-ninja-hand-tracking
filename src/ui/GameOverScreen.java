package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

import game.GameMode;

/**
 * Game over splash. Shows the player's score and the best score for the mode.
 *
 * Best score is read from the Preferences API by the game panel and passed in
 * via show(). The screen has no logic of its own.
 */
public class GameOverScreen {

    private GameMode mode = GameMode.CLASSIC;
    private int score;
    private int bestScore;
    private int level;

    public void show(GameMode mode, int score, int level) {
        this.mode = mode;
        this.score = score;
        this.level = level;
    }

    public void setBestScore(int best) { this.bestScore = best; }

    public void draw(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 230),
                                     0, h, new Color(20, 12, 5, 240)));
        g.fillRect(0, 0, w, h);

        // Title
        g.setFont(FontLoader.title(Font.BOLD, 72));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String title = "GAME OVER";
        int tw = fm.stringWidth(title);
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(title, (w - tw) / 2 + 4, h / 2 - 100 + 4);
        g.setColor(new Color(231, 76, 60));
        g.drawString(title, (w - tw) / 2, h / 2 - 100);

        // Mode label
        g.setFont(FontLoader.title(Font.BOLD, 18));
        String modeLabel = mode.displayName + " MODE";
        int mw = g.getFontMetrics().stringWidth(modeLabel);
        g.setColor(new Color(200, 170, 130, 200));
        g.drawString(modeLabel, (w - mw) / 2, h / 2 - 60);

        // Score box
        drawScoreRow(g, w, h / 2 - 20, "SCORE", String.valueOf(score));
        drawScoreRow(g, w, h / 2 + 36, "BEST",  String.valueOf(bestScore));
        if (mode.hasLevels) {
            drawScoreRow(g, w, h / 2 + 92, "LEVEL", String.valueOf(level));
        }

        // Hint
        g.setColor(new Color(200, 170, 130, 200));
        g.setFont(FontLoader.body(Font.PLAIN, 16));
        String hint = "ENTER to play again, ESC for menu";
        int hw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 80);
    }

    private void drawScoreRow(Graphics2D g, int w, int y, String label, String value) {
        int boxW = 320;
        int boxH = 44;
        int x = (w - boxW) / 2;

        g.setColor(new Color(0, 0, 0, 130));
        g.fill(new RoundRectangle2D.Double(x, y, boxW, boxH, 10, 10));

        g.setFont(FontLoader.title(Font.BOLD, 18));
        g.setColor(new Color(200, 170, 130, 230));
        g.drawString(label, x + 20, y + 30);

        g.setFont(FontLoader.title(Font.BOLD, 30));
        g.setColor(Color.WHITE);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int vw = fm.stringWidth(value);
        g.drawString(value, x + boxW - 20 - vw, y + 34);
    }
}
