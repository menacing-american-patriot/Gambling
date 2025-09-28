package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class BaccaratGUI implements InventoryHolder {

    private static final int[] PLAYER_CARD_SLOTS = {1, 2, 3};
    private static final int[] BANKER_CARD_SLOTS = {5, 6, 7};
    private static final int PLAYER_VALUE_SLOT = 9;
    private static final int BANKER_VALUE_SLOT = 17;
    private static final int STATUS_SLOT = 4;
    private static final int ROUND_SUMMARY_SLOT = 18;
    private static final int ACTION_BUTTON_SLOT = 22;
    private static final int HELP_SLOT = 25;
    private static final int BACK_SLOT = 26;

    private final Player player;
    private final Inventory gui;

    public BaccaratGUI(Gambling plugin, Player player) {
        this.player = player;
        this.gui = Bukkit.createInventory(this, 27, ChatColor.DARK_RED + "Baccarat Table");
        initializeItems();
    }

    private void initializeItems() {
        fillBackground();
        resetHands();

        gui.setItem(STATUS_SLOT, createGuiItem(Material.EMERALD,
            ChatColor.GREEN + "" + ChatColor.BOLD + "BACCARAT",
            ChatColor.GRAY + "Bet on the hand closest to 9",
            ChatColor.YELLOW + "Select a bet and amount to begin"));

        gui.setItem(ROUND_SUMMARY_SLOT, createGuiItem(Material.FILLED_MAP,
            ChatColor.GOLD + "Recent Results",
            ChatColor.GRAY + "Play a round to see the history."));

        // Betting options
        gui.setItem(11, createGuiItem(Material.BLUE_WOOL,
            ChatColor.BLUE + "" + ChatColor.BOLD + "Player",
            ChatColor.GRAY + "Pays even money",
            ChatColor.GREEN + "Click to choose"));

        gui.setItem(13, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "" + ChatColor.BOLD + "Tie",
            ChatColor.GRAY + "Pays 9:1",
            ChatColor.GREEN + "High risk, high reward"));

        gui.setItem(15, createGuiItem(Material.RED_WOOL,
            ChatColor.RED + "" + ChatColor.BOLD + "Banker",
            ChatColor.GRAY + "Pays 1.95:1",
            ChatColor.GREEN + "5% house commission"));

        // Bet amount presets
        gui.setItem(19, createGuiItem(Material.GOLD_NUGGET,
            ChatColor.GOLD + "Bet " + Gambling.getEconomy().format(100),
            ChatColor.GRAY + "Quick bet"));

        gui.setItem(20, createGuiItem(Material.GOLD_INGOT,
            ChatColor.GOLD + "Bet " + Gambling.getEconomy().format(500),
            ChatColor.GRAY + "Balanced bet"));

        gui.setItem(21, createGuiItem(Material.GOLD_BLOCK,
            ChatColor.GOLD + "Bet " + Gambling.getEconomy().format(1000),
            ChatColor.GRAY + "High roller"));

        gui.setItem(ACTION_BUTTON_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "" + ChatColor.BOLD + "DEAL CARDS",
            ChatColor.GRAY + "Select bet type and amount first"));

        gui.setItem(HELP_SLOT, createGuiItem(Material.BOOK,
            ChatColor.AQUA + "" + ChatColor.BOLD + "HOW TO PLAY",
            ChatColor.WHITE + "• Player vs Banker",
            ChatColor.WHITE + "• Closest to 9 wins",
            ChatColor.WHITE + "• Aces = 1, 10s/Face = 0",
            ChatColor.WHITE + "• Automatic third-card rules"));

        gui.setItem(BACK_SLOT, createGuiItem(Material.OAK_DOOR,
            ChatColor.YELLOW + "Leave Table",
            ChatColor.GRAY + "Close the Baccarat menu"));
    }

    private void fillBackground() {
        ItemStack background = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, background);
        }
    }

    private void resetHands() {
        ItemStack playerPlaceholder = createGuiItem(Material.BLUE_STAINED_GLASS_PANE,
            ChatColor.BLUE + "Player Hand",
            ChatColor.GRAY + "Waiting for cards...");
        for (int slot : PLAYER_CARD_SLOTS) {
            gui.setItem(slot, playerPlaceholder);
        }
        gui.setItem(PLAYER_VALUE_SLOT, createGuiItem(Material.BLUE_STAINED_GLASS_PANE,
            ChatColor.BLUE + "Player Total: -"));

        ItemStack bankerPlaceholder = createGuiItem(Material.RED_STAINED_GLASS_PANE,
            ChatColor.RED + "Banker Hand",
            ChatColor.GRAY + "Waiting for cards...");
        for (int slot : BANKER_CARD_SLOTS) {
            gui.setItem(slot, bankerPlaceholder);
        }
        gui.setItem(BANKER_VALUE_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE,
            ChatColor.RED + "Banker Total: -"));
    }

    public void updateHands(List<Card> playerHand, List<Card> bankerHand) {
        resetHands();

        for (int i = 0; i < playerHand.size() && i < PLAYER_CARD_SLOTS.length; i++) {
            gui.setItem(PLAYER_CARD_SLOTS[i], createCardItem(playerHand.get(i)));
        }

        for (int i = 0; i < bankerHand.size() && i < BANKER_CARD_SLOTS.length; i++) {
            gui.setItem(BANKER_CARD_SLOTS[i], createCardItem(bankerHand.get(i)));
        }

        if (!playerHand.isEmpty()) {
            gui.setItem(PLAYER_VALUE_SLOT, createGuiItem(Material.BLUE_STAINED_GLASS_PANE,
                ChatColor.BLUE + "Player Total: " + getHandValue(playerHand),
                ChatColor.GRAY + getHandString(playerHand)));
        }

        if (!bankerHand.isEmpty()) {
            gui.setItem(BANKER_VALUE_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Banker Total: " + getHandValue(bankerHand),
                ChatColor.GRAY + getHandString(bankerHand)));
        }
    }

    public void setSelectedBet(String betType) {
        clearHighlight(11);
        clearHighlight(13);
        clearHighlight(15);

        if (betType.equals("player")) {
            highlightSlot(11);
        } else if (betType.equals("tie")) {
            highlightSlot(13);
        } else if (betType.equals("banker")) {
            highlightSlot(15);
        }
    }

    public void setSelectedAmount(double amount) {
        clearHighlight(19);
        clearHighlight(20);
        clearHighlight(21);

        if (amount == 100) {
            highlightSlot(19);
        } else if (amount == 500) {
            highlightSlot(20);
        } else if (amount == 1000) {
            highlightSlot(21);
        }
    }

    public void setStatus(String title, String... lines) {
        gui.setItem(STATUS_SLOT, createGuiItem(Material.EMERALD,
            ChatColor.GREEN + "" + ChatColor.BOLD + title, lines));
    }

    public void setRoundSummary(String title, String... description) {
        gui.setItem(ROUND_SUMMARY_SLOT, createGuiItem(Material.FILLED_MAP,
            ChatColor.GOLD + title, description));
    }

    public void setActionButton(Material material, String name, String... lore) {
        gui.setItem(ACTION_BUTTON_SLOT, createGuiItem(material, name, lore));
    }

    public void showOutcome(BaccaratGame.Winner winner, String subtitle) {
        Material material;
        ChatColor color;

        if (winner == BaccaratGame.Winner.PLAYER) {
            material = Material.BLUE_WOOL;
            color = ChatColor.BLUE;
        } else if (winner == BaccaratGame.Winner.BANKER) {
            material = Material.RED_WOOL;
            color = ChatColor.RED;
        } else {
            material = Material.WHITE_WOOL;
            color = ChatColor.WHITE;
        }

        gui.setItem(STATUS_SLOT, createGuiItem(material,
            color + "" + ChatColor.BOLD + formatWinnerTitle(winner),
            ChatColor.GRAY + subtitle));

        setActionButton(Material.ORANGE_STAINED_GLASS_PANE,
            ChatColor.GOLD + "" + ChatColor.BOLD + "NEW ROUND",
            ChatColor.YELLOW + "Click to play again");
    }

    private String formatWinnerTitle(BaccaratGame.Winner winner) {
        switch (winner) {
            case PLAYER:
                return "PLAYER WINS";
            case BANKER:
                return "BANKER WINS";
            default:
                return "PUSH - TIE";
        }
    }

    public void resetForNewGame() {
        resetHands();
        setStatus("BACCARAT",
            ChatColor.GRAY + "Select your bet to begin",
            ChatColor.YELLOW + "Good luck!");
        setActionButton(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "" + ChatColor.BOLD + "DEAL CARDS",
            ChatColor.GRAY + "Choose bet type and amount first");
        clearHighlight(11);
        clearHighlight(13);
        clearHighlight(15);
        clearHighlight(19);
        clearHighlight(20);
        clearHighlight(21);
    }

    private void highlightSlot(int slot) {
        ItemStack item = gui.getItem(slot);
        if (item == null) {
            return;
        }
        item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }

    private void clearHighlight(int slot) {
        ItemStack item = gui.getItem(slot);
        if (item == null) {
            return;
        }
        if (item.containsEnchantment(Enchantment.INFINITY)) {
            item.removeEnchantment(Enchantment.INFINITY);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCardItem(Card card) {
        ItemStack item = new ItemStack(card.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + card.getRank().toString() + " of " + card.getSuit().toString());
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Value: " + ChatColor.WHITE + getBaccaratCardValue(card),
                ChatColor.GRAY + "Suit: " + ChatColor.WHITE + card.getSuit().toString()
            ));
            if (card.getCustomModelData() != 0) {
                meta.setCustomModelData(card.getCustomModelData());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getBaccaratCardValue(Card card) {
        if (card.getRank() == Card.Rank.ACE) {
            return 1;
        }
        int value = card.getRank().getValue();
        return value >= 10 ? 0 : value;
    }

    private int getHandValue(List<Card> hand) {
        int value = 0;
        for (Card card : hand) {
            value += getBaccaratCardValue(card);
        }
        return value % 10;
    }

    private String getHandString(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i).getRank()).append(" of ").append(hand.get(i).getSuit());
            if (i < hand.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }
}
