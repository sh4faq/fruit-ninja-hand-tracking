package dev;

import economy.Currency;

/**
 * Dev / debug utility. Adds 99,999 coins to the player's wallet and grants
 * every sword and background as owned (but doesn't equip them, just marks
 * them owned so the shop shows them all).
 *
 * Run with:
 *   java -cp bin dev.AddCoins
 */
public class AddCoins {
    public static void main(String[] args) {
        weapons.SwordCatalog.init();
        themes.BackgroundCatalog.init();

        int before = Currency.get();
        Currency.add(99_999);
        int after = Currency.get();
        System.out.println("Coins: " + before + " -> " + after);

        System.out.println("Done. Launch the game and check the shop.");
    }
}
