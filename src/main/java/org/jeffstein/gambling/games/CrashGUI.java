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

public class CrashGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final CrashGame crashGame;
    private double currentBet = 100.0;
    private double autoCashout = 2.0;

    public CrashGUI(Gambling plugin, Player player, CrashGame crashGame) {
        this.plugin = plugin;
        this.player = player;
        this.crashGame = crashGame;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.RED + "💥 CRASH GAME 💥");
        initializeItems();
    }

    private void initializeItems() {
        // Fill background
        ItemStack background = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Create decorative rocket/crash theme
        gui.setItem(4, createGuiItem(Material.FIREWORK_ROCKET, ChatColor.RED + "" + ChatColor.BOLD + "CRASH GAME",
                ChatColor.GRAY + "Bet and cash out before the crash!",
                ChatColor.YELLOW + "Current Status: " + (crashGame.isBettingPhase() ? "Betting Phase" : 
                    crashGame.isGameRunning() ? "Game Running" : "Waiting")));

        // Multiplier display area (center)
        updateMultiplierDisplay();

        // Betting controls (bottom row)
        setupBettingControls();

        // Game controls
        setupGameControls();

        // Back button
        gui.setItem(45, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Close crash game"));
    }

    private void updateMultiplierDisplay() {
        if (crashGame.isGameRunning()) {
            double multiplier = crashGame.getCurrentMultiplier();
            String multiplierText = String.format("%.2fx", multiplier);
            
            // Create a visual representation of the multiplier
            Material displayMaterial;
            ChatColor color;
            
            if (multiplier < 2.0) {
                displayMaterial = Material.GREEN_CONCRETE;
                color = ChatColor.GREEN;
            } else if (multiplier < 5.0) {
                displayMaterial = Material.YELLOW_CONCRETE;
                color = ChatColor.YELLOW;
            } else if (multiplier < 10.0) {
                displayMaterial = Material.ORANGE_CONCRETE;
                color = ChatColor.GOLD;
            } else {
                displayMaterial = Material.RED_CONCRETE;
                color = ChatColor.RED;
            }
            
            gui.setItem(22, createGuiItem(displayMaterial, color + "" + ChatColor.BOLD + multiplierText,
                    ChatColor.GRAY + "Current multiplier",
                    crashGame.getPlayerBets().containsKey(player.getUniqueId()) ? 
                        ChatColor.GREEN + "Your potential win: " + 
                        Gambling.getEconomy().format(crashGame.getPlayerBets().get(player.getUniqueId()) * multiplier) : 
                        ChatColor.GRAY + "No active bet"));
        } else if (crashGame.isBettingPhase()) {
            gui.setItem(22, createGuiItem(Material.CLOCK, ChatColor.YELLOW + "" + ChatColor.BOLD + "BETTING PHASE",
                    ChatColor.GRAY + "Place your bets!",
                    ChatColor.GREEN + "Game starting soon..."));
        } else {
            gui.setItem(22, createGuiItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "CRASHED!",
                    ChatColor.GRAY + "Waiting for next round...",
                    ChatColor.YELLOW + "Get ready for the next game!"));
        }
    }

    private void setupBettingControls() {
        // Bet amount controls
        gui.setItem(37, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet by 100"));
        gui.setItem(38, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-50",
                ChatColor.GRAY + "Decrease bet by 50"));
        gui.setItem(39, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-10",
                ChatColor.GRAY + "Decrease bet by 10"));

        gui.setItem(40, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet",
                ChatColor.WHITE + Gambling.getEconomy().format(currentBet),
                ChatColor.GRAY + "Click to place this bet"));

        gui.setItem(41, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+10",
                ChatColor.GRAY + "Increase bet by 10"));
        gui.setItem(42, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+50",
                ChatColor.GRAY + "Increase bet by 50"));
        gui.setItem(43, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet by 100"));
    }

    private void setupGameControls() {
        // Auto-cashout controls
        gui.setItem(28, createGuiItem(Material.REDSTONE, ChatColor.RED + "Auto-Cashout -0.1x",
                ChatColor.GRAY + "Decrease auto-cashout"));
        gui.setItem(29, createGuiItem(Material.COMPARATOR, ChatColor.YELLOW + "Auto-Cashout",
                ChatColor.WHITE + String.format("%.2fx", autoCashout),
                ChatColor.GRAY + "Automatically cash out at this multiplier"));
        gui.setItem(30, createGuiItem(Material.REDSTONE_TORCH, ChatColor.GREEN + "Auto-Cashout +0.1x",
                ChatColor.GRAY + "Increase auto-cashout"));

        // Main action buttons
        if (crashGame.isBettingPhase()) {
            gui.setItem(31, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "PLACE BET",
                    ChatColor.GRAY + "Bet: " + Gambling.getEconomy().format(currentBet),
                    ChatColor.YELLOW + "Click to join the next round!"));
        } else if (crashGame.isGameRunning() && crashGame.getPlayerBets().containsKey(player.getUniqueId()) 
                   && !crashGame.getCashedOutPlayers().contains(player.getUniqueId())) {
            gui.setItem(31, createGuiItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "CASH OUT",
                    ChatColor.GRAY + "Current multiplier: " + String.format("%.2fx", crashGame.getCurrentMultiplier()),
                    ChatColor.GREEN + "Click to cash out now!"));
        } else {
            gui.setItem(31, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Waiting...",
                    ChatColor.DARK_GRAY + "Wait for next round"));
        }
    }

    public void updateDisplay() {
        updateMultiplierDisplay();
        setupGameControls();
        
        // Update status in title
        gui.setItem(4, createGuiItem(Material.FIREWORK_ROCKET, ChatColor.RED + "" + ChatColor.BOLD + "CRASH GAME",
                ChatColor.GRAY + "Bet and cash out before the crash!",
                ChatColor.YELLOW + "Current Status: " + (crashGame.isBettingPhase() ? "Betting Phase" : 
                    crashGame.isGameRunning() ? "Game Running - " + String.format("%.2fx", crashGame.getCurrentMultiplier()) : "Waiting")));
    }

    public void adjustBet(double amount) {
        currentBet = Math.max(10.0, Math.min(10000.0, currentBet + amount));
        setupBettingControls();
    }

    public void setBet(double amount) {
        currentBet = Math.max(10.0, Math.min(10000.0, amount));
        setupBettingControls();
    }

    public void adjustAutoCashout(double amount) {
        autoCashout = Math.max(1.01, Math.min(1000.0, autoCashout + amount));
        setupGameControls();
    }

    public void setAutoCashoutValue(double value) {
        autoCashout = Math.max(1.01, Math.min(1000.0, value));
        setupGameControls();
    }

    public boolean placeBet() {
        boolean success = crashGame.placeBet(player, currentBet);
        if (success) {
            crashGame.setAutoCashout(player, autoCashout);
            updateDisplay();
        }
        return success;
    }

    public boolean cashOut() {
        boolean success = crashGame.cashOut(player);
        if (success) {
            updateDisplay();
        }
        return success;
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

    public double getCurrentBet() {
        return currentBet;
    }

    public double getAutoCashout() {
        return autoCashout;
    }
}
