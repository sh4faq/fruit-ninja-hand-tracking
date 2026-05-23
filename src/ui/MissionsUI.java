package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import missions.Mission;
import missions.MissionCatalog;
import missions.MissionTracker;

/**
 * Missions screen. Scrollable grid of mission cards. Click a card to launch
 * the matching mode for that attempt (the caller stores the chosen mission as
 * the active mission so the post-run evaluator can grade it).
 *
 * Layout mirrors ShopUI: dim backdrop, top header, scrolling grid, footer.
 */
public final class MissionsUI {

    private static final int CARD_W   = 260;
    private static final int CARD_H   = 150;
    private static final int CARD_GAP = 18;
    private static final int GRID_TOP = 130;
    private static final int GRID_BOTTOM_PAD = 70;

    private static class CardRect {
        final int x, y, w, h;
        final Mission mission;
        CardRect(int x, int y, int w, int h, Mission m) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.mission = m;
        }
    }

    private final List<CardRect> cardRects = new ArrayList<>();
    private int hoveredIndex = -1;
    private int scroll;

    public void updateHover(int mx, int my) {
        hoveredIndex = -1;
        for (int i = 0; i < cardRects.size(); i++) {
            CardRect r = cardRects.get(i);
            if (mx >= r.x && mx <= r.x + r.w
             && my >= r.y && my <= r.y + r.h) {
                hoveredIndex = i;
                return;
            }
        }
    }

    /** Returns the mission whose card was clicked, or null. */
    public Mission cardHit(int mx, int my) {
        for (CardRect r : cardRects) {
            if (mx >= r.x && mx <= r.x + r.w
             && my >= r.y && my <= r.y + r.h) return r.mission;
        }
        return null;
    }

    public void scrollBy(int dy) {
        scroll = Math.max(0, scroll + dy);
    }

    public void resetScroll() { scroll = 0; }

    public void draw(Graphics2D g, int w, int h) {
        // Dim backdrop
        g.setColor(new Color(0, 0, 0, 210));
        g.fillRect(0, 0, w, h);

        drawHeader(g, w);

        int gridLeft = 20;
        int gridRight = w - 20;
        int gridTop = GRID_TOP;
        int gridBottom = h - GRID_BOTTOM_PAD;
        Shape oldClip = g.getClip();
        g.setClip(gridLeft, gridTop, gridRight - gridLeft, gridBottom - gridTop);

        drawGrid(g, w, gridTop, gridBottom);

        g.setClip(oldClip);

        drawFooter(g, w, h);
    }

    private void drawHeader(Graphics2D g, int w) {
        // Title
        g.setFont(FontLoader.title(Font.BOLD, 38));
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString("MISSIONS", 24 + 2, 56 + 2);
        g.setColor(new Color(245, 240, 232));
        g.drawString("MISSIONS", 24, 56);

        // Star total on the right
        int earned = MissionCatalog.totalEarnedStars();
        int max    = MissionCatalog.totalPossibleStars();
        String total = earned + " / " + max + " STARS";
        g.setFont(FontLoader.hud(Font.BOLD, 22));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(total);

        // Big yellow star icon
        int sx = w - tw - 56;
        int sy = 30;
        drawStar(g, sx, sy, 14, true);

        g.setColor(new Color(245, 200, 66));
        g.drawString(total, w - tw - 24, 56);

        // Subtle progress bar under the header
        int barX = 24, barY = 82, barW = w - 48, barH = 6;
        g.setColor(new Color(40, 30, 22, 220));
        g.fill(new RoundRectangle2D.Double(barX, barY, barW, barH, 6, 6));
        double pct = max == 0 ? 0 : (double) earned / max;
        int fillW = (int) (barW * pct);
        g.setColor(new Color(245, 200, 66, 220));
        g.fill(new RoundRectangle2D.Double(barX, barY, fillW, barH, 6, 6));
    }

    private void drawGrid(Graphics2D g, int w, int top, int bottom) {
        cardRects.clear();
        List<Mission> items = MissionCatalog.all();

        int columns = Math.max(1, (w - 40) / (CARD_W + CARD_GAP));
        int totalW = columns * CARD_W + (columns - 1) * CARD_GAP;
        int startX = (w - totalW) / 2;

        int areaH = bottom - top;
        int rows = (items.size() + columns - 1) / columns;
        int contentH = rows * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, contentH - areaH);
        if (scroll > maxScroll) scroll = maxScroll;

        for (int i = 0; i < items.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (CARD_W + CARD_GAP);
            int y = top + row * (CARD_H + CARD_GAP) - scroll;

            Mission m = items.get(i);
            drawCard(g, x, y, m, i == hoveredIndex);
            cardRects.add(new CardRect(x, y, CARD_W, CARD_H, m));
        }
    }

    private void drawCard(Graphics2D g, int x, int y, Mission m, boolean hover) {
        if (hover) y -= 3;

        int stars = m.bestStars();
        boolean complete = stars >= 3;

        // Shell
        g.setColor(new Color(25, 20, 16, 232));
        g.fill(new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 12, 12));

        Color outline;
        if (complete)    outline = new Color(245, 200, 66);
        else if (stars > 0) outline = new Color(76, 175, 80, 220);
        else if (hover)  outline = new Color(245, 240, 232);
        else             outline = new Color(120, 90, 50, 180);

        g.setStroke(new BasicStroke(complete ? 3f : (hover ? 3f : 2f)));
        g.setColor(outline);
        g.draw(new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 12, 12));

        // Mode chip (top-right inside the card)
        drawModeChip(g, x + CARD_W - 76, y + 12, 64, 22, m.modeChip());

        // Name
        g.setFont(FontLoader.title(Font.BOLD, 18));
        g.setColor(new Color(245, 240, 232));
        String name = m.name;
        java.awt.FontMetrics nfm = g.getFontMetrics();
        int maxNameW = CARD_W - 96;
        while (nfm.stringWidth(name) > maxNameW && name.length() > 4) {
            name = name.substring(0, name.length() - 2);
        }
        if (!name.equals(m.name)) name = name + "...";
        g.drawString(name, x + 14, y + 32);

        // Description (wrapped to two lines if needed)
        g.setFont(FontLoader.body(Font.PLAIN, 12));
        g.setColor(new Color(190, 185, 175, 220));
        drawWrappedDescription(g, m.description, x + 14, y + 56, CARD_W - 28, 2);

        // Star row (bottom-left)
        drawStarsRow(g, x + 14, y + CARD_H - 30, stars);

        // Best progress (bottom-right) — only meaningful for cumulative
        if (m.scope == Mission.Scope.CUMULATIVE) {
            int achieved = MissionTracker.achievedValue(m, 0);
            String prog = achieved + " / " + m.threeStarValue;
            g.setFont(FontLoader.hud(Font.BOLD, 12));
            java.awt.FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(prog);
            g.setColor(new Color(170, 170, 170, 220));
            g.drawString(prog, x + CARD_W - pw - 14, y + CARD_H - 14);
        } else {
            String play = stars > 0 ? "PLAY AGAIN" : "PLAY";
            g.setFont(FontLoader.hud(Font.BOLD, 12));
            java.awt.FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(play);
            g.setColor(new Color(245, 200, 66, 220));
            g.drawString(play, x + CARD_W - pw - 14, y + CARD_H - 14);
        }
    }

    private void drawWrappedDescription(Graphics2D g, String text,
                                        int x, int y, int maxW, int maxLines) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineNum = 0;
        int lineY = y;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxW && line.length() > 0) {
                g.drawString(line.toString(), x, lineY);
                line = new StringBuilder(word);
                lineNum++;
                lineY += fm.getHeight();
                if (lineNum >= maxLines - 1) {
                    // Last line: pack remaining + ellipsize if too long.
                    StringBuilder rest = new StringBuilder(word);
                    for (int j = i + 1; j < words.length; j++) {
                        rest.append(" ").append(words[j]);
                    }
                    String last = rest.toString();
                    while (fm.stringWidth(last) > maxW && last.length() > 4) {
                        last = last.substring(0, last.length() - 2);
                    }
                    if (!last.equals(rest.toString())) last = last + "...";
                    g.drawString(last, x, lineY);
                    return;
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, lineY);
        }
    }

    private void drawModeChip(Graphics2D g, int x, int y, int w, int h, String label) {
        Color chip;
        if ("CLASSIC".equals(label))      chip = new Color(196, 154, 108);
        else if ("ARCADE".equals(label))  chip = new Color(231, 76, 60);
        else if ("ZEN".equals(label))     chip = new Color(109, 186, 94);
        else                              chip = new Color(160, 160, 160);

        g.setColor(new Color(chip.getRed(), chip.getGreen(), chip.getBlue(), 60));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 8, 8));
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(chip.getRed(), chip.getGreen(), chip.getBlue(), 220));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 8, 8));

        g.setFont(FontLoader.hud(Font.BOLD, 11));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(label);
        g.drawString(label, x + (w - lw) / 2, y + 15);
    }

    private void drawStarsRow(Graphics2D g, int x, int y, int filled) {
        int starSize = 9;
        int gap = 6;
        for (int i = 0; i < 3; i++) {
            int cx = x + i * (starSize * 2 + gap) + starSize;
            int cy = y;
            drawStar(g, cx, cy, starSize, i < filled);
        }
    }

    /** Five-pointed star centered at (cx, cy). */
    private void drawStar(Graphics2D g, int cx, int cy, int r, boolean filled) {
        java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double radius = (i % 2 == 0) ? r : r * 0.5;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double px = cx + Math.cos(a) * radius;
            double py = cy + Math.sin(a) * radius;
            if (i == 0) path.moveTo(px, py);
            else        path.lineTo(px, py);
        }
        path.closePath();

        if (filled) {
            g.setColor(new Color(245, 200, 66));
            g.fill(path);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(180, 120, 30));
            g.draw(path);
        } else {
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(120, 100, 60, 200));
            g.draw(path);
        }
    }

    private void drawFooter(Graphics2D g, int w, int h) {
        g.setFont(FontLoader.body(Font.PLAIN, 12));
        g.setColor(new Color(120, 120, 120, 200));
        String hint = "click a mission to play  |  scroll mouse wheel  |  ESC for menu";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int hw = fm.stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 22);
    }
}
