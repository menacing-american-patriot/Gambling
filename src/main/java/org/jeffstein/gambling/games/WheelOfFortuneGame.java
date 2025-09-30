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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WheelOfFortuneGame implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();

    private final Map<String, Double> playerBets = new HashMap<>();
    private boolean spinning = false;

    // Adjustable betting amount
    private double currentBet = 100.0;
    private static final double MIN_BET = 10.0;
    private static final double MAX_BET = 1000000.0;

    // Wheel segments (24 segments total)
    private final String[] wheelSegments = {
        "1x", "LOSE", "2x", "LOSE", "5x", "LOSE", "3x", "LOSE",
        "10x", "LOSE", "2x", "LOSE", "1x", "LOSE", "4x", "LOSE",
        "20x", "LOSE", "3x", "LOSE", "1x", "LOSE", "50x", "JACKPOT"
    };

    private final Material[] segmentColors = {
        Material.WHITE_CONCRETE, Material.RED_CONCRETE, Material.YELLOW_CONCRETE, Material.RED_CONCRETE,
        Material.LIME_CONCRETE, Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE,
        Material.BLUE_CONCRETE, Material.RED_CONCRETE, Material.YELLOW_CONCRETE, Material.RED_CONCRETE,
        Material.WHITE_CONCRETE, Material.RED_CONCRETE, Material.PURPLE_CONCRETE, Material.RED_CONCRETE,
        Material.CYAN_CONCRETE, Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE,
        Material.WHITE_CONCRETE, Material.RED_CONCRETE, Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK
    };

    // Wheel positions in GUI (circular arrangement)
    private final int[] wheelPositions = {
        4, 5, 6, 7, 8, 17, 26, 35, 44, 43, 42, 41,
        40, 31, 22, 13, 12, 11, 10, 9, 18, 27, 36, 45
    };

    public WheelOfFortuneGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.GOLD + "🎡 WHEEL OF FORTUNE 🎡");
        initializeWheel();
    }

    private void initializeWheel() {
        // Fill background
        ItemStack background = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Create wheel segments
        for (int i = 0; i < wheelSegments.length; i++) {
            String segment = wheelSegments[i];
            Material color = segmentColors[i];
            int position = wheelPositions[i];

            ChatColor textColor = getTextColor(segment);
            gui.setItem(position, createGuiItem(color, textColor + segment,
                    ChatColor.GRAY + "Wheel segment",
                    ChatColor.YELLOW + "Bet on this segment to win!"));
        }

        // Center pointer
        gui.setItem(23, createGuiItem(Material.ARROW, ChatColor.WHITE + "" + ChatColor.BOLD + "POINTER",
                ChatColor.GRAY + "This shows the winning segment"));

        // Betting controls
        setupBettingControls();

        // Spin button
        gui.setItem(49, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "SPIN WHEEL",
                ChatColor.GRAY + "Click to spin the wheel!",
                ChatColor.YELLOW + "Make sure to place bets first"));

        // Back button
        gui.setItem(45, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Close Wheel of Fortune"));
    }

    private void setupBettingControls() {
        // Bet amount adjusters (bottom row sides)
        gui.setItem(37, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet by 100"));
        gui.setItem(38, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-50",
                ChatColor.GRAY + "Decrease bet by 50"));
        gui.setItem(39, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-10",
                ChatColor.GRAY + "Decrease bet by 10"));
        gui.setItem(51, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+10",
                ChatColor.GRAY + "Increase bet by 10"));
        gui.setItem(52, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+50",
                ChatColor.GRAY + "Increase bet by 50"));
        gui.setItem(53, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet by 100"));

        // Current bet display (center bottom)
        gui.setItem(32, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet",
                ChatColor.WHITE + economy.format(currentBet),
                ChatColor.GRAY + "Adjust using the controls"));

        // Betting options (use currentBet)
        gui.setItem(46, createGuiItem(Material.GOLD_NUGGET, ChatColor.GOLD + "Bet on Numbers",
                ChatColor.GRAY + "Bet " + economy.format(currentBet) + " on all number segments",
                ChatColor.GREEN + "Click to place bet"));

        gui.setItem(47, createGuiItem(Material.REDSTONE, ChatColor.RED + "Bet on LOSE",
                ChatColor.GRAY + "Bet " + economy.format(currentBet) + " on all LOSE segments",
                ChatColor.GREEN + "Click to place bet"));

        gui.setItem(48, createGuiItem(Material.DIAMOND, ChatColor.AQUA + "Bet on JACKPOT",
                ChatColor.GRAY + "Bet " + economy.format(currentBet) + " on JACKPOT segment",
                ChatColor.GREEN + "Click to place bet"));

        String[] betSummary = getBetSummary();
        gui.setItem(50, createGuiItem(Material.BOOK, ChatColor.YELLOW + "Your Bets", betSummary));
    }
    public void adjustBet(double delta) {
        setBetAmount(currentBet + delta);
    }

    private void setBetAmount(double amount) {
        currentBet = Math.max(MIN_BET, Math.min(MAX_BET, amount));
        // Refresh bet display and controls to reflect updated amount
        setupBettingControls();
    }

    public double getCurrentBet() {
        return currentBet;
    }


    private String[] getBetSummary() {
        if (playerBets.isEmpty()) {
            return new String[]{ChatColor.GRAY + "No bets placed"};
        }

        String[] summary = new String[playerBets.size() + 1];
        summary[0] = ChatColor.GRAY + "Current bets:";
        int i = 1;
        for (Map.Entry<String, Double> entry : playerBets.entrySet()) {
            summary[i] = ChatColor.WHITE + entry.getKey() + ": " + economy.format(entry.getValue());
            i++;
        }
        return summary;
    }

    private ChatColor getTextColor(String segment) {
        switch (segment) {
            case "LOSE": return ChatColor.RED;
            case "JACKPOT": return ChatColor.GOLD;
            case "50x": return ChatColor.AQUA;
            case "20x": return ChatColor.BLUE;
            case "10x": return ChatColor.GREEN;
            default: return ChatColor.WHITE;
        }
    }

    public boolean placeBet(String betType, double amount) {
        if (spinning) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[WHEEL] Cannot bet while wheel is spinning!");
            return false;
        }

        if (economy.getBalance(player) < amount) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[WHEEL] Insufficient funds!");
            return false;
        }

        economy.withdrawPlayer(player, amount);
        playerBets.put(betType, playerBets.getOrDefault(betType, 0.0) + amount);

        // Sound + chat feedback for successful bet
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "[WHEEL] Bet placed: " + economy.format(amount) + " on " + betType);
        setupBettingControls(); // Update bet display
        return true;
    }

    public void spinWheel() {
        if (spinning) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[WHEEL] Wheel is already spinning!");
            return;
        }

        if (playerBets.isEmpty()) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[WHEEL] Place a bet first!");
            return;
        }

        spinning = true;
        // GUI + chat feedback
        updateStatusDisplay(ChatColor.YELLOW + "SPINNING...",
                          ChatColor.GRAY + "Wheel is spinning...",
                          ChatColor.GREEN + "Good luck!");
        player.sendMessage(ChatColor.YELLOW + "[WHEEL] Spinning the wheel...");

        // Determine winning segment
        int winningIndex = ThreadLocalRandom.current().nextInt(wheelSegments.length);
        String winningSegment = wheelSegments[winningIndex];

        // Animate the spin
        animateSpin(winningIndex, winningSegment);
    }

    private void animateSpin(int finalIndex, String winningSegment) {
        new BukkitRunnable() {
            int currentIndex = 0;
            int ticks = 0;
            final int totalTicks = 60; // 3 seconds
            int ticksPerSegment = 2; // Start fast

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    // Spin complete
                    highlightWinningSegment(finalIndex);
                    calculateWinnings(winningSegment);
                    spinning = false;
                    this.cancel();
                    return;
                }

                // Clear previous highlight
                clearHighlights();

                // Highlight current segment
                highlightSegment(currentIndex);

                // Play tick sound
                float pitch = 1.0f + (ticks * 0.02f);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, pitch);

                // Slow down as we approach the end
                if (ticks > totalTicks * 0.7) {
                    ticksPerSegment = 8; // Very slow
                } else if (ticks > totalTicks * 0.5) {
                    ticksPerSegment = 4; // Medium
                }

                if (ticks % ticksPerSegment == 0) {
                    currentIndex = (currentIndex + 1) % wheelSegments.length;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void highlightSegment(int index) {
        int position = wheelPositions[index];
        String segment = wheelSegments[index];
        ChatColor textColor = getTextColor(segment);

        gui.setItem(position, createGuiItem(Material.GLOWSTONE, textColor + "" + ChatColor.BOLD + segment,
                ChatColor.YELLOW + "Current position"));
    }

    private void highlightWinningSegment(int index) {
        int position = wheelPositions[index];
        String segment = wheelSegments[index];

        gui.setItem(position, createGuiItem(Material.BEACON, ChatColor.GOLD + "" + ChatColor.BOLD + segment,
                ChatColor.GREEN + "WINNING SEGMENT!"));
    }

    private void clearHighlights() {
        for (int i = 0; i < wheelSegments.length; i++) {
            String segment = wheelSegments[i];
            Material color = segmentColors[i];
            int position = wheelPositions[i];
            ChatColor textColor = getTextColor(segment);

            gui.setItem(position, createGuiItem(color, textColor + segment,
                    ChatColor.GRAY + "Wheel segment"));
        }
    }

    private void calculateWinnings(String winningSegment) {
        double totalWinnings = 0;
        boolean won = false;

        if (player.getOpenInventory().getTopInventory().getHolder() == this) {
            player.closeInventory();
        }

        // Check each bet type
        for (Map.Entry<String, Double> bet : playerBets.entrySet()) {
            String betType = bet.getKey();
            double betAmount = bet.getValue();
            double payout = 0;

            if (betType.equals("Numbers") && !winningSegment.equals("LOSE") && !winningSegment.equals("JACKPOT")) {
                // Won on numbers bet
                String multiplierStr = winningSegment.replace("x", "");
                double multiplier = Double.parseDouble(multiplierStr);
                payout = betAmount * multiplier;
            } else if (betType.equals("LOSE") && winningSegment.equals("LOSE")) {
                // Won on LOSE bet (2:1 payout)
                payout = betAmount * 2;
            } else if (betType.equals("JACKPOT") && winningSegment.equals("JACKPOT")) {
                // Won jackpot (50:1 payout + current jackpot)
                payout = betAmount * 50 + Gambling.getJackpot().getJackpot();
                Gambling.getJackpot().resetJackpot();
            }

            if (payout > 0) {
                totalWinnings += payout;
                won = true;
            }
        }

        // Show results
        if (won) {
            economy.depositPlayer(player, totalWinnings);
            double profit = totalWinnings - getTotalBets();

            if (winningSegment.equals("JACKPOT")) {
                // Close GUI for jackpot so message is visible
                player.closeInventory();
                player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "JACKPOT!",
                               ChatColor.YELLOW + "+" + economy.format(profit) + " on " + winningSegment, 10, 80, 20);
                // Also send to chat
                player.sendMessage(ChatColor.GOLD + "[WHEEL] " + ChatColor.BOLD + "JACKPOT! " +
                                 ChatColor.YELLOW + "+" + economy.format(profit) + " on " + winningSegment);
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " won the Wheel of Fortune JACKPOT!");
            } else {
                // GUI + chat feedback for regular wins
                updateStatusDisplay(ChatColor.GREEN + "" + ChatColor.BOLD + "YOU WON!",
                                  ChatColor.GOLD + "+" + economy.format(profit),
                                  ChatColor.WHITE + "Landed on: " + winningSegment);
                player.sendMessage(ChatColor.GREEN + "[WHEEL] You won " + economy.format(profit) +
                                 " on " + winningSegment + "!");
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);
        } else {
            // GUI + chat feedback for losses
            updateStatusDisplay(ChatColor.RED + "" + ChatColor.BOLD + "YOU LOST",
                              ChatColor.GRAY + "Landed on: " + winningSegment,
                              ChatColor.DARK_GRAY + "Better luck next time!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[WHEEL] You lost! Landed on " + winningSegment);
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), getTotalBets());
        }

        // Add to jackpot
        Gambling.getJackpot().addToJackpot(getTotalBets() * 0.1);

        // Clear bets for next round
        playerBets.clear();
        setupBettingControls();
    }

    private double getTotalBets() {
        return playerBets.values().stream().mapToDouble(Double::doubleValue).sum();
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

    public boolean isSpinning() {
        return spinning;
    }

    private void updateStatusDisplay(String title, String... lore) {
        // Update the status display item (slot 50) to show current status
        gui.setItem(50, createGuiItem(Material.BOOK, title, lore));
    }
}
