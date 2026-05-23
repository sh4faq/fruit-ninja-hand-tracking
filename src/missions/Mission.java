package missions;

import entities.FruitType;
import game.GameMode;

/**
 * A single mission: a specific goal the player can chase across runs to earn
 * 1-3 stars. Missions are immutable definitions; per-player progress is
 * persisted in {@link economy.Inventory} via the recordStars / getStars API.
 *
 * The {@link GoalType} enum tags how a mission's progress is measured, and
 * the {@link Scope} enum decides whether progress is per-run (Score, Combo,
 * SurviveSeconds) or cumulative across every run (totals like CoinsEarned).
 */
public final class Mission {

    /** What metric the mission is measuring. */
    public enum GoalType {
        SCORE_AT_LEAST,         // per-run final score
        COMBO_AT_LEAST,         // per-run best combo
        FRUITS_SLICED,          // per-run fruit count (no bombs)
        SURVIVE_SECONDS,        // per-run survived without slicing a bomb
        SLICE_TYPE,             // cumulative slices of a specific FruitType
        POMEGRANATES,           // cumulative pomegranate (Watermelon Smasher analogue)
        CRITICAL_HITS,          // cumulative clean-cut count
        MULTI_SLICE_COMBOS,     // cumulative 3+ fruit multi-slices
        EVADERS_CAUGHT,         // cumulative evader catches
        CUBES_DEFEATED,         // cumulative cube boss defeats
        COINS_EARNED,           // cumulative lifetime coins earned
        SWORDS_OWNED,           // count of owned swords (read from Inventory)
        BACKGROUNDS_OWNED       // count of owned backgrounds
    }

    /** Whether progress is collected in one run or accumulated across runs. */
    public enum Scope {
        PER_RUN,
        CUMULATIVE
    }

    public final String id;
    public final String name;
    public final String description;
    public final GameMode mode;         // null means "any mode" / not tied to a run
    public final GoalType goalType;
    public final int goalValue;         // 1-star threshold
    public final int twoStarValue;      // 2-star threshold
    public final int threeStarValue;    // 3-star threshold
    public final Scope scope;
    public final FruitType fruitTarget; // only used for SLICE_TYPE; nullable

    public Mission(String id, String name, String description,
                   GameMode mode, GoalType goalType,
                   int goalValue, int twoStarValue, int threeStarValue,
                   Scope scope, FruitType fruitTarget) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.mode = mode;
        this.goalType = goalType;
        this.goalValue = goalValue;
        this.twoStarValue = twoStarValue;
        this.threeStarValue = threeStarValue;
        this.scope = scope;
        this.fruitTarget = fruitTarget;
    }

    /** Convenience for missions that auto-derive 2*=2x, 3*=3x. */
    public static Mission perRun(String id, String name, String desc,
                                 GameMode mode, GoalType goalType, int oneStar) {
        return new Mission(id, name, desc, mode, goalType,
                           oneStar, oneStar * 2, oneStar * 3,
                           Scope.PER_RUN, null);
    }

    public static Mission cumulative(String id, String name, String desc,
                                     GoalType goalType, int oneStar) {
        return new Mission(id, name, desc, null, goalType,
                           oneStar, oneStar * 2, oneStar * 3,
                           Scope.CUMULATIVE, null);
    }

    /** Returns 0-3 based on how the achieved value compares to the thresholds. */
    public int starsFor(int achieved) {
        if (achieved >= threeStarValue) return 3;
        if (achieved >= twoStarValue)   return 2;
        if (achieved >= goalValue)      return 1;
        return 0;
    }

    /** Best stars previously earned, read from Inventory. */
    public int bestStars() {
        return economy.Inventory.getStars(id);
    }

    /** Short human-readable mode chip ("CLASSIC", "ARCADE", "ZEN", or "ANY"). */
    public String modeChip() {
        return mode == null ? "ANY" : mode.displayName.toUpperCase();
    }
}
