package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;
import java.util.Arrays;
import java.util.List;

public class BaccaratGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public BaccaratGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 27, ChatColor.RED + "Baccarat");
        initializeItems();
    }

    private void initializeItems() {
        // Betting options with detailed descriptions
        gui.setItem(11, createGuiItem(Material.BLUE_WOOL, ChatColor.BLUE + "" + ChatColor.BOLD + "Player",
            ChatColor.GRAY + "Bet on the Player hand",
            ChatColor.YELLOW + "Pays: " + ChatColor.WHITE + "2:1 (even money)",
            ChatColor.GREEN + "Click to select"));

        gui.setItem(13, createGuiItem(Material.WHITE_WOOL, ChatColor.WHITE + "" + ChatColor.BOLD + "Tie",
            ChatColor.GRAY + "Bet on a tie between hands",
            ChatColor.YELLOW + "Pays: " + ChatColor.WHITE + "9:1 (high risk!)",
            ChatColor.GREEN + "Click to select"));

        gui.setItem(15, createGuiItem(Material.RED_WOOL, ChatColor.RED + "" + ChatColor.BOLD + "Banker",
            ChatColor.GRAY + "Bet on the Banker hand",
            ChatColor.YELLOW + "Pays: " + ChatColor.WHITE + "1.95:1 (5% commission)",
            ChatColor.GREEN + "Click to select"));

        // Bet amount buttons with better descriptions
        gui.setItem(19, createGuiItem(Material.GOLD_NUGGET, ChatColor.GOLD + "Bet " + Gambling.getEconomy().format(100),
            ChatColor.GRAY + "Small bet amount",
            ChatColor.GREEN + "Click to select"));

        gui.setItem(20, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Bet " + Gambling.getEconomy().format(500),
            ChatColor.GRAY + "Medium bet amount",
            ChatColor.GREEN + "Click to select"));

        gui.setItem(21, createGuiItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Bet " + Gambling.getEconomy().format(1000),
            ChatColor.GRAY + "Large bet amount",
            ChatColor.GREEN + "Click to select"));

        gui.setItem(22, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "" + ChatColor.BOLD + "DEAL CARDS",
            ChatColor.GRAY + "Start the game!",
            ChatColor.YELLOW + "Make sure to select bet type and amount first"));

        // Add help/info button
        gui.setItem(25, createGuiItem(Material.BOOK, ChatColor.AQUA + "" + ChatColor.BOLD + "HOW TO PLAY",
            ChatColor.GRAY + "Baccarat Rules:",
            ChatColor.WHITE + "• Player vs Banker card game",
            ChatColor.WHITE + "• Goal: Get closest to 9",
            ChatColor.WHITE + "• Aces = 1, Face cards = 0",
            ChatColor.WHITE + "• If total > 9, subtract 10",
            ChatColor.YELLOW + "Payouts:",
            ChatColor.BLUE + "Player: " + ChatColor.WHITE + "2:1 (even money)",
            ChatColor.RED + "Banker: " + ChatColor.WHITE + "1.95:1 (5% commission)",
            ChatColor.WHITE + "Tie: " + ChatColor.WHITE + "9:1 (high risk!)"));

        // Add back button
        gui.setItem(26, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
            ChatColor.GRAY + "Close the baccarat game"));

        // Add decorative elements
        gui.setItem(4, createGuiItem(Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "BACCARAT",
            ChatColor.GRAY + "The classic casino card game",
            ChatColor.YELLOW + "Goal: " + ChatColor.WHITE + "Bet on the hand closest to 9"));
    }

    public void updateHands(List<Card> playerHand, List<Card> bankerHand) {
        // Clear previous cards
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, null);
        }
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, null);
        }

        // Display player cards
        for (int i = 0; i < playerHand.size(); i++) {
            gui.setItem(i, createCardItem(playerHand.get(i)));
        }

        // Display banker cards
        for (int i = 0; i < bankerHand.size(); i++) {
            gui.setItem(i + 18, createCardItem(bankerHand.get(i)));
        }

        // Add hand value displays
        if (!playerHand.isEmpty()) {
            int playerValue = getHandValue(playerHand);
            gui.setItem(9, createGuiItem(Material.BLUE_STAINED_GLASS_PANE,
                ChatColor.BLUE + "Player Hand Value: " + playerValue,
                ChatColor.GRAY + "Cards: " + getHandString(playerHand)));
        }

        if (!bankerHand.isEmpty()) {
            int bankerValue = getHandValue(bankerHand);
            gui.setItem(17, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "Banker Hand Value: " + bankerValue,
                ChatColor.GRAY + "Cards: " + getHandString(bankerHand)));
        }
    }

    public void showResult(String winner) {
        ItemStack resultItem;
        if (winner.equals("player")) {
            resultItem = createGuiItem(Material.BLUE_WOOL, ChatColor.BLUE + "" + ChatColor.BOLD + "PLAYER WINS!",
                ChatColor.GRAY + "The Player hand was closest to 9",
                ChatColor.GREEN + "Congratulations if you bet on Player!");
        } else if (winner.equals("banker")) {
            resultItem = createGuiItem(Material.RED_WOOL, ChatColor.RED + "" + ChatColor.BOLD + "BANKER WINS!",
                ChatColor.GRAY + "The Banker hand was closest to 9",
                ChatColor.GREEN + "Congratulations if you bet on Banker!");
        } else {
            resultItem = createGuiItem(Material.WHITE_WOOL, ChatColor.WHITE + "" + ChatColor.BOLD + "IT'S A TIE!",
                ChatColor.GRAY + "Both hands have the same value",
                ChatColor.GREEN + "Congratulations if you bet on Tie!");
        }
        gui.setItem(4, resultItem);
        gui.setItem(22, createGuiItem(Material.ORANGE_STAINED_GLASS_PANE, ChatColor.GOLD + "" + ChatColor.BOLD + "NEW GAME",
            ChatColor.GRAY + "Click to start a new round",
            ChatColor.YELLOW + "Your previous bet selections will be cleared"));
    }

    public void setSelectedBet(String betType) {
        for (int i = 11; i <= 15; i+=2) {
            ItemStack item = gui.getItem(i);
            if (item != null) {
                item.removeEnchantment(Enchantment.INFINITY);
            }
        }

        if (betType.equals("player")) {
            gui.getItem(11).addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = gui.getItem(11).getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            gui.getItem(11).setItemMeta(meta);
        } else if (betType.equals("tie")) {
            gui.getItem(13).addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = gui.getItem(13).getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            gui.getItem(13).setItemMeta(meta);
        } else if (betType.equals("banker")) {
            gui.getItem(15).addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = gui.getItem(15).getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            gui.getItem(15).setItemMeta(meta);
        }
    }

    public void resetForNewGame() {
        // Clear all card displays
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, null);
        }
        for (int i = 18; i < 27; i++) {
            if (i != 26) { // Don't clear the back button
                gui.setItem(i, null);
            }
        }

        // Reset the center display
        gui.setItem(4, createGuiItem(Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "BACCARAT",
            ChatColor.GRAY + "The classic casino card game",
            ChatColor.YELLOW + "Goal: " + ChatColor.WHITE + "Bet on the hand closest to 9"));

        // Reset the deal button
        gui.setItem(22, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "" + ChatColor.BOLD + "DEAL CARDS",
            ChatColor.GRAY + "Start the game!",
            ChatColor.YELLOW + "Make sure to select bet type and amount first"));

        // Clear hand value displays
        gui.setItem(9, null);
        gui.setItem(17, null);

        // Remove enchantments from bet selection buttons
        for (int i = 11; i <= 15; i+=2) {
            ItemStack item = gui.getItem(i);
            if (item != null) {
                item.removeEnchantment(Enchantment.INFINITY);
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta!= null) {
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
        int value = card.getRank().getValue();
        return value >= 10 ? 0 : value; // Face cards and 10s are worth 0 in baccarat
    }

    private int getHandValue(List<Card> hand) {
        int value = 0;
        for (Card card : hand) {
            value += getBaccaratCardValue(card);
        }
        return value % 10; // Baccarat uses modulo 10
    }

    private String getHandString(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i).getRank().toString()).append(" of ").append(hand.get(i).getSuit().toString());
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
