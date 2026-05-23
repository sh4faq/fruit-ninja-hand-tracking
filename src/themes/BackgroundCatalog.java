package themes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every purchasable background. Same pattern as
 * {@link weapons.SwordCatalog}.
 */
public final class BackgroundCatalog {

    private static final Map<String, Background> BY_ID = new LinkedHashMap<>();

    private BackgroundCatalog() {}

    public static void register(Background bg) {
        BY_ID.put(bg.id(), bg);
    }

    public static Background get(String id) {
        Background b = BY_ID.get(id);
        if (b != null) return b;
        return BY_ID.get(economy.Inventory.DEFAULT_BG);
    }

    public static List<Background> all() {
        return Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    public static int count() {
        return BY_ID.size();
    }

    public static void init() {
        try {
            String[] classes = {
                "themes.backgrounds.BackgroundsDojo",
                "themes.backgrounds.BackgroundsForest",
                "themes.backgrounds.BackgroundsCosmic",
                "themes.backgrounds.BackgroundsVolcano",
                "themes.backgrounds.BackgroundsOcean",
                "themes.backgrounds.BackgroundsCity",
                "themes.backgrounds.BackgroundsDesert",
                "themes.backgrounds.BackgroundsAurora",
            };
            for (String c : classes) {
                try {
                    Class.forName(c);
                } catch (ClassNotFoundException ignore) {
                    // Family file not present yet; skip.
                }
            }
        } catch (Throwable t) {
            System.err.println("[BackgroundCatalog] init failure: " + t);
        }
    }
}
