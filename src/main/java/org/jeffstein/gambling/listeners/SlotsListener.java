package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.SlotMachine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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

            // Check if they clicked the "Spin" button (slot 22)
            if (event.getRawSlot() == 22) {
                SlotMachine slotMachine = (SlotMachine) holder;
                slotMachine.spin();
            }
        }
    }
}