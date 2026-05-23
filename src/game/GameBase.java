package game;

import javax.swing.JPanel;

/**
 * Abstract base class that owns the game-loop scaffolding so subclasses only
 * have to implement {@link #inGameLoop(double)}.
 *
 * This mirrors the refactor Professor Murphy walked through on March 3:
 *   - The base class holds the final, locked-down boilerplate (run(),
 *     startGameThread(), the 60 FPS sleep loop).
 *   - The subclass implements one abstract method that runs every frame.
 *   - Marking the scaffolding methods final prevents subclasses from
 *     accidentally overriding them (his quote: "box yourself into a corner
 *     with your code, because the more freedom you have the more likely you
 *     do something dumb").
 *
 * Concrete subclass: {@link GamePanel}.
 */
public abstract class GameBase extends JPanel implements Runnable {

    private static final long serialVersionUID = 1L;

    public static final int TARGET_FPS = 60;
    public static final long FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;

    private Thread gameThread;
    private volatile boolean running;
    private long previousNs;

    /** Subclasses implement this. Called once per frame with the elapsed ms. */
    protected abstract void inGameLoop(double deltaMs);

    /** Starts the dedicated game-loop thread. Idempotent. */
    public final void startGameThread() {
        if (gameThread != null) return;
        running = true;
        previousNs = System.nanoTime();
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    public final void stopGameThread() {
        running = false;
    }

    @Override
    public final void run() {
        while (running) {
            long nowNs = System.nanoTime();
            // Cap delta so a paused window doesn't make the next frame "jump".
            double deltaMs = Math.min((nowNs - previousNs) / 1_000_000.0, 33.34);
            previousNs = nowNs;

            inGameLoop(deltaMs);
            repaint();

            long sleepNs = FRAME_TIME_NS - (System.nanoTime() - nowNs);
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }
}
