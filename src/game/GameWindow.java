package game;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

/**
 * Main window. Hosts a single GamePanel that handles drawing and input.
 *
 * The window is fixed at 1280x720 by default but can be resized by the user
 * (the panel rescales its rendering to the current size each frame).
 */
public class GameWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;

    private final GamePanel panel;

    public GameWindow() {
        super("Fruit Ninja - Hand Tracking (Java)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        panel = new GamePanel();
        panel.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    public void startGameThread() {
        panel.startGameThread();
    }
}
