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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SlotMachine implements InventoryHolder {

    private static final int INVENTORY_SIZE = 54;
    private static final int STATUS_SLOT = 4;
    private static final int BET_DISPLAY_SLOT = 22;
    private static final int STATS_SLOT = 9;
    private static final int PAYTABLE_SLOT = 8;
    private static final int SPIN_BUTTON_SLOT = 40;
    private static final int BACK_BUTTON_SLOT = 44;
    private static final int REBET_BUTTON_SLOT = 41;

    private static final int BET_DOWN_LARGE_SLOT = 10;
    private static final int BET_DOWN_SMALL_SLOT = 11;
    private static final int BET_UP_SMALL_SLOT = 15;
    private static final int BET_UP_LARGE_SLOT = 16;
    private static final int RISK_INFO_SLOT = 17; // reused for payouts info

    private static final int[][] REEL_POSITIONS = {
            {10, 19, 28},
            {11, 20, 29},
            {12, 21, 30},
            {13, 22, 31},
            {14, 23, 32}
    };

    private static final int[][] PAYLINE_ROWS = {
            {0, 0, 0, 0, 0}, // top
            {1, 1, 1, 1, 1}, // middle
            {2, 2, 2, 2, 2}, // bottom
            {0, 1, 2, 1, 0}, // V
            {2, 1, 0, 1, 2}  // inverted V
    };

    private static final Symbol[] SYMBOLS = {
            new Symbol("Diamond", Material.DIAMOND, ChatColor.AQUA, new double[]{50, 150, 400}, 1.0),
            new Symbol("Crown", Material.GOLD_BLOCK, ChatColor.GOLD, new double[]{20, 75, 220}, 1.8),
            new Symbol("Seven", Material.REDSTONE, ChatColor.RED, new double[]{12, 45, 140}, 2.4),
            new Symbol("Bar", Material.IRON_BLOCK, ChatColor.GRAY, new double[]{6, 18, 60}, 3.2),
            new Symbol("Cherry", Material.APPLE, ChatColor.DARK_RED, new double[]{4, 12, 40}, 4.0),
            new Symbol("Lemon", Material.GLOW_BERRIES, ChatColor.YELLOW, new double[]{2, 6, 24}, 5.5)
    };

    private static final List<Symbol> WEIGHTED_STRIP = buildWeightedStrip();

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();

    private Symbol[][] displayGrid = new Symbol[3][5];
    private double currentBet = 100.0;
    private boolean spinning = false;

    private double totalBet = 0.0;
    private double totalWon = 0.0;
    private int spinsPlayed = 0;
    private double lastWin = 0.0;

    public SlotMachine(Gambling plugin, Player player) {
        this(plugin, player, 100.0);
    }

    public SlotMachine(Gambling plugin, Player player, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, INVENTORY_SIZE, ChatColor.GOLD + "✨ LUX SLOTS ✨");
        setBetAmount(bet);
        buildLayout();
        populateInitialGrid();
        refreshPanels();
    }

    private static List<Symbol> buildWeightedStrip() {
        List<Symbol> strip = new ArrayList<>();
        for (Symbol symbol : SYMBOLS) {
            int copies = Math.max(1, (int) Math.round(symbol.weight * 10));
            for (int i = 0; i < copies; i++) {
                strip.add(symbol);
            }
        }
        return strip;
    }

    private void buildLayout() {
        ItemStack darkGlass = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack lightGlass = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            boolean dark = ((slot % 9) + (slot / 9)) % 2 == 0;
            gui.setItem(slot, dark ? darkGlass.clone() : lightGlass.clone());
        }

        gui.setItem(PAYTABLE_SLOT, createGuiItem(Material.BOOK,
                ChatColor.AQUA + "Payout Table",
                ChatColor.GRAY + "View slot paytable"));
        gui.setItem(BACK_BUTTON_SLOT, createGuiItem(Material.OAK_DOOR,
                ChatColor.YELLOW + "Exit",
                ChatColor.GRAY + "Close the machine"));
        gui.setItem(REBET_BUTTON_SLOT, createGuiItem(Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "Rebet & Spin",
                ChatColor.GRAY + "Repeat last wager"));

        gui.setItem(BET_DOWN_LARGE_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet by 100"));
        gui.setItem(BET_DOWN_SMALL_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-10",
                ChatColor.GRAY + "Decrease bet by 10"));
        gui.setItem(BET_UP_SMALL_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+10",
                ChatColor.GRAY + "Increase bet by 10"));
        gui.setItem(BET_UP_LARGE_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet by 100"));

        gui.setItem(SPIN_BUTTON_SLOT, createGuiItem(Material.LIME_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "SPIN",
                ChatColor.GRAY + "Good luck!"));
        gui.setItem(46, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(47, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(48, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(49, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(50, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(51, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));
        gui.setItem(52, createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "));

        updateStatus(ChatColor.GOLD + "Welcome to Lux Slots", ChatColor.GRAY + "Adjust your bet and spin");
    }

    private void populateInitialGrid() {
        Symbol[][] grid = generateSpinGrid();
        setDisplayGrid(grid);
        updateReels(grid, null);
    }

    private void refreshPanels() {
        refreshBetDisplay();
        updateStatsPanel();
        updateSpinButton();
        updatePaytableDisplay();
    }

    private void updatePaytableDisplay() {
        gui.setItem(RISK_INFO_SLOT, createGuiItem(Material.GOLDEN_CARROT,
                ChatColor.YELLOW + "Jackpot",
                ChatColor.GRAY + "Current pot: " + economy.format(Gambling.getJackpot().getJackpot()),
                ChatColor.DARK_GRAY + "Pays on five diamonds"));
    }

    public void sendSessionSummary(Player player) {
        player.sendMessage(ChatColor.AQUA + "[SLOTS] Session summary:");
        player.sendMessage(ChatColor.GRAY + " Spins: " + ChatColor.WHITE + spinsPlayed);
        player.sendMessage(ChatColor.GRAY + " Wagered: " + ChatColor.WHITE + economy.format(totalBet));
        player.sendMessage(ChatColor.GRAY + " Won: " + ChatColor.WHITE + economy.format(totalWon));
        double profit = totalWon - totalBet;
        ChatColor profitColor = profit >= 0 ? ChatColor.GREEN : ChatColor.RED;
        player.sendMessage(ChatColor.GRAY + " Profit: " + profitColor + economy.format(profit));
        player.sendMessage(ChatColor.GRAY + " Last win: " + ChatColor.WHITE + economy.format(lastWin));
    }

    public void openInventory() {
        player.openInventory(gui);
    }

    public boolean isSpinning() {
        return spinning;
    }

    public double getCurrentBet() {
        return currentBet;
    }

    public void adjustBet(double delta) {
        setBetAmount(currentBet + delta);
    }

    public void setBetAmount(double bet) {
        currentBet = Math.max(10.0, Math.min(1_000_000.0, bet));
        refreshBetDisplay();
        updateSpinButton();
    }

    public void spin() {
        if (spinning) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[SLOTS] The reels are already spinning!");
            return;
        }

        if (economy.getBalance(player) < currentBet) {
            updateStatus(ChatColor.RED + "Insufficient funds",
                    ChatColor.GRAY + "Need " + economy.format(currentBet),
                    ChatColor.YELLOW + "Balance: " + economy.format(economy.getBalance(player)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        economy.withdrawPlayer(player, currentBet);
        totalBet += currentBet;
        spinsPlayed++;
        lastWin = 0.0;

        Symbol[][] finalGrid = generateSpinGrid();
        SpinOutcome outcome = evaluateOutcome(finalGrid);

        spinning = true;
        updateSpinButton();
        updateStatus(ChatColor.YELLOW + "Spinning...",
                ChatColor.GRAY + "Stake: " + economy.format(currentBet),
                ChatColor.DARK_GRAY + "Reels slowing down");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.2f);

        int[] stopTicks = {18, 24, 30, 36, 42};
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= stopTicks[stopTicks.length - 1]) {
                    setDisplayGrid(finalGrid);
                    updateReels(finalGrid, outcome.highlightSlots);
                    finishSpin(outcome);
                    spinning = false;
                    updateSpinButton();
                    refreshBetDisplay();
                    updateStatsPanel();
                    cancel();
                    return;
                }

                for (int reel = 0; reel < REEL_POSITIONS.length; reel++) {
                    if (tick < stopTicks[reel]) {
                        Symbol[] tempColumn = randomColumn();
                        applyColumnSymbols(reel, tempColumn);
                    } else {
                        applyColumnSymbols(reel, extractColumn(finalGrid, reel));
                    }
                }

                float pitch = 1.0f + (tick * 0.015f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, pitch);
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void rebetAndSpin() {
        if (currentBet <= 0) {
            setBetAmount(100.0);
        }
        spin();
    }

    public void showPaytable() {
        player.sendMessage(ChatColor.AQUA + "[SLOTS] Paytable (multipliers are per 1.0 bet)");
        for (Symbol symbol : SYMBOLS) {
            player.sendMessage(symbol.color + "  " + symbol.name + ChatColor.GRAY + ": " +
                    ChatColor.YELLOW + "3x=" + symbol.multipliers[0] + "  " +
                    ChatColor.GOLD + "4x=" + symbol.multipliers[1] + "  " +
                    ChatColor.LIGHT_PURPLE + "5x=" + symbol.multipliers[2]);
        }
    }

    private void finishSpin(SpinOutcome outcome) {
        if (outcome.totalWin > 0) {
            economy.depositPlayer(player, outcome.totalWin);
            totalWon += outcome.totalWin;
            lastWin = outcome.totalWin;
            if (outcome.jackpot) {
                player.closeInventory();
                player.sendTitle(ChatColor.GOLD + "JACKPOT!",
                        ChatColor.YELLOW + "+" + economy.format(outcome.totalWin), 10, 60, 20);
                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " hit the slot jackpot for " +
                        economy.format(outcome.totalWin) + "!");
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                updateStatus(ChatColor.GREEN + "WIN: " + economy.format(outcome.totalWin),
                        ChatColor.WHITE + "Winning lines: " + outcome.winningLines,
                        ChatColor.GRAY + "Lucky symbol: " + outcome.primarySymbol);
            }
            Gambling.getLeaderboard().addWin(player.getUniqueId(), outcome.totalWin - currentBet);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            updateStatus(ChatColor.RED + "No win this time",
                    ChatColor.GRAY + "Try again!",
                    ChatColor.DARK_GRAY + "Better luck on the next spin");
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), currentBet);
        }

        Gambling.getJackpot().addToJackpot(currentBet * 0.1);
        updatePaytableDisplay();
    }

    private void setDisplayGrid(Symbol[][] grid) {
        displayGrid = new Symbol[grid.length][grid[0].length];
        for (int row = 0; row < grid.length; row++) {
            System.arraycopy(grid[row], 0, displayGrid[row], 0, grid[row].length);
        }
    }

    private void updateReels(Symbol[][] grid, Set<Integer> highlightSlots) {
        Set<Integer> highlights = highlightSlots == null ? new HashSet<>() : highlightSlots;
        for (int reel = 0; reel < REEL_POSITIONS.length; reel++) {
            for (int row = 0; row < grid.length; row++) {
                int slot = REEL_POSITIONS[reel][row];
                Symbol symbol = grid[row][reel];
                ItemStack item = new ItemStack(symbol.material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(symbol.color + symbol.name);
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "3x " + ChatColor.YELLOW + symbol.multipliers[0] + "x",
                            ChatColor.GRAY + "4x " + ChatColor.GOLD + symbol.multipliers[1] + "x",
                            ChatColor.GRAY + "5x " + ChatColor.LIGHT_PURPLE + symbol.multipliers[2] + "x"));
                    if (highlights.contains(slot)) {
                        meta.setEnchantmentGlintOverride(true);
                    }
                    item.setItemMeta(meta);
                }
                gui.setItem(slot, item);
            }
        }
    }

    private void refreshBetDisplay() {
        gui.setItem(BET_DISPLAY_SLOT, createGuiItem(Material.GOLD_INGOT,
                ChatColor.GOLD + "Current Bet",
                ChatColor.WHITE + economy.format(currentBet)));
    }

    private void updateStatsPanel() {
        gui.setItem(STATS_SLOT, createGuiItem(Material.WRITABLE_BOOK,
                ChatColor.AQUA + "Session",
                ChatColor.GRAY + "Spins: " + spinsPlayed,
                ChatColor.GRAY + "Wagered: " + economy.format(totalBet),
                ChatColor.GRAY + "Won: " + economy.format(totalWon),
                ChatColor.GRAY + "Last win: " + economy.format(lastWin)));
    }

    private void updateStatus(String title, String... lore) {
        gui.setItem(STATUS_SLOT, createGuiItem(Material.FIREWORK_ROCKET, title, lore));
    }

    private void updateSpinButton() {
        if (spinning) {
            gui.setItem(SPIN_BUTTON_SLOT, createGuiItem(Material.YELLOW_CONCRETE,
                    ChatColor.YELLOW + "Spinning..."));
        } else {
            boolean affordable = economy.getBalance(player) >= currentBet;
            Material mat = affordable ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            ChatColor color = affordable ? ChatColor.GREEN : ChatColor.RED;
            gui.setItem(SPIN_BUTTON_SLOT, createGuiItem(mat,
                    color + "" + ChatColor.BOLD + (affordable ? "SPIN" : "ADD FUNDS"),
                    ChatColor.GRAY + "Cost: " + economy.format(currentBet)));
        }
    }

    private Symbol[] randomColumn() {
        Symbol[] column = new Symbol[3];
        for (int row = 0; row < 3; row++) {
            column[row] = randomSymbol();
        }
        return column;
    }

    private void applyColumnSymbols(int reel, Symbol[] columnSymbols) {
        for (int row = 0; row < columnSymbols.length; row++) {
            Symbol symbol = columnSymbols[row];
            int slot = REEL_POSITIONS[reel][row];
            ItemStack item = new ItemStack(symbol.material);
            gui.setItem(slot, item);
        }
    }

    private Symbol[] extractColumn(Symbol[][] grid, int reel) {
        Symbol[] column = new Symbol[grid.length];
        for (int row = 0; row < grid.length; row++) {
            column[row] = grid[row][reel];
        }
        return column;
    }

    private Symbol randomSymbol() {
        return WEIGHTED_STRIP.get(ThreadLocalRandom.current().nextInt(WEIGHTED_STRIP.size()));
    }

    private Symbol[][] generateSpinGrid() {
        Symbol[][] grid = new Symbol[3][5];
        for (int reel = 0; reel < 5; reel++) {
            int start = ThreadLocalRandom.current().nextInt(WEIGHTED_STRIP.size());
            for (int row = 0; row < 3; row++) {
                grid[row][reel] = WEIGHTED_STRIP.get((start + row) % WEIGHTED_STRIP.size());
            }
        }
        return grid;
    }

    private SpinOutcome evaluateOutcome(Symbol[][] grid) {
        double total = 0.0;
        Set<Integer> highlights = new HashSet<>();
        int lines = 0;
        String primary = "";
        boolean jackpot = false;

        for (int[] payline : PAYLINE_ROWS) {
            Symbol first = grid[payline[0]][0];
            int matches = 1;
            for (int col = 1; col < 5; col++) {
                if (grid[payline[col]][col] == first) {
                    matches++;
                } else {
                    break;
                }
            }
            if (matches >= 3) {
                double multiplier = first.getMultiplier(matches);
                double lineWin = currentBet * multiplier;
                total += lineWin;
                lines++;
                if (primary.isEmpty()) {
                    primary = first.color + first.name + ChatColor.GRAY + " x" + matches;
                }
                for (int col = 0; col < matches; col++) {
                    highlights.add(REEL_POSITIONS[col][payline[col]]);
                }
                if (first == SYMBOLS[0] && matches == 5) {
                    total += Gambling.getJackpot().getJackpot();
                    Gambling.getJackpot().resetJackpot();
                    jackpot = true;
                }
            }
        }

        return new SpinOutcome(total, lines, highlights, primary, jackpot);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }

    private static final class Symbol {
        private final String name;
        private final Material material;
        private final ChatColor color;
        private final double[] multipliers; // for 3,4,5
        private final double weight;

        private Symbol(String name, Material material, ChatColor color, double[] multipliers, double weight) {
            this.name = name;
            this.material = material;
            this.color = color;
            this.multipliers = multipliers;
            this.weight = weight;
        }

        private double getMultiplier(int matches) {
            return switch (matches) {
                case 5 -> multipliers[2];
                case 4 -> multipliers[1];
                default -> multipliers[0];
            };
        }
    }

    private record SpinOutcome(double totalWin, int winningLines, Set<Integer> highlightSlots,
                               String primarySymbol, boolean jackpot) { }
}