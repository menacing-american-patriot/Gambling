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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MinesGame implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();
    
    private double betAmount = 100.0;
    private int mineCount = 3;
    private boolean gameStarted = false;
    private boolean gameEnded = false;
    
    private final Set<Integer> minePositions = new HashSet<>();
    private final Set<Integer> revealedSafes = new HashSet<>();
    private final int gridSize = 25; // 5x5 grid (slots 0-24)
    
    // Multiplier table based on mines and revealed safes
    private final double[][] multiplierTable = {
        // 1 mine: [0.96, 1.04, 1.12, 1.21, 1.32, 1.44, 1.58, 1.74, 1.92, 2.14, 2.40, 2.71, 3.09, 3.56, 4.16, 4.95, 6.00, 7.44, 9.54, 12.86, 18.49, 29.59, 59.17, 236.68]
        {0.96, 1.04, 1.12, 1.21, 1.32, 1.44, 1.58, 1.74, 1.92, 2.14, 2.40, 2.71, 3.09, 3.56, 4.16, 4.95, 6.00, 7.44, 9.54, 12.86, 18.49, 29.59, 59.17, 236.68},
        // 2 mines
        {0.93, 1.08, 1.25, 1.45, 1.71, 2.04, 2.47, 3.05, 3.81, 4.86, 6.35, 8.5, 11.73, 16.86, 25.3, 39.75, 66.25, 119.25, 238.5, 556.17, 1668.5, 8342.5},
        // 3 mines
        {0.89, 1.13, 1.43, 1.84, 2.4, 3.18, 4.31, 6.0, 8.6, 12.86, 19.8, 31.68, 53.95, 98.08, 190.58, 396.83, 912.58, 2337.5, 6912.5, 25387.5, 127937.5},
        // 4 mines
        {0.86, 1.18, 1.64, 2.25, 3.18, 4.59, 6.88, 10.75, 17.2, 28.67, 50.17, 93.65, 186.3, 396.83, 912.58, 2337.5, 6912.5, 25387.5, 127937.5},
        // 5 mines
        {0.82, 1.24, 1.88, 2.85, 4.44, 7.13, 11.85, 20.57, 37.03, 70.31, 140.63, 296.32, 675.73, 1689.32, 4723.29, 15244.14, 60976.56}
    };

    public MinesGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.RED + "💣 MINES 💣");
        initializeGame();
    }

    private void initializeGame() {
        // Fill background
        ItemStack background = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Create the 5x5 grid (slots 0-24)
        setupGrid();
        
        // Setup controls
        setupControls();
    }

    private void setupGrid() {
        for (int i = 0; i < gridSize; i++) {
            if (gameStarted && revealedSafes.contains(i)) {
                // Revealed safe square
                gui.setItem(i, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "💎 SAFE",
                        ChatColor.GRAY + "You found a gem!"));
            } else if (gameEnded && minePositions.contains(i)) {
                // Show mine after game ends
                gui.setItem(i, createGuiItem(Material.TNT, ChatColor.RED + "💣 MINE",
                        ChatColor.GRAY + "Boom!"));
            } else {
                // Hidden square
                gui.setItem(i, createGuiItem(Material.GRAY_CONCRETE, ChatColor.WHITE + "❓",
                        ChatColor.GRAY + "Click to reveal",
                        gameStarted ? ChatColor.YELLOW + "Risk: " + getCurrentMultiplier() + "x" : 
                                     ChatColor.RED + "Start game first!"));
            }
        }
    }

    private void setupControls() {
        // Bet controls
        gui.setItem(27, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100",
                ChatColor.GRAY + "Decrease bet"));
        gui.setItem(28, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-50",
                ChatColor.GRAY + "Decrease bet"));
        gui.setItem(29, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Bet Amount",
                ChatColor.WHITE + economy.format(betAmount)));
        gui.setItem(30, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+50",
                ChatColor.GRAY + "Increase bet"));
        gui.setItem(31, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100",
                ChatColor.GRAY + "Increase bet"));

        // Mine count controls
        gui.setItem(33, createGuiItem(Material.REDSTONE, ChatColor.RED + "Mines: -1",
                ChatColor.GRAY + "Decrease mine count"));
        gui.setItem(34, createGuiItem(Material.TNT, ChatColor.YELLOW + "Mine Count",
                ChatColor.WHITE + "" + mineCount + " mines",
                ChatColor.GRAY + "More mines = higher multipliers"));
        gui.setItem(35, createGuiItem(Material.REDSTONE_TORCH, ChatColor.GREEN + "Mines: +1",
                ChatColor.GRAY + "Increase mine count"));

        // Game controls
        if (!gameStarted) {
            gui.setItem(40, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "START GAME",
                    ChatColor.GRAY + "Begin mining!",
                    ChatColor.YELLOW + "Bet: " + economy.format(betAmount),
                    ChatColor.YELLOW + "Mines: " + mineCount));
        } else if (!gameEnded) {
            double currentWinnings = betAmount * getCurrentMultiplier();
            gui.setItem(40, createGuiItem(Material.DIAMOND_BLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "CASH OUT",
                    ChatColor.GRAY + "Take your winnings!",
                    ChatColor.GREEN + "Current: " + economy.format(currentWinnings),
                    ChatColor.YELLOW + "Multiplier: " + String.format("%.2fx", getCurrentMultiplier())));
        } else {
            gui.setItem(40, createGuiItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "GAME OVER",
                    ChatColor.GRAY + "Start a new game"));
        }

        // Info display
        gui.setItem(49, createGuiItem(Material.BOOK, ChatColor.YELLOW + "Game Info",
                ChatColor.GRAY + "Revealed: " + revealedSafes.size() + "/25",
                ChatColor.GRAY + "Mines: " + mineCount,
                gameStarted ? ChatColor.GREEN + "Multiplier: " + String.format("%.2fx", getCurrentMultiplier()) :
                             ChatColor.GRAY + "Game not started"));

        // Back button
        gui.setItem(45, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back",
                ChatColor.GRAY + "Close Mines game"));
    }

    public void startGame() {
        if (gameStarted) return;
        
        if (economy.getBalance(player) < betAmount) {
            player.sendActionBar(ChatColor.RED + "Insufficient funds! Need " + economy.format(betAmount));
            return;
        }

        economy.withdrawPlayer(player, betAmount);
        gameStarted = true;
        gameEnded = false;

        // Place mines randomly
        placeMines();

        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GAME STARTED!",
                        ChatColor.YELLOW + "Find gems, avoid mines!", 5, 30, 10);
        player.sendActionBar(ChatColor.GREEN + "Game started! Bet: " + economy.format(betAmount) + " | Mines: " + mineCount);
        
        setupGrid();
        setupControls();
    }

    private void placeMines() {
        minePositions.clear();
        while (minePositions.size() < mineCount) {
            int position = ThreadLocalRandom.current().nextInt(gridSize);
            minePositions.add(position);
        }
    }

    public void revealSquare(int position) {
        if (!gameStarted || gameEnded || revealedSafes.contains(position)) {
            return;
        }

        if (minePositions.contains(position)) {
            // Hit a mine - game over
            gameEnded = true;
            player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "💥 BOOM!",
                           ChatColor.GRAY + "You hit a mine! -" + economy.format(betAmount), 10, 60, 20);
            player.sendActionBar(ChatColor.RED + "Game over! You lost " + economy.format(betAmount));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

            Gambling.getLeaderboard().addLoss(player.getUniqueId(), betAmount);

            // Reveal all mines
            setupGrid();
            setupControls();
        } else {
            // Safe square
            revealedSafes.add(position);
            double multiplier = getCurrentMultiplier();
            double currentWinnings = betAmount * multiplier;

            player.sendActionBar(ChatColor.GREEN + "💎 Safe! Winnings: " + economy.format(currentWinnings) +
                               " (" + String.format("%.2fx", multiplier) + ")");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            
            setupGrid();
            setupControls();
            
            // Check if all safe squares revealed
            if (revealedSafes.size() >= (gridSize - mineCount)) {
                // Auto cash out - found all safe squares
                cashOut();
            }
        }
    }

    public void cashOut() {
        if (!gameStarted || gameEnded || revealedSafes.isEmpty()) {
            return;
        }

        double multiplier = getCurrentMultiplier();
        double winnings = betAmount * multiplier;
        
        economy.depositPlayer(player, winnings);
        gameEnded = true;

        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "💰 CASHED OUT!",
                        ChatColor.GOLD + "+" + economy.format(winnings) + " at " + String.format("%.2fx", multiplier), 10, 60, 20);
        player.sendActionBar(ChatColor.GREEN + "Congratulations! You won " + economy.format(winnings) +
                           " with " + String.format("%.2fx", multiplier) + " multiplier!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        Gambling.getLeaderboard().addWin(player.getUniqueId(), winnings - betAmount);
        
        setupControls();
    }

    private double getCurrentMultiplier() {
        if (revealedSafes.isEmpty()) return 1.0;
        
        int mineIndex = Math.max(0, Math.min(mineCount - 1, 4)); // Cap at 5 mines
        int revealedIndex = Math.max(0, Math.min(revealedSafes.size() - 1, multiplierTable[mineIndex].length - 1));
        
        return multiplierTable[mineIndex][revealedIndex];
    }

    public void adjustBet(double amount) {
        if (gameStarted) return;
        betAmount = Math.max(10.0, Math.min(10000.0, betAmount + amount));
        setupControls();
    }

    public void adjustMines(int amount) {
        if (gameStarted) return;
        mineCount = Math.max(1, Math.min(5, mineCount + amount));
        setupControls();
    }

    public void resetGame() {
        gameStarted = false;
        gameEnded = false;
        minePositions.clear();
        revealedSafes.clear();
        setupGrid();
        setupControls();
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

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }
}
