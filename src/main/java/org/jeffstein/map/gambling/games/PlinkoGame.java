package org.jeffstein.map.gambling.games;

import org.jeffstein.map.gambling.Gambling;
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

    private enum RiskLevel {
        SAFE,
        STANDARD,
        HIGH
    }

    private static final double[][] RISK_MULTIPLIERS = {
        {5.6, 2.1, 1.1, 1.0, 0.5, 1.0, 1.1, 2.1, 5.6},
        {13.0, 3.0, 1.3, 0.7, 0.4, 0.7, 1.3, 3.0, 13.0},
        {29.0, 4.0, 1.5, 0.3, 0.2, 0.3, 1.5, 4.0, 29.0}
    };

    private static final Material[][] RISK_COLORS = {
        {Material.EMERALD_BLOCK, Material.LIME_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.WHITE_CONCRETE,
         Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.LIME_CONCRETE, Material.EMERALD_BLOCK},
        {Material.DIAMOND_BLOCK, Material.YELLOW_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.ORANGE_CONCRETE,
         Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.DIAMOND_BLOCK},
        {Material.NETHERITE_BLOCK, Material.RED_CONCRETE, Material.PURPLE_CONCRETE, Material.BLACK_CONCRETE,
         Material.COAL_BLOCK, Material.BLACK_CONCRETE, Material.PURPLE_CONCRETE, Material.RED_CONCRETE, Material.NETHERITE_BLOCK}
    };

    private static final double[] RISK_CENTER_BIAS = {0.18, 0.28, 0.4};

    private static final ChatColor[] RISK_LABEL_COLORS = {
        ChatColor.GREEN, ChatColor.GOLD, ChatColor.RED
    };

    private double[] prizeMultipliers;
    private Material[] prizeColors;

    private double betAmount = 100.0;
    private double lastBetAmount = betAmount;
    private boolean ballDropping = false;
    private RiskLevel riskLevel = RiskLevel.STANDARD;
    private double centerBias = RISK_CENTER_BIAS[riskLevel.ordinal()];

    private double totalWagered = 0.0;
    private double totalReturned = 0.0;
    private int dropsPlayed = 0;
    private int lastDropColumn = -1;

    public PlinkoGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.YELLOW + "🎯 PLINKO 🎯");
        applyRiskProfile();
        buildBoard();
        initializeTopRowControls();
        createPegs();
        refreshPrizes();
        refreshBetDisplay();
        updateStatusDisplay(ChatColor.GOLD + "Select a column", ChatColor.GRAY + "Columns 3-7 are active drop slots");
        updateRiskButton();
        updateStatsPanel();
        updateDropIndicators();
    }
    private void buildBoard() {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                boolean dark = (row + col) % 2 == 0;
                Material background = dark ? Material.BLACK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                gui.setItem(slot, createGuiItem(background, " "));
            }
        }

        updateDropIndicators();
    }

    private void createPegs() {
        Material[] pegMaterials = {Material.END_ROD, Material.AMETHYST_CLUSTER};
        String[] pegNames = {ChatColor.GRAY + "Guiding Peg", ChatColor.LIGHT_PURPLE + "Crystal Peg"};
        int[][] pegLayout = {
            {10, 11, 12, 13, 14, 15, 16},
            {18, 19, 20, 21, 22, 23, 24, 25, 26},
            {27, 28, 29, 30, 31, 32, 33, 34, 35},
            {36, 37, 38, 39, 40, 41, 42, 43, 44}
        };
        int index = 0;
        for (int[] rowSlots : pegLayout) {
            for (int slot : rowSlots) {
                Material material = pegMaterials[index % pegMaterials.length];
                String name = pegNames[index % pegNames.length];
                gui.setItem(slot, createGuiItem(material, name,
                        ChatColor.DARK_GRAY + "Deflects the ball"));
                index++;
            }
        }
    }

    private void refreshPrizes() {
        for (int i = 0; i < prizeMultipliers.length; i++) {
            int slot = 45 + i;
            double multiplier = prizeMultipliers[i];
            Material color = prizeColors[i];
            gui.setItem(slot, createGuiItem(color,
                    ChatColor.GOLD + String.format("%.1fx", multiplier),
                    ChatColor.GRAY + "Payout: " + economy.format(betAmount * multiplier),
                    ChatColor.DARK_GRAY + "Risk: " + riskLevel.name()));
        }
    }

    private void initializeTopRowControls() {
        // Symmetrical top-row controls: 0/1 are decreases, 7/8 are increases.
        gui.setItem(0, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-1000",
                ChatColor.GRAY + "Decrease bet by 1000"));
        gui.setItem(1, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-50",
                ChatColor.GRAY + "Decrease bet by 50"));
        gui.setItem(7, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+50",
                ChatColor.GRAY + "Increase bet by 50"));
        gui.setItem(8, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+1000",
                ChatColor.GRAY + "Increase bet by 1000"));
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
        lastBetAmount = betAmount;
        lastDropColumn = dropPosition;
        updateDropIndicators();
        refreshBetDisplay();

        // GUI + chat feedback
        updateStatusDisplay(ChatColor.YELLOW + "Ball Dropping...",
                          ChatColor.GRAY + "Bet: " + economy.format(betAmount),
                          ChatColor.GREEN + "Good luck!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.YELLOW + "[PLINKO] Ball dropping... Bet: " + economy.format(betAmount));

        // Simulate ball physics
        simulateBallDrop(dropPosition);
    }

    public void dropBallCommand(double amount, int column) {
        column = Math.max(1, Math.min(prizeMultipliers.length, column)) - 1;

        if (ballDropping) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] A ball is already in progress.");
            return;
        }

        if (amount < 10 || amount > 100000) {
            player.sendMessage(ChatColor.RED + "[PLINKO] Bet must be between 10 and 100000.");
            return;
        }

        if (economy.getBalance(player) < amount) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] Insufficient funds! Need " + economy.format(amount));
            return;
        }

        economy.withdrawPlayer(player, amount);
        ballDropping = true;
        lastBetAmount = amount;
        lastDropColumn = column;
        updateDropIndicators();
        refreshBetDisplay();

        player.sendMessage(ChatColor.YELLOW + "[PLINKO] Dropping ball from column " + (column + 1) +
            " with bet " + economy.format(amount) + "...");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        updateStatusDisplay(ChatColor.YELLOW + "Ball Dropping...",
                ChatColor.GRAY + "Bet: " + economy.format(amount),
                ChatColor.GREEN + "Good luck!");

        double previousBet = betAmount;
        betAmount = amount;
        simulateBallDropCommand(column, previousBet);
    }

    private void simulateBallDropCommand(int startPosition, double previousBet) {
        new BukkitRunnable() {
            int currentPosition = startPosition;
            int currentRow = 0;
            final int totalRows = 5;

            @Override
            public void run() {
                if (currentRow >= totalRows) {
                    int finalSlot = Math.max(0, Math.min(prizeMultipliers.length - 1, currentPosition));
                    double winnings = resolvePrize(finalSlot, true);
                    clearBallMarkers();
                    betAmount = previousBet;
                    ballDropping = false;
                    this.cancel();
                    sendPrizeMessage(finalSlot, winnings);
                    refreshBetDisplay();
                    refreshPrizes();
                    updateDropIndicators();
                    updateStatusDisplay(ChatColor.GOLD + "Ready for next drop", ChatColor.GRAY + "Pick a column to play again");
                    return;
                }

                if (currentRow < totalRows - 1) {
                    double random = ThreadLocalRandom.current().nextDouble();
                    int center = 4;
                    int distanceFromCenter = Math.abs(currentPosition - center);
                    double actualBias = centerBias * (1.0 + distanceFromCenter * 0.1);

                    if (currentPosition < center && random < (0.5 + actualBias)) {
                        currentPosition = Math.min(prizeMultipliers.length - 1, currentPosition + 1);
                    } else if (currentPosition > center && random < (0.5 + actualBias)) {
                        currentPosition = Math.max(0, currentPosition - 1);
                    } else {
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            currentPosition = Math.max(0, currentPosition - 1);
                        } else {
                            currentPosition = Math.min(prizeMultipliers.length - 1, currentPosition + 1);
                        }
                    }
                }

                currentRow++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
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
                    int finalSlot = Math.max(0, Math.min(prizeMultipliers.length - 1, currentPosition));
                    awardPrize(finalSlot);
                    clearBallMarkers();
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
                    int center = 4; // Middle position (columns 4-6)

                    // Calculate distance from center - closer = stronger pull
                    int distanceFromCenter = Math.abs(currentPosition - center);
                    double actualBias = centerBias * (1.0 + distanceFromCenter * 0.1); // Stronger pull when further from center

                    if (currentPosition < center && random < (0.5 + actualBias)) {
                        // Strong bias toward center (move right if left of center)
                        currentPosition = Math.min(prizeMultipliers.length - 1, currentPosition + 1);
                    } else if (currentPosition > center && random < (0.5 + actualBias)) {
                        // Strong bias toward center (move left if right of center)
                        currentPosition = Math.max(0, currentPosition - 1);
                    } else {
                        // Normal random bounce (but less likely due to strong center bias)
                        if (ThreadLocalRandom.current().nextBoolean()) {
                            currentPosition = Math.max(0, currentPosition - 1);
                        } else {
                            currentPosition = Math.min(prizeMultipliers.length - 1, currentPosition + 1);
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
            case 0:
                return position; // Drop row (0-8)
            case 1:
                return 9 + clampColumn(position); // Peg row 1 (9-17)
            case 2:
                return 27 + clampColumn(position); // Peg row 2 (27-35)
            case 3:
                return 36 + clampColumn(position); // Peg row 3 (36-44)
            case 4:
                return 45 + clampColumn(position); // Prize row (45-53)
            default:
                return -1;
        }
    }

    private int clampColumn(int column) {
        return Math.max(0, Math.min(prizeMultipliers.length - 1, column));
    }

    private void clearBallMarkers() {
        for (int i = 0; i < 54; i++) {
            ItemStack item = gui.getItem(i);
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                gui.setItem(i, createGuiItem(getBackgroundMaterial(i), " "));
            }
        }
        updateDropIndicators();
        createPegs();
        refreshPrizes();
        refreshBetDisplay();
        updateRiskButton();
        updateStatsPanel();
    }

    private void awardPrize(int prizeSlot) {
        double winnings = resolvePrize(prizeSlot, false);
        sendPrizeMessage(prizeSlot, winnings);
    }

    private double resolvePrize(int prizeSlot, boolean commandMode) {
        double winnings = betAmount * prizeMultipliers[prizeSlot];
        economy.depositPlayer(player, winnings);
        recordDropResult(betAmount, winnings);
        refreshPrizes();
        if (commandMode) {
            return winnings;
        }
        return winnings;
    }

    private void sendPrizeMessage(int prizeSlot, double winnings) {
        double multiplier = prizeMultipliers[prizeSlot];
        double profit = winnings - lastBetAmount;
        boolean guiOpen = player.getOpenInventory().getTopInventory().getHolder() == this;

        if (multiplier >= 1.0) {
            if (multiplier >= 25.0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " hit a " + String.format("%.0fx", multiplier)
                        + " Plinko prize for " + economy.format(Math.max(profit, 0)) + " profit!");
                if (guiOpen) {
                    player.closeInventory();
                }
                player.sendTitle(ChatColor.GOLD + "PLINKO JACKPOT!",
                        ChatColor.GREEN + "+" + economy.format(Math.max(profit, 0)) + " at " + String.format("%.0fx", multiplier),
                        10, 80, 20);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.15f);
            }
            player.sendMessage(ChatColor.GREEN + "[PLINKO] You won " + economy.format(Math.max(profit, 0))
                    + " at " + String.format("%.1fx", multiplier) + ".");
            if (guiOpen) {
                updateStatusDisplay(ChatColor.GREEN + "" + ChatColor.BOLD + String.format("%.1fx WIN!", multiplier),
                        ChatColor.GOLD + "+" + economy.format(Math.max(profit, 0)));
            }
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.85f);
            player.sendMessage(ChatColor.RED + "[PLINKO] You lost " + economy.format(Math.abs(profit))
                    + " at " + String.format("%.1fx", multiplier) + ".");
            if (guiOpen) {
                updateStatusDisplay(ChatColor.RED + "" + ChatColor.BOLD + String.format("%.1fx LOSS", multiplier),
                        ChatColor.GRAY + economy.format(Math.abs(profit)));
            }
        }
        updateDropIndicators();
    }

    private void updateDropIndicators() {
        // Only render drop indicators on active columns (3-5) and show locked placeholders elsewhere.
        for (int column = 0; column < prizeMultipliers.length; column++) {
            boolean droppable = column >= 2 && column <= 6;
            if (droppable) {
                gui.setItem(column, createDropItem(column, column == lastDropColumn, true));
            } else {
                // Default locked placeholder; bet controls will override select locked columns below
                gui.setItem(column, createGuiItem(Material.GRAY_STAINED_GLASS_PANE,
                        ChatColor.DARK_GRAY + "Locked",
                        ChatColor.GRAY + "Drop available in columns 3-7"));
            }
        }
        // Re-apply bet controls on the top row where locked tiles are
        initializeTopRowControls();
    }

    private ItemStack createDropItem(int column, boolean highlighted, boolean droppable) {
        if (!droppable) {
            return createGuiItem(Material.GRAY_STAINED_GLASS_PANE,
                    ChatColor.DARK_GRAY + "Locked",
                    ChatColor.GRAY + "Drop available in columns 3-7");
        }

        Material material = highlighted ? Material.GLOWSTONE : Material.YELLOW_STAINED_GLASS_PANE;
        String name = (highlighted ? ChatColor.AQUA + "" + ChatColor.BOLD : ChatColor.GREEN.toString()) + "Column " + (column + 1);
        if (highlighted && ballDropping) {
            name = ChatColor.GOLD + "Ball Dropping...";
        }
        return createGuiItem(material, name,
                ChatColor.GRAY + "Click to drop from here",
                highlighted ? ChatColor.YELLOW + "Last used column" : ChatColor.DARK_GRAY + "Ready");
    }

    private void updateStatusDisplay(String title, String... lore) {
        gui.setItem(24, createGuiItem(Material.PAPER, title, lore));
    }

    private void refreshBetDisplay() {
        gui.setItem(22, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet",
                ChatColor.WHITE + economy.format(betAmount),
                ChatColor.GRAY + "Adjust using the controls"));
    }

    private void updateRiskButton() {
        ChatColor color = RISK_LABEL_COLORS[riskLevel.ordinal()];
        int slot = 9 + 8;
        gui.setItem(slot, createGuiItem(Material.COMPASS,
                color + "Risk Mode: " + riskLevel.name(),
                ChatColor.GRAY + "Click to cycle risk profile",
                ChatColor.DARK_GRAY + "Alters payouts and bias"));
    }

    private void updateStatsPanel() {
        double profit = totalReturned - totalWagered;
        ChatColor profitColor = profit >= 0 ? ChatColor.GREEN : ChatColor.RED;
        int slot = 9; // row 1 column 1
        gui.setItem(slot, createGuiItem(Material.WRITABLE_BOOK,
                ChatColor.AQUA + "Session Stats",
                ChatColor.GRAY + "Drops: " + dropsPlayed,
                ChatColor.GRAY + "Wagered: " + economy.format(totalWagered),
                ChatColor.GRAY + "Won: " + economy.format(totalReturned),
                profitColor + "Profit: " + economy.format(profit)));
    }

    private void applyRiskProfile() {
        int idx = riskLevel.ordinal();
        prizeMultipliers = Arrays.copyOf(RISK_MULTIPLIERS[idx], RISK_MULTIPLIERS[idx].length);
        prizeColors = Arrays.copyOf(RISK_COLORS[idx], RISK_COLORS[idx].length);
        centerBias = RISK_CENTER_BIAS[idx];
    }

    public void cycleRiskProfile() {
        if (ballDropping) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] Wait for the current ball to finish before changing risk.");
            return;
        }
        switch (riskLevel) {
            case SAFE:
                riskLevel = RiskLevel.STANDARD;
                break;
            case STANDARD:
                riskLevel = RiskLevel.HIGH;
                break;
            case HIGH:
                riskLevel = RiskLevel.SAFE;
                break;
        }
        applyRiskProfile();
        refreshPrizes();
        updateRiskButton();
        updateStatusDisplay(RISK_LABEL_COLORS[riskLevel.ordinal()] + riskLevel.name() + " risk selected",
                ChatColor.GRAY + "Payouts recalibrated");
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.1f);
    }

    public void dropAgain() {
        if (ballDropping) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] Wait for the current ball to finish.");
            return;
        }
        if (lastDropColumn < 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[PLINKO] Drop a ball first before reusing the column.");
            return;
        }
        setBetAmount(lastBetAmount);
        dropBall(lastDropColumn);
    }

    private void recordDropResult(double wager, double winnings) {
        lastBetAmount = wager;
        dropsPlayed++;
        totalWagered += wager;
        totalReturned += winnings;
        double profit = winnings - wager;
        if (profit > 0) {
            Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);
        } else if (profit < 0) {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), Math.abs(profit));
        }
        updateStatsPanel();
    }

    private Material getBackgroundMaterial(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return ((row + col) % 2 == 0) ? Material.BLACK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
    }

    public void adjustBet(double amount) {
        betAmount = Math.max(10.0, Math.min(10000.0, betAmount + amount));
        refreshBetDisplay();
        refreshPrizes();
    }

    public void setBetAmount(double amount) {
        betAmount = Math.max(10.0, Math.min(10000.0, amount));
        refreshBetDisplay();
        refreshPrizes();
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
