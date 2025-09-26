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
import java.util.Map;

public class CrapsGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public CrapsGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Craps");
        initializeItems();
    }

    private void initializeItems() {
        // Point indicator
        gui.setItem(4, createGuiItem(Material.WHITE_STAINED_GLASS_PANE, ChatColor.WHITE + "Point: OFF"));

        // Roll button
        gui.setItem(49, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "" + ChatColor.BOLD + "ROLL"));
    }

    public void update(CrapsGame game) {
        // Update point
        if (game.getGameState() == CrapsGame.GameState.POINT) {
            gui.setItem(4, createGuiItem(Material.WHITE_STAINED_GLASS_PANE, ChatColor.WHITE + "Point: " + game.getPoint()));
        } else {
            gui.setItem(4, createGuiItem(Material.WHITE_STAINED_GLASS_PANE, ChatColor.WHITE + "Point: OFF"));
        }

        // Update bets
        for (Map.Entry<String, Double> entry : game.getBets().entrySet()) {
            String betType = entry.getKey();
            double betAmount = entry.getValue();
            // This is a simple implementation. A more advanced implementation would have specific slots for each bet type.
            if (betType.equals("Pass Line")) {
                gui.setItem(10, createGuiItem(Material.LIME_CONCRETE, "Pass Line", ChatColor.GRAY + "Bet: " + ChatColor.WHITE + Gambling.getEconomy().format(betAmount)));
            } else if (betType.equals("Don't Pass Line")) {
                gui.setItem(11, createGuiItem(Material.RED_CONCRETE, "Don't Pass Line", ChatColor.GRAY + "Bet: " + ChatColor.WHITE + Gambling.getEconomy().format(betAmount)));
            }
        }
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
}
