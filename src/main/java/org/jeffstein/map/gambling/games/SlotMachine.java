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

    // Symmetric top-row bet controls (avoid reel area): 0/1 for -, 6/7 for +
    private static final int BET_DOWN_LARGE_SLOT = 0;
    private static final int BET_DOWN_SMALL_SLOT = 1;
    private static final int BET_UP_LARGE_SLOT = 6;
    private static final int BET_UP_SMALL_SLOT = 7;
    private static final int RISK_INFO_SLOT = 17; // reused for payouts info

    private static final int[][] REEL_POSITIONS = {
            {10, 19, 28}, // Reel 1
            {11, 20, 29}, // Reel 2
            {12, 21, 30}, // Reel 3
            {13, 22, 31}, // Reel 4
            {14, 23, 32}, // Reel 5
            {15, 24, 33}, // Reel 6
            {16, 25, 34}  // Reel 7
    };

    private static final int[][] PAYLINE_ROWS = {
            {1, 1, 1, 1, 1, 1, 1}, // middle
            {0, 0, 0, 0, 0, 0, 0}, // top
            {2, 2, 2, 2, 2, 2, 2}, // bottom
            {0, 1, 2, 1, 0, 1, 2}, // V zig
            {2, 1, 0, 1, 2, 1, 0}, // inverted V zig
            {0, 0, 1, 1, 2, 2, 2}, // gentle down
            {2, 2, 1, 1, 0, 0, 0}, // gentle up
            {1, 0, 1, 2, 1, 0, 1}, // wave A
            {1, 2, 1, 0, 1, 2, 1}  // wave B
    };

    private static final Symbol[] SYMBOLS = {
            // Premiums to lows (line pays are per-line bet):
            new Symbol("Diamond", Material.DIAMOND, ChatColor.AQUA, new double[]{50, 150, 400, 1000, 2500}, 1.0, false, false),
            new Symbol("Crown", Material.GOLD_BLOCK, ChatColor.GOLD, new double[]{20, 75, 220, 500, 1200}, 1.8, false, false),
            new Symbol("Seven", Material.REDSTONE, ChatColor.RED, new double[]{12, 45, 140, 300, 800}, 2.4, false, false),
            new Symbol("Bar", Material.IRON_BLOCK, ChatColor.GRAY, new double[]{6, 18, 60, 120, 300}, 3.2, false, false),
            new Symbol("Cherry", Material.APPLE, ChatColor.DARK_RED, new double[]{4, 12, 40, 90, 200}, 4.0, false, false),
            new Symbol("Lemon", Material.GLOW_BERRIES, ChatColor.YELLOW, new double[]{2, 6, 24, 50, 120}, 5.5, false, false),
            // Specials:
            new Symbol("Wild", Material.NETHER_STAR, ChatColor.LIGHT_PURPLE, new double[]{10, 50, 200, 600, 1500}, 0.5, true, false),
            new Symbol("Scatter", Material.ENCHANTED_GOLDEN_APPLE, ChatColor.GOLD, new double[]{2, 10, 50, 100, 250}, 0.8, false, true)
    };

    private static final List<Symbol> WEIGHTED_STRIP = buildWeightedStrip();

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();

    // Auto-spin state for rebet x10
    private int autoSpinsRemaining = 0;

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
        this.gui = Bukkit.createInventory(this, INVENTORY_SIZE, ChatColor.GOLD + "✨ LUCKY SLOTS ✨");
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
                ChatColor.LIGHT_PURPLE + "Rebet x10 (Auto-Spin)",
                ChatColor.GRAY + "Spin 10 times at current bet"));

        gui.setItem(BET_DOWN_LARGE_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-1000",
                ChatColor.GRAY + "Decrease bet by 1000"));
        gui.setItem(BET_DOWN_SMALL_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet by 100"));
        gui.setItem(BET_UP_LARGE_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet by 100"));
        gui.setItem(BET_UP_SMALL_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "+1000",
                ChatColor.GRAY + "Increase bet by 1000"));

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

        updateStatus(ChatColor.GOLD + "Welcome to Lucky Slots", ChatColor.GRAY + "7-Reel machine • Adjust your bet and spin");
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
            if (autoSpinsRemaining > 0) {
                player.sendActionBar(ChatColor.RED + "Auto-Spin stopped (insufficient funds)." );
                autoSpinsRemaining = 0;
            }
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

        int[] stopTicks = {18, 23, 28, 33, 38, 43, 48};
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
                    // Queue next auto-spin if requested and GUI still open
                    if (autoSpinsRemaining > 0) {
                        new BukkitRunnable() {
                            @Override public void run() {
                                InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
                                if (holder instanceof SlotMachine) {
                                    startNextAutoSpin();
                                } else {
                                    autoSpinsRemaining = 0;
                                }
                            }
                        }.runTaskLater(plugin, 10L);
                    }
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
        if (spinning) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[SLOTS] Wait for the current spin to finish.");
            return;
        }
        if (currentBet <= 0) {
            setBetAmount(100.0);
        }
        autoSpinsRemaining = 10;
        player.sendActionBar(ChatColor.LIGHT_PURPLE + "Auto-Spin x" + autoSpinsRemaining + " started");
        startNextAutoSpin();
    }
    private void startNextAutoSpin() {
        if (autoSpinsRemaining <= 0) return;
        // Ensure GUI still open and holder matches this machine
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof SlotMachine)) { autoSpinsRemaining = 0; return; }
        autoSpinsRemaining--;
        spin();
    }


    public void showPaytable() {
        player.sendMessage(ChatColor.AQUA + "[SLOTS] Paytable (line pays are per-line bet; scatters pay x total bet)");
        for (Symbol symbol : SYMBOLS) {
            player.sendMessage(symbol.color + "  " + symbol.name + ChatColor.GRAY + ": " +
                    ChatColor.YELLOW + "3x=" + symbol.multipliers[0] + "  " +
                    ChatColor.GOLD + "4x=" + symbol.multipliers[1] + "  " +
                    ChatColor.LIGHT_PURPLE + "5x=" + symbol.multipliers[2] + "  " +
                    ChatColor.AQUA + "6x=" + symbol.multipliers[3] + "  " +
                    ChatColor.BLUE + "7x=" + symbol.multipliers[4]);
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
                    if (symbol.isScatter) {
                        meta.setLore(Arrays.asList(
                            ChatColor.GOLD + "Pays anywhere (x total bet)",
                            ChatColor.GRAY + "3x=" + ChatColor.YELLOW + symbol.multipliers[0] + "x  " +
                            ChatColor.GRAY + "4x=" + ChatColor.GOLD + symbol.multipliers[1] + "x",
                            ChatColor.GRAY + "5x=" + ChatColor.LIGHT_PURPLE + symbol.multipliers[2] + "x  " +
                            ChatColor.GRAY + "6x=" + ChatColor.AQUA + symbol.multipliers[3] + "x  " +
                            ChatColor.GRAY + "7x=" + ChatColor.BLUE + symbol.multipliers[4] + "x"));
                    } else if (symbol.isWild) {
                        meta.setLore(Arrays.asList(
                            ChatColor.LIGHT_PURPLE + "Substitutes any symbol",
                            ChatColor.DARK_GRAY + "(except Scatter)",
                            ChatColor.GRAY + "3x=" + ChatColor.YELLOW + symbol.multipliers[0] + "x  " +
                            ChatColor.GRAY + "4x=" + ChatColor.GOLD + symbol.multipliers[1] + "x  " +
                            ChatColor.GRAY + "5x=" + ChatColor.LIGHT_PURPLE + symbol.multipliers[2] + "x",
                            ChatColor.GRAY + "6x=" + ChatColor.AQUA + symbol.multipliers[3] + "x  " +
                            ChatColor.GRAY + "7x=" + ChatColor.BLUE + symbol.multipliers[4] + "x"));
                    } else {
                        meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "3x " + ChatColor.YELLOW + symbol.multipliers[0] + "x",
                            ChatColor.GRAY + "4x " + ChatColor.GOLD + symbol.multipliers[1] + "x",
                            ChatColor.GRAY + "5x " + ChatColor.LIGHT_PURPLE + symbol.multipliers[2] + "x",
                            ChatColor.GRAY + "6x " + ChatColor.AQUA + symbol.multipliers[3] + "x",
                            ChatColor.GRAY + "7x " + ChatColor.BLUE + symbol.multipliers[4] + "x"));
                    }
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
        Symbol[][] grid = new Symbol[3][7];
        for (int reel = 0; reel < 7; reel++) {
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

        double perLineBet = currentBet / PAYLINE_ROWS.length;

        // Payline wins with Wild substitutions (Scatter does not participate on lines)
        for (int[] payline : PAYLINE_ROWS) {
            Symbol target = null; // first non-wild, non-scatter symbol from the left
            int matches = 0;
            List<Integer> matchedCols = new ArrayList<>();

            for (int col = 0; col < REEL_POSITIONS.length; col++) {
                Symbol sym = grid[payline[col]][col];
                if (sym.isScatter) {
                    // Scatters don't extend payline combos
                    break;
                }
                if (sym.isWild) {
                    // Wild always helps extend the run
                    matches++;
                    matchedCols.add(col);
                    continue;
                }
                if (target == null) {
                    target = sym;
                    matches++;
                    matchedCols.add(col);
                } else if (sym == target) {
                    matches++;
                    matchedCols.add(col);
                } else {
                    break;
                }
            }

            if (matches >= 3) {
                // If all leading symbols were wild and no target found, pay wild's table
                Symbol payingSymbol = (target == null) ? getWildSymbol() : target;
                double multiplier = payingSymbol.getMultiplier(matches);
                double lineWin = perLineBet * multiplier;
                if (lineWin > 0) {
                    total += lineWin;
                    lines++;
                    if (primary.isEmpty()) {
                        primary = payingSymbol.color + payingSymbol.name + ChatColor.GRAY + " x" + matches;
                    }
                    for (int c : matchedCols) {
                        highlights.add(REEL_POSITIONS[c][payline[c]]);
                    }
                    // Jackpot on Diamonds (index 0) for 5+ in a row
                    if (payingSymbol == SYMBOLS[0] && matches >= 5) {
                        total += Gambling.getJackpot().getJackpot();
                        Gambling.getJackpot().resetJackpot();
                        jackpot = true;
                    }
                }
            }
        }

        // Scatter pays anywhere based on total bet
        int scatterCount = countScatters(grid);
        double scatterMult = getScatterMultiplier(scatterCount);
        if (scatterMult > 0) {
            total += currentBet * scatterMult;
            if (primary.isEmpty()) {
                primary = ChatColor.GOLD + "Scatter" + ChatColor.GRAY + " x" + scatterCount;
            }
        }

        return new SpinOutcome(total, lines, highlights, primary, jackpot);
    }
    private Symbol getWildSymbol() {
        for (Symbol s : SYMBOLS) if (s.isWild) return s;
        return SYMBOLS[0]; // fallback
    }

    private int countScatters(Symbol[][] grid) {
        int count = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c].isScatter) count++;
            }
        }
        return count;
    }

    private double getScatterMultiplier(int count) {
        // Using Scatter symbol's multipliers (3..7 anywhere) from SYMBOLS[]
        Symbol scatter = null;
        for (Symbol s : SYMBOLS) if (s.isScatter) { scatter = s; break; }
        if (scatter == null) return 0.0;
        return scatter.getMultiplier(count);
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
        // For non-scatter: multipliers are 3..7 of a kind (per-line bet). For scatter: 3..7 anywhere (x total bet)
        private final double[] multipliers;
        private final double weight;
        private final boolean isWild;
        private final boolean isScatter;

        private Symbol(String name, Material material, ChatColor color, double[] multipliers, double weight, boolean isWild, boolean isScatter) {
            this.name = name;
            this.material = material;
            this.color = color;
            this.multipliers = multipliers;
            this.weight = weight;
            this.isWild = isWild;
            this.isScatter = isScatter;
        }

        private double getMultiplier(int matches) {
            // Returns 0 if <3, otherwise multipliers indexed at (matches-3)
            if (matches < 3) return 0;
            int idx = Math.min(matches, 7) - 3;
            if (idx < 0 || idx >= multipliers.length) return 0;
            return multipliers[idx];
        }
    }

    private record SpinOutcome(double totalWin, int winningLines, Set<Integer> highlightSlots,
                               String primarySymbol, boolean jackpot) { }
}