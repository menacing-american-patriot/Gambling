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
import java.util.List;

public class BlackjackGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public BlackjackGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 27, ChatColor.DARK_GREEN + "Blackjack");
        initializeItems();
    }

    private void initializeItems() {
        gui.setItem(11, createGuiItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Hit"));
        gui.setItem(15, createGuiItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Stand"));
    }

    public void updateHands(List<Card> playerHand, List<Card> dealerHand, boolean revealDealer) {
        for (int i = 0; i < playerHand.size(); i++) {
            gui.setItem(i, createCardItem(playerHand.get(i)));
        }

        if (revealDealer) {
            for (int i = 0; i < dealerHand.size(); i++) {
                gui.setItem(i + 18, createCardItem(dealerHand.get(i)));
            }
        } else {
            gui.setItem(18, createCardItem(dealerHand.get(0)));
            gui.setItem(19, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "[HIDDEN]"));
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

    private ItemStack createCardItem(Card card) {
        ItemStack item = new ItemStack(card.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(card.getRank() + " of " + card.getSuit());
        meta.setCustomModelData(card.getCustomModelData());
        item.setItemMeta(meta);
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
