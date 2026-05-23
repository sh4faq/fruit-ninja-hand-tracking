package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

import effects.BitmapBackground;
import effects.EffectsManager;
import effects.PetalField;
import effects.SoundManager;
import entities.Bomb;
import entities.CubeBoss;
import entities.DragonFruit;
import entities.EvaderFruit;
import entities.Fruit;
import entities.PolygonModel2D;
import entities.Pomegranate;
import entities.SpecialFruit;
import entities.TileMap;
import input.HandTrackingClient;
import input.InputManager;
import input.SliceLine;
import missions.Mission;
import missions.MissionTracker;
import ui.FontLoader;
import ui.GameOverScreen;
import ui.Hud;
import ui.MainMenu;
import ui.MissionsUI;

/**
 * Main playfield. Owns the game loop, the entities, and the top-level state.
 *
 * Render path:
 *   1. dark dojo background
 *   2. fruits, halves, bombs, special fruits (back to front by spawn order)
 *   3. effects (slice trails, splatters, score popups)
 *   4. overlay (menu, HUD, or game over)
 *
 * Update path each frame:
 *   1. input.update() builds a SliceLine if the pointer moved enough
 *   2. update entity physics (gravity)
 *   3. test the slice line against every fruit/bomb/special (line vs circle)
 *   4. spawn new fruits based on the mode's interval
 *   5. cull off-screen entities, losing a life if a fruit fell unsliced
 *   6. reset combo if no slice in the last COMBO_WINDOW_MS
 *
 * The combo system multiplies score for chained slices. Both the score popup
 * and the HUD combo splash use it.
 */
public class GamePanel extends GameBase {

    private static final long serialVersionUID = 1L;

    private static final long COMBO_WINDOW_MS = 1000;
    private static final int MIN_SCORE_FOR_BOMBS = 80;

    private static final Random RNG = new Random();
    private static final Preferences PREFS =
        Preferences.userRoot().node("/fruit_ninja_java");

    // --- top-level state ---
    private GameState state = GameState.MENU;
    private GameMode currentMode = GameMode.CLASSIC;

    // The mission the player is currently attempting, if any. Set when a
    // mission card is clicked in the missions screen; cleared on exit-to-menu.
    private Mission currentMission;

    private final MissionsUI missionsUI = new MissionsUI();

    // --- game objects ---
    private final List<Fruit> fruits = new ArrayList<>();
    private final List<Fruit> halves = new ArrayList<>();
    private final List<Bomb> bombs = new ArrayList<>();
    private final List<SpecialFruit> specials = new ArrayList<>();
    private final List<EvaderFruit> evaders = new ArrayList<>();
    private final List<CubeBoss> cubes = new ArrayList<>();
    private final List<Pomegranate> pomegranates = new ArrayList<>();
    private final List<DragonFruit> dragonFruits = new ArrayList<>();
    // Tracking for Arcade pomegranate spawn pacing + finale guarantee.
    private long lastPomSpawnMs;
    private boolean pomSpawnedThisRun;
    private static final long POM_SPAWN_INTERVAL_MS = 30_000;
    private static final double DRAGON_FRUIT_CHANCE = 0.005;

    // --- scoring / progression ---
    private int score;
    private int combo;
    private long lastSliceTimeMs;
    private int lives;
    private boolean godMode;
    private int level;
    private int fruitsSlicedThisLevel;
    private long lastSpawnTimeMs;
    private long spawnIntervalMs;
    private long lastEvaderSpawnMs;
    private long lastCubeSpawnMs;
    private static final int FRUITS_PER_LEVEL = 15;
    private static final long EVADER_SPAWN_INTERVAL_MS = 12_000;
    private static final long CUBE_SPAWN_INTERVAL_MS   = 22_000;
    private static final int  CUBE_SCORE_THRESHOLD     = 100;

    // --- timer (Arcade / Zen) ---
    private double timeRemainingSec;
    private long timerStartMs;

    // --- subsystems ---
    private final EffectsManager effects = new EffectsManager();
    private final InputManager input;
    private final HandTrackingClient handTracking;
    private final Hud hud = new Hud();
    private final MainMenu mainMenu = new MainMenu();
    private final GameOverScreen gameOverScreen = new GameOverScreen();
    private final PetalField petals = new PetalField(34);
    private final BitmapBackground parallax = new BitmapBackground();

    // Decorative shuriken loaded from a .model2D file. Spins on the menu and
    // game-over screens to showcase the polygon-model-from-file pattern
    // (Murphy's April 16 lecture: PolygonModel2D + BufferedReader + trim).
    private PolygonModel2D shuriken;

    // Dojo tile map rendered behind the chapter splash. Tiles are generated
    // procedurally; the layout is loaded from data/dojo.map at startup. This
    // is Murphy's March 17 lesson (TileMap + ASCII-index trick).
    private TileMap dojoMap;

    // --- game feel ---
    private double shakeIntensity;    // current pixel offset magnitude
    private long shakeUntilMs;        // wall-clock cutoff
    private long hitPauseUntilMs;     // freeze the simulation until this time
    private long slowMoUntilMs;       // multiply dtScale by SLOWMO_FACTOR until this time
    private static final double SLOWMO_FACTOR = 0.35;

    // --- Classic wave / chapter state machine ---
    private enum WaveState { CHAPTER_TRANSITION, SPAWNING, CLEARING, PAUSE, LEVEL_COMPLETE }
    private WaveState waveState = WaveState.SPAWNING;
    private boolean waveEnabled;
    private Chapter currentChapter;
    private int currentWave;        // 0-based
    private int totalWaves;
    private int fruitsInWave;
    private int fruitsSpawnedInWave;
    private int fruitsResolvedInWave;
    private long pauseEndMs;
    private long nextWaveSpawnMs;
    private long chapterTransitionUntilMs;
    private static final long WAVE_SPAWN_DELAY = 400;
    private static final long CHAPTER_TRANSITION_DURATION = 2600;

    public GamePanel() {
        setBackground(new Color(10, 8, 6));
        setFocusable(true);
        requestFocusInWindow();

        input = new InputManager(this);
        addMouseListener(input.mouseListener());
        addMouseMotionListener(input.mouseMotionListener());
        addKeyListener(input.keyListener());
        addMouseWheelListener(input.mouseWheelListener());

        input.setMenuCallback(this::onMenuKey);
        input.setClickCallback(this::onClick);
        input.setWheelCallback(this::onWheel);
        input.setDragCallback(this::onDrag);
        input.setReleaseCallback(this::onRelease);

        // Pre-load any sound effects found in assets/sounds/. Missing files
        // are silently ignored so the game runs cleanly with or without them.
        SoundManager.preload();

        // Register every Sword and Background skin so the shop and inventory
        // can look them up by id. Then make sure the starter sword and
        // starter background are owned (a fresh install has no save data).
        weapons.SwordCatalog.init();
        themes.BackgroundCatalog.init();
        if (!economy.Inventory.ownsSword(economy.Inventory.DEFAULT_SWORD)) {
            economy.Inventory.grantSword(economy.Inventory.DEFAULT_SWORD);
            economy.Inventory.equipSword(economy.Inventory.DEFAULT_SWORD);
        }
        if (!economy.Inventory.ownsBackground(economy.Inventory.DEFAULT_BG)) {
            economy.Inventory.grantBackground(economy.Inventory.DEFAULT_BG);
            economy.Inventory.equipBackground(economy.Inventory.DEFAULT_BG);
        }

        // Load the shuriken polygon model from data/shuriken.model2D. If the
        // file is missing or malformed we just skip the decoration; the game
        // still runs.
        try {
            shuriken = new PolygonModel2D(0, 0, 0);
            shuriken.load("data/shuriken.model2D");
            shuriken.outlineColor = new java.awt.Color(245, 200, 66);
            shuriken.fillColor    = new java.awt.Color(120, 80, 20, 130);
            shuriken.strokeWidth  = 3f;
        } catch (Exception ex) {
            shuriken = null;
        }

        // Build the procedurally generated dojo tile palette and load the
        // floor plan from data/dojo.map. If either step fails the chapter
        // splash falls back to plain darkness.
        try {
            dojoMap = new TileMap(TileMap.buildDojoTiles(64));
            dojoMap.load("data/dojo.map");
        } catch (Exception ex) {
            dojoMap = null;
        }

        // Start the hand tracking sidecar reader. If the Python script isn't
        // running yet the client will quietly retry every two seconds, so the
        // game still launches and falls back to mouse input cleanly.
        handTracking = new HandTrackingClient(input, this);
        handTracking.start();
    }

    // ====================================================================
    //  GAME FEEL TRIGGERS
    // ====================================================================

    /** Triggers a screen shake at the given intensity (px) for durationMs. */
    public void shake(double intensity, int durationMs) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeUntilMs   = Math.max(shakeUntilMs, System.currentTimeMillis() + durationMs);
    }

    /** Freezes the game world for `durationMs` to add weight to an impact. */
    public void hitPause(int durationMs) {
        hitPauseUntilMs = Math.max(hitPauseUntilMs,
                                   System.currentTimeMillis() + durationMs);
    }

    /** Slows the game world to SLOWMO_FACTOR speed for `durationMs`. */
    public void slowMo(int durationMs) {
        slowMoUntilMs = Math.max(slowMoUntilMs,
                                 System.currentTimeMillis() + durationMs);
    }

    private final shop.ShopUI shopUI = new shop.ShopUI();
    private final ui.SettingsUI settingsUI = new ui.SettingsUI();

    /**
     * Click handler for menu / shop / game-over screens.
     */
    private void onClick(int x, int y) {
        if (state == GameState.MENU) {
            // Shop button (top-right corner of the menu) opens the shop.
            if (mainMenu.shopButtonHit(x, y, getWidth(), getHeight())) {
                state = GameState.SHOP;
                return;
            }
            // Missions button: opens the missions screen.
            if (mainMenu.missionsButtonHit(x, y, getWidth(), getHeight())) {
                missionsUI.resetScroll();
                state = GameState.MISSIONS;
                return;
            }
            // Settings button: opens the sensitivity / smoothing / threshold sliders.
            if (mainMenu.settingsButtonHit(x, y, getWidth(), getHeight())) {
                state = GameState.SETTINGS;
                return;
            }
            int idx = mainMenu.hitTest(x, y, getWidth(), getHeight());
            if (idx == 0) startGame(GameMode.CLASSIC);
            else if (idx == 1) startGame(GameMode.ARCADE);
            else if (idx == 2) startGame(GameMode.ZEN);
        } else if (state == GameState.SHOP) {
            // Tab clicks switch the shop tab.
            shop.Shop.Tab newTab = shopUI.tabHit(x, y, getWidth());
            if (newTab != null) {
                shop.Shop.setTab(newTab);
                return;
            }
            // Card clicks buy or equip.
            String cardId = shopUI.cardHit(x, y);
            if (cardId != null) {
                if (shop.Shop.tab() == shop.Shop.Tab.SWORDS) {
                    shop.Shop.activateSword(cardId);
                } else {
                    shop.Shop.activateBackground(cardId);
                }
            }
        } else if (state == GameState.MISSIONS) {
            Mission m = missionsUI.cardHit(x, y);
            if (m != null) {
                // Launch the mission's mode for this attempt. CLASSIC fallback
                // for "any mode" missions so the player always gets a playable
                // run.
                GameMode launchMode = m.mode != null ? m.mode : GameMode.CLASSIC;
                startGame(launchMode);
                currentMission = m;
            }
        } else if (state == GameState.SETTINGS) {
            settingsUI.onMousePressed(x, y);
        } else if (state == GameState.GAME_OVER) {
            // Click anywhere on game-over to play again.
            startGame(currentMode);
        }
    }

    /** Mouse wheel handler. Drives the shop / missions scroll position. */
    private void onWheel(int rotation) {
        if (state == GameState.SHOP) {
            shop.Shop.scrollBy(rotation * 50);
        } else if (state == GameState.MISSIONS) {
            missionsUI.scrollBy(rotation * 50);
        }
    }

    /** Mouse drag handler. Used by the settings screen to drag slider thumbs. */
    private void onDrag(int x, int y) {
        if (state == GameState.SETTINGS) {
            settingsUI.onDrag(x, y);
        }
    }

    /** Mouse release handler. Stops slider dragging in settings. */
    private void onRelease() {
        if (state == GameState.SETTINGS) {
            settingsUI.onMouseReleased();
        }
    }

    /**
     * Concrete implementation of the abstract method from GameBase. Called
     * by the base class's run() once per frame at ~60 FPS. All game-specific
     * per-frame logic lives in update() below.
     */
    @Override
    protected void inGameLoop(double deltaMs) {
        update(deltaMs);
    }

    // ====================================================================
    //  STATE TRANSITIONS
    // ====================================================================

    public void startGame(GameMode mode) {
        currentMode = mode;
        state = GameState.PLAYING;
        // Starting a mode directly clears any previously-selected mission;
        // onClick re-assigns currentMission afterwards if launched from a card.
        currentMission = null;
        MissionTracker.startRun(mode);

        score = 0;
        combo = 0;
        lives = mode.maxLives;
        level = 1;
        fruitsSlicedThisLevel = 0;
        lastSpawnTimeMs = 0;
        spawnIntervalMs = mode.baseSpawnIntervalMs;
        lastSliceTimeMs = 0;

        fruits.clear();
        halves.clear();
        bombs.clear();
        specials.clear();
        evaders.clear();
        cubes.clear();
        pomegranates.clear();
        dragonFruits.clear();
        effects.clearTrails();
        lastEvaderSpawnMs = System.currentTimeMillis();
        lastCubeSpawnMs   = System.currentTimeMillis();
        lastPomSpawnMs    = System.currentTimeMillis();
        pomSpawnedThisRun = false;
        bombDeflectCharges = 0;
        peachyTimeUntilMs = 0;

        if (mode.hasTimer) {
            timeRemainingSec = mode.timerSeconds;
            timerStartMs = System.currentTimeMillis();
        }

        // Wave / chapter setup for Classic
        waveEnabled = mode.hasChapters;
        if (waveEnabled) {
            beginChapterFor(1);
        }

        SoundManager.play(SoundManager.START);
    }

    // ====================================================================
    //  WAVE / CHAPTER STATE MACHINE
    // ====================================================================

    private void beginChapterFor(int level) {
        currentChapter = Chapter.getForLevel(level);
        chapterTransitionUntilMs = System.currentTimeMillis() + CHAPTER_TRANSITION_DURATION;
        waveState = WaveState.CHAPTER_TRANSITION;
        currentWave = 0;
    }

    private void initLevelWaves(int level) {
        Chapter c = Chapter.getForLevel(level);
        currentChapter = c;
        int idx = c.indexOfLevel(level);
        totalWaves = c.wavesPerLevel[Math.min(idx, c.wavesPerLevel.length - 1)];
        currentWave = 0;
        startWave();
    }

    private void startWave() {
        Chapter c = currentChapter;
        int idx = c.indexOfLevel(level);
        int min = c.fruitsPerWave[Math.min(idx, c.fruitsPerWave.length - 1)];
        int max = c.fruitsPerWaveMax[Math.min(idx, c.fruitsPerWaveMax.length - 1)];
        fruitsInWave = min + RNG.nextInt(Math.max(1, max - min + 1));
        fruitsSpawnedInWave = 0;
        fruitsResolvedInWave = 0;
        nextWaveSpawnMs = System.currentTimeMillis() + 300;
        waveState = WaveState.SPAWNING;
    }

    private void updateWaveSystem(long nowMs) {
        switch (waveState) {
            case CHAPTER_TRANSITION:
                if (nowMs >= chapterTransitionUntilMs) {
                    initLevelWaves(level);
                }
                break;
            case SPAWNING:
                if (nowMs >= nextWaveSpawnMs && fruitsSpawnedInWave < fruitsInWave) {
                    spawnWaveFruit();
                    fruitsSpawnedInWave++;
                    nextWaveSpawnMs = nowMs + WAVE_SPAWN_DELAY;
                }
                if (fruitsSpawnedInWave >= fruitsInWave) {
                    waveState = WaveState.CLEARING;
                }
                break;
            case CLEARING:
                if (fruitsResolvedInWave >= fruitsInWave) {
                    if (currentWave + 1 < totalWaves) {
                        waveState = WaveState.PAUSE;
                        pauseEndMs = nowMs + currentChapter.wavePauseMs;
                    } else {
                        waveState = WaveState.LEVEL_COMPLETE;
                    }
                }
                break;
            case PAUSE:
                if (nowMs >= pauseEndMs) {
                    currentWave++;
                    startWave();
                }
                break;
            case LEVEL_COMPLETE:
                advanceLevelChapter();
                break;
        }
    }

    private void advanceLevelChapter() {
        level++;
        if (Chapter.isNewChapterStart(level)) {
            beginChapterFor(level);
        } else {
            initLevelWaves(level);
        }
    }

    private void spawnWaveFruit() {
        Fruit f = new Fruit(getWidthSafe(), getHeightSafe());
        // Speed scales gently with level
        double speedMult = 1.0 + (level - 1) * 0.05;
        f.vx *= speedMult;
        f.vy *= speedMult;
        fruits.add(f);

        // Wave-mode bombs: rising chance with level once a score floor is met
        if (currentMode.hasBombs && score >= MIN_SCORE_FOR_BOMBS) {
            double bombChance = 0.05 + Math.min(0.20, (level - 1) * 0.018);
            if (RNG.nextDouble() < bombChance) {
                Bomb b = new Bomb(getWidthSafe(), getHeightSafe());
                b.vx *= speedMult;
                b.vy *= speedMult;
                bombs.add(b);
            }
        }
    }

    public void endGame() {
        state = GameState.GAME_OVER;
        int best = bestScoreForMode(currentMode);
        if (score > best) {
            best = score;
            PREFS.putInt(bestKey(currentMode), score);
        }
        gameOverScreen.show(currentMode, score, level);
        gameOverScreen.setBestScore(best);

        // Mission evaluation: grade the current attempt against the chosen
        // mission's thresholds and persist the best star count seen so far.
        if (currentMission != null) {
            int stars = MissionTracker.evaluate(currentMission, score);
            if (stars > 0) {
                economy.Inventory.recordStars(currentMission.id, stars);
            }
        }
    }

    public void showMenu() {
        state = GameState.MENU;
        // Exit-to-menu clears any active mission attempt so the next run
        // started from the main menu isn't accidentally graded.
        currentMission = null;
    }

    private int bestScoreForMode(GameMode mode) {
        return PREFS.getInt(bestKey(mode), 0);
    }

    private static String bestKey(GameMode mode) {
        return "best_" + mode.name().toLowerCase();
    }

    private void onMenuKey(int keyCode) {
        if (state == GameState.MENU) {
            switch (keyCode) {
                case java.awt.event.KeyEvent.VK_1: startGame(GameMode.CLASSIC); break;
                case java.awt.event.KeyEvent.VK_2: startGame(GameMode.ARCADE); break;
                case java.awt.event.KeyEvent.VK_3: startGame(GameMode.ZEN);     break;
                case java.awt.event.KeyEvent.VK_S: state = GameState.SHOP;     break;
                case java.awt.event.KeyEvent.VK_M:
                    missionsUI.resetScroll();
                    state = GameState.MISSIONS;
                    break;
                case java.awt.event.KeyEvent.VK_T:
                    state = GameState.SETTINGS;
                    break;
                default: break;
            }
        } else if (state == GameState.SETTINGS) {
            if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) showMenu();
            else if (keyCode == java.awt.event.KeyEvent.VK_LEFT)  settingsUI.onArrowKey(-1);
            else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT) settingsUI.onArrowKey(+1);
            else if (keyCode == java.awt.event.KeyEvent.VK_TAB)   settingsUI.onTabKey();
            else if (keyCode == java.awt.event.KeyEvent.VK_R)     settingsUI.resetDefaults();
        } else if (state == GameState.SHOP) {
            if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) showMenu();
            else if (keyCode == java.awt.event.KeyEvent.VK_LEFT)  shop.Shop.setTab(shop.Shop.Tab.SWORDS);
            else if (keyCode == java.awt.event.KeyEvent.VK_RIGHT) shop.Shop.setTab(shop.Shop.Tab.BACKGROUNDS);
            else if (keyCode == java.awt.event.KeyEvent.VK_UP)    shop.Shop.scrollBy(-80);
            else if (keyCode == java.awt.event.KeyEvent.VK_DOWN)  shop.Shop.scrollBy(80);
        } else if (state == GameState.MISSIONS) {
            if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) showMenu();
            else if (keyCode == java.awt.event.KeyEvent.VK_UP)   missionsUI.scrollBy(-80);
            else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) missionsUI.scrollBy(80);
        } else if (state == GameState.GAME_OVER) {
            if (keyCode == java.awt.event.KeyEvent.VK_ENTER) startGame(currentMode);
            else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) showMenu();
        } else if (state == GameState.PLAYING) {
            if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) showMenu();
            else if (keyCode == java.awt.event.KeyEvent.VK_G) {
                godMode = !godMode;
                effects.addComboBanner(
                    godMode ? "GOD MODE ON" : "GOD MODE OFF", 0);
            }
        }
    }

    // ====================================================================
    //  UPDATE
    // ====================================================================

    private void update(double deltaMs) {
        input.update();

        // Even in the menu we want the trail to follow the pointer so the user
        // can see the blade.
        if (input.isPointerActive()) {
            effects.addTrailPoint(1, input.pointerX(), input.pointerY());
        }

        long nowMs = System.currentTimeMillis();

        // Decay the shake intensity smoothly so it eases out instead of
        // stopping abruptly when the timer expires.
        if (nowMs >= shakeUntilMs) shakeIntensity *= 0.85;
        if (shakeIntensity < 0.1) shakeIntensity = 0;

        // Hit pause: keep redrawing but freeze the simulation. We still update
        // the petal field and effects so the menu doesn't visibly hitch.
        if (nowMs < hitPauseUntilMs) {
            petals.update(deltaMs / 16.67, getWidthSafe(), getHeightSafe());
            effects.update(deltaMs * 0.05);
            return;
        }

        // Slow motion scales the per-frame delta for everything that follows.
        if (nowMs < slowMoUntilMs) deltaMs *= SLOWMO_FACTOR;

        petals.update(deltaMs / 16.67, getWidthSafe(), getHeightSafe());
        // Tick the equipped background skin so its parallax / particles drift.
        themes.Background eqBg = themes.BackgroundCatalog.get(
            economy.Inventory.equippedBackground());
        if (eqBg != null) eqBg.update(deltaMs);
        else parallax.update(deltaMs);
        effects.update(deltaMs);

        // Spin the decorative shuriken (degrees per frame, scaled by dt).
        if (shuriken != null) {
            shuriken.turnRight(2.0 * (deltaMs / 16.67));
        }

        if (state == GameState.MENU) {
            mainMenu.tick((int) input.pointerX(), (int) input.pointerY(),
                          getWidthSafe(), getHeightSafe());
            return;
        }

        if (state == GameState.SHOP) {
            shopUI.updateHover((int) input.pointerX(), (int) input.pointerY());
            return;
        }

        if (state == GameState.MISSIONS) {
            missionsUI.updateHover((int) input.pointerX(), (int) input.pointerY());
            return;
        }

        if (state != GameState.PLAYING) return;

        // Timer modes
        if (currentMode.hasTimer) {
            double elapsedSec = (nowMs - timerStartMs) / 1000.0;
            timeRemainingSec = Math.max(0, currentMode.timerSeconds - elapsedSec);
            if (timeRemainingSec <= 0) {
                endGame();
                return;
            }
        }

        if (waveEnabled) {
            updateWaveSystem(nowMs);
        } else {
            // Continuous spawn for Arcade / Zen. Frenzy cuts the interval.
            long effectiveInterval = frenzyActive(nowMs)
                ? spawnIntervalMs / 2
                : spawnIntervalMs;
            if (nowMs - lastSpawnTimeMs > effectiveInterval) {
                spawnFruits();
                lastSpawnTimeMs = nowMs;
            }
        }

        // Update entities. Freeze powerup multiplies dtScale by FREEZE_FACTOR
        // so falling fruits / bombs visibly slow.
        double dtScale = Math.max(0.25, Math.min(3.0, deltaMs / 16.67));
        if (freezeActive(nowMs)) dtScale *= 0.4;
        for (Fruit f : fruits)  f.update(dtScale, currentMode.gravity);
        for (Fruit h : halves)  h.update(dtScale, currentMode.gravity);
        for (Bomb b : bombs)    b.update(dtScale, currentMode.gravity);
        for (SpecialFruit s : specials) s.update(dtScale, currentMode.gravity);
        for (Pomegranate p : pomegranates) p.update(dtScale, currentMode.gravity);
        for (DragonFruit d : dragonFruits) d.update(dtScale, currentMode.gravity);

        // Evader fruits use the player's pointer as their threat target.
        // The cross-product steering math is in EvaderFruit.update.
        double threatX = input.pointerX();
        double threatY = input.pointerY();
        boolean threatActive = input.isPointerActive();
        for (EvaderFruit e : evaders) {
            e.update(dtScale, threatX, threatY, threatActive);
        }

        // Occasionally spawn a new evader so the player has something to chase.
        if (nowMs - lastEvaderSpawnMs > EVADER_SPAWN_INTERVAL_MS
                && evaders.size() < 2) {
            evaders.add(new EvaderFruit(getWidthSafe(), getHeightSafe()));
            lastEvaderSpawnMs = nowMs;
        }

        // Cube boss enemies (the 3D showcase) spawn once the player has built
        // up a bit of score. Tumbles through the playfield, takes multiple
        // hits to defeat, demonstrates X/Y/Z rotation + back-face removal +
        // directional lighting (Murphy's May 5, 7, 12 lessons).
        for (CubeBoss c : cubes) c.update(dtScale, currentMode.gravity);
        if (score >= CUBE_SCORE_THRESHOLD
                && nowMs - lastCubeSpawnMs > CUBE_SPAWN_INTERVAL_MS
                && cubes.size() < 1) {
            cubes.add(new CubeBoss(getWidthSafe(), getHeightSafe()));
            lastCubeSpawnMs = nowMs;
        }

        // Pomegranate spawning: only in Arcade. One every ~30s, plus a
        // guaranteed finale appearance in the final 10 seconds of the run
        // if none has spawned yet.
        if (currentMode == GameMode.ARCADE) {
            boolean periodic = nowMs - lastPomSpawnMs > POM_SPAWN_INTERVAL_MS
                            && pomegranates.isEmpty();
            boolean finale = !pomSpawnedThisRun
                          && currentMode.hasTimer
                          && timeRemainingSec > 0 && timeRemainingSec < 10
                          && pomegranates.isEmpty();
            if (periodic || finale) {
                pomegranates.add(new Pomegranate(getWidthSafe(), getHeightSafe()));
                lastPomSpawnMs = nowMs;
                pomSpawnedThisRun = true;
            }
        }

        // Slice detection
        SliceLine line = input.consumeSliceLine();
        if (line != null) checkSlices(line, nowMs);

        // Cull off-screen entities
        cull();

        // Reset combo
        if (combo > 0 && nowMs - lastSliceTimeMs > COMBO_WINDOW_MS) combo = 0;

        // Level up (continuous spawn modes only)
        if (currentMode.hasLevels && fruitsSlicedThisLevel >= FRUITS_PER_LEVEL) {
            level++;
            fruitsSlicedThisLevel = 0;
            // Slightly tighter spawn at higher levels
            spawnIntervalMs = (long) Math.max(400,
                currentMode.baseSpawnIntervalMs * Math.pow(0.93, level - 1));
        }
    }

    private void checkSlices(SliceLine sLine, long nowMs) {
        int slicedThisFrame = 0;

        // Build the swipe as a Line so we can measure how close each fruit
        // was cut to its center. This is exactly the signed-shortest-distance
        // formula from Brian's April 23 / April 30 lectures, using a unit
        // normal and the pre-computed C = -(N . A).
        math.Line slice = new math.Line(sLine.x1, sLine.y1, sLine.x2, sLine.y2);

        // Fruits
        for (int i = fruits.size() - 1; i >= 0; i--) {
            Fruit f = fruits.get(i);
            if (f.isSliced) continue;
            if (f.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                Fruit[] cut = f.slice(sLine.dx(), sLine.dy());
                halves.add(cut[0]);
                halves.add(cut[1]);
                effects.addSplatter(f.x, f.y, f.type.color);
                slicedThisFrame++;

                // Cut quality: distance from fruit center to the slice line,
                // normalized by the fruit's depth-scaled radius. Closer to 0
                // means we cut straight through the middle ("clean" slice).
                double dist = Math.abs(slice.distanceTo(f.x, f.y));
                double visualR = f.radius * f.depthScale();
                boolean cleanCut = dist < visualR * 0.3;

                int basePts = f.type.points;
                int pts = combo > 1 ? basePts * combo : basePts;
                if (doublePointsActive(nowMs)) pts *= 2;
                if (cleanCut) pts += 5;   // perfect-cut bonus

                effects.addScorePopup(f.x, f.y, pts, combo > 1 || cleanCut);
                score += pts;
                // Coins: 1 per point earned (so apple=1, watermelon=3, with
                // combo + clean-cut bonuses already baked into pts).
                economy.Currency.add(pts);
                MissionTracker.onCoinsEarned(pts);
                MissionTracker.onFruitSliced(f.type);
                if (cleanCut) MissionTracker.onCriticalHit();
                fruitsSlicedThisLevel++;
                if (waveEnabled && (waveState == WaveState.SPAWNING
                                  || waveState == WaveState.CLEARING)) {
                    fruitsResolvedInWave++;
                }

                // PEACHY TIME powerup: slicing a peach grants timer time.
                if (peachyTimeActive(nowMs) && currentMode.hasTimer
                        && f.type == entities.FruitType.PEACH) {
                    timeRemainingSec += 2.0;
                    // Push the start back so the bar reflects the bonus.
                    timerStartMs -= 2000;
                    effects.addScorePopup(f.x, f.y - 30, 2, true);
                }

                // Critical hit: 8% chance of a bonus on regular fruit slices.
                if (RNG.nextDouble() < 0.08) {
                    score += 10;
                    economy.Currency.add(10);
                    // CRIT! popup in red, slightly above the fruit. We pass
                    // the bonus as the score so the popup reads "+10".
                    effects.addScorePopup(f.x, f.y - 36, 10, true);
                    shake(4, 80);
                }

                fruits.remove(i);
                SoundManager.play(SoundManager.SLICE);
                SoundManager.play(SoundManager.SPLAT, 0.6f);
                hitPause(cleanCut ? 60 : 35);
                shake(cleanCut ? 8 : 4, cleanCut ? 120 : 80);
            }
        }

        // Pomegranate: multi-hit fruit. Each slice grants HIT_POINTS, the
        // final slice grants FINALE_BONUS and a screen-rattle finale.
        for (int i = pomegranates.size() - 1; i >= 0; i--) {
            Pomegranate p = pomegranates.get(i);
            if (p.dead) continue;
            if (p.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                boolean killed = p.registerHit();
                effects.addSplatter(p.x, p.y, new Color(220, 30, 30));
                int gain = Pomegranate.HIT_POINTS;
                if (doublePointsActive(nowMs)) gain *= 2;
                score += gain;
                economy.Currency.add(gain);
                effects.addScorePopup(p.x, p.y, gain, false);
                shake(7, 140);
                SoundManager.play(SoundManager.SLICE);
                if (killed) {
                    int finale = Pomegranate.FINALE_BONUS;
                    if (doublePointsActive(nowMs)) finale *= 2;
                    score += finale;
                    economy.Currency.add(finale);
                    // Massive juice splatter: queue several at once
                    for (int j = 0; j < 5; j++) {
                        effects.addSplatter(
                            p.x + (RNG.nextDouble() - 0.5) * 60,
                            p.y + (RNG.nextDouble() - 0.5) * 60,
                            new Color(255, 60, 80));
                    }
                    effects.addComboBanner("POMEGRANATE!", finale);
                    shake(28, 600);
                    slowMo(380);
                    SoundManager.play(SoundManager.BOMB);
                    MissionTracker.onPomegranateDestroyed();
                    pomegranates.remove(i);
                }
            }
        }

        // Dragon Fruit: single slice, big payout.
        for (int i = dragonFruits.size() - 1; i >= 0; i--) {
            DragonFruit d = dragonFruits.get(i);
            if (d.isSliced) continue;
            if (d.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                d.isSliced = true;
                effects.addSplatter(d.x, d.y, new Color(255, 80, 180));
                int pts = DragonFruit.POINT_VALUE;
                if (doublePointsActive(nowMs)) pts *= 2;
                score += pts;
                economy.Currency.add(DragonFruit.COIN_VALUE);
                MissionTracker.onCoinsEarned(DragonFruit.COIN_VALUE);
                effects.addScorePopup(d.x, d.y, pts, true);
                effects.addComboBanner("DRAGON FRUIT!", pts);
                shake(16, 320);
                slowMo(260);
                SoundManager.play(SoundManager.COMBO);
                dragonFruits.remove(i);
                slicedThisFrame++;
            }
        }

        // Cube bosses (3D rotating enemies). Each successful slice removes one
        // hit; when the boss is defeated, big point bonus + slow-mo splash.
        for (int i = cubes.size() - 1; i >= 0; i--) {
            CubeBoss c = cubes.get(i);
            if (c.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                boolean defeated = c.registerHit();
                effects.addSplatter(c.x, c.y, new Color(255, 200, 80));
                shake(12, 200);
                if (defeated) {
                    score += 50;
                    economy.Currency.add(50);
                    effects.addScorePopup(c.x, c.y, 50, true);
                    MissionTracker.onCubeDefeated();
                    MissionTracker.onCoinsEarned(50);
                    cubes.remove(i);
                    SoundManager.play(SoundManager.BOMB);
                    shake(24, 500);
                    slowMo(320);
                } else {
                    score += 10;
                    economy.Currency.add(10);
                    MissionTracker.onCoinsEarned(10);
                    effects.addScorePopup(c.x, c.y, 10, false);
                    SoundManager.play(SoundManager.SLICE);
                }
            }
        }

        // Evader fruits (cross-product AI runners). Big bonus for catching one.
        for (int i = evaders.size() - 1; i >= 0; i--) {
            EvaderFruit e = evaders.get(i);
            if (e.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                e.isSliced = true;
                effects.addSplatter(e.x, e.y, new Color(120, 220, 150));
                score += 25;
                economy.Currency.add(25);
                MissionTracker.onCoinsEarned(25);
                MissionTracker.onEvaderCaught();
                effects.addScorePopup(e.x, e.y, 25, true);
                evaders.remove(i);
                SoundManager.play(SoundManager.COMBO);
                shake(10, 220);
                slowMo(180);
            }
        }

        // Special fruits (Arcade powerups)
        for (int i = specials.size() - 1; i >= 0; i--) {
            SpecialFruit s = specials.get(i);
            if (s.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                s.isSliced = true;
                effects.addSplatter(s.x, s.y, new Color(255, 255, 255));
                activatePowerup(s.kind, nowMs);
                specials.remove(i);
                SoundManager.play(SoundManager.COMBO);
                shake(8, 200);
            }
        }

        // Bombs
        for (int i = bombs.size() - 1; i >= 0; i--) {
            Bomb b = bombs.get(i);
            if (b.isSliced) continue;
            if (b.checkSlice(sLine.x1, sLine.y1, sLine.x2, sLine.y2)) {
                b.isSliced = true;
                effects.addSplatter(b.x, b.y, new Color(255, 80, 30));
                SoundManager.play(SoundManager.BOMB);
                shake(18, 400);
                hitPause(120);
                MissionTracker.onBombSliced();
                if (bombDeflectCharges > 0) {
                    // Absorbed by the BOMB DEFLECT powerup.
                    bombDeflectCharges--;
                    effects.addComboBanner("BOMB DEFLECTED!", 0);
                    bombs.remove(i);
                    continue;
                }
                if (!godMode) {
                    if (currentMode.bombPenalty == GameMode.BombPenalty.GAME_OVER) {
                        endGame();
                        return;
                    } else if (currentMode.bombPenalty == GameMode.BombPenalty.POINTS) {
                        score = Math.max(0, score - GameMode.ARCADE_BOMB_POINT_PENALTY);
                        effects.addScorePopup(b.x, b.y,
                            -GameMode.ARCADE_BOMB_POINT_PENALTY, false);
                    }
                }
                bombs.remove(i);
            }
        }

        if (slicedThisFrame > 0) {
            if (nowMs - lastSliceTimeMs < COMBO_WINDOW_MS) combo += slicedThisFrame;
            else combo = slicedThisFrame;
            lastSliceTimeMs = nowMs;
            MissionTracker.onComboReached(combo);

            if (combo >= 3) {
                slowMo(220);
                SoundManager.play(SoundManager.COMBO);
            }
        }

        // Multi-slice in a single swipe: big banner + bonus points.
        // Three fruits = +30, four = +40, etc.
        if (slicedThisFrame >= 3) {
            int bonus = slicedThisFrame * 10;
            if (doublePointsActive(nowMs)) bonus *= 2;
            score += bonus;
            economy.Currency.add(bonus);
            MissionTracker.onCoinsEarned(bonus);
            MissionTracker.onMultiSlice(slicedThisFrame);
            effects.addComboBanner("FRUIT FRENZY +" + bonus + "!", bonus);
            shake(12, 260);
            slowMo(220);
        }
    }

    // ====================================================================
    //  POWERUPS (Arcade)
    // ====================================================================

    private long freezeUntilMs;
    private long frenzyUntilMs;
    private long doubleUntilMs;
    private long peachyTimeUntilMs;
    private int  bombDeflectCharges;
    private static final int BOMB_DEFLECT_MAX = 3;

    private void activatePowerup(SpecialFruit.Kind kind, long nowMs) {
        switch (kind) {
            case FREEZE: freezeUntilMs = nowMs + 5000; break;
            case FRENZY: frenzyUntilMs = nowMs + 5000; break;
            case DOUBLE: doubleUntilMs = nowMs + 8000; break;
            case BERRY_BLAST:  activateBerryBlast(); break;
            case PEACHY_TIME:  peachyTimeUntilMs = nowMs + 8000; break;
            case BOMB_DEFLECT: bombDeflectCharges =
                Math.min(BOMB_DEFLECT_MAX, bombDeflectCharges + BOMB_DEFLECT_MAX); break;
        }
    }

    public boolean freezeActive(long nowMs) { return nowMs < freezeUntilMs; }
    public boolean frenzyActive(long nowMs) { return nowMs < frenzyUntilMs; }
    public boolean doublePointsActive(long nowMs) { return nowMs < doubleUntilMs; }
    public boolean peachyTimeActive(long nowMs)   { return nowMs < peachyTimeUntilMs; }

    /**
     * Berry Blast: every fruit currently in flight is converted into a
     * strawberry (here just an apple-shape stand-in colored red), then
     * auto-exploded for a small flat bonus apiece. Halves and other entity
     * types are unaffected.
     */
    private void activateBerryBlast() {
        if (fruits.isEmpty()) return;
        int total = 0;
        for (int i = fruits.size() - 1; i >= 0; i--) {
            Fruit f = fruits.get(i);
            // Splatter using a strawberry-red regardless of original color
            effects.addSplatter(f.x, f.y, new java.awt.Color(231, 39, 60));
            effects.addScorePopup(f.x, f.y, 5, false);
            score += 5;
            economy.Currency.add(5);
            total += 5;
            fruits.remove(i);
        }
        if (total > 0) {
            shake(10, 320);
            slowMo(180);
            effects.addComboBanner("BERRY BLAST!", total);
        }
    }

    private void spawnFruits() {
        int w = getWidthSafe();
        int h = getHeightSafe();
        int count = 1 + RNG.nextInt(2);
        for (int i = 0; i < count; i++) {
            // Each regular fruit spawn rolls for a Dragon Fruit substitution.
            if (RNG.nextDouble() < DRAGON_FRUIT_CHANCE) {
                dragonFruits.add(new DragonFruit(w, h));
            } else {
                fruits.add(new Fruit(w, h));
            }
        }

        if (currentMode.hasBombs && score >= MIN_SCORE_FOR_BOMBS) {
            double bombChance = 0.06 + Math.min(0.18, (level - 1) * 0.015);
            if (RNG.nextDouble() < bombChance) {
                bombs.add(new Bomb(w, h));
            }
        }

        if (currentMode.hasSpecialFruits
                && RNG.nextDouble() < GameMode.ARCADE_SPECIAL_FRUIT_CHANCE) {
            SpecialFruit.Kind[] kinds = SpecialFruit.Kind.values();
            specials.add(new SpecialFruit(w, h, kinds[RNG.nextInt(kinds.length)]));
        }
    }

    private void cull() {
        int w = getWidthSafe();
        int h = getHeightSafe();
        for (int i = fruits.size() - 1; i >= 0; i--) {
            Fruit f = fruits.get(i);
            if (f.isOffScreen(w, h)) {
                if (!f.isSliced) {
                    if (currentMode.maxLives <= 10) loseLife();
                    if (waveEnabled && (waveState == WaveState.SPAWNING
                                      || waveState == WaveState.CLEARING)) {
                        fruitsResolvedInWave++;
                    }
                }
                fruits.remove(i);
            }
        }
        halves.removeIf(h2 -> h2.isOffScreen(w, h));
        bombs.removeIf(b -> b.isOffScreen(w, h));
        specials.removeIf(s -> s.isOffScreen(w, h));
        evaders.removeIf(e -> e.isOffScreen(w, h));
        cubes.removeIf(c -> c.isOffScreen(w, h));
        pomegranates.removeIf(p -> p.isOffScreen(w, h));
        dragonFruits.removeIf(d -> d.isOffScreen(w, h));
    }

    public void loseLife() {
        if (godMode) return;
        lives--;
        if (lives <= 0) endGame();
    }

    // ====================================================================
    //  RENDER
    // ====================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Screen shake offset applied to the world layer only, so the HUD
        // stays rock-steady while the playfield jolts on impacts.
        double shakeX = 0, shakeY = 0;
        if (shakeIntensity > 0) {
            shakeX = (Math.random() - 0.5) * 2 * shakeIntensity;
            shakeY = (Math.random() - 0.5) * 2 * shakeIntensity;
        }

        drawBackground(g2, w, h);

        Graphics2D world = (Graphics2D) g2.create();
        world.translate(shakeX, shakeY);

        petals.draw(world);

        // Painter's algorithm: collect every entity, sort by z descending
        // (deepest first), then draw. This makes closer fruits cover deeper
        // ones and gives the playfield real depth ordering.
        java.util.List<Object> drawOrder = new java.util.ArrayList<>(
            fruits.size() + halves.size() + bombs.size() + specials.size()
            + pomegranates.size() + dragonFruits.size());
        drawOrder.addAll(fruits);
        drawOrder.addAll(halves);
        drawOrder.addAll(bombs);
        drawOrder.addAll(specials);
        drawOrder.addAll(evaders);
        drawOrder.addAll(pomegranates);
        drawOrder.addAll(dragonFruits);
        drawOrder.sort((a, b) -> Double.compare(entityZ(b), entityZ(a)));
        for (Object o : drawOrder) {
            if (o instanceof Fruit) ((Fruit) o).draw(world);
            else if (o instanceof Bomb) ((Bomb) o).draw(world);
            else if (o instanceof SpecialFruit) ((SpecialFruit) o).draw(world);
            else if (o instanceof EvaderFruit) ((EvaderFruit) o).draw(world);
            else if (o instanceof Pomegranate) ((Pomegranate) o).draw(world);
            else if (o instanceof DragonFruit) ((DragonFruit) o).draw(world);
        }

        // Cube bosses draw separately because they have their own internal
        // per-face painter's sort and back-face removal pipeline. Origin
        // passed in so the perspective transform projects relative to screen
        // center (matching Murphy's "centering 3D origin on screen" step).
        for (CubeBoss c : cubes) {
            c.draw(world, 0, 0);
        }

        effects.draw(world);
        world.dispose();

        // Combo banner draws in screen space, above the vignette, so the
        // shake doesn't drag it around.
        effects.drawOverlay(g2, w, h);

        drawVignette(g2, w, h);

        switch (state) {
            case MENU:
                mainMenu.draw(g2, w, h);
                drawHandStatusPill(g2, w, h);
                break;
            case SHOP:
                shopUI.draw(g2, w, h);
                break;
            case MISSIONS:
                missionsUI.draw(g2, w, h);
                break;
            case SETTINGS:
                settingsUI.draw(g2, w, h);
                break;
            case PLAYING:
                long nowMs = System.currentTimeMillis();
                hud.draw(g2, w, h, currentMode, score, combo, lives, level,
                         timeRemainingSec,
                         freezeActive(nowMs), frenzyActive(nowMs),
                         doublePointsActive(nowMs),
                         Math.max(0, freezeUntilMs - nowMs),
                         Math.max(0, frenzyUntilMs - nowMs),
                         Math.max(0, doubleUntilMs - nowMs),
                         peachyTimeActive(nowMs),
                         Math.max(0, peachyTimeUntilMs - nowMs),
                         bombDeflectCharges);
                if (waveEnabled && waveState == WaveState.CHAPTER_TRANSITION) {
                    drawChapterTransition(g2, w, h, nowMs);
                }
                if (godMode) {
                    g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 18f));
                    String label = "GOD MODE";
                    int tw = g2.getFontMetrics().stringWidth(label);
                    int gx = w - tw - 18;
                    int gy = h - 18;
                    g2.setColor(new Color(0, 0, 0, 140));
                    g2.fillRoundRect(gx - 10, gy - 22, tw + 20, 32, 10, 10);
                    g2.setColor(new Color(255, 215, 80));
                    g2.drawString(label, gx, gy);
                }
                break;
            case GAME_OVER:
                gameOverScreen.draw(g2, w, h);
                break;
        }

        g2.dispose();
    }

    /** Z accessor used by the painter's-algorithm sort. */
    private static double entityZ(Object o) {
        if (o instanceof Fruit) return ((Fruit) o).z;
        if (o instanceof Bomb) return ((Bomb) o).z;
        if (o instanceof SpecialFruit) return ((SpecialFruit) o).z;
        if (o instanceof EvaderFruit) return ((EvaderFruit) o).z;
        if (o instanceof Pomegranate) return ((Pomegranate) o).z;
        if (o instanceof DragonFruit) return ((DragonFruit) o).z;
        return 0;
    }

    private void drawChapterTransition(Graphics2D g, int w, int h, long nowMs) {
        long remaining = Math.max(0, chapterTransitionUntilMs - nowMs);
        double elapsed = (CHAPTER_TRANSITION_DURATION - remaining)
                       / (double) CHAPTER_TRANSITION_DURATION;

        // Fade in over first 20%, hold, fade out over last 20%
        double alpha;
        if (elapsed < 0.2) alpha = elapsed / 0.2;
        else if (elapsed > 0.8) alpha = (1.0 - elapsed) / 0.2;
        else alpha = 1.0;
        alpha = Math.max(0, Math.min(1, alpha));

        int dimAlpha = (int) (alpha * 200);
        g.setColor(new Color(0, 0, 0, dimAlpha));
        g.fillRect(0, 0, w, h);

        // (Tile map deliberately removed from the chapter splash: the dense
        // wood-grain pattern competed too hard with the title text. The
        // TileMap class is still loaded and the code-walkthrough video can
        // demo it from data/dojo.map directly.)

        // Slide up from below as the chapter appears
        int yOffset = (int) ((1 - alpha) * 24);

        Color accentTint = new Color(currentChapter.accent.getRed(),
                                     currentChapter.accent.getGreen(),
                                     currentChapter.accent.getBlue(),
                                     (int) (alpha * 255));
        Color creamTint = new Color(255, 238, 221, (int) (alpha * 220));
        Color whiteTint = new Color(255, 255, 255, (int) (alpha * 255));

        g.setColor(creamTint);
        g.setFont(FontLoader.title(java.awt.Font.BOLD, 22));
        String label = "CHAPTER " + Chapter.toRoman(currentChapter.id);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(label);
        g.drawString(label, (w - lw) / 2, h / 2 - 70 + yOffset);

        g.setColor(accentTint);
        g.setFont(FontLoader.title(java.awt.Font.BOLD, 64));
        fm = g.getFontMetrics();
        int nw = fm.stringWidth(currentChapter.name);
        g.drawString(currentChapter.name, (w - nw) / 2, h / 2 + yOffset);

        // Decorative slash
        g.setColor(new Color(currentChapter.accent.getRed(),
                             currentChapter.accent.getGreen(),
                             currentChapter.accent.getBlue(),
                             (int) (alpha * 200)));
        g.setStroke(new java.awt.BasicStroke(3f));
        g.drawLine(w / 2 - 110, h / 2 + 30 + yOffset,
                   w / 2 + 110, h / 2 + 30 + yOffset);

        g.setColor(whiteTint);
        g.setFont(FontLoader.body(java.awt.Font.ITALIC, 20));
        fm = g.getFontMetrics();
        int tw = fm.stringWidth(currentChapter.tagline);
        g.drawString(currentChapter.tagline, (w - tw) / 2, h / 2 + 70 + yOffset);
    }

    private void drawVignette(Graphics2D g, int w, int h) {
        float[] dist  = { 0.55f, 1.0f };
        Color[] cols  = { new Color(0, 0, 0, 0), new Color(0, 0, 0, 170) };
        java.awt.geom.Point2D center = new java.awt.geom.Point2D.Float(w / 2f, h / 2f);
        float radius = (float) Math.hypot(w, h) * 0.65f;
        g.setPaint(new java.awt.RadialGradientPaint(center, radius, dist, cols));
        g.fillRect(0, 0, w, h);
    }

    private void drawHandStatusPill(Graphics2D g, int w, int h) {
        boolean ok = handTracking.isConnected();
        String text = ok ? "Hand tracker: connected" : "Hand tracker: offline (mouse fallback)";
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);

        int pillW = tw + 44;
        int pillH = 30;
        int x = 24;
        int y = h - pillH - 24;

        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(x, y, pillW, pillH, 14, 14));

        int dotR = 6;
        g.setColor(ok ? new Color(76, 175, 80) : new Color(231, 76, 60));
        g.fillOval(x + 14, y + (pillH - dotR * 2) / 2, dotR * 2, dotR * 2);

        g.setColor(new Color(255, 255, 255, 220));
        g.drawString(text, x + 30, y + 20);
    }

    private void drawBackground(Graphics2D g2, int w, int h) {
        // Z-divided parallax background using the global static Camera. Each
        // Equipped background skin from BackgroundCatalog (shop-purchasable).
        // Each background uses the same March 10 parallax math internally;
        // the player just gets to pick the art.
        themes.Background eq = themes.BackgroundCatalog.get(
            economy.Inventory.equippedBackground());
        if (eq != null) {
            eq.draw(g2, w, h);
        } else {
            parallax.draw(g2, w, h);
        }
    }

    // ====================================================================
    //  ACCESSORS
    // ====================================================================

    public EffectsManager getEffects() { return effects; }
    public GameState getState()  { return state; }
    public int getWidthSafe()  { return Math.max(1, getWidth()); }
    public int getHeightSafe() { return Math.max(1, getHeight()); }
}
