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

public class CrapsBettingGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private double currentBet = 100.0;

    public CrapsBettingGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Craps - Place Bets");
        initializeItems();
    }

    private void initializeItems() {
        // Add betting options
        gui.setItem(10, createGuiItem(Material.LIME_CONCRETE, "Pass Line"));
        gui.setItem(11, createGuiItem(Material.RED_CONCRETE, "Don't Pass Line"));

        // Add bet controls
        gui.setItem(48, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100"));
        gui.setItem(49, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet", ChatColor.GRAY + "Amount: " + ChatColor.WHITE + Gambling.getEconomy().format(currentBet)));
        gui.setItem(50, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100"));

        gui.setItem(53, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "" + ChatColor.BOLD + "ROLL"));
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
        this.currentBet = currentBet;
        gui.setItem(49, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet", ChatColor.GRAY + "Amount: " + ChatColor.WHITE + Gambling.getEconomy().format(currentBet)));
    }
}
