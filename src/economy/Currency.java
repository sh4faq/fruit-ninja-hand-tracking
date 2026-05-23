package economy;

import java.util.prefs.Preferences;

/**
 * Global coin wallet. Coins are earned by slicing fruit and spent in the
 * shop on swords and background themes.
 *
 * Persisted across runs via java.util.prefs so the player keeps their
 * balance and any owned items.
 */
public final class Currency {

    private static final Preferences PREFS =
        Preferences.userRoot().node("/fruit_ninja_java");
    private static final String KEY_COINS = "wallet.coins";

    private static int coins;
    private static boolean loaded;

    private Currency() {}

    public static int get() {
        ensureLoaded();
        return coins;
    }

    public static void add(int amount) {
        ensureLoaded();
        coins = Math.max(0, coins + amount);
        save();
    }

    public static boolean spend(int amount) {
        ensureLoaded();
        if (coins < amount) return false;
        coins -= amount;
        save();
        return true;
    }

    public static void resetAll() {
        coins = 0;
        save();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        coins = PREFS.getInt(KEY_COINS, 0);
        loaded = true;
    }

    private static void save() {
        PREFS.putInt(KEY_COINS, coins);
    }
}
