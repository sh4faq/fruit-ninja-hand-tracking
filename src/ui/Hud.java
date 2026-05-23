package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import game.GameMode;

/**
 * Ink-samurai HUD. No rounded-rectangle pills, no glossy gradients, no
 * cluttered widgets. Just:
 *   - HUGE white score number top-left with a thin red underline
 *   - mode label set tiny beneath it
 *   - timer as a thin red bar along the top edge (no box, no number)
 *   - lives as red X marks top-right
 *   - combo as a red word stamped across the middle when active
 *   - active powerups as a small bottom-center tag row
 *
 * The HUD has no state of its own; the panel hands it the current values and
 * it just paints.
 */
public class Hud {

    public void draw(Graphics2D g, int w, int h, GameMode mode,
                     int score, int combo, int lives, int level,
                     double timeRemainingSec,
                     boolean freezeOn, boolean frenzyOn, boolean doubleOn,
                     long freezeMs, long frenzyMs, long doubleMs,
                     boolean peachyOn, long peachyMs,
                     int bombDeflectCharges) {

        drawScore(g, score, mode, level);

        if (mode.maxLives <= 10) {
            drawLives(g, w, lives, mode.maxLives);
        }

        if (mode.hasTimer) {
            drawTimerBar(g, w, timeRemainingSec, mode.timerSeconds);
        }

        if (combo > 1) {
            drawCombo(g, w, h, combo);
        }

        if (mode.hasSpecialFruits) {
            drawPowerupTags(g, w, h, freezeOn, frenzyOn, doubleOn,
                            freezeMs, frenzyMs, doubleMs,
                            peachyOn, peachyMs);
        }

        if (bombDeflectCharges > 0) {
            drawBombDeflectShield(g, w, h, bombDeflectCharges);
        }
    }

    private void drawScore(Graphics2D g, int score, GameMode mode, int level) {
        // Score number, big and bone white.
        g.setFont(FontLoader.hud(Font.BOLD, 88));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String s = String.valueOf(score);

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString(s, 36 + 4, 96 + 4);
        // Main fill
        g.setColor(new Color(245, 240, 232));
        g.drawString(s, 36, 96);

        // Red underline
        int sw = fm.stringWidth(s);
        g.setColor(new Color(220, 50, 50));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(36, 108, 36 + sw, 108);

        // Tiny mode label below the underline
        g.setFont(FontLoader.body(Font.PLAIN, 12));
        g.setColor(new Color(180, 180, 180, 220));
        String label = mode.displayName.toUpperCase();
        if (mode.hasLevels) label = label + "  /  LEVEL " + level;
        g.drawString(label, 36, 128);
    }

    private void drawLives(Graphics2D g, int w, int lives, int maxLives) {
        // Lives as red X marks top-right. Empty slots dim grey.
        int x = w - 30;
        int top = 50;
        int cell = 30;
        for (int i = 0; i < maxLives; i++) {
            int cx = x - i * cell - 12;
            int cy = top;
            int armLen = 10;

            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (i < maxLives - lives) {
                g.setColor(new Color(220, 50, 50));
            } else {
                g.setColor(new Color(120, 120, 120, 90));
            }
            g.drawLine(cx - armLen, cy - armLen, cx + armLen, cy + armLen);
            g.drawLine(cx + armLen, cy - armLen, cx - armLen, cy + armLen);
        }
    }

    private void drawTimerBar(Graphics2D g, int w, double secLeft, double totalSec) {
        // Thin red bar pinned to the very top edge, full width when fresh.
        double pct = Math.max(0, Math.min(1, secLeft / totalSec));
        int barH = 6;

        // Track
        g.setColor(new Color(40, 20, 22, 180));
        g.fillRect(0, 0, w, barH);

        // Fill
        Color fill;
        if (pct < 0.15) fill = new Color(255, 90, 80);
        else if (pct < 0.35) fill = new Color(230, 130, 50);
        else fill = new Color(220, 50, 50);
        g.setColor(fill);
        g.fillRect(0, 0, (int) (w * pct), barH);

        // Big seconds digit near the bar
        g.setFont(FontLoader.hud(Font.BOLD, 36));
        g.setColor(new Color(245, 240, 232, 220));
        String s = String.valueOf((int) Math.ceil(secLeft));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(s, w - fm.stringWidth(s) - 36, 56);
    }

    private void drawCombo(Graphics2D g, int w, int h, int combo) {
        g.setFont(FontLoader.title(Font.BOLD, 130));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String text = "x" + combo;

        int tw = fm.stringWidth(text);
        int tx = (w - tw) / 2;
        int ty = h / 2 + 40;

        // Heavy black ink shadow
        g.setColor(new Color(0, 0, 0, 230));
        g.drawString(text, tx + 6, ty + 6);
        // Red fill
        g.setColor(new Color(220, 50, 50));
        g.drawString(text, tx, ty);

        // "COMBO" label below, smaller and white
        g.setFont(FontLoader.title(Font.BOLD, 28));
        java.awt.FontMetrics fm2 = g.getFontMetrics();
        String label = "COMBO";
        int lw = fm2.stringWidth(label);
        g.setColor(new Color(0, 0, 0, 210));
        g.drawString(label, (w - lw) / 2 + 3, ty + 50 + 3);
        g.setColor(new Color(245, 240, 232));
        g.drawString(label, (w - lw) / 2, ty + 50);
    }

    private void drawPowerupTags(Graphics2D g, int w, int h,
                                  boolean freezeOn, boolean frenzyOn, boolean doubleOn,
                                  long freezeMs, long frenzyMs, long doubleMs,
                                  boolean peachyOn, long peachyMs) {
        // Small bottom-center row of plain text tags. No pill backgrounds.
        java.util.List<String> tags = new java.util.ArrayList<>();
        if (freezeOn) tags.add("FREEZE " + (int) Math.ceil(freezeMs / 1000.0) + "s");
        if (frenzyOn) tags.add("FRENZY " + (int) Math.ceil(frenzyMs / 1000.0) + "s");
        if (doubleOn) tags.add("x2 " + (int) Math.ceil(doubleMs / 1000.0) + "s");
        if (peachyOn) tags.add("PEACHY " + (int) Math.ceil(peachyMs / 1000.0) + "s");
        if (tags.isEmpty()) return;

        g.setFont(FontLoader.hud(Font.BOLD, 18));
        java.awt.FontMetrics fm = g.getFontMetrics();

        // Measure total row width
        int totalW = 0;
        for (int i = 0; i < tags.size(); i++) {
            totalW += fm.stringWidth(tags.get(i));
            if (i < tags.size() - 1) totalW += 30;
        }
        int x = (w - totalW) / 2;
        int y = h - 80;

        for (int i = 0; i < tags.size(); i++) {
            String t = tags.get(i);
            // Ink shadow
            g.setColor(new Color(0, 0, 0, 220));
            g.drawString(t, x + 2, y + 2);
            // Color by powerup
            Color c;
            String head = t.split(" ")[0];
            if (head.equals("FREEZE"))      c = new Color(120, 200, 255);
            else if (head.equals("FRENZY")) c = new Color(255, 90, 80);
            else if (head.equals("PEACHY")) c = new Color(255, 180, 130);
            else if (head.equals("x2"))     c = new Color(245, 220, 80);
            else                            c = new Color(245, 220, 80);
            g.setColor(c);
            g.drawString(t, x, y);
            x += fm.stringWidth(t) + 30;
        }
    }

    /**
     * Small shield icon top-right (below the lives row) with the remaining
     * deflect-charge count next to it.
     */
    private void drawBombDeflectShield(Graphics2D g, int w, int h, int charges) {
        int cx = w - 60;
        int cy = 100;
        int s  = 18; // half-size of shield

        // Shield silhouette
        java.awt.geom.Path2D.Double shield = new java.awt.geom.Path2D.Double();
        shield.moveTo(cx, cy - s);
        shield.lineTo(cx + s, cy - s * 0.7);
        shield.lineTo(cx + s, cy + s * 0.2);
        shield.curveTo(cx + s, cy + s, cx, cy + s, cx, cy + s);
        shield.curveTo(cx, cy + s, cx - s, cy + s, cx - s, cy + s * 0.2);
        shield.lineTo(cx - s, cy - s * 0.7);
        shield.closePath();

        g.setColor(new Color(220, 230, 240, 230));
        g.fill(shield);
        g.setColor(new Color(40, 60, 80, 230));
        g.setStroke(new BasicStroke(2f));
        g.draw(shield);

        // Cross detail
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(cx, cy - 6, cx, cy + 8);
        g.drawLine(cx - 7, cy + 1, cx + 7, cy + 1);

        // Count number
        g.setFont(FontLoader.hud(Font.BOLD, 22));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String txt = "x" + charges;
        int tx = cx + s + 8;
        int ty = cy + fm.getAscent() / 2 - 4;
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString(txt, tx + 2, ty + 2);
        g.setColor(new Color(245, 240, 232));
        g.drawString(txt, tx, ty);
    }
}
