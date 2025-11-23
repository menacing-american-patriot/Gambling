package org.jeffstein.map.gambling.listeners;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.MinesGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class MinesListener implements Listener {

    private final Gambling plugin;

    public MinesListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MinesGame) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            MinesGame minesGame = (MinesGame) holder;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int slot = event.getSlot();

            // Handle grid clicks (slots 0-24)
            if (slot >= 0 && slot <= 24) {
                if (!minesGame.isGameStarted()) {
                    player.sendActionBar(ChatColor.RED + "Start the game first!");
                    return;
                }
                if (minesGame.isGameEnded()) {
                    player.sendActionBar(ChatColor.RED + "Game is over! Start a new game.");
                    return;
                }
                
                minesGame.revealSquare(slot);
            }
            
            // Handle bet adjustments
            else if (slot == 27 && displayName.contains("-100")) {
                minesGame.adjustBet(-100);
            }
            else if (slot == 28 && displayName.contains("-50")) {
                minesGame.adjustBet(-50);
            }
            else if (slot == 30 && displayName.contains("+50")) {
                minesGame.adjustBet(50);
            }
            else if (slot == 31 && displayName.contains("+100")) {
                minesGame.adjustBet(100);
            }
            
            // Handle mine count adjustments
            else if (slot == 33 && displayName.contains("Mines: -1")) {
                minesGame.adjustMines(-1);
            }
            else if (slot == 35 && displayName.contains("Mines: +1")) {
                minesGame.adjustMines(1);
            }
            
            // Handle game controls
            else if (slot == 40) {
                if (displayName.contains("START GAME")) {
                    minesGame.startGame();
                } else if (displayName.contains("CASH OUT")) {
                    minesGame.cashOut();
                } else if (displayName.contains("GAME OVER")) {
                    minesGame.resetGame();
                    player.sendActionBar(ChatColor.YELLOW + "New game ready! Adjust your settings and click START GAME.");
                }
            }
            
            // Handle back button
            else if (slot == 45 && displayName.contains("Back")) {
                if (minesGame.isGameStarted() && !minesGame.isGameEnded()) {
                    player.sendActionBar(ChatColor.RED + "Cannot leave during an active game! Cash out or finish the game first.");
                    return;
                }
                player.closeInventory();
                player.sendActionBar(ChatColor.GRAY + "Thanks for playing Mines!");
            }
        }
    }
}
