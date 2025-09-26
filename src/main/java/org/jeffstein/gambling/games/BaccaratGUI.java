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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.enchantments.Enchantment;
import java.util.Arrays;
import java.util.List;

public class BaccaratGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public BaccaratGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 27, ChatColor.RED + "Baccarat");
        initializeItems();
    }

    private void initializeItems() {
        gui.setItem(11, createGuiItem(Material.BLUE_WOOL, ChatColor.BLUE + "Player"));
        gui.setItem(13, createGuiItem(Material.WHITE_WOOL, ChatColor.WHITE + "Tie"));
        gui.setItem(15, createGuiItem(Material.RED_WOOL, ChatColor.RED + "Banker"));

        gui.setItem(19, createGuiItem(Material.GOLD_NUGGET, ChatColor.GOLD + "Bet 100"));
        gui.setItem(20, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Bet 500"));
        gui.setItem(21, createGuiItem(Material.GOLD_BLOCK, ChatColor.GOLD + "Bet 1000"));

        gui.setItem(22, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Deal"));
    }

    public void updateHands(List<Card> playerHand, List<Card> bankerHand) {
        for (int i = 0; i < playerHand.size(); i++) {
            gui.setItem(i, new ItemStack(playerHand.get(i).getMaterial()));
        }
        for (int i = 0; i < bankerHand.size(); i++) {
            gui.setItem(i + 18, new ItemStack(bankerHand.get(i).getMaterial()));
        }
    }

    public void showResult(String winner) {
        ItemStack resultItem;
        if (winner.equals("player")) {
            resultItem = createGuiItem(Material.BLUE_WOOL, ChatColor.BLUE + "Player Wins!");
        } else if (winner.equals("banker")) {
            resultItem = createGuiItem(Material.RED_WOOL, ChatColor.RED + "Banker Wins!");
        } else {
            resultItem = createGuiItem(Material.WHITE_WOOL, ChatColor.WHITE + "Tie!");
        }
        gui.setItem(4, resultItem);
        gui.setItem(22, createGuiItem(Material.ORANGE_STAINED_GLASS_PANE, ChatColor.GOLD + "New Game"));
    }

    public void setSelectedBet(String betType) {
        for (int i = 11; i <= 15; i+=2) {
            ItemStack item = gui.getItem(i);
            if (item != null) {
                item.removeEnchantment(Enchantment.INFINITY);
            }
        }

        if (betType.equals("player")) {
            gui.getItem(11).addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = gui.getItem(11).getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            gui.getItem(11).setItemMeta(meta);
        } else if (betType.equals("tie")) {
            gui.getItem(13).addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = gui.getItem(13).getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            gui.getItem(13).setItemMeta(meta);
        } else if (betType.equals("banker")) {
            gui.getItem(15).addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = gui.getItem(15).getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            gui.getItem(15).setItemMeta(meta);
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
