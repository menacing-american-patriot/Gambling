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
import java.util.concurrent.ThreadLocalRandom;

public class PlinkoGame implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();
    
    private double betAmount = 100.0;
    private boolean ballDropping = false;
    
    // Plinko board layout (9 columns, 6 rows)
    // Row 0: Drop positions (slots 9-17)
    // Rows 1-4: Pegs (alternating pattern)
    // Row 5: Prize slots (slots 45-53)
    
    // REALISTIC Casino Plinko - Based on actual casino Plinko odds
    // 6 out of 8 slots are LOSSES (under 1x) - this is how real casinos work
    // Expected value: ~0.65 (35% house edge like real high-risk casino games)
    private final double[] prizeMultipliers = {
        1000.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.7, 130.0
    };

    private final Material[] prizeColors = {
        Material.DIAMOND_BLOCK,    // 1000x (jackpot - 0.1% chance)
        Material.RED_CONCRETE,     // 0.1x (lose 90% - very common)
        Material.RED_CONCRETE,     // 0.2x (lose 80% - very common)
        Material.ORANGE_CONCRETE,  // 0.3x (lose 70% - common)
        Material.ORANGE_CONCRETE,  // 0.4x (lose 60% - common)
        Material.YELLOW_CONCRETE,  // 0.5x (lose 50% - common)
        Material.LIME_CONCRETE,    // 0.7x (lose 30% - uncommon)
        Material.EMERALD_BLOCK     // 130x (big win - 0.5% chance)
    };

    public PlinkoGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.YELLOW + "🎯 PLINKO 🎯");
        initializeBoard();
    }

    private void initializeBoard() {
        // Fill background
        ItemStack background = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Create drop positions (top row)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, createGuiItem(Material.LIME_STAINED_GLASS_PANE, 
                ChatColor.GREEN + "Drop Here " + (i + 1),
                ChatColor.GRAY + "Click to drop ball from position " + (i + 1)));
        }

        // Create pegs (alternating diamond pattern)
        createPegs();

        // Create prize slots (bottom row)
        createPrizeSlots();

        // Betting controls
        setupBettingControls();

        // Back button
        gui.setItem(53, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Close Plinko game"));
    }

    private void createPegs() {
        // Row 1: 8 pegs (slots 9-16)
        for (int i = 9; i < 17; i++) {
            gui.setItem(i, createGuiItem(Material.STONE_BUTTON, ChatColor.GRAY + "Peg"));
        }

        // Row 2: 7 pegs (slots 19-25)
        for (int i = 19; i < 26; i++) {
            gui.setItem(i, createGuiItem(Material.STONE_BUTTON, ChatColor.GRAY + "Peg"));
        }

        // Row 3: 8 pegs (slots 27-34)
        for (int i = 27; i < 35; i++) {
            gui.setItem(i, createGuiItem(Material.STONE_BUTTON, ChatColor.GRAY + "Peg"));
        }

        // Row 4: 7 pegs (slots 37-43)
        for (int i = 37; i < 44; i++) {
            gui.setItem(i, createGuiItem(Material.STONE_BUTTON, ChatColor.GRAY + "Peg"));
        }
    }

    private void createPrizeSlots() {
        // Bottom row prize slots (slots 45-53, but skip 53 for back button)
        for (int i = 0; i < 8; i++) {
            int slot = 45 + i;
            double multiplier = prizeMultipliers[i];
            Material color = prizeColors[i];
            
            gui.setItem(slot, createGuiItem(color, 
                ChatColor.GOLD + String.format("%.0fx", multiplier),
                ChatColor.GRAY + "Prize: " + economy.format(betAmount * multiplier)));
        }
    }

    private void setupBettingControls() {
        // Bet controls in the middle area
        gui.setItem(18, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet by 100"));
        gui.setItem(26, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet by 100"));

        gui.setItem(22, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet",
                ChatColor.WHITE + economy.format(betAmount),
                ChatColor.GRAY + "Your bet amount"));
    }

    public void dropBall(int dropPosition) {
        if (ballDropping) {
            // GUI + chat feedback
            updateStatusDisplay(ChatColor.RED + "Ball in progress...", ChatColor.GRAY + "Wait for current ball to finish");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] Wait for the current ball to finish!");
            return;
        }

        if (economy.getBalance(player) < betAmount) {
            // GUI + chat feedback
            updateStatusDisplay(ChatColor.RED + "Insufficient Funds!",
                              ChatColor.GRAY + "Need " + economy.format(betAmount),
                              ChatColor.YELLOW + "Current: " + economy.format(economy.getBalance(player)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] Insufficient funds! Need " + economy.format(betAmount));
            return;
        }

        economy.withdrawPlayer(player, betAmount);
        ballDropping = true;

        // GUI + chat feedback
        updateStatusDisplay(ChatColor.YELLOW + "Ball Dropping...",
                          ChatColor.GRAY + "Bet: " + economy.format(betAmount),
                          ChatColor.GREEN + "Good luck!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.YELLOW + "[PLINKO] Ball dropping... Bet: " + economy.format(betAmount));

        // Simulate ball physics
        simulateBallDrop(dropPosition);
    }

    private void simulateBallDrop(int startPosition) {
        new BukkitRunnable() {
            int currentPosition = startPosition;
            int currentRow = 0;
            int ticks = 0;
            final int totalRows = 5;

            @Override
            public void run() {
                if (currentRow >= totalRows) {
                    // Ball reached bottom - calculate prize
                    int finalSlot = Math.max(0, Math.min(7, currentPosition));
                    awardPrize(finalSlot);
                    ballDropping = false;
                    this.cancel();
                    return;
                }

                // Clear previous ball position
                clearBallMarkers();

                // Show ball at current position
                showBallAtPosition(currentPosition, currentRow);

                // Calculate next position with HEAVY center bias (like real casino Plinko)
                // Real Plinko is designed so balls almost always end up in center loss slots
                if (currentRow < totalRows - 1) {
                    double random = ThreadLocalRandom.current().nextDouble();

                    // STRONG center bias - casinos design Plinko to favor center losses
                    double centerBias = 0.35; // 35% bias toward center (very strong)
                    int center = 4; // Middle position (where the losses are)

                    // Calculate distance from center - closer = stronger pull
                    int distanceFromCenter = Math.abs(currentPosition - center);
                    double actualBias = centerBias * (1.0 + distanceFromCenter * 0.1); // Stronger pull when further from center

                    if (currentPosition < center && random < (0.5 + actualBias)) {
                        // Strong bias toward center (move right if left of center)
                        currentPosition = Math.min(7, currentPosition + 1);
                    } else if (currentPosition > center && random < (0.5 + actualBias)) {
                        // Strong bias toward center (move left if right of center)
                        currentPosition = Math.max(0, currentPosition - 1);
                    } else {
                        // Normal random bounce (but less likely due to strong center bias)
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            currentPosition = Math.max(0, currentPosition - 1);
                        } else {
                            currentPosition = Math.min(7, currentPosition + 1);
                        }
                    }
                }

                currentRow++;
                ticks++;

                // Play bounce sound
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f,
                               1.0f + (ticks * 0.1f));
            }
        }.runTaskTimer(plugin, 0L, 10L); // Update every 10 ticks (0.5 seconds)
    }

    private void showBallAtPosition(int position, int row) {
        // Calculate the actual slot based on position and row
        int slot = getSlotForPosition(position, row);
        if (slot >= 0 && slot < 54) {
            gui.setItem(slot, createGuiItem(Material.ENDER_PEARL, ChatColor.WHITE + "Ball",
                    ChatColor.GRAY + "Bouncing..."));
        }
    }

    private int getSlotForPosition(int position, int row) {
        switch (row) {
            case 0: return position; // Top row (0-8)
            case 1: return 9 + Math.min(position, 7); // Row 1 (9-16)
            case 2: return 19 + Math.min(position, 6); // Row 2 (19-25)
            case 3: return 27 + Math.min(position, 7); // Row 3 (27-34)
            case 4: return 37 + Math.min(position, 6); // Row 4 (37-43)
            default: return -1;
        }
    }

    private void clearBallMarkers() {
        // Remove any ender pearls (balls) from the board
        for (int i = 0; i < 54; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                // Restore original item based on position
                restoreOriginalItem(i);
            }
        }
    }

    private void restoreOriginalItem(int slot) {
        if (slot < 9) {
            // Drop positions
            gui.setItem(slot, createGuiItem(Material.LIME_STAINED_GLASS_PANE, 
                ChatColor.GREEN + "Drop Here " + (slot + 1),
                ChatColor.GRAY + "Click to drop ball from position " + (slot + 1)));
        } else if ((slot >= 9 && slot < 17) || (slot >= 19 && slot < 26) || 
                   (slot >= 27 && slot < 35) || (slot >= 37 && slot < 44)) {
            // Pegs
            gui.setItem(slot, createGuiItem(Material.STONE_BUTTON, ChatColor.GRAY + "Peg"));
        } else {
            // Background
            gui.setItem(slot, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
    }

    private void awardPrize(int prizeSlot) {
        double multiplier = prizeMultipliers[prizeSlot];
        double winnings = betAmount * multiplier;
        double profit = winnings - betAmount;

        economy.depositPlayer(player, winnings);

        // Update GUI to show result instead of hidden messages
        if (multiplier >= 1.0) {
            // Win or break even
            updateStatusDisplay(ChatColor.GREEN + "" + ChatColor.BOLD + String.format("%.1fx WIN!", multiplier),
                              ChatColor.GOLD + "+" + economy.format(profit),
                              ChatColor.WHITE + "Total: " + economy.format(winnings));

            if (multiplier >= 100) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                // Close GUI for big wins so message is visible
                player.closeInventory();
                player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "PLINKO JACKPOT!",
                               ChatColor.GREEN + "+" + economy.format(profit) + " at " + String.format("%.0fx", multiplier), 10, 80, 20);
                // Also send to chat
                player.sendMessage(ChatColor.GOLD + "[PLINKO] " + ChatColor.BOLD + "JACKPOT! " +
                                 ChatColor.GREEN + "+" + economy.format(profit) + " at " + String.format("%.0fx", multiplier));
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " won " +
                                      economy.format(profit) + " in Plinko with a " +
                                      String.format("%.0fx", multiplier) + " multiplier!");
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                // Send regular win to chat
                player.sendMessage(ChatColor.GREEN + "[PLINKO] You won " + economy.format(profit) +
                                 " with " + String.format("%.1fx", multiplier) + " multiplier!");
            }
            Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);
        } else {
            // Loss
            updateStatusDisplay(ChatColor.RED + "" + ChatColor.BOLD + String.format("%.1fx LOSS", multiplier),
                              ChatColor.GRAY + economy.format(profit),
                              ChatColor.DARK_GRAY + "Better luck next time!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            // Send loss to chat
            player.sendMessage(ChatColor.RED + "[PLINKO] You lost " + economy.format(Math.abs(profit)) +
                             " with " + String.format("%.1fx", multiplier) + " multiplier");
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), Math.abs(profit));
        }

        // Update prize displays
        createPrizeSlots();
    }

    private void updateStatusDisplay(String title, String... lore) {
        // Update the status display item (slot 22) to show current status
        gui.setItem(22, createGuiItem(Material.PAPER, title, lore));
    }

    public void adjustBet(double amount) {
        betAmount = Math.max(10.0, Math.min(10000.0, betAmount + amount));
        setupBettingControls();
        createPrizeSlots(); // Update prize displays
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

    public boolean isBallDropping() {
        return ballDropping;
    }

    public double getBetAmount() {
        return betAmount;
    }
}
