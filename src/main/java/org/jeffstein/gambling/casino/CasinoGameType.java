package org.jeffstein.gambling.casino;

import org.bukkit.Material;
import org.bukkit.ChatColor;

/**
 * Enum representing different types of casino games available as physical blocks
 */
public enum CasinoGameType {
    
    BLACKJACK("Blackjack", "♠", ChatColor.RED, Material.ENCHANTING_TABLE,
             "Right-click to play! Use buttons around the table for Hit/Stand"),
    
    ROULETTE("Roulette", "🎯", ChatColor.RED, Material.CAULDRON,
            "Right-click to place bets! Spin the wheel and win big!"),
    
    SLOTS("Slots", "🎰", ChatColor.GOLD, Material.DISPENSER,
         "Right-click to spin! Pull the lever for your chance to win!"),
    
    POKER("Poker", "🃏", ChatColor.DARK_GREEN, Material.CRAFTING_TABLE,
         "Right-click to join the table! Play Texas Hold'em with others"),
    
    BACCARAT("Baccarat", "💎", ChatColor.BLUE, Material.CARTOGRAPHY_TABLE,
            "Right-click to play! Bet on Player, Banker, or Tie"),
    
    CRAPS("Craps", "🎲", ChatColor.YELLOW, Material.SMOOTH_STONE_SLAB,
         "Right-click to roll! Place your bets on the dice"),
    
    KENO("Keno", "🔢", ChatColor.LIGHT_PURPLE, Material.LECTERN,
        "Right-click to play! Pick your lucky numbers"),
    
    CRASH("Crash", "🚀", ChatColor.RED, Material.REDSTONE_BLOCK,
         "Right-click to bet! Cash out before the crash!"),
    
    PLINKO("Plinko", "⚪", ChatColor.AQUA, Material.HOPPER,
          "Right-click to drop! Watch the ball bounce to victory"),
    
    WHEEL_OF_FORTUNE("Wheel of Fortune", "🎡", ChatColor.GOLD, Material.COMPASS,
                    "Right-click to spin! Bet on segments for big wins"),
    
    MINES("Mines", "💣", ChatColor.DARK_RED, Material.TNT,
         "Right-click to play! Find gems while avoiding mines"),
    
    COINFLIP("Coinflip", "🪙", ChatColor.YELLOW, Material.GOLD_INGOT,
            "Right-click to flip! Simple heads or tails betting");
    
    private final String displayName;
    private final String icon;
    private final ChatColor color;
    private final Material defaultBlock;
    private final String description;
    
    CasinoGameType(String displayName, String icon, ChatColor color, 
                   Material defaultBlock, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
        this.defaultBlock = defaultBlock;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public ChatColor getColor() { return color; }
    public Material getDefaultBlock() { return defaultBlock; }
    public String getDescription() { return description; }
    
    public String getFormattedName() {
        return color + "" + ChatColor.BOLD + icon + " " + displayName.toUpperCase() + " " + icon;
    }
    
    public String getFormattedDescription() {
        return ChatColor.GRAY + description;
    }
    
    /**
     * Get the command that should be executed when this game type is triggered
     */
    public String getCommand() {
        switch (this) {
            case BLACKJACK: return "blackjack"; // Use argument-driven blackjack, not GUI
            case ROULETTE: return "roulette";
            case SLOTS: return "slots";
            case POKER: return "poker join";
            case BACCARAT: return "baccarat";
            case CRAPS: return "craps";
            case KENO: return "keno";
            case CRASH: return "crash";
            case PLINKO: return "plinko";
            case WHEEL_OF_FORTUNE: return "wheel";
            case MINES: return "mines";
            case COINFLIP: return "coinflip 100"; // Default amount
            default: return "casino";
        }
    }
    
    /**
     * Check if this game type supports physical interactions (buttons, levers, etc.)
     */
    public boolean supportsPhysicalInteractions() {
        switch (this) {
            case BLACKJACK:
            case ROULETTE:
            case CRAPS:
            case CRASH:
            case PLINKO:
            case MINES:
                return true;
            default:
                return false;
        }
    }
    
    public static CasinoGameType fromString(String name) {
        for (CasinoGameType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
