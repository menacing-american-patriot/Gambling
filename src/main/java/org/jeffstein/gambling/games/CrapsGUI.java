package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

public class CrapsGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final CrapsGame game;
    private double currentBet = 100.0;

    public CrapsGUI(Gambling plugin, Player player, CrapsGame game) {
        this.plugin = plugin;
        this.player = player;
        this.game = game;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.GREEN + "" + ChatColor.BOLD + "🎲 CRAPS 🎲");
        initializeItems();
    }

    private void initializeItems() {
        // Fill background
        ItemStack background = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Game title and status
        updateGameStatus();

        // Betting controls
        setupBettingControls();

        // Main betting areas
        setupBettingAreas();

        // Dice display area
        updateDiceDisplay();

        // Back button
        gui.setItem(45, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Close Craps game"));
    }

    private void updateGameStatus() {
        String gamePhase;
        String pointText;
        Material statusMaterial;

        if (game.getGameState() == CrapsGame.GameState.COME_OUT) {
            gamePhase = "COME OUT ROLL";
            pointText = "No point set";
            statusMaterial = Material.YELLOW_CONCRETE;
        } else {
            gamePhase = "POINT PHASE";
            pointText = "Point: " + game.getPoint();
            statusMaterial = Material.ORANGE_CONCRETE;
        }

        gui.setItem(4, createGuiItem(statusMaterial, ChatColor.GOLD + "" + ChatColor.BOLD + gamePhase,
                ChatColor.WHITE + pointText,
                ChatColor.GRAY + "Current game phase"));
    }

    private void setupBettingControls() {
        // Bet amount controls
        gui.setItem(37, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet by 100"));
        gui.setItem(38, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-50",
                ChatColor.GRAY + "Decrease bet by 50"));

        gui.setItem(40, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet",
                ChatColor.WHITE + Gambling.getEconomy().format(currentBet),
                ChatColor.GRAY + "Your bet amount"));

        gui.setItem(42, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+50",
                ChatColor.GRAY + "Increase bet by 50"));
        gui.setItem(43, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet by 100"));
    }

    private void setupBettingAreas() {
        // Pass Line bet (most common bet)
        boolean hasPassBet = game.getBets().containsKey("Pass Line");
        gui.setItem(19, createGuiItem(hasPassBet ? Material.LIME_CONCRETE : Material.GREEN_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "PASS LINE",
                ChatColor.GRAY + "Bet that shooter will win",
                hasPassBet ? ChatColor.WHITE + "Current bet: " + Gambling.getEconomy().format(game.getBets().get("Pass Line")) :
                           ChatColor.YELLOW + "Click to bet " + Gambling.getEconomy().format(currentBet)));

        // Don't Pass bet
        boolean hasDontPassBet = game.getBets().containsKey("Don't Pass Line");
        gui.setItem(20, createGuiItem(hasDontPassBet ? Material.RED_CONCRETE : Material.ORANGE_CONCRETE,
                ChatColor.RED + "" + ChatColor.BOLD + "DON'T PASS",
                ChatColor.GRAY + "Bet against the shooter",
                hasDontPassBet ? ChatColor.WHITE + "Current bet: " + Gambling.getEconomy().format(game.getBets().get("Don't Pass Line")) :
                               ChatColor.YELLOW + "Click to bet " + Gambling.getEconomy().format(currentBet)));

        // Roll button
        gui.setItem(49, createGuiItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "🎲 ROLL DICE 🎲",
                ChatColor.GRAY + "Roll the dice!",
                game.getBets().isEmpty() ? ChatColor.RED + "Place a bet first!" : ChatColor.GREEN + "Click to roll!"));
    }

    private void updateDiceDisplay() {
        // Show last roll if any
        if (game.getLastRoll() != null) {
            int[] lastRoll = game.getLastRoll();
            int total = lastRoll[0] + lastRoll[1];

            gui.setItem(12, createGuiItem(Material.QUARTZ_BLOCK, ChatColor.WHITE + "Die 1",
                    ChatColor.GOLD + "Rolled: " + lastRoll[0]));
            gui.setItem(13, createGuiItem(Material.QUARTZ_BLOCK, ChatColor.WHITE + "Die 2",
                    ChatColor.GOLD + "Rolled: " + lastRoll[1]));
            gui.setItem(14, createGuiItem(Material.GOLD_BLOCK, ChatColor.GOLD + "" + ChatColor.BOLD + "TOTAL",
                    ChatColor.WHITE + "Sum: " + total,
                    ChatColor.GRAY + "Last roll result"));
        } else {
            gui.setItem(12, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Die 1",
                    ChatColor.DARK_GRAY + "Not rolled yet"));
            gui.setItem(13, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Die 2",
                    ChatColor.DARK_GRAY + "Not rolled yet"));
            gui.setItem(14, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Total",
                    ChatColor.DARK_GRAY + "Roll dice to see result"));
        }
    }

    public void update() {
        updateGameStatus();
        setupBettingAreas();
        updateDiceDisplay();
    }

    public void adjustBet(double amount) {
        currentBet = Math.max(10.0, Math.min(10000.0, currentBet + amount));
        setupBettingControls();
        setupBettingAreas(); // Update betting areas to show new bet amounts
    }

    public double getCurrentBet() {
        return currentBet;
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

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }
}
