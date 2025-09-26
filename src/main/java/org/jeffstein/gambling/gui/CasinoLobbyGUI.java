package org.jeffstein.gambling.gui;

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

public class CasinoLobbyGUI implements InventoryHolder {

    private final Gambling plugin;
    private final Player player;
    private final Inventory gui;

    public CasinoLobbyGUI(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gui = Bukkit.createInventory(this, 54, ChatColor.GOLD + "" + ChatColor.BOLD + "🎰 CASINO LOBBY 🎰");
        initializeLobby();
    }

    private void initializeLobby() {
        // Fill background with casino-themed items
        ItemStack background = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, background);
        }

        // Create decorative border
        ItemStack border = createGuiItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "Casino Border");
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border); // Top row
            gui.setItem(45 + i, border); // Bottom row
        }
        for (int i = 0; i < 6; i++) {
            gui.setItem(i * 9, border); // Left column
            gui.setItem(i * 9 + 8, border); // Right column
        }

        // Casino title
        gui.setItem(4, createGuiItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "WELCOME TO THE CASINO!",
                ChatColor.GRAY + "Choose your game below",
                ChatColor.YELLOW + "Good luck and have fun!"));

        // Classic Casino Games (Top Section)
        gui.setItem(10, createGuiItem(Material.PAPER, ChatColor.RED + "" + ChatColor.BOLD + "♠ BLACKJACK ♠",
                ChatColor.GRAY + "Beat the dealer to 21!",
                ChatColor.YELLOW + "Commands: /blackjack or /blackjackgui",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(11, createGuiItem(Material.REDSTONE_BLOCK, ChatColor.RED + "" + ChatColor.BOLD + "🎯 ROULETTE 🎯",
                ChatColor.GRAY + "Spin the wheel of fortune!",
                ChatColor.YELLOW + "Bet on numbers, colors, or odds",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(12, createGuiItem(Material.EMERALD, ChatColor.GREEN + "" + ChatColor.BOLD + "💎 BACCARAT 💎",
                ChatColor.GRAY + "Player vs Banker card game",
                ChatColor.YELLOW + "High-class casino experience",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(13, createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "" + ChatColor.BOLD + "🎰 SLOTS 🎰",
                ChatColor.GRAY + "Spin the reels for big wins!",
                ChatColor.YELLOW + "5x3 reels with multiple paylines",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(14, createGuiItem(Material.BONE, ChatColor.WHITE + "" + ChatColor.BOLD + "🎲 CRAPS 🎲",
                ChatColor.GRAY + "Roll the dice and win big!",
                ChatColor.YELLOW + "Classic casino dice game",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(15, createGuiItem(Material.PAPER, ChatColor.BLUE + "" + ChatColor.BOLD + "🔢 KENO 🔢",
                ChatColor.GRAY + "Pick numbers and hope they're drawn!",
                ChatColor.YELLOW + "Lottery-style number game",
                ChatColor.GREEN + "Click to play!"));

        // Modern Online Casino Games (Middle Section)
        gui.setItem(19, createGuiItem(Material.FIREWORK_ROCKET, ChatColor.RED + "" + ChatColor.BOLD + "🚀 CRASH 🚀",
                ChatColor.GRAY + "Cash out before the crash!",
                ChatColor.YELLOW + "Most popular online casino game",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(20, createGuiItem(Material.ENDER_PEARL, ChatColor.YELLOW + "" + ChatColor.BOLD + "🎯 PLINKO 🎯",
                ChatColor.GRAY + "Drop balls down the peg board!",
                ChatColor.YELLOW + "Watch the ball bounce to prizes",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(21, createGuiItem(Material.COMPASS, ChatColor.GOLD + "" + ChatColor.BOLD + "🎡 WHEEL OF FORTUNE 🎡",
                ChatColor.GRAY + "Spin the wheel for prizes!",
                ChatColor.YELLOW + "Bet on segments and spin",
                ChatColor.GREEN + "Click to play!"));

        gui.setItem(22, createGuiItem(Material.TNT, ChatColor.RED + "" + ChatColor.BOLD + "💣 MINES 💣",
                ChatColor.GRAY + "Find gems, avoid mines!",
                ChatColor.YELLOW + "Strategic risk vs reward game",
                ChatColor.GREEN + "Click to play!"));

        // Casino Information (Bottom Section)
        gui.setItem(37, createGuiItem(Material.BOOK, ChatColor.AQUA + "" + ChatColor.BOLD + "📊 LEADERBOARD 📊",
                ChatColor.GRAY + "View top players and stats",
                ChatColor.YELLOW + "See who's winning big!",
                ChatColor.GREEN + "Click to view!"));

        gui.setItem(38, createGuiItem(Material.DIAMOND, ChatColor.AQUA + "" + ChatColor.BOLD + "💰 JACKPOT 💰",
                ChatColor.GRAY + "Current jackpot: " + Gambling.getEconomy().format(Gambling.getJackpot().getJackpot()),
                ChatColor.YELLOW + "Win big in any game!",
                ChatColor.GREEN + "Click to check!"));

        gui.setItem(39, createGuiItem(Material.CLOCK, ChatColor.YELLOW + "" + ChatColor.BOLD + "🎁 DAILY REWARD 🎁",
                ChatColor.GRAY + "Claim your daily bonus!",
                ChatColor.YELLOW + "Free money every day",
                ChatColor.GREEN + "Click to claim!"));

        gui.setItem(40, createGuiItem(Material.GOLD_NUGGET, ChatColor.GOLD + "" + ChatColor.BOLD + "💰 COINFLIP 💰",
                ChatColor.GRAY + "Simple heads or tails bet",
                ChatColor.YELLOW + "Quick gambling for any amount",
                ChatColor.GREEN + "Use /coinflip <amount>"));

        // Player Info
        double balance = Gambling.getEconomy().getBalance(player);
        gui.setItem(49, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "YOUR BALANCE",
                ChatColor.WHITE + Gambling.getEconomy().format(balance),
                ChatColor.GRAY + "Your current money",
                balance < 100 ? ChatColor.RED + "Consider claiming daily reward!" : ChatColor.GREEN + "Ready to gamble!"));
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

    public Player getPlayer() {
        return player;
    }
}
