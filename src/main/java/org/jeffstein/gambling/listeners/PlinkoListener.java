package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.PlinkoGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
            if (slot >= 0 && slot <= 8 && displayName.contains("Drop Here")) {
                if (plinkoGame.isBallDropping()) {
                    player.sendActionBar(ChatColor.RED + "Wait for the current ball to finish!");
                    return;
                }
                
                plinkoGame.dropBall(slot);
                player.sendActionBar(ChatColor.YELLOW + "Ball dropped from position " + (slot + 1) + "!");
            }
            
            // Handle bet adjustments
            else if (slot == 18 && displayName.contains("-100")) {
                plinkoGame.adjustBet(-100);
                player.sendActionBar(ChatColor.YELLOW + "Bet decreased to " + 
                                   org.jeffstein.gambling.Gambling.getEconomy().format(plinkoGame.getBetAmount()));
            }
            else if (slot == 26 && displayName.contains("+100")) {
                plinkoGame.adjustBet(100);
                player.sendActionBar(ChatColor.YELLOW + "Bet increased to " + 
                                   org.jeffstein.gambling.Gambling.getEconomy().format(plinkoGame.getBetAmount()));
            }
            
            // Handle back button
            else if (slot == 53 && displayName.contains("Back")) {
                if (plinkoGame.isBallDropping()) {
                    player.sendActionBar(ChatColor.RED + "Cannot leave while ball is dropping!");
                    return;
                }
                player.closeInventory();
                player.sendActionBar(ChatColor.GRAY + "Thanks for playing Plinko!");
            }
        }
    }
}
