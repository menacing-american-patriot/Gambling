package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BaccaratGUI;
import org.jeffstein.gambling.games.BaccaratGame;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import net.milkbowl.vault.economy.Economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaccaratListener implements Listener {

    private final Gambling plugin;
    private final Map<UUID, String> bets = new HashMap<>();
    private final Map<UUID, Double> betAmounts = new HashMap<>();

    public BaccaratListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BaccaratGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            int slot = event.getRawSlot();
            BaccaratGUI gui = (BaccaratGUI) holder;

            if (slot == 11) { // Player bet
                bets.put(playerId, "player");
                player.sendMessage(ChatColor.BLUE + "You are betting on " + ChatColor.BOLD + "Player" + ChatColor.BLUE + ".");
                player.sendMessage(ChatColor.GRAY + "Player wins pay 2:1 (even money)");
                gui.setSelectedBet("player");
            } else if (slot == 13) { // Tie bet
                bets.put(playerId, "tie");
                player.sendMessage(ChatColor.WHITE + "You are betting on a " + ChatColor.BOLD + "Tie" + ChatColor.WHITE + ".");
                player.sendMessage(ChatColor.GRAY + "Tie wins pay 9:1 (high risk, high reward!)");
                gui.setSelectedBet("tie");
            } else if (slot == 15) { // Banker bet
                bets.put(playerId, "banker");
                player.sendMessage(ChatColor.RED + "You are betting on " + ChatColor.BOLD + "Banker" + ChatColor.RED + ".");
                player.sendMessage(ChatColor.GRAY + "Banker wins pay 1.95:1 (5% commission)");
                gui.setSelectedBet("banker");
            } else if (slot >= 19 && slot <= 21) { // Bet amount buttons
                double amount = 0;
                if (slot == 19) amount = 100;
                if (slot == 20) amount = 500;
                if (slot == 21) amount = 1000;
                betAmounts.put(playerId, amount);
                Economy economy = Gambling.getEconomy();
                player.sendMessage(ChatColor.GOLD + "Bet amount set to " + economy.format(amount));
                if (economy.getBalance(player) < amount) {
                    player.sendMessage(ChatColor.RED + "Warning: You don't have enough money for this bet!");
                }
            } else if (slot == 22) { // Deal button
                if (!bets.containsKey(playerId)) {
                    player.sendMessage(ChatColor.RED + "Please select a bet type first (Player, Tie, or Banker).");
                    return;
                }
                if (!betAmounts.containsKey(playerId)) {
                    player.sendMessage(ChatColor.RED + "Please select a bet amount first.");
                    return;
                }

                double betAmount = betAmounts.get(playerId);
                Economy economy = Gambling.getEconomy();

                if (economy.getBalance(player) < betAmount) {
                    player.sendMessage(ChatColor.RED + "You do not have enough money for that bet. You need " + economy.format(betAmount) + ".");
                    return;
                }

                economy.withdrawPlayer(player, betAmount);
                player.sendMessage(ChatColor.YELLOW + "Bet placed: " + economy.format(betAmount) + " on " + bets.get(playerId).toUpperCase());
                player.sendMessage(ChatColor.GOLD + "Dealing cards...");

                BaccaratGame game = new BaccaratGame(plugin, player);

                new BukkitRunnable() {
                    private int ticks = 0;
                    private final int totalTicks = 20; // 1 second of spinning

                    @Override
                    public void run() {
                        if (ticks >= totalTicks) {
                            this.cancel();
                            game.deal();
                            gui.updateHands(game.getPlayerHand(), game.getBankerHand());

                            int playerValue = game.getHandValue(game.getPlayerHand());
                            int bankerValue = game.getHandValue(game.getBankerHand());

                            // Send detailed game results
                            player.sendMessage(ChatColor.AQUA + "=== BACCARAT RESULTS ===");
                            player.sendMessage(ChatColor.BLUE + "Player Hand: " + getHandString(game.getPlayerHand()) + ChatColor.GRAY + " (Value: " + playerValue + ")");
                            player.sendMessage(ChatColor.RED + "Banker Hand: " + getHandString(game.getBankerHand()) + ChatColor.GRAY + " (Value: " + bankerValue + ")");

                            String winner;
                            if (playerValue > bankerValue) {
                                winner = "player";
                                player.sendMessage(ChatColor.BLUE + ChatColor.BOLD + "PLAYER WINS!");
                            } else if (bankerValue > playerValue) {
                                winner = "banker";
                                player.sendMessage(ChatColor.RED + ChatColor.BOLD + "BANKER WINS!");
                            } else {
                                winner = "tie";
                                player.sendMessage(ChatColor.WHITE + ChatColor.BOLD + "IT'S A TIE!");
                            }

                            gui.showResult(winner);

                            String betType = bets.get(playerId);
                            double payout = 0;

                            if (betType.equals(winner)) {
                                if (betType.equals("player")) {
                                    payout = betAmount * 2;
                                } else if (betType.equals("banker")) {
                                    payout = betAmount * 1.95; // 5% commission
                                } else if (betType.equals("tie")) {
                                    payout = betAmount * 9;
                                }
                                player.sendMessage(ChatColor.GREEN + ChatColor.BOLD + "CONGRATULATIONS! You won " + economy.format(payout) + "!");
                                player.sendMessage(ChatColor.GRAY + "Your bet on " + betType.toUpperCase() + " was correct!");
                                economy.depositPlayer(player, payout);
                                Gambling.getLeaderboard().addWin(playerId, betAmount);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                            } else {
                                player.sendMessage(ChatColor.RED + "You lost " + economy.format(betAmount) + ".");
                                player.sendMessage(ChatColor.GRAY + "You bet on " + betType.toUpperCase() + " but " + winner.toUpperCase() + " won.");
                                Gambling.getLeaderboard().addLoss(playerId, betAmount);
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            }
                            player.sendMessage(ChatColor.AQUA + "======================");
                            betAmounts.remove(playerId);
                            return;
                        }

                        gui.updateHands(game.getPlayerHand(), game.getBankerHand());
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.5f);
                        ticks += 2;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            } else if (slot == 22 && event.getCurrentItem() != null &&
                       event.getCurrentItem().getItemMeta().getDisplayName().contains("NEW GAME")) {
                // Reset the game for a new round
                bets.remove(playerId);
                betAmounts.remove(playerId);
                player.sendMessage(ChatColor.YELLOW + "Starting a new game of Baccarat!");

                // Reopen a fresh GUI
                BaccaratGUI newGui = new BaccaratGUI(plugin, player);
                newGui.openInventory();
            }
        }
    }

    private String getHandString(java.util.List<org.jeffstein.gambling.games.Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i).getRank()).append(" of ").append(hand.get(i).getSuit());
            if (i < hand.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
