package game;

/**
 * Top-level state of the game.
 *
 * The state machine is intentionally tiny so it stays easy to reason about:
 *
 *     MENU  -> PLAYING  (user picks a mode)
 *     PLAYING -> GAME_OVER  (lost all lives, timer expired, etc.)
 *     PLAYING -> MENU  (user opens main menu)
 *     GAME_OVER -> PLAYING  (restart)
 *     GAME_OVER -> MENU  (back to menu)
 */
public enum GameState {
    MENU,
    PLAYING,
    GAME_OVER,
    SHOP,
    SETTINGS,
    MISSIONS
}
