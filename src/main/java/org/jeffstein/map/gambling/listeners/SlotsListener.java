package org.jeffstein.map.gambling.listeners;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.SlotMachine;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class SlotsListener implements Listener {

    private final Gambling plugin;

    public SlotsListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the clicked inventory is our custom slot machine GUI
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof SlotMachine) {
            // Prevent players from taking items out of the GUI [7, 10]
            event.setCancelled(true);

            // Ensure the clicker is a player
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            SlotMachine slotMachine = (SlotMachine) holder;
            int slot = event.getRawSlot();

            switch (slot) {
                case 40 -> slotMachine.spin();
                case 41 -> slotMachine.rebetAndSpin();
                case 44 -> {
                    Player player = (Player) event.getWhoClicked();
                    player.closeInventory();
                    player.sendActionBar(ChatColor.GRAY + "Thanks for playing slots!");
                }
                case 8 -> slotMachine.showPaytable();
                case 0 -> slotMachine.adjustBet(-100);
                case 1 -> slotMachine.adjustBet(-10);
                case 7 -> slotMachine.adjustBet(10);
                case 6 -> slotMachine.adjustBet(100);
                default -> {}
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof SlotMachine machine) {
            machine.sendSessionSummary(player);
            player.sendActionBar(ChatColor.GRAY + "Thanks for playing slots!");
        }
    }
}