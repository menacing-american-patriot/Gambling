package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.KenoGUI;
import org.jeffstein.gambling.games.KenoGame;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import net.milkbowl.vault.economy.Economy;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class KenoListener implements Listener {

    private static KenoListener instance;
    public static KenoListener getInstance() { return instance; }

    private final Gambling plugin;
    private final Map<UUID, Double> betAmounts = new HashMap<>();

    public KenoListener(Gambling plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public void setBetForPlayer(Player player, double amount) {
        betAmounts.put(player.getUniqueId(), Math.max(10.0, Math.min(1000000.0, amount)));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof KenoGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            UUID playerId = player.getUniqueId();
            KenoGUI gui = (KenoGUI) holder;
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            int slot = event.getRawSlot();

            // Number selection
            if (slot < 45) {
                try {
                    int number = Integer.parseInt(clickedItem.getItemMeta().getDisplayName());
                    if (gui.getSelectedNumbers().contains(number)) {
                        gui.getSelectedNumbers().remove(Integer.valueOf(number));
                        event.getInventory().setItem(slot, new ItemStack(Material.PAPER)); // Deselect
                    } else {
                        if (gui.getSelectedNumbers().size() < 10) { // Max 10 numbers
                            gui.getSelectedNumbers().add(number);
                            event.getInventory().setItem(slot, new ItemStack(Material.EMERALD)); // Select
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Start Game
            if (slot == 48) {
                if (gui.getSelectedNumbers().isEmpty()) {
                    player.sendActionBar(ChatColor.RED + "You must select at least one number.");
                    return;
                }
                double betAmount = betAmounts.getOrDefault(playerId, 100.0);
                Economy economy = Gambling.getEconomy();
                if (economy.getBalance(player) < betAmount) {
                    player.sendActionBar(ChatColor.RED + "You don't have enough money.");
                    return;
                }
                economy.withdrawPlayer(player, betAmount);

                KenoGame game = new KenoGame(gui.getSelectedNumbers());
                game.drawNumbers();
                List<Integer> winningNumbers = game.getDrawnNumbers();
                int matches = game.getMatches();
                double payout = game.getPayout(betAmount);

                // Show results with title and action bar instead of chat
                if (payout > 0) {
                    player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "KENO WIN!",
                                   ChatColor.GOLD + "+" + economy.format(payout) + " (" + matches + " matches)", 10, 60, 20);
                    player.sendActionBar(ChatColor.GREEN + "You won " + economy.format(payout) + " with " + matches + " matches!");
                    economy.depositPlayer(player, payout);
                } else {
                    player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "NO WIN",
                                   ChatColor.GRAY + "You matched " + matches + " numbers", 10, 40, 10);
                    player.sendActionBar(ChatColor.RED + "No win this time. You matched " + matches + " numbers.");
                }
                player.closeInventory();
            }

            // Clear Selection
            if (slot == 49) {
                gui.getSelectedNumbers().clear();
                for (int i = 0; i < 45; i++) {
                    gui.getInventory().setItem(i, new ItemStack(Material.PAPER));
                }
            }

            // Bet Amount
            if (slot == 50) {
                betAmounts.put(playerId, 100.0);
                player.sendActionBar(ChatColor.GOLD + "Bet amount set to 100.");
            }
        }
    }
}
