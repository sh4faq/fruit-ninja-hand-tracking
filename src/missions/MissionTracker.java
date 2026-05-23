package missions;

import java.util.HashMap;
import java.util.Map;

import economy.Inventory;
import entities.FruitType;
import game.GameMode;

/**
 * Static tracker that collects per-run + cumulative gameplay events used by
 * the missions system. Per-run counters reset every {@link #startRun(GameMode)}.
 * Cumulative counters are persisted to Inventory via the "inv.counters"
 * Preferences key so missions like "Watermelon Smasher" survive game restarts.
 *
 * Wired from GamePanel: every fruit slice / combo / bomb / boss event calls
 * one of the on* methods here, then {@link #evaluate} is invoked from
 * GamePanel.endGame to award stars for whatever the active mission was.
 */
public final class MissionTracker {

    private MissionTracker() {}

    // --- per-run state ---
    private static GameMode currentMode;
    private static int runFruitsSliced;
    private static int runBestCombo;
    private static long runStartMs;
    private static boolean runBombSlicedFlag;
    private static final Map<FruitType, Integer> runFruitCounts = new HashMap<>();

    // ====================================================================
    //  RUN LIFECYCLE
    // ====================================================================

    public static void startRun(GameMode mode) {
        currentMode = mode;
        runFruitsSliced = 0;
        runBestCombo = 0;
        runStartMs = System.currentTimeMillis();
        runBombSlicedFlag = false;
        runFruitCounts.clear();
    }

    public static GameMode currentMode() { return currentMode; }

    // ====================================================================
    //  EVENT HOOKS (called from GamePanel)
    // ====================================================================

    public static void onFruitSliced(FruitType type) {
        runFruitsSliced++;
        Integer c = runFruitCounts.get(type);
        runFruitCounts.put(type, (c == null ? 0 : c) + 1);
        // Per-fruit cumulative counter for SLICE_TYPE missions
        Inventory.bumpCounter("fruit_" + type.name(), 1);
    }

    public static void onComboReached(int n) {
        if (n > runBestCombo) runBestCombo = n;
    }

    public static void onPomegranateDestroyed() {
        Inventory.bumpCounter("pomegranates", 1);
    }

    public static void onCriticalHit() {
        Inventory.bumpCounter("criticals", 1);
    }

    public static void onBombSliced() {
        runBombSlicedFlag = true;
    }

    public static void onMultiSlice(int count) {
        if (count >= 3) Inventory.bumpCounter("multi_slices", 1);
    }

    public static void onEvaderCaught() {
        Inventory.bumpCounter("evaders", 1);
    }

    public static void onCubeDefeated() {
        Inventory.bumpCounter("cubes", 1);
    }

    public static void onCoinsEarned(int coins) {
        if (coins > 0) Inventory.bumpCounter("coins_lifetime", coins);
    }

    // ====================================================================
    //  EVALUATION
    // ====================================================================

    /**
     * Computes 0-3 stars for the given mission using the current run state
     * (for PER_RUN missions) or cumulative counters (for CUMULATIVE missions).
     */
    public static int evaluate(Mission m, int finalScore) {
        int achieved = achievedValue(m, finalScore);
        return m.starsFor(achieved);
    }

    /** Raw achieved value for diagnostics / UI progress bars. */
    public static int achievedValue(Mission m, int finalScore) {
        switch (m.goalType) {
            case SCORE_AT_LEAST:
                return finalScore;
            case COMBO_AT_LEAST:
                return runBestCombo;
            case FRUITS_SLICED:
                return runFruitsSliced;
            case SURVIVE_SECONDS: {
                if (runBombSlicedFlag) return 0;
                long elapsed = (System.currentTimeMillis() - runStartMs) / 1000L;
                return (int) elapsed;
            }
            case SLICE_TYPE:
                if (m.fruitTarget == null) return 0;
                return Inventory.getCounter("fruit_" + m.fruitTarget.name());
            case POMEGRANATES:
                return Inventory.getCounter("pomegranates");
            case CRITICAL_HITS:
                return Inventory.getCounter("criticals");
            case MULTI_SLICE_COMBOS:
                return Inventory.getCounter("multi_slices");
            case EVADERS_CAUGHT:
                return Inventory.getCounter("evaders");
            case CUBES_DEFEATED:
                return Inventory.getCounter("cubes");
            case COINS_EARNED:
                return Inventory.getCounter("coins_lifetime");
            case SWORDS_OWNED:
                return Inventory.ownedSwordCount();
            case BACKGROUNDS_OWNED:
                return Inventory.ownedBackgroundCount();
            default:
                return 0;
        }
    }
}
