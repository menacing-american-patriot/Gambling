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

public class RouletteBettingGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public RouletteBettingGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.DARK_RED + "Roulette - Place Bets");
        initializeItems();
    }

    private void initializeItems() {
        // Add betting options
        gui.setItem(10, createGuiItem(Material.BLACK_WOOL, "Black"));
        gui.setItem(11, createGuiItem(Material.RED_WOOL, "Red"));
        gui.setItem(12, createGuiItem(Material.GREEN_WOOL, "0"));

        for (int i = 1; i <= 36; i++) {
            gui.setItem(i + 17, createGuiItem(Material.PAPER, String.valueOf(i)));
        }
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
