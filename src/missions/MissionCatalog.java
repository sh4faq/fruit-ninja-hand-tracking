package missions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import entities.FruitType;
import game.GameMode;
import missions.Mission.GoalType;
import missions.Mission.Scope;

/**
 * Static list of every mission in the game. Roughly twenty entries spread
 * across CLASSIC, ARCADE, ZEN and "any mode". Per-run missions inherit a 2x /
 * 3x star scaling unless they ship custom thresholds for flavor.
 */
public final class MissionCatalog {

    private static final List<Mission> ALL = buildAll();

    private MissionCatalog() {}

    public static List<Mission> all() {
        return ALL;
    }

    public static Mission byId(String id) {
        for (Mission m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    /** Sum of best stars across every mission (used for the header total). */
    public static int totalEarnedStars() {
        int sum = 0;
        for (Mission m : ALL) sum += m.bestStars();
        return sum;
    }

    /** Highest possible stars (3 * mission count). */
    public static int totalPossibleStars() {
        return ALL.size() * 3;
    }

    private static List<Mission> buildAll() {
        List<Mission> list = new ArrayList<>();

        // --- CLASSIC ---
        list.add(new Mission(
            "m_first_blood", "First Blood", "Slice 30 fruits in one Classic run",
            GameMode.CLASSIC, GoalType.FRUITS_SLICED,
            30, 60, 100, Scope.PER_RUN, null));
        list.add(new Mission(
            "m_dojo_master", "Dojo Master", "Score 50 points in Classic",
            GameMode.CLASSIC, GoalType.SCORE_AT_LEAST,
            50, 120, 250, Scope.PER_RUN, null));
        list.add(new Mission(
            "m_bomb_defuser", "Bomb Defuser",
            "Survive 90 seconds in Classic without slicing a bomb",
            GameMode.CLASSIC, GoalType.SURVIVE_SECONDS,
            90, 150, 240, Scope.PER_RUN, null));
        list.add(new Mission(
            "m_chapter_climb", "Chapter Climb", "Reach level 5 in Classic",
            GameMode.CLASSIC, GoalType.SCORE_AT_LEAST,
            120, 220, 380, Scope.PER_RUN, null));

        // --- ARCADE ---
        list.add(new Mission(
            "m_centurion", "Centurion", "Score 100 points in one Arcade run",
            GameMode.ARCADE, GoalType.SCORE_AT_LEAST,
            100, 200, 350, Scope.PER_RUN, null));
        list.add(new Mission(
            "m_arcade_combo", "Arcade Streak", "Reach a combo of 5 in Arcade",
            GameMode.ARCADE, GoalType.COMBO_AT_LEAST,
            5, 8, 12, Scope.PER_RUN, null));
        list.add(new Mission(
            "m_arcade_slicer", "Arcade Slicer", "Slice 40 fruits in one Arcade run",
            GameMode.ARCADE, GoalType.FRUITS_SLICED,
            40, 80, 130, Scope.PER_RUN, null));

        // --- ZEN ---
        list.add(Mission.perRun(
            "m_zen_survivor", "Survivor",
            "Slice 100 fruits in one Zen run",
            GameMode.ZEN, GoalType.FRUITS_SLICED, 100));
        list.add(new Mission(
            "m_zen_centurion", "Zen Centurion", "Score 150 in Zen",
            GameMode.ZEN, GoalType.SCORE_AT_LEAST,
            150, 300, 500, Scope.PER_RUN, null));

        // --- ANY MODE (cumulative & flexible) ---
        list.add(new Mission(
            "m_combo_master", "Combo Master", "Hit a combo of 5 in any mode",
            null, GoalType.COMBO_AT_LEAST,
            5, 8, 12, Scope.PER_RUN, null));
        list.add(new Mission(
            "m_watermelon_smasher", "Watermelon Smasher",
            "Slice 20 watermelons across runs",
            null, GoalType.SLICE_TYPE,
            20, 50, 100, Scope.CUMULATIVE, FruitType.WATERMELON));
        list.add(new Mission(
            "m_apple_picker", "Apple Picker",
            "Slice 100 apples across runs",
            null, GoalType.SLICE_TYPE,
            100, 250, 500, Scope.CUMULATIVE, FruitType.APPLE));
        list.add(new Mission(
            "m_citrus_squeezer", "Citrus Squeezer",
            "Slice 50 oranges across runs",
            null, GoalType.SLICE_TYPE,
            50, 120, 250, Scope.CUMULATIVE, FruitType.ORANGE));
        list.add(Mission.cumulative(
            "m_pomegranate", "Pomegranate Hunter",
            "Destroy 5 pomegranate bursts",
            GoalType.POMEGRANATES, 5));
        list.add(Mission.cumulative(
            "m_critical", "Critical Master",
            "Land 50 clean-cut critical hits",
            GoalType.CRITICAL_HITS, 50));
        list.add(new Mission(
            "m_mountain_coins", "Mountain of Coins",
            "Earn 5,000 coins lifetime",
            null, GoalType.COINS_EARNED,
            5000, 12000, 25000, Scope.CUMULATIVE, null));
        list.add(new Mission(
            "m_bladesmith", "Bladesmith", "Own 10 different swords",
            null, GoalType.SWORDS_OWNED,
            10, 15, 20, Scope.CUMULATIVE, null));
        list.add(new Mission(
            "m_world_traveler", "World Traveler",
            "Own 10 different background themes",
            null, GoalType.BACKGROUNDS_OWNED,
            10, 15, 20, Scope.CUMULATIVE, null));
        list.add(Mission.cumulative(
            "m_frenzy_king", "Frenzy King",
            "Trigger 20 multi-slice (3+) combos",
            GoalType.MULTI_SLICE_COMBOS, 20));
        list.add(Mission.cumulative(
            "m_dragon_slayer", "Dragon Slayer",
            "Defeat 3 cube bosses",
            GoalType.CUBES_DEFEATED, 3));
        list.add(Mission.cumulative(
            "m_ghost_hunter", "Ghost Hunter",
            "Catch 15 evader fruits",
            GoalType.EVADERS_CAUGHT, 15));

        // 6 more themed missions for variety
        list.add(new Mission(
            "m_coconut_cracker", "Coconut Cracker",
            "Slice 25 coconuts across runs",
            null, GoalType.SLICE_TYPE,
            25, 60, 120, Scope.CUMULATIVE, FruitType.COCONUT));
        list.add(new Mission(
            "m_grape_glutton", "Grape Glutton",
            "Slice 75 grapes across runs",
            null, GoalType.SLICE_TYPE,
            75, 175, 350, Scope.CUMULATIVE, FruitType.GRAPE));
        list.add(new Mission(
            "m_kiwi_connoisseur", "Kiwi Connoisseur",
            "Slice 40 kiwis across runs",
            null, GoalType.SLICE_TYPE,
            40, 90, 180, Scope.CUMULATIVE, FruitType.KIWI));
        list.add(new Mission(
            "m_lemon_legend", "Lemon Legend",
            "Slice 60 lemons across runs",
            null, GoalType.SLICE_TYPE,
            60, 140, 280, Scope.CUMULATIVE, FruitType.LEMON));
        list.add(new Mission(
            "m_peach_perfect", "Peach Perfect",
            "Slice 45 peaches across runs",
            null, GoalType.SLICE_TYPE,
            45, 100, 200, Scope.CUMULATIVE, FruitType.PEACH));
        list.add(new Mission(
            "m_iron_resolve", "Iron Resolve",
            "Reach a combo of 8 in any mode",
            null, GoalType.COMBO_AT_LEAST,
            8, 12, 18, Scope.PER_RUN, null));

        return Collections.unmodifiableList(list);
    }
}
