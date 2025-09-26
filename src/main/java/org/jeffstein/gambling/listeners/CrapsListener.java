package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.CrapsGUI;
import org.jeffstein.gambling.games.CrapsGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrapsListener implements Listener {

    private final Gambling plugin;
    private final Map<UUID, CrapsGame> games = new HashMap<>();

    public CrapsListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CrapsGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            CrapsGUI crapsGUI = (CrapsGUI) holder;
            CrapsGame game = games.computeIfAbsent(player.getUniqueId(), k -> new CrapsGame(plugin, player));

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int slot = event.getSlot();

            // Handle bet adjustments
            if (slot == 37 && displayName.contains("-100")) {
                crapsGUI.adjustBet(-100);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else if (slot == 38 && displayName.contains("-50")) {
                crapsGUI.adjustBet(-50);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else if (slot == 42 && displayName.contains("+50")) {
                crapsGUI.adjustBet(50);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else if (slot == 43 && displayName.contains("+100")) {
                crapsGUI.adjustBet(100);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            }
            
            // Handle betting
            else if (slot == 19 && displayName.contains("PASS LINE")) {
                if (Gambling.getEconomy().getBalance(player) >= crapsGUI.getCurrentBet()) {
                    Gambling.getEconomy().withdrawPlayer(player, crapsGUI.getCurrentBet());
                    game.placeBet("Pass Line", crapsGUI.getCurrentBet());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    crapsGUI.update();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else if (slot == 20 && displayName.contains("DON'T PASS")) {
                if (Gambling.getEconomy().getBalance(player) >= crapsGUI.getCurrentBet()) {
                    Gambling.getEconomy().withdrawPlayer(player, crapsGUI.getCurrentBet());
                    game.placeBet("Don't Pass Line", crapsGUI.getCurrentBet());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    crapsGUI.update();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
            
            // Handle dice roll
            else if (slot == 49 && displayName.contains("ROLL DICE")) {
                if (game.getBets().isEmpty()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // Roll the dice
                int[] dice = game.rollDice();
                int total = dice[0] + dice[1];
                
                // Close GUI first so results are visible
                player.closeInventory();
                
                // Show dice roll result
                player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "DICE ROLLED!",
                               ChatColor.WHITE + "" + dice[0] + " + " + dice[1] + " = " + total, 10, 40, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                // Also send to chat
                player.sendMessage(ChatColor.YELLOW + "[CRAPS] Dice rolled: " + dice[0] + " + " + dice[1] + " = " + total);

                // Process the roll and determine winners/losers
                processRoll(player, game, total);
            }
            
            // Handle back button
            else if (slot == 45 && displayName.contains("Back")) {
                player.closeInventory();
                player.sendMessage(ChatColor.GRAY + "Thanks for playing Craps!");
            }
        }
    }
    
    private void processRoll(Player player, CrapsGame game, int total) {
        double totalPayout = 0;
        boolean won = false;
        
        if (game.getGameState() == CrapsGame.GameState.COME_OUT) {
            if (total == 7 || total == 11) {
                // Pass line wins, Don't Pass loses
                if (game.getBets().containsKey("Pass Line")) {
                    double bet = game.getBets().get("Pass Line");
                    totalPayout += bet * 2; // 1:1 payout
                    won = true;
                }
                game.getBets().clear(); // Clear bets after resolution
            } else if (total == 2 || total == 3 || total == 12) {
                // Pass line loses, Don't Pass wins (except 12 is push for Don't Pass)
                if (game.getBets().containsKey("Don't Pass Line") && total != 12) {
                    double bet = game.getBets().get("Don't Pass Line");
                    totalPayout += bet * 2; // 1:1 payout
                    won = true;
                }
                game.getBets().clear(); // Clear bets after resolution
            } else {
                // Point is established
                game.setPoint(total);
                game.setGameState(CrapsGame.GameState.POINT);
                player.sendMessage(ChatColor.YELLOW + "[CRAPS] Point established: " + total + ". Roll again!");
                return; // Don't clear bets, continue game
            }
        } else {
            // Point phase
            if (total == game.getPoint()) {
                // Point made - Pass line wins
                if (game.getBets().containsKey("Pass Line")) {
                    double bet = game.getBets().get("Pass Line");
                    totalPayout += bet * 2; // 1:1 payout
                    won = true;
                }
                game.setGameState(CrapsGame.GameState.COME_OUT);
                game.getBets().clear();
            } else if (total == 7) {
                // Seven out - Don't Pass wins
                if (game.getBets().containsKey("Don't Pass Line")) {
                    double bet = game.getBets().get("Don't Pass Line");
                    totalPayout += bet * 2; // 1:1 payout
                    won = true;
                }
                game.setGameState(CrapsGame.GameState.COME_OUT);
                game.getBets().clear();
            } else {
                // Other numbers: roll continues
                player.sendMessage(ChatColor.GRAY + "[CRAPS] Roll again! Need " + game.getPoint() + " to win, 7 to lose.");
                return;
            }
        }
        
        // Pay out winnings and show results
        if (totalPayout > 0) {
            Gambling.getEconomy().depositPlayer(player, totalPayout);
            double profit = totalPayout - (totalPayout / 2); // Profit is half of total payout
            player.sendMessage(ChatColor.GREEN + "[CRAPS] " + ChatColor.BOLD + "YOU WON! " +
                             ChatColor.GOLD + "+" + Gambling.getEconomy().format(profit));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);
        } else if (game.getBets().isEmpty()) {
            // Only show loss if bets were cleared (meaning they lost)
            player.sendMessage(ChatColor.RED + "[CRAPS] " + ChatColor.BOLD + "YOU LOST!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            // Loss already recorded when bet was placed
        }
    }
}
