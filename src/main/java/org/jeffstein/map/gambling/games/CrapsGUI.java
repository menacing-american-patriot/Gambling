package org.jeffstein.map.gambling.games;

import org.jeffstein.map.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class CrapsGUI implements InventoryHolder {

    private static final int STATUS_SLOT = 4;
    private static final int POINT_SLOT = 13;
    private static final int DIE_ONE_SLOT = 20;
    private static final int TOTAL_SLOT = 22;
    private static final int DIE_TWO_SLOT = 24;
    private static final int PASS_LINE_SLOT = 30;
    private static final int DONT_PASS_SLOT = 32;
    private static final int CURRENT_BET_SLOT = 39;
    private static final int ACTION_SLOT = 44;
    private static final int SUMMARY_SLOT = 49;
    private static final int[] HISTORY_SLOTS = {0, 1, 2, 3, 4, 5};

    private final Player player;
    private final Inventory gui;
    private double currentBet = 100.0;

    public CrapsGUI(Player player) {
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.DARK_GREEN + "Craps Table");
        initializeLayout();
    }

    private void initializeLayout() {
        fillBackground();

        gui.setItem(STATUS_SLOT, createGuiItem(Material.EMERALD,
            ChatColor.GREEN + "" + ChatColor.BOLD + "WELCOME TO CRAPS",
            ChatColor.GRAY + "Place a Pass or Don't Pass bet",
            ChatColor.YELLOW + "Click the dice to roll"));

        gui.setItem(POINT_SLOT, createGuiItem(Material.YELLOW_CONCRETE,
            ChatColor.GOLD + "Come Out Roll",
            ChatColor.GRAY + "No point established"));

        gui.setItem(DIE_ONE_SLOT, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "Die One",
            ChatColor.DARK_GRAY + "Waiting for roll"));

        gui.setItem(TOTAL_SLOT, createGuiItem(Material.GOLD_BLOCK,
            ChatColor.GOLD + "Total",
            ChatColor.DARK_GRAY + "Roll to begin"));

        gui.setItem(DIE_TWO_SLOT, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "Die Two",
            ChatColor.DARK_GRAY + "Waiting for roll"));

        gui.setItem(PASS_LINE_SLOT, createGuiItem(Material.LIME_CONCRETE,
            ChatColor.GREEN + "Pass Line",
            ChatColor.GRAY + "Pays 1:1 on 7 or 11",
            ChatColor.GRAY + "Loses on 2, 3, 12"));

        gui.setItem(DONT_PASS_SLOT, createGuiItem(Material.RED_CONCRETE,
            ChatColor.RED + "Don't Pass",
            ChatColor.GRAY + "Pays 1:1 on 2, 3, 12 push",
            ChatColor.GRAY + "Loses on 7 or 11"));

        gui.setItem(SUMMARY_SLOT, createGuiItem(Material.MAP,
            ChatColor.GOLD + "Last Outcome",
            ChatColor.GRAY + "No rolls yet"));

        gui.setItem(37, createGuiItem(Material.RED_STAINED_GLASS_PANE,
            ChatColor.RED + "-100", ChatColor.GRAY + "Decrease bet"));
        gui.setItem(38, createGuiItem(Material.RED_STAINED_GLASS_PANE,
            ChatColor.RED + "-50", ChatColor.GRAY + "Decrease bet"));

        gui.setItem(CURRENT_BET_SLOT, createGuiItem(Material.GOLD_INGOT,
            ChatColor.GOLD + "Current Bet",
            ChatColor.WHITE + Gambling.getEconomy().format(currentBet)));

        gui.setItem(42, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "+50", ChatColor.GRAY + "Increase bet"));
        gui.setItem(43, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
            ChatColor.GREEN + "+100", ChatColor.GRAY + "Increase bet"));

        gui.setItem(ACTION_SLOT, createGuiItem(Material.DIAMOND_BLOCK,
            ChatColor.AQUA + "" + ChatColor.BOLD + "ROLL DICE",
            ChatColor.GRAY + "Requires an active bet"));

        gui.setItem(45, createGuiItem(Material.OAK_DOOR,
            ChatColor.YELLOW + "Leave Table",
            ChatColor.GRAY + "Close the Craps menu"));

        for (int slot : HISTORY_SLOTS) {
            gui.setItem(slot, createGuiItem(Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.GRAY + "No history",
                ChatColor.DARK_GRAY + "Roll to fill"));
        }
    }

    private void fillBackground() {
        ItemStack background = createGuiItem(Material.GREEN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, background);
        }
    }

    public void setStatus(String title, String... description) {
        gui.setItem(STATUS_SLOT, createGuiItem(Material.EMERALD,
            ChatColor.GREEN + "" + ChatColor.BOLD + title,
            description));
    }

    public void setPointDisplay(CrapsGame.GameState state, int point) {
        if (state == CrapsGame.GameState.COME_OUT) {
            gui.setItem(POINT_SLOT, createGuiItem(Material.YELLOW_CONCRETE,
                ChatColor.GOLD + "Come Out Roll",
                ChatColor.GRAY + "No point established"));
        } else {
            gui.setItem(POINT_SLOT, createGuiItem(Material.ORANGE_CONCRETE,
                ChatColor.GOLD + "Point Phase",
                ChatColor.WHITE + "Point: " + point,
                ChatColor.GRAY + "Pass needs " + point + ", 7 loses"));
        }
    }

    public void setDiceDisplay(int dieOne, int dieTwo) {
        int total = dieOne + dieTwo;

        gui.setItem(DIE_ONE_SLOT, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "Die One",
            ChatColor.GOLD + "Rolled: " + dieOne));

        gui.setItem(DIE_TWO_SLOT, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "Die Two",
            ChatColor.GOLD + "Rolled: " + dieTwo));

        gui.setItem(TOTAL_SLOT, createGuiItem(Material.GOLD_BLOCK,
            ChatColor.GOLD + "Total",
            ChatColor.WHITE + "Sum: " + total));
    }

    public void showRollingFrame(int dieOne, int dieTwo) {
        gui.setItem(DIE_ONE_SLOT, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "Die One",
            ChatColor.YELLOW + "Rolling... " + dieOne));
        gui.setItem(DIE_TWO_SLOT, createGuiItem(Material.WHITE_WOOL,
            ChatColor.WHITE + "Die Two",
            ChatColor.YELLOW + "Rolling... " + dieTwo));
        gui.setItem(TOTAL_SLOT, createGuiItem(Material.GOLD_BLOCK,
            ChatColor.GOLD + "Total",
            ChatColor.YELLOW + "Rolling..."));
    }

    public void highlightBets(double passAmount, double dontPassAmount) {
        updateBetTile(PASS_LINE_SLOT, passAmount, Material.LIME_CONCRETE, ChatColor.GREEN + "Pass Line");
        updateBetTile(DONT_PASS_SLOT, dontPassAmount, Material.RED_CONCRETE, ChatColor.RED + "Don't Pass");
    }

    private void updateBetTile(int slot, double amount, Material material, String title) {
        ItemStack item = createGuiItem(material,
            title,
            amount > 0 ? ChatColor.WHITE + "Bet: " + Gambling.getEconomy().format(amount)
                       : ChatColor.GRAY + "Click to bet " + Gambling.getEconomy().format(currentBet));

        if (amount > 0) {
            item.addUnsafeEnchantment(Enchantment.INFINITY, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        gui.setItem(slot, item);
    }

    public void setActionReady(boolean enabled, boolean hasBets) {
        if (!enabled) {
            gui.setItem(ACTION_SLOT, createGuiItem(Material.REDSTONE_BLOCK,
                ChatColor.RED + "Waiting",
                ChatColor.GRAY + "Finish animation"));
            return;
        }

        if (!hasBets) {
            gui.setItem(ACTION_SLOT, createGuiItem(Material.BARRIER,
                ChatColor.RED + "Place a Bet",
                ChatColor.GRAY + "You need Pass or Don't Pass"));
        } else {
            gui.setItem(ACTION_SLOT, createGuiItem(Material.DIAMOND_BLOCK,
                ChatColor.AQUA + "" + ChatColor.BOLD + "ROLL DICE",
                ChatColor.GREEN + "Click to shoot!"));
        }
    }

    public void setHistory(List<Integer> totals) {
        for (int i = 0; i < HISTORY_SLOTS.length; i++) {
            if (i < totals.size()) {
                int total = totals.get(i);
                gui.setItem(HISTORY_SLOTS[i], createGuiItem(Material.PAPER,
                    ChatColor.YELLOW + "Roll: " + total));
            } else {
                gui.setItem(HISTORY_SLOTS[i], createGuiItem(Material.GRAY_STAINED_GLASS_PANE,
                    ChatColor.GRAY + "No history"));
            }
        }
    }

    public void setSummary(String title, String... lines) {
        gui.setItem(SUMMARY_SLOT, createGuiItem(Material.MAP,
            ChatColor.GOLD + title,
            lines));
    }

    public void adjustBet(double amount) {
        currentBet = Math.max(10.0, Math.min(10000.0, currentBet + amount));
        gui.setItem(CURRENT_BET_SLOT, createGuiItem(Material.GOLD_INGOT,
            ChatColor.GOLD + "Current Bet",
            ChatColor.WHITE + Gambling.getEconomy().format(currentBet)));
    }

    public double getCurrentBet() {
        return currentBet;
    }

    public void openInventory() {
        player.openInventory(gui);
    }

    @Override
    public Inventory getInventory() {
        return gui;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
