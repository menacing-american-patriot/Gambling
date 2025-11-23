package org.jeffstein.map.gambling.listeners;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.BlackjackBettingGUI;
import org.jeffstein.map.gambling.games.BlackjackGUI;
import org.jeffstein.map.gambling.games.BlackjackGame;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlackjackBettingGUIListener implements Listener {

    private final Gambling plugin;
    private final Map<UUID, BlackjackGame> games = new HashMap<>();

    public BlackjackBettingGUIListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlackjackBettingGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            BlackjackBettingGUI bettingGUI = (BlackjackBettingGUI) holder;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            double currentBet = bettingGUI.getCurrentBet();

            if (displayName.equals(ChatColor.RED + "-100")) {
                currentBet = Math.max(10, currentBet - 100);
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.RED + "-50")) {
                currentBet = Math.max(10, currentBet - 50);
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.RED + "-10")) {
                currentBet = Math.max(10, currentBet - 10);
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "+10")) {
                currentBet += 10;
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "+50")) {
                currentBet += 50;
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "+100")) {
                currentBet += 100;
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "START GAME")) {
                Economy economy = Gambling.getEconomy();
                
                // Check if player has enough money
                if (economy.getBalance(player) < currentBet) {
                    player.sendActionBar(ChatColor.RED + "You don't have enough money to place that bet.");
                    return;
                }
                
                // Withdraw the bet amount
                economy.withdrawPlayer(player, currentBet);
                player.sendActionBar(ChatColor.YELLOW + "Blackjack started! Bet: " + economy.format(currentBet));
                
                // Create and start the game
                BlackjackGame game = new BlackjackGame(plugin, player, currentBet, games);
                games.put(player.getUniqueId(), game);
                game.start();
                
                // Open the blackjack game GUI
                BlackjackGUI gameGUI = new BlackjackGUI(plugin, player);
                gameGUI.openInventory();
                gameGUI.updateHands(game.getPlayerHand(), game.getDealerHand(), false);
                
            } else if (displayName.equals(ChatColor.YELLOW + "Back")) {
                player.closeInventory();
            }
        }
    }

    public Map<UUID, BlackjackGame> getGames() {
        return games;
    }
}
