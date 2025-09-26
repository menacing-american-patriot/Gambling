package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SlotMachine implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final Economy economy = Gambling.getEconomy();
    private final double spinCost = 100.0; // Cost per spin

    // The items that will appear on the reels
    private final List<Material> reelItems = Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT,
            Material.IRON_INGOT, Material.LAPIS_LAZULI, Material.COAL
    );

    public SlotMachine(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        // Create a 27-slot inventory for the GUI [7, 8]
        this.gui = Bukkit.createInventory(this, 27, ChatColor.GOLD + "Slot Machine");
        initializeItems();
    }

    private void initializeItems() {
        // Fill the background with glass panes
        ItemStack background = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, background);
        }

        // Create the "Spin" button
        gui.setItem(22, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Spin!",
                ChatColor.GRAY + "Cost: " + economy.format(spinCost)));

        // Set initial reel items
        gui.setItem(12, new ItemStack(Material.DIAMOND));
        gui.setItem(13, new ItemStack(Material.EMERALD));
        gui.setItem(14, new ItemStack(Material.GOLD_INGOT));
    }

    // Helper method to create an ItemStack for the GUI
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta!= null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void spin() {
        // Check balance and withdraw before spinning
        if (economy.getBalance(player) < spinCost) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to spin!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        economy.withdrawPlayer(player, spinCost);
        player.sendMessage(ChatColor.RED + "-" + economy.format(spinCost));

        // Start the animation using the Bukkit Scheduler
        new BukkitRunnable() {
            private int ticks = 0;
            private final int totalTicks = 40; // 2 seconds of spinning (20 ticks/sec)

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    this.cancel(); // Stop the animation
                    calculateWinnings();
                    return;
                }

                // Update the reel items to simulate spinning
                for (int slot : new int{12, 13, 14}) {
                    gui.setItem(slot, new ItemStack(reelItems.get(ThreadLocalRandom.current().nextInt(reelItems.size()))));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.5f);
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L); // Run this task every 2 ticks
    }

    private void calculateWinnings() {
        ItemStack reel1 = gui.getItem(12);
        ItemStack reel2 = gui.getItem(13);
        ItemStack reel3 = gui.getItem(14);

        if (reel1 == null |

                | reel2 == null |
                | reel3 == null) return;

        // Check for a win (all three items are the same)
        if (reel1.getType() == reel2.getType() && reel2.getType() == reel3.getType()) {
            double multiplier = getMultiplier(reel1.getType());
            double winnings = spinCost * multiplier;
            economy.depositPlayer(player, winnings);

            player.sendMessage(ChatColor.GREEN + "Jackpot! You won " + economy.format(winnings) + "!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(ChatColor.RED + "You lost. Better luck next time!");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
    }

    private double getMultiplier(Material material) {
        switch (material) {
            case DIAMOND:
                return 100.0; // 100x payout for diamonds [9]
            case EMERALD:
                return 50.0;
            case GOLD_INGOT:
                return 25.0;
            case IRON_INGOT:
                return 10.0;
            case LAPIS_LAZULI:
                return 5.0;
            case COAL:
                return 2.0;
            default:
                return 0.0;
        }
    }

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }
}