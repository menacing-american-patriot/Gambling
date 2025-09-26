package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.RouletteBettingGUI;
import org.jeffstein.gambling.games.RouletteGUI;
import org.jeffstein.gambling.games.RouletteGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RouletteListener implements Listener {

    private final Gambling plugin;
    private final Map<UUID, RouletteGame> games = new HashMap<>();

    public RouletteListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof RouletteGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot == 22) { // Spin button
                RouletteGame game = games.get(player.getUniqueId());
                if (game == null || game.getBets().isEmpty()) {
                    player.sendMessage("Please place a bet first.");
                    return;
                }

                new BukkitRunnable() {
                    private int ticks = 0;
                    private final int totalTicks = 40; // 2 seconds of spinning
                    private final int[] border = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53, 52, 51, 50, 49, 48, 47, 46, 37, 28, 19, 10};

                    @Override
                    public void run() {
                        if (ticks >= totalTicks) {
                            this.cancel();
                            game.spin();
                            int winningNumber = game.getWinningNumber();
                            player.sendMessage("The winning number is " + winningNumber);

                            // Payout logic
                            double totalPayout = 0;
                            for (Map.Entry<String, Double> entry : game.getBets().entrySet()) {
                                String betType = entry.getKey();
                                double betAmount = entry.getValue();
                                double payout = 0;

                                if (betType.equals(String.valueOf(winningNumber))) {
                                    payout = betAmount * 36;
                                } else if (betType.equals("Black") && winningNumber % 2 == 0 && winningNumber != 0) {
                                    payout = betAmount * 2;
                                } else if (betType.equals("Red") && winningNumber % 2 != 0) {
                                    payout = betAmount * 2;
                                }

                                if (payout > 0) {
                                    totalPayout += payout;
                                    Gambling.getEconomy().depositPlayer(player, payout);
                                }
                            }

                            if (totalPayout > 0) {
                                player.sendMessage(ChatColor.GREEN + "You won " + Gambling.getEconomy().format(totalPayout));
                                Gambling.getLeaderboard().addWin(player.getUniqueId(), totalPayout);
                            } else {
                                player.sendMessage(ChatColor.RED + "You lost!");
                                Gambling.getLeaderboard().addLoss(player.getUniqueId(), game.getBets().values().stream().mapToDouble(Double::doubleValue).sum());
                            }

                            games.remove(player.getUniqueId());
                            return;
                        }

                        int index = ticks % border.length;
                        for (int i = 0; i < border.length; i++) {
                            ItemStack item = event.getInventory().getItem(border[i]);
                            if (item != null) {
                                if (i == index) {
                                    item.setType(Material.DIAMOND_BLOCK);
                                } else {
                                    item.setType((i % 2 == 0)? Material.RED_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE);
                                }
                            }
                        }

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.5f);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            } else {
                RouletteBettingGUI bettingGUI = new RouletteBettingGUI(plugin, player);
                bettingGUI.openInventory();
            }
        } else if (holder instanceof RouletteBettingGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String betType = clickedItem.getItemMeta().getDisplayName();
            // For now, let's just assume a bet of 100
            double betAmount = 100;

            RouletteGame game = games.computeIfAbsent(player.getUniqueId(), k -> new RouletteGame(plugin, player));
            game.placeBet(betType, betAmount);
            player.sendMessage("You placed a bet of " + Gambling.getEconomy().format(betAmount) + " on " + betType);
        }
    }
}
