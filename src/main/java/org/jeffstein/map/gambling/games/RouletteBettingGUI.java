package org.jeffstein.map.gambling.games;

import org.jeffstein.map.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RouletteBettingGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private double currentBet = 100.0;

    public RouletteBettingGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.DARK_RED + "Roulette - Place Bets");
        initializeItems();
    }

    private void initializeItems() {
        // Add betting options
        gui.setItem(10, createGuiItem(Material.BLACK_CONCRETE, "Black"));
        gui.setItem(11, createGuiItem(Material.RED_CONCRETE, "Red"));
        gui.setItem(12, createGuiItem(Material.GREEN_CONCRETE, "0"));

        // Numbers 1-36
        for (int i = 1; i <= 36; i++) {
            gui.setItem(i + 17, createGuiItem(Material.PAPER, String.valueOf(i)));
        }

        // Bet controls
        gui.setItem(48, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "-100"));
        gui.setItem(49, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Current Bet", ChatColor.GRAY + "Amount: " + ChatColor.WHITE + Gambling.getEconomy().format(currentBet)));
        gui.setItem(50, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "+100"));

        // Spin button
        gui.setItem(53, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "" + ChatColor.BOLD + "SPIN"));
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
