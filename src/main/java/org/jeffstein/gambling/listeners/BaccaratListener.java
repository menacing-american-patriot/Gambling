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
                player.sendMessage("You are betting on Player.");
                gui.setSelectedBet("player");
            } else if (slot == 13) { // Tie bet
                bets.put(playerId, "tie");
                player.sendMessage("You are betting on a Tie.");
                gui.setSelectedBet("tie");
            } else if (slot == 15) { // Banker bet
                bets.put(playerId, "banker");
                player.sendMessage("You are betting on Banker.");
                gui.setSelectedBet("banker");
            } else if (slot >= 19 && slot <= 21) { // Bet amount buttons
                double amount = 0;
                if (slot == 19) amount = 100;
                if (slot == 20) amount = 500;
                if (slot == 21) amount = 1000;
                betAmounts.put(playerId, amount);
                player.sendMessage("You are betting " + amount);
            } else if (slot == 22) { // Deal button
                if (!bets.containsKey(playerId)) {
                    player.sendMessage("Please select a bet type.");
                    return;
                }
                if (!betAmounts.containsKey(playerId)) {
                    player.sendMessage("Please select a bet amount.");
                    return;
                }

                double betAmount = betAmounts.get(playerId);
                Economy economy = Gambling.getEconomy();

                if (economy.getBalance(player) < betAmount) {
                    player.sendMessage("You do not have enough money for that bet.");
                    return;
                }

                economy.withdrawPlayer(player, betAmount);

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

                            String winner;
                            if (playerValue > bankerValue) {
                                winner = "player";
                            } else if (bankerValue > playerValue) {
                                winner = "banker";
                            } else {
                                winner = "tie";
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
                                player.sendMessage(ChatColor.GREEN + "You won! " + economy.format(payout));
                                economy.depositPlayer(player, payout);
                                Gambling.getLeaderboard().addWin(playerId, betAmount);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                            } else {
                                player.sendMessage(ChatColor.RED + "You lost! " + economy.format(betAmount));
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
            }
        }
    }
}
