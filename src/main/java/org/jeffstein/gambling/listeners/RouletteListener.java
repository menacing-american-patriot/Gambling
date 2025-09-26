package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.RouletteBettingGUI;
import org.jeffstein.gambling.games.RouletteGUI;
import org.jeffstein.gambling.games.RouletteGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RouletteListener implements Listener {

    private final Gambling plugin;
    private final Map<UUID, RouletteGame> games = new HashMap<>();
    private final Map<UUID, Double> currentBets = new HashMap<>();

    public RouletteListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        if (holder instanceof RouletteGUI) {
            event.setCancelled(true);

            RouletteGUI rouletteGUI = (RouletteGUI) holder;
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();

            if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "SPIN")) {
                RouletteGame game = games.get(player.getUniqueId());
                if (game == null) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[ROULETTE] No game found. Please place a bet first.");
                    return;
                }
                if (game.getBets().isEmpty()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[ROULETTE] Please place a bet first.");
                    return;
                }
                // Sound + chat feedback for spin start
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(ChatColor.YELLOW + "[ROULETTE] Starting roulette spin...");
                startSpin(player, game, rouletteGUI);
            } else if (displayName.equals(ChatColor.GREEN + "Spin Again")) {
                RouletteGame oldGame = games.get(player.getUniqueId());
                if (oldGame == null || oldGame.getBets().isEmpty()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[ROULETTE] No previous bet found.");
                    return;
                }

                RouletteGame newGame = new RouletteGame(plugin, player);
                newGame.getBets().putAll(oldGame.getBets());
                games.put(player.getUniqueId(), newGame);

                double totalBet = newGame.getBets().values().stream().mapToDouble(Double::doubleValue).sum();
                RouletteGUI newRouletteGUI = new RouletteGUI(plugin, player, totalBet);
                newRouletteGUI.openInventory();
                startSpin(player, newGame, newRouletteGUI);

            } else if (displayName.equals(ChatColor.YELLOW + "Edit Bet")) {
                RouletteBettingGUI bettingGUI = new RouletteBettingGUI(plugin, player);
                bettingGUI.openInventory();
            }
        } else if (holder instanceof RouletteBettingGUI) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            RouletteBettingGUI bettingGUI = (RouletteBettingGUI) holder;
            double currentBet = bettingGUI.getCurrentBet();

            String displayName = clickedItem.getItemMeta().getDisplayName();
            if (displayName.equals(ChatColor.RED + "-100")) {
                currentBet = Math.max(0, currentBet - 100);
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "+100")) {
                currentBet += 100;
                bettingGUI.setCurrentBet(currentBet);
            } else if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "SPIN")) {
                RouletteGame game = games.get(player.getUniqueId());
                if (game == null || game.getBets().isEmpty()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[ROULETTE] Please place a bet first.");
                    return;
                }
                double totalBet = game.getBets().values().stream().mapToDouble(Double::doubleValue).sum();
                RouletteGUI newRouletteGUI = new RouletteGUI(plugin, player, totalBet);
                newRouletteGUI.openInventory();
                startSpin(player, game, newRouletteGUI);
            } else if (displayName.equals(ChatColor.GOLD + "Current Bet")) {
                // Do nothing
            } else if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) {
                // Do nothing for spin button
            } else {
                String betType = displayName;
                if (Gambling.getEconomy().getBalance(player) >= currentBet) {
                    Gambling.getEconomy().withdrawPlayer(player, currentBet);
                    RouletteGame game = games.computeIfAbsent(player.getUniqueId(), k -> new RouletteGame(plugin, player));
                    game.placeBet(betType, currentBet);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(ChatColor.GREEN + "[ROULETTE] Bet placed: " + Gambling.getEconomy().format(currentBet) + " on " + betType);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(ChatColor.RED + "[ROULETTE] You don't have enough money to place that bet.");
                }
            }
        }
    }

    private void startSpin(Player player, RouletteGame game, RouletteGUI gui) {
        // Send to chat instead of hidden action bar
        player.sendMessage(ChatColor.GOLD + "[ROULETTE] Spinning the roulette wheel...");
        new BukkitRunnable() {
            private int ticks = 0;
            private final int totalTicks = 60; // 3 seconds of spinning
            private final int[] border = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53, 52, 51, 50, 49, 48, 47, 46, 37, 28, 19, 10};
            private final Material[] spinMaterials = {Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK, Material.GOLD_BLOCK};

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    this.cancel();
                    game.spin();
                    int winningNumber = game.getWinningNumber();

                    // Show winning number as title
                    String color = "";
                    if (winningNumber == 0) {
                        color = ChatColor.GREEN + "";
                    } else if (game.isRed(winningNumber)) {
                        color = ChatColor.RED + "";
                    } else {
                        color = ChatColor.BLACK + "";
                    }
                    player.sendTitle(color + ChatColor.BOLD + "WINNING NUMBER", color + ChatColor.BOLD + "" + winningNumber, 10, 60, 20);
                    // Also send to chat
                    player.sendMessage(ChatColor.YELLOW + "[ROULETTE] " + color + "Winning number: " + winningNumber);

                    // Payout logic
                    double totalPayout = 0;
                    for (Map.Entry<String, Double> entry : game.getBets().entrySet()) {
                        String betType = entry.getKey();
                        double betAmount = entry.getValue();
                        double payout = 0;

                        if (betType.equals(String.valueOf(winningNumber))) {
                            payout = betAmount * 36;
                        } else if (betType.equalsIgnoreCase("Black") && game.isBlack(winningNumber)) {
                            payout = betAmount * 2;
                        } else if (betType.equalsIgnoreCase("Red") && game.isRed(winningNumber)) {
                            payout = betAmount * 2;
                        }

                        if (payout > 0) {
                            totalPayout += payout;
                            Gambling.getEconomy().depositPlayer(player, payout);
                        }
                    }

                    // Make variables final for inner class
                    final double finalTotalPayout = totalPayout;
                    final double totalLoss = game.getBets().values().stream().mapToDouble(Double::doubleValue).sum();

                    // Show win/loss result after a delay
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Close GUI first so messages are visible
                            player.closeInventory();

                            if (finalTotalPayout > 0) {
                                player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "YOU WON!",
                                               ChatColor.GOLD + "+" + Gambling.getEconomy().format(finalTotalPayout), 10, 60, 20);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                                // Also send to chat
                                player.sendMessage(ChatColor.GREEN + "[ROULETTE] " + ChatColor.BOLD + "YOU WON! " +
                                                 ChatColor.GOLD + "+" + Gambling.getEconomy().format(finalTotalPayout));
                                Gambling.getLeaderboard().addWin(player.getUniqueId(), finalTotalPayout);
                            } else {
                                player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "YOU LOST",
                                               ChatColor.GRAY + "Better luck next time!", 10, 60, 20);
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                                // Also send to chat
                                player.sendMessage(ChatColor.RED + "[ROULETTE] " + ChatColor.BOLD + "YOU LOST! " +
                                                 ChatColor.GRAY + "Better luck next time!");
                                Gambling.getLeaderboard().addLoss(player.getUniqueId(), totalLoss);
                            }
                        }
                    }.runTaskLater(plugin, 80L); // 4 seconds delay

                    gui.showResult(winningNumber);
                    return;
                }

                int index = ticks % border.length;
                for (int i = 0; i < border.length; i++) {
                    ItemStack item = gui.getInventory().getItem(border[i]);
                    if (item != null) {
                        ItemStack newItem = item.clone(); // Clone the item to avoid modifying the original
                        if (i == index) {
                            newItem.setType(spinMaterials[ticks % spinMaterials.length]);
                        } else {
                            // Get the original color of the number
                            try {
                                String cleanName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                                int number = Integer.parseInt(cleanName);
                                if (number == 0) {
                                    newItem.setType(Material.GREEN_CONCRETE);
                                } else if (game.isRed(number)) {
                                    newItem.setType(Material.RED_CONCRETE);
                                } else {
                                    newItem.setType(Material.BLACK_CONCRETE);
                                }
                            } catch (NumberFormatException e) {
                                // If we can't parse the number, just keep the original material
                                // This happens for non-number slots
                            }
                        }
                        gui.getInventory().setItem(border[i], newItem); // Set the modified item back into the inventory
                    }
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.5f);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }


}
