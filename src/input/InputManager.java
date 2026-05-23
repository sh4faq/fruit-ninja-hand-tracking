package input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import javax.swing.JPanel;

/**
 * Unified input source for the game.
 *
 * The panel can be controlled by either:
 *   - the mouse (default fallback, always available), or
 *   - a hand tracking sidecar (Python + MediaPipe) that streams fingertip
 *     coordinates over a local socket. The HandTrackingClient pushes those
 *     coordinates into this manager exactly the same way the mouse listener
 *     does.
 *
 * Both sources call submitPointer(x, y) each time a new pointer position
 * is available. update() then builds a SliceLine from the previous pointer
 * position to the current one. The game loop consumes that line once per
 * frame via consumeSliceLine().
 */
public class InputManager {

    private final JPanel host;

    // Latest pointer position from any source. Volatile because the hand
    // tracking client writes from its own reader thread.
    private volatile double pointerX = -1;
    private volatile double pointerY = -1;
    private volatile boolean pointerActive;

    private double prevX, prevY;
    private boolean havePrev;

    private SliceLine pendingLine;

    private IntConsumer menuCallback;
    private BiConsumer<Integer, Integer> clickCallback;
    private BiConsumer<Integer, Integer> dragCallback;
    private Runnable releaseCallback;
    private IntConsumer wheelCallback;

    public InputManager(JPanel host) {
        this.host = host;
    }

    public void setMenuCallback(IntConsumer cb) { this.menuCallback = cb; }
    public void setClickCallback(BiConsumer<Integer, Integer> cb) {
        this.clickCallback = cb;
    }
    public void setDragCallback(BiConsumer<Integer, Integer> cb) {
        this.dragCallback = cb;
    }
    public void setReleaseCallback(Runnable cb) { this.releaseCallback = cb; }
    public void setWheelCallback(IntConsumer cb) { this.wheelCallback = cb; }

    public MouseWheelListener mouseWheelListener() {
        return new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (wheelCallback != null) {
                    // Positive when wheel rolls down (scroll down).
                    wheelCallback.accept(e.getWheelRotation());
                }
            }
        };
    }

    public void submitPointer(double x, double y) {
        this.pointerX = x;
        this.pointerY = y;
        this.pointerActive = true;
    }

    public void clearPointer() {
        this.pointerActive = false;
        this.havePrev = false;
    }

    /**
     * Called once per game-loop tick. Builds a slice line from the last
     * pointer position to the current one if both are valid.
     */
    public void update() {
        if (!pointerActive) {
            havePrev = false;
            return;
        }
        double x = pointerX;
        double y = pointerY;
        if (havePrev) {
            double dx = x - prevX;
            double dy = y - prevY;
            // Threshold is player-configurable (default 1px). Tiny threshold
            // means even quick small movements emit a slice line, which fixes
            // the "sometimes doesn't slice" problem on fast hand swipes.
            int threshold = economy.Settings.sliceThreshold();
            if (Math.sqrt(dx * dx + dy * dy) >= threshold) {
                pendingLine = new SliceLine(prevX, prevY, x, y);
            }
        }
        prevX = x;
        prevY = y;
        havePrev = true;
    }

    public SliceLine consumeSliceLine() {
        SliceLine out = pendingLine;
        pendingLine = null;
        return out;
    }

    public boolean isPointerActive() { return pointerActive; }
    public double pointerX() { return pointerX; }
    public double pointerY() { return pointerY; }

    // ====================================================================
    //  LISTENERS
    // ====================================================================

    public MouseListener mouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                submitPointer(e.getX(), e.getY());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                clearPointer();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                host.requestFocusInWindow();
                submitPointer(e.getX(), e.getY());
                if (clickCallback != null) {
                    clickCallback.accept(e.getX(), e.getY());
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (releaseCallback != null) releaseCallback.run();
            }
        };
    }

    public MouseMotionListener mouseMotionListener() {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                submitPointer(e.getX(), e.getY());
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                submitPointer(e.getX(), e.getY());
                if (dragCallback != null) {
                    dragCallback.accept(e.getX(), e.getY());
                }
            }
        };
    }

    public KeyListener keyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (menuCallback != null) menuCallback.accept(e.getKeyCode());
            }
        };
    }
}
