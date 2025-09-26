package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.CrapsGame;
import org.jeffstein.gambling.games.CrapsGUI;
import org.jeffstein.gambling.games.CrapsBettingGUI;
import org.bukkit.ChatColor;
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        if (holder instanceof CrapsGUI) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            CrapsGame game = games.get(player.getUniqueId());
            if (game == null) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "ROLL")) {
                int[] dice = game.rollDice();
                int total = dice[0] + dice[1];
                player.sendMessage("You rolled a " + dice[0] + " and a " + dice[1] + " for a total of " + total);

                double payout = 0;
                if (game.getGameState() == CrapsGame.GameState.COME_OUT) {
                    if (total == 7 || total == 11) {
                        payout = game.getBets().getOrDefault("Pass Line", 0.0) * 2;
                        player.sendMessage(ChatColor.GREEN + "You win!");
                        games.remove(player.getUniqueId());
                    } else if (total == 2 || total == 3 || total == 12) {
                        payout = game.getBets().getOrDefault("Don't Pass Line", 0.0) * 2;
                        player.sendMessage(ChatColor.RED + "You lose!");
                        games.remove(player.getUniqueId());
                    } else {
                        game.setPoint(total);
                        game.setGameState(CrapsGame.GameState.POINT);
                        player.sendMessage("The point is now " + total);
                    }
                } else { // Point phase
                    if (total == game.getPoint()) {
                        payout = game.getBets().getOrDefault("Pass Line", 0.0) * 2;
                        player.sendMessage(ChatColor.GREEN + "You win!");
                        games.remove(player.getUniqueId());
                    } else if (total == 7) {
                        payout = game.getBets().getOrDefault("Don't Pass Line", 0.0) * 2;
                        player.sendMessage(ChatColor.RED + "You lose!");
                        games.remove(player.getUniqueId());
                    }
                }

                if (payout > 0) {
                    Gambling.getEconomy().depositPlayer(player, payout);
                    player.sendMessage(ChatColor.GREEN + "You won " + Gambling.getEconomy().format(payout));
                }
                ((CrapsGUI) holder).update(game);
            }
        } else if (holder instanceof CrapsBettingGUI) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null) {
                return;
            }

            CrapsBettingGUI bettingGUI = (CrapsBettingGUI) holder;
            double currentBet = bettingGUI.getCurrentBet();

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals(ChatColor.RED + "-100")) {
                currentBet = Math.max(0, currentBet - 100);
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "+100")) {
                currentBet += 100;
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "ROLL")) {
                CrapsGame game = games.get(player.getUniqueId());
                if (game == null || game.getBets().isEmpty()) {
                    player.sendMessage("Please place a bet first.");
                    return;
                }
                CrapsGUI crapsGUI = new CrapsGUI(plugin, player);
                crapsGUI.openInventory();
                crapsGUI.update(game);
            } else {
                CrapsGame game = games.computeIfAbsent(player.getUniqueId(), k -> new CrapsGame(plugin, player));
                if (Gambling.getEconomy().getBalance(player) >= currentBet) {
                    Gambling.getEconomy().withdrawPlayer(player, currentBet);
                    game.placeBet(displayName, currentBet);
                    player.sendMessage("You placed a bet of " + Gambling.getEconomy().format(currentBet) + " on " + displayName);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have enough money to place that bet.");
                }
            }
        }
    }
}
