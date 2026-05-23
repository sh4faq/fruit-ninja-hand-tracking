package weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every purchasable sword. Populated at class-load time by
 * pulling in each {@link Sword} family file.
 *
 * Lookup is by stable string id (e.g. "sword_007"). The {@link #all()} list
 * preserves insertion order so the shop grid is consistent across runs.
 */
public final class SwordCatalog {

    private static final Map<String, Sword> BY_ID = new LinkedHashMap<>();

    private SwordCatalog() {}

    public static void register(Sword sword) {
        BY_ID.put(sword.id(), sword);
    }

    public static Sword get(String id) {
        Sword s = BY_ID.get(id);
        if (s != null) return s;
        // Fallback: starter sword if a saved id is no longer in the catalog.
        return BY_ID.get(economy.Inventory.DEFAULT_SWORD);
    }

    public static List<Sword> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    public static int count() {
        return BY_ID.size();
    }

    // ====================================================================
    //  STATIC INITIALIZER -- registers every Sword class by class-loading it.
    //
    //  Each Sword implementation registers itself in a static block, so we
    //  just need to force the JVM to load them. Catalog.init() does that.
    // ====================================================================

    public static void init() {
        // Class-loading these triggers their static blocks which call
        // SwordCatalog.register(...). New swords just need to be added to
        // this list.
        try {
            // Family classes loaded by name so adding a new sword family
            // only requires touching this list, no other coordination.
            String[] classes = {
                "weapons.swords.SwordsKatana",
                "weapons.swords.SwordsEnergy",
                "weapons.swords.SwordsCrystal",
                "weapons.swords.SwordsDemon",
                "weapons.swords.SwordsElemental",
                "weapons.swords.SwordsMecha",
                "weapons.swords.SwordsMystic",
                "weapons.swords.SwordsCosmic",
            };
            for (String c : classes) {
                try {
                    Class.forName(c);
                } catch (ClassNotFoundException ignore) {
                    // A family may not exist yet during incremental dev; skip.
                }
            }
        } catch (Throwable t) {
            // Never let a broken sword class crash the game.
            System.err.println("[SwordCatalog] init failure: " + t);
        }
    }
}
