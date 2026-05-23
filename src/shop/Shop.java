package shop;

import java.util.List;

import economy.Currency;
import economy.Inventory;
import themes.Background;
import themes.BackgroundCatalog;
import weapons.Sword;
import weapons.SwordCatalog;

/**
 * Pure logic layer for the shop. ShopUI handles rendering and hit-testing;
 * Shop owns the state and the buy/equip rules.
 */
public final class Shop {

    public enum Tab { SWORDS, BACKGROUNDS }

    private static Tab tab = Tab.SWORDS;
    private static int scroll;            // current scroll offset (pixels)
    private static String flashMessage;   // last action feedback (purchased / not enough coins)
    private static long flashUntilMs;

    private Shop() {}

    public static Tab tab() { return tab; }
    public static void setTab(Tab t) {
        if (t == tab) return;
        tab = t;
        scroll = 0;
    }

    public static int scroll() { return scroll; }
    public static void scrollBy(int dy) {
        scroll = Math.max(0, scroll + dy);
    }
    public static void clampScroll(int max) {
        if (scroll > max) scroll = Math.max(0, max);
    }

    public static String flashMessage(long nowMs) {
        if (flashMessage == null || nowMs > flashUntilMs) return null;
        return flashMessage;
    }

    private static void flash(String msg) {
        flashMessage = msg;
        flashUntilMs = System.currentTimeMillis() + 1800;
    }

    public static List<Sword> swords() { return SwordCatalog.all(); }
    public static List<Background> backgrounds() { return BackgroundCatalog.all(); }

    /**
     * Buy if not owned, equip if already owned. Returns true if anything
     * changed (so the UI can play a small confirmation animation).
     */
    public static boolean activateSword(String id) {
        Sword s = SwordCatalog.get(id);
        if (s == null) return false;
        if (!Inventory.ownsSword(id)) {
            if (!Currency.spend(s.price())) {
                flash("NOT ENOUGH COINS");
                return false;
            }
            Inventory.grantSword(id);
            Inventory.equipSword(id);
            flash("UNLOCKED: " + s.name());
            return true;
        }
        if (!id.equals(Inventory.equippedSword())) {
            Inventory.equipSword(id);
            flash("EQUIPPED: " + s.name());
            return true;
        }
        return false;
    }

    public static boolean activateBackground(String id) {
        Background b = BackgroundCatalog.get(id);
        if (b == null) return false;
        if (!Inventory.ownsBackground(id)) {
            if (!Currency.spend(b.price())) {
                flash("NOT ENOUGH COINS");
                return false;
            }
            Inventory.grantBackground(id);
            Inventory.equipBackground(id);
            flash("UNLOCKED: " + b.name());
            return true;
        }
        if (!id.equals(Inventory.equippedBackground())) {
            Inventory.equipBackground(id);
            flash("EQUIPPED: " + b.name());
            return true;
        }
        return false;
    }
}
