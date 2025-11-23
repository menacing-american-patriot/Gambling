package org.jeffstein.map.gambling.listeners;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.WheelOfFortuneGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class WheelOfFortuneListener implements Listener {

    private final Gambling plugin;

    public WheelOfFortuneListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof WheelOfFortuneGame) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            WheelOfFortuneGame wheelGame = (WheelOfFortuneGame) holder;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int slot = event.getSlot();

            // Handle bet amount adjustments
            if (slot == 37) {
                wheelGame.adjustBet(-100);
                player.sendActionBar(ChatColor.YELLOW + "Bet: " + Gambling.getEconomy().format(wheelGame.getCurrentBet()));
            } else if (slot == 38) {
                wheelGame.adjustBet(-50);
                player.sendActionBar(ChatColor.YELLOW + "Bet: " + Gambling.getEconomy().format(wheelGame.getCurrentBet()));
            } else if (slot == 39) {
                wheelGame.adjustBet(-10);
                player.sendActionBar(ChatColor.YELLOW + "Bet: " + Gambling.getEconomy().format(wheelGame.getCurrentBet()));
            } else if (slot == 51) {
                wheelGame.adjustBet(10);
                player.sendActionBar(ChatColor.YELLOW + "Bet: " + Gambling.getEconomy().format(wheelGame.getCurrentBet()));
            } else if (slot == 52) {
                wheelGame.adjustBet(50);
                player.sendActionBar(ChatColor.YELLOW + "Bet: " + Gambling.getEconomy().format(wheelGame.getCurrentBet()));
            } else if (slot == 53) {
                wheelGame.adjustBet(100);
                player.sendActionBar(ChatColor.YELLOW + "Bet: " + Gambling.getEconomy().format(wheelGame.getCurrentBet()));
            }

            // Handle betting buttons
            else if (slot == 46 && displayName.contains("Bet on Numbers")) {
                boolean success = wheelGame.placeBet("Numbers", wheelGame.getCurrentBet());
                if (success) {
                    player.sendActionBar(ChatColor.GREEN + "Bet placed on all number segments!");
                }
            }
            else if (slot == 47 && displayName.contains("Bet on LOSE")) {
                boolean success = wheelGame.placeBet("LOSE", wheelGame.getCurrentBet());
                if (success) {
                    player.sendActionBar(ChatColor.GREEN + "Bet placed on LOSE segments!");
                }
            }
            else if (slot == 48 && displayName.contains("Bet on JACKPOT")) {
                boolean success = wheelGame.placeBet("JACKPOT", wheelGame.getCurrentBet());
                if (success) {
                    player.sendActionBar(ChatColor.GREEN + "Bet placed on JACKPOT segment!");
                }
            }

            // Handle spin button
            else if (slot == 49 && displayName.contains("SPIN WHEEL")) {
                if (wheelGame.isSpinning()) {
                    player.sendActionBar(ChatColor.RED + "Wheel is already spinning!");
                } else {
                    wheelGame.spinWheel();
                }
            }
            
            // Handle back button
            else if (slot == 45 && displayName.contains("Back")) {
                if (wheelGame.isSpinning()) {
                    player.sendActionBar(ChatColor.RED + "Cannot leave while wheel is spinning!");
                    return;
                }
                player.closeInventory();
                player.sendActionBar(ChatColor.GRAY + "Thanks for playing Wheel of Fortune!");
            }
        }
    }
}
