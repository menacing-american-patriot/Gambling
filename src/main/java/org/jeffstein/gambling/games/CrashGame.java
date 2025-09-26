package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CrashGame {
    
    private final Gambling plugin;
    private final Map<UUID, Double> playerBets = new HashMap<>();
    private final Map<UUID, Double> playerAutoCashout = new HashMap<>();
    private final Set<UUID> cashedOutPlayers = new HashSet<>();
    
    private double currentMultiplier = 1.0;
    private boolean gameRunning = false;
    private boolean bettingPhase = true;
    private double crashPoint;
    private BukkitTask gameTask;
    
    private static final double MIN_BET = 10.0;
    private static final double MAX_BET = 10000.0;
    private static final int BETTING_TIME = 10; // seconds
    private static final double MULTIPLIER_INCREMENT = 0.01;
    
    public CrashGame(Gambling plugin) {
        this.plugin = plugin;
        generateCrashPoint();
    }
    
    private void generateCrashPoint() {
        // Generate crash point using house edge algorithm
        // Higher chance of lower multipliers, rare chance of very high multipliers
        double random = ThreadLocalRandom.current().nextDouble();
        
        if (random < 0.5) {
            // 50% chance: 1.0x - 2.0x
            crashPoint = 1.0 + ThreadLocalRandom.current().nextDouble() * 1.0;
        } else if (random < 0.8) {
            // 30% chance: 2.0x - 5.0x
            crashPoint = 2.0 + ThreadLocalRandom.current().nextDouble() * 3.0;
        } else if (random < 0.95) {
            // 15% chance: 5.0x - 10.0x
            crashPoint = 5.0 + ThreadLocalRandom.current().nextDouble() * 5.0;
        } else {
            // 5% chance: 10.0x - 100.0x (rare big wins)
            crashPoint = 10.0 + ThreadLocalRandom.current().nextDouble() * 90.0;
        }
    }
    
    public boolean placeBet(Player player, double amount) {
        if (!bettingPhase) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRASH] Betting is closed! Wait for next round.");
            return false;
        }

        if (amount < MIN_BET || amount > MAX_BET) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRASH] Bet must be between " +
                Gambling.getEconomy().format(MIN_BET) + " and " + Gambling.getEconomy().format(MAX_BET));
            return false;
        }

        Economy economy = Gambling.getEconomy();
        if (economy.getBalance(player) < amount) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRASH] Insufficient funds! Need " + economy.format(amount));
            return false;
        }

        // Remove previous bet if exists
        if (playerBets.containsKey(player.getUniqueId())) {
            double previousBet = playerBets.get(player.getUniqueId());
            economy.depositPlayer(player, previousBet);
        }

        economy.withdrawPlayer(player, amount);
        playerBets.put(player.getUniqueId(), amount);
        // Sound + chat feedback for successful bet
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "[CRASH] Bet placed: " + economy.format(amount));

        return true;
    }
    
    public boolean setAutoCashout(Player player, double multiplier) {
        if (!playerBets.containsKey(player.getUniqueId())) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRASH] Place a bet first!");
            return false;
        }

        if (multiplier < 1.01 || multiplier > 1000.0) {
            // Sound + chat feedback
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "[CRASH] Auto-cashout must be between 1.01x and 1000x");
            return false;
        }

        playerAutoCashout.put(player.getUniqueId(), multiplier);
        // Sound + chat feedback for successful auto-cashout setting
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        player.sendMessage(ChatColor.YELLOW + "[CRASH] Auto-cashout set at " + String.format("%.2fx", multiplier));
        return true;
    }
    
    public boolean cashOut(Player player) {
        UUID playerId = player.getUniqueId();

        if (!gameRunning) {
            // Use sound feedback instead of hidden message
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (!playerBets.containsKey(playerId)) {
            // Use sound feedback instead of hidden message
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (cashedOutPlayers.contains(playerId)) {
            // Use sound feedback instead of hidden message
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        double betAmount = playerBets.get(playerId);
        double winnings = betAmount * currentMultiplier;
        double profit = winnings - betAmount;

        Economy economy = Gambling.getEconomy();
        economy.depositPlayer(player, winnings);
        cashedOutPlayers.add(playerId);

        // For cash outs, close GUI first so message is visible
        player.closeInventory();
        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "CASHED OUT!",
                        ChatColor.GOLD + "+" + economy.format(profit) + " at " + String.format("%.2fx", currentMultiplier),
                        10, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        // Also send to chat
        player.sendMessage(ChatColor.GREEN + "[CRASH] " + ChatColor.BOLD + "CASHED OUT! " +
                         ChatColor.GOLD + "+" + economy.format(profit) + " at " + String.format("%.2fx", currentMultiplier));

        Gambling.getLeaderboard().addWin(playerId, profit);
        return true;
    }
    
    public void startGame() {
        if (gameRunning) return;
        
        bettingPhase = false;
        gameRunning = true;
        currentMultiplier = 1.0;
        cashedOutPlayers.clear();
        
        // Notify all players
        broadcastToPlayers(ChatColor.YELLOW + "🚀 CRASH GAME STARTING! 🚀");
        
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentMultiplier >= crashPoint) {
                    // CRASH!
                    crash();
                    this.cancel();
                    return;
                }
                
                // Check for auto-cashouts
                checkAutoCashouts();
                
                // Increment multiplier
                currentMultiplier += MULTIPLIER_INCREMENT;
                
                // Update all players
                updateAllPlayers();
            }
        }.runTaskTimer(plugin, 0L, 1L); // Update every tick for smooth animation
    }
    
    private void checkAutoCashouts() {
        for (Map.Entry<UUID, Double> entry : playerAutoCashout.entrySet()) {
            UUID playerId = entry.getKey();
            double autoCashoutAt = entry.getValue();
            
            if (currentMultiplier >= autoCashoutAt && !cashedOutPlayers.contains(playerId)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && playerBets.containsKey(playerId)) {
                    cashOut(player);
                }
            }
        }
    }
    
    private void crash() {
        gameRunning = false;
        
        // Handle losses for players who didn't cash out
        for (Map.Entry<UUID, Double> entry : playerBets.entrySet()) {
            UUID playerId = entry.getKey();
            if (!cashedOutPlayers.contains(playerId)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    double lostAmount = entry.getValue();
                    // For crashes, close GUI first so message is visible
                    player.closeInventory();
                    player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "CRASHED!",
                                   ChatColor.GRAY + "-" + Gambling.getEconomy().format(lostAmount) +
                                   " at " + String.format("%.2fx", crashPoint), 10, 60, 20);
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    // Also send to chat
                    player.sendMessage(ChatColor.RED + "[CRASH] " + ChatColor.BOLD + "CRASHED! " +
                                     ChatColor.GRAY + "-" + Gambling.getEconomy().format(lostAmount) +
                                     " at " + String.format("%.2fx", crashPoint));
                    Gambling.getLeaderboard().addLoss(playerId, lostAmount);
                }
            }
        }
        
        broadcastToPlayers(ChatColor.RED + "💥 CRASHED at " + String.format("%.2fx", crashPoint) + "! 💥");
        
        // Start new round after delay
        new BukkitRunnable() {
            @Override
            public void run() {
                startNewRound();
            }
        }.runTaskLater(plugin, 100L); // 5 second delay
    }
    
    private void startNewRound() {
        playerBets.clear();
        playerAutoCashout.clear();
        cashedOutPlayers.clear();
        bettingPhase = true;
        generateCrashPoint();
        
        broadcastToPlayers(ChatColor.GREEN + "🎰 New round starting! Place your bets! 🎰");
        
        // Start betting countdown
        new BukkitRunnable() {
            int countdown = BETTING_TIME;
            
            @Override
            public void run() {
                if (countdown <= 0) {
                    startGame();
                    this.cancel();
                    return;
                }
                
                if (countdown <= 5) {
                    broadcastToPlayers(ChatColor.YELLOW + "Starting in " + countdown + "...");
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }
    
    private void updateAllPlayers() {
        // Remove broken action bar updates - GUI already shows multiplier in real-time
        // The CrashGUI.updateDisplay() method handles all visual updates
        // No need for hidden action bar messages that players can't see
    }
    
    private void broadcastToPlayers(String message) {
        for (UUID playerId : playerBets.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }
    
    // Getters
    public boolean isBettingPhase() { return bettingPhase; }
    public boolean isGameRunning() { return gameRunning; }
    public double getCurrentMultiplier() { return currentMultiplier; }
    public Map<UUID, Double> getPlayerBets() { return playerBets; }
    public Set<UUID> getCashedOutPlayers() { return cashedOutPlayers; }
    
    public void stopGame() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        gameRunning = false;
        bettingPhase = false;
    }

    // Public method to initialize the first round (called from command)
    public void initializeFirstRound() {
        startNewRound();
    }
}
