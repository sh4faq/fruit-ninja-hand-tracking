package game;

/**
 * The three play modes from the original game.
 *
 * Each mode tunes a handful of parameters (lives, timer, spawn rate, presence
 * of bombs and special fruits). The values mirror the JavaScript MODE_CONFIGS
 * table from the web prototype.
 */
public enum GameMode {

    CLASSIC(
        "Classic",
        3,        // maxLives
        false, 0, // hasTimer, timerSeconds
        true,     // hasBombs
        BombPenalty.GAME_OVER,
        false,    // hasSpecialFruits
        true,     // hasLevels
        true,     // hasChapters
        1500,     // baseSpawnIntervalMs
        0.15,     // gravity
        12, 16    // minFruitSpeed, maxFruitSpeed
    ),
    ARCADE(
        "Arcade",
        999,
        true, 60,
        true,
        BombPenalty.POINTS,
        true,
        true,
        false,
        1300,
        0.15,
        12, 17
    ),
    ZEN(
        "Zen",
        999,
        true, 90,
        false,
        BombPenalty.NONE,
        false,
        false,
        false,
        1200,
        0.12,
        10, 14
    );

    public enum BombPenalty { GAME_OVER, POINTS, NONE }

    public final String displayName;
    public final int maxLives;
    public final boolean hasTimer;
    public final int timerSeconds;
    public final boolean hasBombs;
    public final BombPenalty bombPenalty;
    public final boolean hasSpecialFruits;
    public final boolean hasLevels;
    public final boolean hasChapters;
    public final int baseSpawnIntervalMs;
    public final double gravity;
    public final double minFruitSpeed;
    public final double maxFruitSpeed;

    GameMode(String displayName,
             int maxLives,
             boolean hasTimer, int timerSeconds,
             boolean hasBombs,
             BombPenalty bombPenalty,
             boolean hasSpecialFruits,
             boolean hasLevels,
             boolean hasChapters,
             int baseSpawnIntervalMs,
             double gravity,
             double minFruitSpeed, double maxFruitSpeed) {
        this.displayName = displayName;
        this.maxLives = maxLives;
        this.hasTimer = hasTimer;
        this.timerSeconds = timerSeconds;
        this.hasBombs = hasBombs;
        this.bombPenalty = bombPenalty;
        this.hasSpecialFruits = hasSpecialFruits;
        this.hasLevels = hasLevels;
        this.hasChapters = hasChapters;
        this.baseSpawnIntervalMs = baseSpawnIntervalMs;
        this.gravity = gravity;
        this.minFruitSpeed = minFruitSpeed;
        this.maxFruitSpeed = maxFruitSpeed;
    }

    public static final int ARCADE_BOMB_POINT_PENALTY = 10;
    public static final double ARCADE_SPECIAL_FRUIT_CHANCE = 0.08;
}
