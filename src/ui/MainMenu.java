package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Quiet minimal main menu. Title at the top, three mode names in the middle,
 * tiny hint at the bottom. No decorative shapes, no shurikens, no big sun.
 * Everything sized so it fits cleanly in a 720-tall window.
 */
public class MainMenu {

    private final int[] modeBoundsX = new int[3];
    private final int[] modeBoundsY = new int[3];
    private final int[] modeBoundsW = new int[3];
    private final int[] modeBoundsH = new int[3];

    // SHOP button rect (recomputed each frame)
    private int shopBtnX, shopBtnY, shopBtnW, shopBtnH;
    // MISSIONS button rect (recomputed each frame)
    private int missionsBtnX, missionsBtnY, missionsBtnW, missionsBtnH;
    // SETTINGS button rect (recomputed each frame)
    private int settingsBtnX, settingsBtnY, settingsBtnW, settingsBtnH;

    private int hoveredIndex = -1;
    private double introT;
    private long lastTickNs;

    public void tick(int mx, int my, int w, int h) {
        long now = System.nanoTime();
        double dt = (lastTickNs == 0) ? 0.016 : (now - lastTickNs) / 1e9;
        lastTickNs = now;
        introT = Math.min(1.0, introT + dt / 0.6);
        hoveredIndex = hitTest(mx, my, w, h);
    }

    public int hitTest(int mx, int my, int w, int h) {
        for (int i = 0; i < 3; i++) {
            if (modeBoundsW[i] == 0) continue;
            math.AABB box = new math.AABB(
                modeBoundsX[i], modeBoundsY[i],
                modeBoundsW[i], modeBoundsH[i]);
            if (box.contains(mx, my)) return i;
        }
        return -1;
    }

    /** Returns true if (mx, my) lies inside the SHOP button. */
    public boolean shopButtonHit(int mx, int my, int w, int h) {
        if (shopBtnW == 0) return false;
        math.AABB box = new math.AABB(shopBtnX, shopBtnY, shopBtnW, shopBtnH);
        return box.contains(mx, my);
    }

    /** Returns true if (mx, my) lies inside the MISSIONS button. */
    public boolean missionsButtonHit(int mx, int my, int w, int h) {
        if (missionsBtnW == 0) return false;
        math.AABB box = new math.AABB(missionsBtnX, missionsBtnY,
                                      missionsBtnW, missionsBtnH);
        return box.contains(mx, my);
    }

    /** Returns true if (mx, my) lies inside the SETTINGS button. */
    public boolean settingsButtonHit(int mx, int my, int w, int h) {
        if (settingsBtnW == 0) return false;
        math.AABB box = new math.AABB(settingsBtnX, settingsBtnY,
                                      settingsBtnW, settingsBtnH);
        return box.contains(mx, my);
    }

    public void draw(Graphics2D g, int w, int h) {
        double t = easeOutCubic(introT);
        int yOffset = (int) ((1 - t) * -40);

        // ===== TITLE BLOCK (top of the screen) =====
        int titleY = (int) (h * 0.18) + yOffset;
        Font titleFont = FontLoader.title(Font.BOLD, 76);
        g.setFont(titleFont);
        java.awt.FontMetrics fm = g.getFontMetrics();

        String fruit = "FRUIT";
        int fw = fm.stringWidth(fruit);
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString(fruit, (w - fw) / 2 + 4, titleY + 4);
        g.setColor(new Color(245, 240, 232));
        g.drawString(fruit, (w - fw) / 2, titleY);

        String ninja = "NINJA";
        int nw = fm.stringWidth(ninja);
        int ninjaY = titleY + 72;
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString(ninja, (w - nw) / 2 + 4, ninjaY + 4);
        g.setColor(new Color(220, 50, 50));
        g.drawString(ninja, (w - nw) / 2, ninjaY);

        // Thin slash mark between FRUIT and NINJA
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(245, 240, 232, (int) (200 * t)));
        int slashY = (titleY + ninjaY) / 2 + 4;
        g.drawLine((w - fw) / 2 - 20, slashY + 8, (w + fw) / 2 + 20, slashY - 8);

        // Subtitle
        g.setFont(FontLoader.body(Font.PLAIN, 13));
        g.setColor(new Color(170, 170, 170, (int) (200 * t)));
        String sub = "HAND TRACKING EDITION";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (w - sw) / 2, ninjaY + 30);

        // ===== MODE LIST (middle of the screen) =====
        String[] labels = { "CLASSIC", "ARCADE", "ZEN" };
        Font modeFont = FontLoader.title(Font.BOLD, 36);
        g.setFont(modeFont);
        java.awt.FontMetrics mfm = g.getFontMetrics();

        int modeBlockH = mfm.getAscent() * 3 + 60 * 2;
        int modeY0 = (h + ninjaY + 30) / 2 - modeBlockH / 2 + mfm.getAscent();
        int modeGap = 56;

        for (int i = 0; i < 3; i++) {
            String label = labels[i];
            int lw = mfm.stringWidth(label);
            int x = (w - lw) / 2;
            int y = modeY0 + i * modeGap;

            modeBoundsX[i] = x - 30;
            modeBoundsY[i] = y - mfm.getAscent() + 4;
            modeBoundsW[i] = lw + 60;
            modeBoundsH[i] = mfm.getAscent() + 10;

            boolean hovered = (hoveredIndex == i);

            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(label, x + 3, y + 3);
            g.setColor(hovered ? new Color(255, 255, 255) : new Color(170, 170, 170, 220));
            g.drawString(label, x, y);

            if (hovered) {
                g.setColor(new Color(220, 50, 50));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(x, y + 6, x + lw, y + 6);
            }
        }

        // ===== FOOTER =====
        g.setColor(new Color(120, 120, 120, 180));
        g.setFont(FontLoader.body(Font.PLAIN, 12));
        String hint = "click a mode  |  press 1 / 2 / 3  |  S = shop  |  M = missions";
        int hw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 40);

        // ===== SHOP + MISSIONS BUTTONS (top-right corner) =====
        drawTopRightButtons(g, w);

        // ===== COIN BALANCE (top-left corner) =====
        drawCoinBalance(g);
    }

    private void drawTopRightButtons(Graphics2D g, int w) {
        g.setFont(FontLoader.title(Font.BOLD, 18));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int padX = 22, padY = 12;

        // SHOP button (rightmost)
        String shopLabel = "SHOP";
        int shopW = fm.stringWidth(shopLabel) + padX * 2;
        int btnH  = fm.getAscent() + padY * 2;
        int shopX = w - shopW - 24;
        int btnY  = 24;
        shopBtnX = shopX; shopBtnY = btnY; shopBtnW = shopW; shopBtnH = btnH;
        drawPillButton(g, shopX, btnY, shopW, btnH, shopLabel,
                       new Color(245, 200, 66), padX, padY, fm);

        // MISSIONS button (just to the left of SHOP)
        String missionsLabel = "MISSIONS";
        int missionsW = fm.stringWidth(missionsLabel) + padX * 2;
        int missionsX = shopX - missionsW - 12;
        missionsBtnX = missionsX; missionsBtnY = btnY;
        missionsBtnW = missionsW; missionsBtnH = btnH;
        drawPillButton(g, missionsX, btnY, missionsW, btnH, missionsLabel,
                       new Color(76, 175, 80), padX, padY, fm);

        // SETTINGS button (just to the left of MISSIONS)
        String settingsLabel = "SETTINGS";
        int settingsW = fm.stringWidth(settingsLabel) + padX * 2;
        int settingsX = missionsX - settingsW - 12;
        settingsBtnX = settingsX; settingsBtnY = btnY;
        settingsBtnW = settingsW; settingsBtnH = btnH;
        drawPillButton(g, settingsX, btnY, settingsW, btnH, settingsLabel,
                       new Color(120, 180, 255), padX, padY, fm);
    }

    private void drawPillButton(Graphics2D g, int x, int y, int w, int h,
                                String label, Color accent,
                                int padX, int padY, java.awt.FontMetrics fm) {
        g.setColor(new Color(45, 28, 12, 230));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g.setColor(accent);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new java.awt.geom.RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g.setColor(accent);
        g.drawString(label, x + padX, y + padY + fm.getAscent() - 4);
    }

    private void drawCoinBalance(Graphics2D g) {
        int coins = economy.Currency.get();
        String balance = String.valueOf(coins);
        g.setFont(FontLoader.hud(Font.BOLD, 22));
        java.awt.FontMetrics fm = g.getFontMetrics();

        int cx = 36, cy = 36;
        // Coin icon
        g.setColor(new Color(245, 200, 66));
        g.fillOval(cx - 12, cy - 12, 24, 24);
        g.setColor(new Color(180, 120, 30));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(cx - 12, cy - 12, 24, 24);
        // Number
        g.setColor(new Color(245, 200, 66));
        g.drawString(balance, cx + 22, cy + fm.getAscent() / 2 - 4);
    }

    private static double easeOutCubic(double t) {
        double inv = 1 - t;
        return 1 - inv * inv * inv;
    }
}
