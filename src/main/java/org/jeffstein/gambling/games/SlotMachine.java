package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SlotMachine implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();
    private final double spinCost = 100.0; // Cost per spin

    // The items that will appear on the reels
    private final List<Material> reelItems = Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT,
            Material.IRON_INGOT, Material.LAPIS_LAZULI, Material.COAL
    );

    public SlotMachine(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        // Create a 54-slot inventory (6 rows) for a bigger slot machine
        this.gui = Bukkit.createInventory(this, 54, ChatColor.GOLD + "Slot Machine");
        initializeItems();
    }

    // Define the reel positions (5 reels, 3 rows each)
    private final int[][] reelPositions = {
        {10, 19, 28}, // Reel 1 (column 1)
        {11, 20, 29}, // Reel 2 (column 2)
        {12, 21, 30}, // Reel 3 (column 3)
        {13, 22, 31}, // Reel 4 (column 4)
        {14, 23, 32}  // Reel 5 (column 5)
    };

    private void initializeItems() {
        // Fill the background with glass panes
        ItemStack background = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Create decorative borders
        ItemStack border = createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "SLOT MACHINE");
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border); // Top row
        }
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, border); // Bottom row
        }

        // Side borders
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border); // Left side
            gui.setItem(row * 9 + 8, border); // Right side
        }

        // Create the "Spin" button
        gui.setItem(49, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "SPIN!",
                ChatColor.GRAY + "Cost: " + economy.format(spinCost),
                ChatColor.YELLOW + "Win by matching symbols!"));

        // Add back button
        gui.setItem(45, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Close the slot machine"));

        // Set initial reel items (5 reels, 3 rows each)
        for (int reel = 0; reel < 5; reel++) {
            for (int row = 0; row < 3; row++) {
                Material randomItem = reelItems.get(ThreadLocalRandom.current().nextInt(reelItems.size()));
                gui.setItem(reelPositions[reel][row], new ItemStack(randomItem));
            }
        }

        // Add title
        gui.setItem(4, createGuiItem(Material.DIAMOND, ChatColor.GOLD + "" + ChatColor.BOLD + "SLOT MACHINE",
                ChatColor.GRAY + "Match 3+ symbols in a row to win!",
                ChatColor.YELLOW + "Jackpot: " + economy.format(Gambling.getJackpot().getJackpot())));
    }

    // Helper method to create an ItemStack for the GUI
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

    public void spin() {
        // Check balance and withdraw before spinning
        if (economy.getBalance(player) < spinCost) {
            player.sendActionBar(ChatColor.RED + "Insufficient funds! Need " + economy.format(spinCost));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        economy.withdrawPlayer(player, spinCost);
        player.sendActionBar(ChatColor.YELLOW + "Spinning... Bet: " + economy.format(spinCost));

        // Start the animation using the Bukkit Scheduler
        new BukkitRunnable() {
            private int ticks = 0;
            private final int totalTicks = 60; // 3 seconds of spinning
            private final boolean[] reelStopped = new boolean[5]; // Track which reels have stopped

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    this.cancel(); // Stop the animation
                    calculateWinnings();
                    return;
                }

                // Stop reels progressively (more realistic)
                int reelsToSpin = Math.max(0, 5 - (ticks / 12)); // Stop one reel every 12 ticks

                // Update the reel items to simulate spinning
                for (int reel = 0; reel < 5; reel++) {
                    if (reel < reelsToSpin) { // Only spin if reel hasn't stopped
                        for (int row = 0; row < 3; row++) {
                            Material randomItem = reelItems.get(ThreadLocalRandom.current().nextInt(reelItems.size()));
                            gui.setItem(reelPositions[reel][row], new ItemStack(randomItem));
                        }
                    }
                }

                // Play sound with varying pitch for excitement
                float pitch = 1.0f + (ticks * 0.02f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, pitch);
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run this task every 2 ticks
    }

    private void calculateWinnings() {
        // Add a portion of the spin cost to the jackpot
        Gambling.getJackpot().addToJackpot(spinCost * 0.1);

        double totalWinnings = 0;
        int winningLines = 0;

        // Define paylines (horizontal, diagonal, etc.)
        int[][] paylines = {
            {10, 11, 12, 13, 14}, // Top row
            {19, 20, 21, 22, 23}, // Middle row
            {28, 29, 30, 31, 32}, // Bottom row
            {10, 20, 30, 22, 14}, // Diagonal top-left to bottom-right
            {28, 20, 12, 22, 14}  // Diagonal bottom-left to top-right
        };

        // Check each payline for wins
        for (int[] payline : paylines) {
            double lineWinnings = checkPayline(payline);
            if (lineWinnings > 0) {
                totalWinnings += lineWinnings;
                winningLines++;
            }
        }

        // Show results with title/subtitle
        if (totalWinnings > 0) {
            // Check for jackpot (5 diamonds on any line)
            boolean isJackpot = checkForJackpot();
            if (isJackpot) {
                totalWinnings += Gambling.getJackpot().getJackpot();
                Gambling.getJackpot().resetJackpot();
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " has won the MEGA JACKPOT of " + economy.format(totalWinnings) + "!");
                player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "MEGA JACKPOT!",
                               ChatColor.YELLOW + "+" + economy.format(totalWinnings), 10, 60, 20);
            } else {
                player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "YOU WON!",
                               ChatColor.GOLD + "+" + economy.format(totalWinnings) + " (" + winningLines + " lines)", 10, 40, 10);
            }

            economy.depositPlayer(player, totalWinnings);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), totalWinnings);
            player.sendActionBar(ChatColor.GREEN + "Congratulations! You won " + economy.format(totalWinnings) + " on " + winningLines + " paylines!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), spinCost);
            player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "NO WIN",
                           ChatColor.GRAY + "Better luck next time!", 10, 40, 10);
            player.sendActionBar(ChatColor.RED + "No winning combinations this spin");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
    }

    private double checkPayline(int[] payline) {
        // Get the materials in this payline
        Material[] materials = new Material[5];
        for (int i = 0; i < 5; i++) {
            ItemStack item = gui.getItem(payline[i]);
            if (item == null) return 0;
            materials[i] = item.getType();
        }

        // Check for consecutive matches from left to right
        Material firstMaterial = materials[0];
        int consecutiveMatches = 1;

        for (int i = 1; i < 5; i++) {
            if (materials[i] == firstMaterial) {
                consecutiveMatches++;
            } else {
                break;
            }
        }

        // Calculate winnings based on consecutive matches
        if (consecutiveMatches >= 3) {
            double baseMultiplier = getMultiplier(firstMaterial);
            double consecutiveBonus = Math.pow(2, consecutiveMatches - 3); // 2x for 4 matches, 4x for 5 matches
            return spinCost * baseMultiplier * consecutiveBonus;
        }

        return 0;
    }

    private boolean checkForJackpot() {
        // Check if any payline has 5 diamonds
        int[][] paylines = {
            {10, 11, 12, 13, 14}, // Top row
            {19, 20, 21, 22, 23}, // Middle row
            {28, 29, 30, 31, 32}, // Bottom row
            {10, 20, 30, 22, 14}, // Diagonal top-left to bottom-right
            {28, 20, 12, 22, 14}  // Diagonal bottom-left to top-right
        };

        for (int[] payline : paylines) {
            boolean allDiamonds = true;
            for (int slot : payline) {
                ItemStack item = gui.getItem(slot);
                if (item == null || item.getType() != Material.DIAMOND) {
                    allDiamonds = false;
                    break;
                }
            }
            if (allDiamonds) return true;
        }
        return false;
    }

    private double getMultiplier(Material material) {
        switch (material) {
            case DIAMOND:
                return 50.0; // High payout for diamonds
            case EMERALD:
                return 25.0;
            case GOLD_INGOT:
                return 15.0;
            case IRON_INGOT:
                return 8.0;
            case LAPIS_LAZULI:
                return 4.0;
            case COAL:
                return 2.0;
            default:
                return 0.0;
        }
    }

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }
}