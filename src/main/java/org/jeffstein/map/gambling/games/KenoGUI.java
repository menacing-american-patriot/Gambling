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

import java.util.ArrayList;
import java.util.List;

public class KenoGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;
    private final List<Integer> selectedNumbers = new ArrayList<>();

    public KenoGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.GOLD + "Keno");
        initializeItems();
    }

    private void initializeItems() {
        // Numbers 1-80
        for (int i = 1; i <= 80; i++) {
            // This will need more pages, for now just 1-45
            if (i > 45) break;
            gui.setItem(i - 1, createGuiItem(Material.PAPER, String.valueOf(i)));
        }

        gui.setItem(48, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Start Game"));
        gui.setItem(49, createGuiItem(Material.BARRIER, ChatColor.RED + "Clear Selection"));
        gui.setItem(50, createGuiItem(Material.GOLD_NUGGET, ChatColor.GOLD + "Bet 100"));
    }

    public List<Integer> getSelectedNumbers() {
        return selectedNumbers;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>(List.of(lore));
            meta.setLore(loreList);
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
