package economy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Owned items + currently equipped items. Persisted across runs.
 *
 * Sword and background IDs are arbitrary stable strings (e.g. "sword_001",
 * "bg_dojo_dusk"). The starter sword "sword_001" and starter background
 * "bg_dojo_dusk" are owned + equipped by default.
 */
public final class Inventory {

    private static final Preferences PREFS =
        Preferences.userRoot().node("/fruit_ninja_java");
    private static final String KEY_OWNED_SWORDS = "inv.swords";
    private static final String KEY_OWNED_BGS    = "inv.backgrounds";
    private static final String KEY_EQUIP_SWORD  = "inv.equipSword";
    private static final String KEY_EQUIP_BG     = "inv.equipBg";
    private static final String KEY_STARS        = "inv.stars";
    private static final String KEY_COUNTERS     = "inv.counters";

    public static final String DEFAULT_SWORD = "sword_001";
    public static final String DEFAULT_BG    = "bg_001";

    private static Set<String> ownedSwords;
    private static Set<String> ownedBackgrounds;
    private static String equippedSword;
    private static String equippedBackground;
    private static boolean loaded;

    // Mission stars: missionId -> 0..3
    private static Map<String, Integer> missionStars;
    // Cumulative event counters: name -> count (lifetime)
    private static Map<String, Integer> counters;

    private Inventory() {}

    private static void ensureLoaded() {
        if (loaded) return;
        ownedSwords      = decode(PREFS.get(KEY_OWNED_SWORDS, DEFAULT_SWORD));
        ownedBackgrounds = decode(PREFS.get(KEY_OWNED_BGS, DEFAULT_BG));
        equippedSword      = PREFS.get(KEY_EQUIP_SWORD, DEFAULT_SWORD);
        equippedBackground = PREFS.get(KEY_EQUIP_BG, DEFAULT_BG);
        missionStars       = decodeIntMap(PREFS.get(KEY_STARS, ""));
        counters           = decodeIntMap(PREFS.get(KEY_COUNTERS, ""));
        loaded = true;
    }

    public static boolean ownsSword(String id) {
        ensureLoaded();
        return ownedSwords.contains(id);
    }

    public static boolean ownsBackground(String id) {
        ensureLoaded();
        return ownedBackgrounds.contains(id);
    }

    public static void grantSword(String id) {
        ensureLoaded();
        ownedSwords.add(id);
        PREFS.put(KEY_OWNED_SWORDS, encode(ownedSwords));
    }

    public static void grantBackground(String id) {
        ensureLoaded();
        ownedBackgrounds.add(id);
        PREFS.put(KEY_OWNED_BGS, encode(ownedBackgrounds));
    }

    public static String equippedSword() {
        ensureLoaded();
        return equippedSword;
    }

    public static String equippedBackground() {
        ensureLoaded();
        return equippedBackground;
    }

    public static void equipSword(String id) {
        ensureLoaded();
        if (!ownedSwords.contains(id)) return;
        equippedSword = id;
        PREFS.put(KEY_EQUIP_SWORD, id);
    }

    public static void equipBackground(String id) {
        ensureLoaded();
        if (!ownedBackgrounds.contains(id)) return;
        equippedBackground = id;
        PREFS.put(KEY_EQUIP_BG, id);
    }

    public static void resetAll() {
        ownedSwords = new HashSet<>();
        ownedSwords.add(DEFAULT_SWORD);
        ownedBackgrounds = new HashSet<>();
        ownedBackgrounds.add(DEFAULT_BG);
        equippedSword = DEFAULT_SWORD;
        equippedBackground = DEFAULT_BG;
        missionStars = new HashMap<>();
        counters = new HashMap<>();
        PREFS.put(KEY_OWNED_SWORDS, encode(ownedSwords));
        PREFS.put(KEY_OWNED_BGS, encode(ownedBackgrounds));
        PREFS.put(KEY_EQUIP_SWORD, equippedSword);
        PREFS.put(KEY_EQUIP_BG, equippedBackground);
        PREFS.put(KEY_STARS, encodeIntMap(missionStars));
        PREFS.put(KEY_COUNTERS, encodeIntMap(counters));
    }

    // ====================================================================
    //  MISSIONS: stars + cumulative counters
    // ====================================================================

    /** Returns 0-3 stars previously recorded for a mission (default 0). */
    public static int getStars(String missionId) {
        ensureLoaded();
        Integer s = missionStars.get(missionId);
        return s == null ? 0 : s;
    }

    /** Records mission stars; only persists when the new value beats the old. */
    public static void recordStars(String missionId, int stars) {
        ensureLoaded();
        stars = Math.max(0, Math.min(3, stars));
        int existing = getStars(missionId);
        if (stars <= existing) return;
        missionStars.put(missionId, stars);
        PREFS.put(KEY_STARS, encodeIntMap(missionStars));
    }

    /** Lifetime cumulative counter read (default 0). */
    public static int getCounter(String name) {
        ensureLoaded();
        Integer v = counters.get(name);
        return v == null ? 0 : v;
    }

    /** Add to a lifetime counter and persist. */
    public static void bumpCounter(String name, int delta) {
        ensureLoaded();
        if (delta == 0) return;
        int cur = getCounter(name);
        int next = Math.max(0, cur + delta);
        counters.put(name, next);
        PREFS.put(KEY_COUNTERS, encodeIntMap(counters));
    }

    /** Number of swords currently owned by the player. */
    public static int ownedSwordCount() {
        ensureLoaded();
        return ownedSwords.size();
    }

    /** Number of background themes currently owned by the player. */
    public static int ownedBackgroundCount() {
        ensureLoaded();
        return ownedBackgrounds.size();
    }

    // --- "id:n,id:n,..." encoding for int-valued maps ---

    private static Map<String, Integer> decodeIntMap(String s) {
        Map<String, Integer> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split(",")) {
            int colon = pair.indexOf(':');
            if (colon <= 0) continue;
            String key = pair.substring(0, colon).trim();
            String val = pair.substring(colon + 1).trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            try {
                map.put(key, Integer.parseInt(val));
            } catch (NumberFormatException ignored) {
                // skip malformed entry
            }
        }
        return map;
    }

    private static String encodeIntMap(Map<String, Integer> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static Set<String> decode(String s) {
        Set<String> set = new HashSet<>();
        if (s == null || s.isEmpty()) return set;
        for (String p : s.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) set.add(t);
        }
        return set;
    }

    private static String encode(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : set) {
            if (!first) sb.append(",");
            sb.append(s);
            first = false;
        }
        return sb.toString();
    }
}
