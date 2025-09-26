package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BlackjackGUI;
import org.jeffstein.gambling.games.BlackjackGame;
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
    private final Map<UUID, BlackjackGame> games = new HashMap<>();

    public BlackjackGUIListener(Gambling plugin) {
        this.plugin = plugin;
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
                // For now, let's just assume a bet of 100
                BlackjackGame game = new BlackjackGame(plugin, player, 100, games);
                games.put(playerId, game);
                ((BlackjackGUI) holder).updateHands(game.getPlayerHand(), game.getDealerHand(), false);
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
