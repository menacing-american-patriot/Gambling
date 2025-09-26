package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BlackjackBettingGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private double currentBet = 100.0;

    public BlackjackBettingGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 27, ChatColor.DARK_GREEN + "Blackjack - Place Bet");
        initializeItems();
    }

    private void initializeItems() {
        // Bet controls
        gui.setItem(10, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100"));
        gui.setItem(11, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-50"));
        gui.setItem(12, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-10"));
        
        gui.setItem(13, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet", 
            ChatColor.GRAY + "Amount: " + ChatColor.WHITE + Gambling.getEconomy().format(currentBet)));
        
        gui.setItem(14, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+10"));
        gui.setItem(15, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+50"));
        gui.setItem(16, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100"));

        // Start game button
        gui.setItem(22, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "START GAME"));
        
        // Back button
        gui.setItem(18, createGuiItem(Material.OAK_DOOR, ChatColor.YELLOW + "Back"));
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

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }

    public double getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(double currentBet) {
        this.currentBet = Math.max(10, currentBet); // Minimum bet of 10
        gui.setItem(13, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet", 
            ChatColor.GRAY + "Amount: " + ChatColor.WHITE + Gambling.getEconomy().format(this.currentBet)));
    }
}
