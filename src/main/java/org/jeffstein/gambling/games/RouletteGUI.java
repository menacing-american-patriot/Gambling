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

public class RouletteGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public RouletteGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.DARK_RED + "Roulette");
        initializeItems();
    }

    private void initializeItems() {
        // Create the circle of numbers
        int[] border = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53, 52, 51, 50, 49, 48, 47, 46, 37, 28, 19, 10};
        int[] numbers = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};

        for (int i = 0; i < numbers.length; i++) {
            Material material = (numbers[i] != 0 && numbers[i] % 2 == 0)? Material.RED_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
            if (numbers[i] == 0) material = Material.GREEN_STAINED_GLASS_PANE;
            if(i < border.length)
                gui.setItem(border[i], createGuiItem(material, String.valueOf(numbers[i])));
        }

        gui.setItem(22, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Spin"));
    }

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

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }
}
