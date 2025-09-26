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
                if (game == null || game.getBets().isEmpty()) {
                    player.sendMessage("Please place a bet first.");
                    return;
                }
                startSpin(player, game, rouletteGUI);
            } else if (displayName.equals(ChatColor.GREEN + "Spin Again")) {
                RouletteGame oldGame = games.get(player.getUniqueId());
                if (oldGame == null || oldGame.getBets().isEmpty()) {
                    player.sendMessage("No previous bet found.");
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
            } else if (displayName.equals(ChatColor.GREEN + "Spin")) {
                RouletteGame game = games.get(player.getUniqueId());
                if (game == null || game.getBets().isEmpty()) {
                    player.sendMessage("Please place a bet first.");
                    return;
                }
                double totalBet = game.getBets().values().stream().mapToDouble(Double::doubleValue).sum();
                RouletteGUI newRouletteGUI = new RouletteGUI(plugin, player, totalBet);
                newRouletteGUI.openInventory();
            } else if (displayName.equals(ChatColor.GOLD + "Current Bet")) {
                // Do nothing
            } else {
                String betType = displayName;
                RouletteGame game = games.computeIfAbsent(player.getUniqueId(), k -> new RouletteGame(plugin, player));
                game.placeBet(betType, currentBet);
                player.sendMessage("You placed a bet of " + Gambling.getEconomy().format(currentBet) + " on " + betType);
            }
        }
    }

    private void startSpin(Player player, RouletteGame game, RouletteGUI gui) {
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

                    gui.showResult(winningNumber);
                    return;
                }

                int index = ticks % border.length;
                for (int i = 0; i < border.length; i++) {
                    ItemStack item = gui.getInventory().getItem(border[i]);
                    if (item != null) {
                        if (i == index) {
                            item.setType(spinMaterials[ticks % spinMaterials.length]);
                        } else {
                            item.setType((i % 2 == 0) ? Material.RED_CONCRETE : Material.BLACK_CONCRETE);
                        }
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
