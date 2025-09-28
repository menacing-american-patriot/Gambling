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
                // Sound + chat feedback instead of hidden action bar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                player.sendMessage(ChatColor.BLUE + "[BACCARAT] Betting on " + ChatColor.BOLD + "PLAYER" + ChatColor.BLUE + " - Pays 2:1");
                gui.setSelectedBet("player");
            } else if (slot == 13) { // Tie bet
                bets.put(playerId, "tie");
                // Sound + chat feedback instead of hidden action bar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                player.sendMessage(ChatColor.WHITE + "[BACCARAT] Betting on " + ChatColor.BOLD + "TIE" + ChatColor.WHITE + " - Pays 9:1 (High Risk!)");
                gui.setSelectedBet("tie");
            } else if (slot == 15) { // Banker bet
                bets.put(playerId, "banker");
                // Sound + chat feedback instead of hidden action bar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                player.sendMessage(ChatColor.RED + "[BACCARAT] Betting on " + ChatColor.BOLD + "BANKER" + ChatColor.RED + " - Pays 1.95:1");
                gui.setSelectedBet("banker");
            } else if (slot >= 19 && slot <= 21) { // Bet amount buttons
                double amount = 0;
                if (slot == 19) amount = 100;
                if (slot == 20) amount = 500;
                if (slot == 21) amount = 1000;
                betAmounts.put(playerId, amount);
                Economy economy = Gambling.getEconomy();
                if (economy.getBalance(player) < amount) {
                    // Sound + chat feedback instead of hidden action bar
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[BACCARAT] Insufficient funds! Need " + economy.format(amount));
                } else {
                    // Sound + chat feedback instead of hidden action bar
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(ChatColor.GOLD + "[BACCARAT] Bet amount: " + economy.format(amount) + " | Balance: " + economy.format(economy.getBalance(player)));
                }
            } else if (slot == 22) { // Deal button
                if (!bets.containsKey(playerId)) {
                    // Sound + chat feedback instead of hidden action bar
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[BACCARAT] Select a bet type first: Player, Tie, or Banker");
                    return;
                }
                if (!betAmounts.containsKey(playerId)) {
                    // Sound + chat feedback instead of hidden action bar
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[BACCARAT] Select a bet amount first");
                    return;
                }

                double betAmount = betAmounts.get(playerId);
                Economy economy = Gambling.getEconomy();

                if (economy.getBalance(player) < betAmount) {
                    // Sound + chat feedback instead of hidden action bar
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[BACCARAT] Insufficient funds! Need " + economy.format(betAmount));
                    return;
                }

                economy.withdrawPlayer(player, betAmount);
                // Sound + chat feedback instead of hidden action bar
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(ChatColor.YELLOW + "[BACCARAT] Dealing cards... Bet: " + economy.format(betAmount) + " on " + bets.get(playerId).toUpperCase());

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

                            // Close GUI first so results are visible
                            player.closeInventory();

                            // Send hand values to chat instead of hidden action bar
                            player.sendMessage(ChatColor.YELLOW + "[BACCARAT] " + ChatColor.BLUE + "Player: " + playerValue + ChatColor.GRAY + " | " + ChatColor.RED + "Banker: " + bankerValue);

                            String winner;
                            String titleText;
                            String subtitleText;

                            if (playerValue > bankerValue) {
                                winner = "player";
                                titleText = ChatColor.BLUE + "" + ChatColor.BOLD + "PLAYER WINS!";
                                subtitleText = ChatColor.GRAY + "Player: " + playerValue + " | Banker: " + bankerValue;
                            } else if (bankerValue > playerValue) {
                                winner = "banker";
                                titleText = ChatColor.RED + "" + ChatColor.BOLD + "BANKER WINS!";
                                subtitleText = ChatColor.GRAY + "Player: " + playerValue + " | Banker: " + bankerValue;
                            } else {
                                winner = "tie";
                                titleText = ChatColor.WHITE + "" + ChatColor.BOLD + "IT'S A TIE!";
                                subtitleText = ChatColor.GRAY + "Both hands: " + playerValue;
                            }

                            // Show result as title/subtitle (now visible since GUI is closed)
                            player.sendTitle(titleText, subtitleText, 10, 60, 20);

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

                                // Make variables final for inner class
                                final double finalPayout = payout;
                                final String finalBetType = betType;

                                // Show win notification after a delay
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "YOU WON!",
                                                        ChatColor.GOLD + "+" + economy.format(finalPayout), 10, 40, 10);
                                        // Also send to chat for visibility
                                        player.sendMessage(ChatColor.GREEN + "[BACCARAT] " + ChatColor.BOLD + "YOU WON! " +
                                                         ChatColor.GOLD + "+" + economy.format(finalPayout) +
                                                         ChatColor.GREEN + " - Your bet on " + finalBetType.toUpperCase() + " was correct!");
                                    }
                                }.runTaskLater(plugin, 80L); // 4 seconds delay

                                economy.depositPlayer(player, payout);
                                Gambling.getLeaderboard().addWin(playerId, betAmount);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                            } else {
                                // Make variables final for inner class
                                final String finalBetType = betType;
                                final String finalWinner = winner;
                                final double finalBetAmount = betAmount;

                                // Show loss notification after a delay
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "YOU LOST",
                                                        ChatColor.GRAY + "-" + economy.format(finalBetAmount), 10, 40, 10);
                                        // Also send to chat for visibility
                                        player.sendMessage(ChatColor.RED + "[BACCARAT] " + ChatColor.BOLD + "YOU LOST " +
                                                         ChatColor.GRAY + "-" + economy.format(finalBetAmount) +
                                                         ChatColor.RED + " - You bet on " + finalBetType.toUpperCase() + " but " + finalWinner.toUpperCase() + " won");
                                    }
                                }.runTaskLater(plugin, 80L); // 4 seconds delay

                                Gambling.getLeaderboard().addLoss(playerId, betAmount);
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            }
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
                // Sound + chat feedback instead of hidden action bar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                player.sendMessage(ChatColor.YELLOW + "[BACCARAT] Starting a new game of Baccarat!");

                // Reset the current GUI instead of creating a new one
                BaccaratGUI currentGui = (BaccaratGUI) holder;
                currentGui.resetForNewGame();

            } else if (slot == 25) { // Help button
                // Sound + helpful message
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                player.sendMessage(ChatColor.AQUA + "[BACCARAT] " + ChatColor.BOLD + "HOW TO PLAY:");
                player.sendMessage(ChatColor.WHITE + "• Choose Player, Banker, or Tie");
                player.sendMessage(ChatColor.WHITE + "• Select your bet amount");
                player.sendMessage(ChatColor.WHITE + "• Click 'DEAL CARDS' to start");
                player.sendMessage(ChatColor.WHITE + "• Hand closest to 9 wins!");
                player.sendMessage(ChatColor.YELLOW + "Payouts: " + ChatColor.BLUE + "Player 2:1" + ChatColor.GRAY + " | " +
                                 ChatColor.RED + "Banker 1.95:1" + ChatColor.GRAY + " | " + ChatColor.WHITE + "Tie 9:1");
            } else if (slot == 26) { // Back button
                player.closeInventory();
                // Sound + chat feedback instead of hidden action bar
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GRAY + "[BACCARAT] Thanks for playing Baccarat!");
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
