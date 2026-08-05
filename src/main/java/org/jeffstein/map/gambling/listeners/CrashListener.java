package org.jeffstein.map.gambling.listeners;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.CrashGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CrashListener implements Listener {

    private final Gambling plugin;

    public CrashListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CrashGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            CrashGUI crashGUI = (CrashGUI) holder;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int slot = event.getSlot();

            // Handle bet amount adjustments
            if (slot == 37) { // -10000
                crashGUI.adjustBet(-10000);
            } else if (slot == 38) { // -1000
                crashGUI.adjustBet(-1000);
            } else if (slot == 39) { // -100
                crashGUI.adjustBet(-100);
            } else if (slot == 41) { // +100
                crashGUI.adjustBet(100);
            } else if (slot == 42) { // +1000
                crashGUI.adjustBet(1000);
            } else if (slot == 43) { // +10000
                crashGUI.adjustBet(10000);
            }
            
            // Handle auto-cashout adjustments
            else if (slot == 28) { // Auto-cashout -0.1x
                crashGUI.adjustAutoCashout(-0.1);
            } else if (slot == 30) { // Auto-cashout +0.1x
                crashGUI.adjustAutoCashout(0.1);
            }
            
            // Handle main actions
            else if (slot == 31) { // Main action button
                if (displayName.contains("PLACE BET")) {
                    boolean success = crashGUI.placeBet();
                    if (success) {
                        player.sendActionBar(ChatColor.GREEN + "Bet placed! Good luck!");
                        
                        // Schedule GUI updates during the game
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (player.getOpenInventory().getTopInventory().getHolder() instanceof CrashGUI) {
                                    crashGUI.updateDisplay();
                                } else {
                                    this.cancel();
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 5L); // Update every 5 ticks
                    }
                } else if (displayName.contains("CASH OUT")) {
                    crashGUI.cashOut();
                }
            }
            
            // Handle current bet display click (alternative way to place bet)
            else if (slot == 40 && displayName.contains("Current Bet")) {
                boolean success = crashGUI.placeBet();
                if (success) {
                    player.sendActionBar(ChatColor.GREEN + "Bet placed! Good luck!");
                    
                    // Schedule GUI updates during the game
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.getOpenInventory().getTopInventory().getHolder() instanceof CrashGUI) {
                                crashGUI.updateDisplay();
                            } else {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 5L); // Update every 5 ticks
                }
            }
            
            // Handle back button
            else if (slot == 45) { // Back button
                player.closeInventory();
                player.sendActionBar(ChatColor.GRAY + "Thanks for playing Crash!");
            }
        }
    }
}
