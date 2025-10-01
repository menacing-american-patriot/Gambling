package org.jeffstein.gambling.listeners;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.gui.CasinoLobbyGUI;
import org.jeffstein.gambling.games.*;
import org.jeffstein.gambling.commands.CrashCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CasinoLobbyListener implements Listener {

    private final Gambling plugin;

    public CasinoLobbyListener(Gambling plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CasinoLobbyGUI) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            CasinoLobbyGUI lobbyGUI = (CasinoLobbyGUI) holder;
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta().getDisplayName();
            int slot = event.getSlot();

            // Handle game selections
            switch (slot) {
                case 10: // Blackjack
                    if (displayName.contains("BLACKJACK")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("blackjack")) {
                            player.sendMessage(ChatColor.RED + "Blackjack is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Blackjack...");
                        player.performCommand("blackjackgui");
                    }
                    break;

                case 11: // Roulette
                    if (displayName.contains("ROULETTE")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("roulette")) {
                            player.sendMessage(ChatColor.RED + "Roulette is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Roulette...");
                        player.performCommand("roulette");
                    }
                    break;

                case 12: // Baccarat
                    if (displayName.contains("BACCARAT")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("baccarat")) {
                            player.sendMessage(ChatColor.RED + "Baccarat is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Baccarat...");
                        player.performCommand("baccarat");
                    }
                    break;

                case 13: // Slots
                    if (displayName.contains("SLOTS")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("slots")) {
                            player.sendMessage(ChatColor.RED + "Slots is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Slots...");
                        player.performCommand("slots");
                    }
                    break;

                case 14: // Craps
                    if (displayName.contains("CRAPS")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("craps")) {
                            player.sendMessage(ChatColor.RED + "Craps is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Craps...");
                        player.performCommand("craps");
                    }
                    break;

                case 15: // Keno
                    if (displayName.contains("KENO")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("keno")) {
                            player.sendMessage(ChatColor.RED + "Keno is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Keno...");
                        player.performCommand("keno");
                    }
                    break;

                case 16: // Poker
                    if (displayName.contains("POKER")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("poker")) {
                            player.sendMessage(ChatColor.RED + "Poker is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendMessage(ChatColor.GREEN + "[POKER] Joining poker table...");
                        player.performCommand("poker join");
                    }
                    break;

                case 19: // Crash
                    if (displayName.contains("CRASH")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("crash")) {
                            player.sendMessage(ChatColor.RED + "Crash is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Crash...");
                        player.performCommand("crash");
                    }
                    break;

                case 20: // Plinko
                    if (displayName.contains("PLINKO")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("plinko")) {
                            player.sendMessage(ChatColor.RED + "Plinko is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Plinko...");
                        player.performCommand("plinko");
                    }
                    break;

                case 21: // Wheel of Fortune
                    if (displayName.contains("WHEEL OF FORTUNE")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("wheel")) {
                            player.sendMessage(ChatColor.RED + "Wheel of Fortune is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Wheel of Fortune...");
                        player.performCommand("wheel");
                    }
                    break;

                case 22: // Mines
                    if (displayName.contains("MINES")) {
                        if (!org.jeffstein.gambling.Gambling.getGamblingConfig().isGameEnabled("mines")) {
                            player.sendMessage(ChatColor.RED + "Mines is disabled by admin.");
                            break;
                        }
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Mines...");
                        player.performCommand("mines");
                    }
                    break;

                case 37: // Leaderboard
                    if (displayName.contains("LEADERBOARD")) {
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Opening Leaderboard...");
                        player.performCommand("leaderboard");
                    }
                    break;

                case 38: // Jackpot
                    if (displayName.contains("JACKPOT")) {
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Checking Jackpot...");
                        player.performCommand("jackpot");
                    }
                    break;

                case 39: // Daily Reward
                    if (displayName.contains("DAILY REWARD")) {
                        player.closeInventory();
                        player.sendActionBar(ChatColor.GREEN + "Claiming Daily Reward...");
                        player.performCommand("daily");
                    }
                    break;

                case 40: // Coinflip
                    if (displayName.contains("COINFLIP")) {
                        player.closeInventory();
                        player.sendActionBar(ChatColor.YELLOW + "Use /coinflip <amount> to play!");
                    }
                    break;

                case 49: // Balance display
                    if (displayName.contains("YOUR BALANCE")) {
                        // Refresh the GUI to update balance
                        CasinoLobbyGUI newGUI = new CasinoLobbyGUI(plugin, player);
                        newGUI.openInventory();
                        player.sendActionBar(ChatColor.GREEN + "Balance refreshed!");
                    }
                    break;

                default:
                    // Do nothing for other slots
                    break;
            }
        }
    }
}
