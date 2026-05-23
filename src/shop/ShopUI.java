package shop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import economy.Currency;
import economy.Inventory;
import themes.Background;
import ui.FontLoader;
import weapons.Sword;

/**
 * Shop screen. Two tabs (SWORDS, BACKGROUNDS), each showing a scrollable
 * grid of cards. Click a card to buy (if not owned) or equip (if owned).
 *
 * Layout per frame is recomputed so window resizing works cleanly.
 */
public final class ShopUI {

    private static final int CARD_W   = 220;
    private static final int CARD_H   = 170;
    private static final int CARD_GAP = 18;
    private static final int GRID_TOP = 170;
    private static final int GRID_BOTTOM_PAD = 90;

    private static class CardRect {
        int x, y, w, h;
        String id;
        CardRect(int x, int y, int w, int h, String id) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.id = id;
        }
    }

    private final List<CardRect> cardRects = new ArrayList<>();
    private int hoveredIndex = -1;

    /** Returns the tab if a tab was clicked, else null. */
    public Shop.Tab tabHit(int mx, int my, int w) {
        int tabY = 90;
        int tabH = 36;
        int swordX = w / 2 - 140;
        int bgX    = w / 2 + 8;
        if (my >= tabY && my <= tabY + tabH) {
            if (mx >= swordX && mx <= swordX + 130) return Shop.Tab.SWORDS;
            if (mx >= bgX    && mx <= bgX    + 132) return Shop.Tab.BACKGROUNDS;
        }
        return null;
    }

    /** Returns the card id if a card was clicked, else null. */
    public String cardHit(int mx, int my) {
        for (CardRect r : cardRects) {
            if (mx >= r.x && mx <= r.x + r.w
             && my >= r.y && my <= r.y + r.h) return r.id;
        }
        return null;
    }

    public int contentHeight(int w) {
        int columns = Math.max(1, (w - 40) / (CARD_W + CARD_GAP));
        int items = Shop.tab() == Shop.Tab.SWORDS
            ? Shop.swords().size() : Shop.backgrounds().size();
        int rows = (items + columns - 1) / columns;
        return rows * (CARD_H + CARD_GAP);
    }

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

    public void draw(Graphics2D g, int w, int h) {
        // Dim backdrop so the shop sits cleanly over whatever the player's
        // equipped background is.
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, w, h);

        drawHeader(g, w);
        drawTabs(g, w);

        // Clip the grid area so cards can scroll beneath the header / footer.
        int gridLeft = 20;
        int gridRight = w - 20;
        int gridTop = GRID_TOP;
        int gridBottom = h - GRID_BOTTOM_PAD;
        Shape oldClip = g.getClip();
        g.setClip(gridLeft, gridTop, gridRight - gridLeft, gridBottom - gridTop);

        if (Shop.tab() == Shop.Tab.SWORDS)      drawSwordGrid(g, w, gridTop, gridBottom);
        else                                     drawBackgroundGrid(g, w, gridTop, gridBottom);

        g.setClip(oldClip);

        drawFooter(g, w, h);
    }

    private void drawHeader(Graphics2D g, int w) {
        // Title
        g.setFont(FontLoader.title(Font.BOLD, 38));
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString("SHOP", 24 + 2, 56 + 2);
        g.setColor(new Color(245, 240, 232));
        g.drawString("SHOP", 24, 56);

        // Coin balance (right side)
        String balance = String.valueOf(Currency.get()) + " COINS";
        g.setFont(FontLoader.hud(Font.BOLD, 26));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int bw = fm.stringWidth(balance);
        // Coin icon
        int coinCx = w - bw - 60;
        int coinCy = 46;
        g.setColor(new Color(245, 200, 66));
        g.fillOval(coinCx, coinCy - 14, 28, 28);
        g.setColor(new Color(180, 120, 30));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(coinCx, coinCy - 14, 28, 28);
        g.setColor(new Color(245, 200, 66));
        g.drawString(balance, w - bw - 24, 56);
    }

    private void drawTabs(Graphics2D g, int w) {
        int tabY = 90;
        int tabH = 36;
        int swordX = w / 2 - 140;
        int bgX = w / 2 + 8;

        drawTab(g, swordX, tabY, 130, tabH, "SWORDS", Shop.tab() == Shop.Tab.SWORDS);
        drawTab(g, bgX, tabY, 132, tabH, "THEMES", Shop.tab() == Shop.Tab.BACKGROUNDS);
    }

    private void drawTab(Graphics2D g, int x, int y, int w, int h, String label, boolean active) {
        g.setColor(active ? new Color(45, 28, 12, 220) : new Color(15, 12, 10, 180));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g.setStroke(new BasicStroke(active ? 3f : 2f));
        g.setColor(active ? new Color(245, 200, 66) : new Color(120, 120, 120, 200));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        g.setFont(FontLoader.title(Font.BOLD, 18));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        g.setColor(active ? new Color(245, 240, 232) : new Color(160, 160, 160));
        g.drawString(label, x + (w - tw) / 2, y + 24);
    }

    private void drawSwordGrid(Graphics2D g, int w, int top, int bottom) {
        cardRects.clear();
        List<Sword> items = Shop.swords();

        int columns = Math.max(1, (w - 40) / (CARD_W + CARD_GAP));
        int totalW = columns * CARD_W + (columns - 1) * CARD_GAP;
        int startX = (w - totalW) / 2;

        // Clamp scroll based on content height vs available area
        int areaH = bottom - top;
        int rows = (items.size() + columns - 1) / columns;
        int contentH = rows * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, contentH - areaH);
        Shop.clampScroll(maxScroll);

        for (int i = 0; i < items.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (CARD_W + CARD_GAP);
            int y = top + row * (CARD_H + CARD_GAP) - Shop.scroll();

            Sword s = items.get(i);
            boolean owned = Inventory.ownsSword(s.id());
            boolean equipped = s.id().equals(Inventory.equippedSword());
            drawSwordCard(g, x, y, s, owned, equipped, i == hoveredIndex);
            cardRects.add(new CardRect(x, y, CARD_W, CARD_H, s.id()));
        }
    }

    private void drawBackgroundGrid(Graphics2D g, int w, int top, int bottom) {
        cardRects.clear();
        List<Background> items = Shop.backgrounds();

        int columns = Math.max(1, (w - 40) / (CARD_W + CARD_GAP));
        int totalW = columns * CARD_W + (columns - 1) * CARD_GAP;
        int startX = (w - totalW) / 2;

        int areaH = bottom - top;
        int rows = (items.size() + columns - 1) / columns;
        int contentH = rows * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, contentH - areaH);
        Shop.clampScroll(maxScroll);

        for (int i = 0; i < items.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (CARD_W + CARD_GAP);
            int y = top + row * (CARD_H + CARD_GAP) - Shop.scroll();

            Background b = items.get(i);
            boolean owned = Inventory.ownsBackground(b.id());
            boolean equipped = b.id().equals(Inventory.equippedBackground());
            drawBackgroundCard(g, x, y, b, owned, equipped, i == hoveredIndex);
            cardRects.add(new CardRect(x, y, CARD_W, CARD_H, b.id()));
        }
    }

    private void drawSwordCard(Graphics2D g, int x, int y, Sword s,
                               boolean owned, boolean equipped, boolean hover) {
        drawCardShell(g, x, y, s.previewAccent(), owned, equipped, hover);
        // Preview area: top 95 px
        s.drawPreview(g, x + 14, y + 14, CARD_W - 28, 80);
        drawCardText(g, x, y, s.name(), s.tagline(), s.price(), owned, equipped);
    }

    private void drawBackgroundCard(Graphics2D g, int x, int y, Background b,
                                    boolean owned, boolean equipped, boolean hover) {
        drawCardShell(g, x, y, b.previewAccent(), owned, equipped, hover);
        b.drawPreview(g, x + 14, y + 14, CARD_W - 28, 80);
        drawCardText(g, x, y, b.name(), b.tagline(), b.price(), owned, equipped);
    }

    private void drawCardShell(Graphics2D g, int x, int y, Color accent,
                               boolean owned, boolean equipped, boolean hover) {
        if (hover) y -= 3;

        g.setColor(new Color(25, 20, 16, 230));
        g.fill(new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 12, 12));

        Color outline;
        if (equipped)      outline = new Color(245, 200, 66);
        else if (owned)    outline = new Color(76, 175, 80, 220);
        else if (hover)    outline = new Color(245, 240, 232);
        else               outline = new Color(accent.getRed(), accent.getGreen(),
                                               accent.getBlue(), 160);

        g.setStroke(new BasicStroke(equipped ? 3f : (hover ? 3f : 2f)));
        g.setColor(outline);
        g.draw(new RoundRectangle2D.Double(x, y, CARD_W, CARD_H, 12, 12));
    }

    private void drawCardText(Graphics2D g, int x, int y,
                              String name, String tagline, int price,
                              boolean owned, boolean equipped) {
        // Name
        g.setFont(FontLoader.title(Font.BOLD, 16));
        g.setColor(new Color(245, 240, 232));
        g.drawString(name, x + 14, y + 116);

        // Tagline
        g.setFont(FontLoader.body(Font.PLAIN, 11));
        g.setColor(new Color(170, 170, 170, 220));
        String t = tagline;
        if (t.length() > 32) t = t.substring(0, 31) + "...";
        g.drawString(t, x + 14, y + 132);

        // Bottom-right status
        String status;
        Color statusC;
        if (equipped)    { status = "EQUIPPED"; statusC = new Color(245, 200, 66); }
        else if (owned)  { status = "OWNED";    statusC = new Color(76, 175, 80); }
        else if (price == 0) { status = "FREE"; statusC = new Color(160, 160, 160); }
        else             { status = price + " c"; statusC = new Color(245, 200, 66); }

        g.setFont(FontLoader.hud(Font.BOLD, 14));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(status);
        g.setColor(statusC);
        g.drawString(status, x + CARD_W - sw - 14, y + CARD_H - 12);
    }

    private void drawFooter(Graphics2D g, int w, int h) {
        // Flash message (if any)
        long nowMs = System.currentTimeMillis();
        String flash = Shop.flashMessage(nowMs);
        if (flash != null) {
            g.setFont(FontLoader.title(Font.BOLD, 24));
            java.awt.FontMetrics fm = g.getFontMetrics();
            int fw = fm.stringWidth(flash);
            g.setColor(new Color(0, 0, 0, 220));
            g.drawString(flash, (w - fw) / 2 + 3, h - 50 + 3);
            g.setColor(new Color(245, 200, 66));
            g.drawString(flash, (w - fw) / 2, h - 50);
        }

        // Hint
        g.setFont(FontLoader.body(Font.PLAIN, 12));
        g.setColor(new Color(120, 120, 120, 200));
        String hint = "click a card to buy or equip  |  scroll mouse wheel  |  ESC for menu";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int hw = fm.stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 22);
    }
}
