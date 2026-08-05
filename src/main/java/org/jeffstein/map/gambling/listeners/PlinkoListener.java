package org.jeffstein.map.gambling.listeners;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.PlinkoGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PlinkoListener implements Listener {

    private final Gambling plugin;

    public PlinkoListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof PlinkoGame) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            PlinkoGame plinkoGame = (PlinkoGame) holder;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int slot = event.getSlot();

            // Handle drop positions (top row, slots 0-8)
            String strippedName = ChatColor.stripColor(displayName).toLowerCase();

            if (slot >= 2 && slot <= 6 && (strippedName.contains("drop") || strippedName.contains("column"))) {
                plinkoGame.dropBall(slot);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }

            // Handle bet adjustments (top row locked tiles, symmetrical)
            else if (slot == 0 && displayName.contains("-1000")) {
                plinkoGame.adjustBet(-1000);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else if (slot == 1 && displayName.contains("-50")) {
                plinkoGame.adjustBet(-50);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else if (slot == 7 && displayName.contains("+50")) {
                plinkoGame.adjustBet(50);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            } else if (slot == 8 && displayName.contains("+1000")) {
                plinkoGame.adjustBet(1000);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            }

            // Drop again button
            else if (slot == 32 && displayName.toLowerCase().contains("drop again")) {
                plinkoGame.dropAgain();
            }

            // Risk profile button
            else if (slot == 17 && displayName.contains("Risk Mode")) {
                plinkoGame.cycleRiskProfile();
            }

            // Handle back button
            else if (slot == 53) {
                if (plinkoGame.isBallDropping()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                } else {
                    player.closeInventory();
                    player.sendMessage(ChatColor.GRAY + "Thanks for playing Plinko!");
                }
            }
        }
    }
}
