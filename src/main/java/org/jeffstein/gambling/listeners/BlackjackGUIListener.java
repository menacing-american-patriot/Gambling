package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BlackjackGUI;
import org.jeffstein.gambling.games.BlackjackGame;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlackjackGUIListener implements Listener {

    private final Gambling plugin;
    private final Map<UUID, BlackjackGame> games;

    public BlackjackGUIListener(Gambling plugin, Map<UUID, BlackjackGame> games) {
        this.plugin = plugin;
        this.games = games;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BlackjackGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            int slot = event.getRawSlot();

            if (!games.containsKey(playerId)) {
                player.sendMessage(ChatColor.RED + "No active game found. Please start a new game from the betting menu.");
                player.closeInventory();
                return;
            }

            BlackjackGame game = games.get(playerId);

            if (slot == 11) { // Hit button
                game.hit();
                ((BlackjackGUI) holder).updateHands(game.getPlayerHand(), game.getDealerHand(), false);
            } else if (slot == 15) { // Stand button
                game.stand();
                ((BlackjackGUI) holder).updateHands(game.getPlayerHand(), game.getDealerHand(), true);
            }
        }
    }
}
