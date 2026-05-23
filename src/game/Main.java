package game;

import javax.swing.SwingUtilities;

/**
 * Entry point.
 *
 * Builds the window on the Swing event dispatch thread, then asks the
 * GamePanel to start its update / render loop on a separate thread.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
            window.startGameThread();
        });
    }
}
