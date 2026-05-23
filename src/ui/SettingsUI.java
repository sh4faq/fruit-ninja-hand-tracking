package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

import economy.Settings;

/**
 * Settings screen. Three slider rows: sensitivity, smoothing, slice threshold.
 * Drag the slider thumbs with the mouse or press LEFT/RIGHT to nudge the
 * focused row.
 */
public final class SettingsUI {

    private static final int ROW_H      = 110;
    private static final int SLIDER_W   = 480;
    private static final int THUMB_R    = 14;

    private int focusedRow = 0;
    private int draggingRow = -1;

    private final Rectangle[] thumbRects = { new Rectangle(), new Rectangle(), new Rectangle() };
    private final Rectangle[] trackRects = { new Rectangle(), new Rectangle(), new Rectangle() };

    public void draw(Graphics2D g, int w, int h) {
        // Dim backdrop
        g.setColor(new Color(0, 0, 0, 210));
        g.fillRect(0, 0, w, h);

        // Title
        g.setFont(FontLoader.title(Font.BOLD, 42));
        g.setColor(new Color(0, 0, 0, 220));
        g.drawString("SETTINGS", 36 + 3, 64 + 3);
        g.setColor(new Color(245, 240, 232));
        g.drawString("SETTINGS", 36, 64);

        int rowsStartY = 130;
        int rowX = (w - SLIDER_W) / 2;

        drawSliderRow(g, rowX, rowsStartY + 0 * ROW_H, w, 0,
            "POINTER SENSITIVITY",
            "How much your hand motion is amplified onto the screen.",
            Settings.sensitivity(), 0.5, 2.5,
            String.format("%.2fx", Settings.sensitivity()));

        drawSliderRow(g, rowX, rowsStartY + 1 * ROW_H, w, 1,
            "RESPONSIVENESS",
            "Higher = snappier, lower = more smoothing (and more lag).",
            Settings.smoothing(), 0.30, 0.95,
            String.format("%.0f%%", Settings.smoothing() * 100));

        drawSliderRow(g, rowX, rowsStartY + 2 * ROW_H, w, 2,
            "SLICE THRESHOLD",
            "How much movement counts as a slice. Lower = easier to slice.",
            Settings.sliceThreshold(), 1, 8,
            Settings.sliceThreshold() + " px");

        // Footer
        g.setColor(new Color(120, 120, 120, 200));
        g.setFont(FontLoader.body(Font.PLAIN, 13));
        String hint = "drag the sliders  |  ESC to return  |  R to reset to defaults";
        java.awt.FontMetrics fm = g.getFontMetrics();
        int hw = fm.stringWidth(hint);
        g.drawString(hint, (w - hw) / 2, h - 36);
    }

    private void drawSliderRow(Graphics2D g, int x, int y, int panelW, int rowIdx,
                                String label, String desc,
                                double value, double min, double max,
                                String valueText) {
        boolean focused = (rowIdx == focusedRow);

        // Label
        g.setFont(FontLoader.title(Font.BOLD, 22));
        g.setColor(focused ? new Color(255, 255, 255) : new Color(200, 200, 200));
        g.drawString(label, x, y);

        // Description
        g.setFont(FontLoader.body(Font.PLAIN, 12));
        g.setColor(new Color(160, 160, 160, 220));
        g.drawString(desc, x, y + 22);

        // Slider track
        int trackY = y + 50;
        int trackH = 6;
        trackRects[rowIdx].setBounds(x, trackY - 12, SLIDER_W, 28);

        g.setColor(new Color(40, 40, 48, 220));
        g.fill(new RoundRectangle2D.Double(x, trackY, SLIDER_W, trackH, 4, 4));

        // Filled portion
        double t = (value - min) / (max - min);
        if (t < 0) t = 0; else if (t > 1) t = 1;
        int filledW = (int) (SLIDER_W * t);
        g.setColor(focused ? new Color(245, 200, 66) : new Color(180, 120, 30));
        g.fill(new RoundRectangle2D.Double(x, trackY, filledW, trackH, 4, 4));

        // Thumb
        int thumbX = x + filledW;
        int thumbY = trackY + trackH / 2;
        thumbRects[rowIdx].setBounds(thumbX - THUMB_R, thumbY - THUMB_R,
                                      THUMB_R * 2, THUMB_R * 2);

        g.setColor(new Color(245, 240, 232));
        g.fillOval(thumbX - THUMB_R, thumbY - THUMB_R, THUMB_R * 2, THUMB_R * 2);
        g.setStroke(new BasicStroke(focused ? 3f : 2f));
        g.setColor(focused ? new Color(245, 200, 66) : new Color(60, 60, 70));
        g.drawOval(thumbX - THUMB_R, thumbY - THUMB_R, THUMB_R * 2, THUMB_R * 2);

        // Value readout
        g.setFont(FontLoader.hud(Font.BOLD, 22));
        g.setColor(new Color(245, 200, 66));
        g.drawString(valueText, x + SLIDER_W + 20, trackY + 8);
    }

    /** Mouse pressed: start dragging if a thumb was hit, returns true if so. */
    public boolean onMousePressed(int mx, int my) {
        for (int i = 0; i < 3; i++) {
            if (thumbRects[i].contains(mx, my) || trackRects[i].contains(mx, my)) {
                draggingRow = i;
                focusedRow = i;
                onDrag(mx, my);
                return true;
            }
        }
        return false;
    }

    /** Mouse released: stop dragging. */
    public void onMouseReleased() {
        draggingRow = -1;
    }

    /** Mouse dragged: update the current slider's value if any. */
    public void onDrag(int mx, int my) {
        if (draggingRow < 0 || draggingRow >= 3) return;
        Rectangle track = trackRects[draggingRow];
        double t = (mx - track.x) / (double) track.width;
        if (t < 0) t = 0; else if (t > 1) t = 1;
        applyT(draggingRow, t);
    }

    /** LEFT / RIGHT keyboard nudge for the focused row. */
    public void onArrowKey(int dir) {
        double t = currentT(focusedRow);
        t += dir * 0.05;
        if (t < 0) t = 0; else if (t > 1) t = 1;
        applyT(focusedRow, t);
    }

    public void onTabKey() {
        focusedRow = (focusedRow + 1) % 3;
    }

    public void resetDefaults() {
        Settings.resetAll();
    }

    private double currentT(int row) {
        switch (row) {
            case 0: return (Settings.sensitivity() - 0.5) / (2.5 - 0.5);
            case 1: return (Settings.smoothing() - 0.30) / (0.95 - 0.30);
            case 2: return (Settings.sliceThreshold() - 1) / (double) (8 - 1);
            default: return 0;
        }
    }

    private void applyT(int row, double t) {
        switch (row) {
            case 0: Settings.setSensitivity(0.5 + t * (2.5 - 0.5)); break;
            case 1: Settings.setSmoothing(0.30 + t * (0.95 - 0.30)); break;
            case 2: Settings.setSliceThreshold((int) Math.round(1 + t * (8 - 1))); break;
            default: break;
        }
    }
}
