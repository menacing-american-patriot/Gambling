package org.jeffstein.map.gambling.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.casino.CasinoBlock;
import org.jeffstein.map.gambling.casino.CasinoBlockManager;
import org.jeffstein.map.gambling.casino.CasinoGameType;
import org.jeffstein.map.gambling.casino.PhysicalGameInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for casino block management
 * Usage: /casinoblock <create|remove|list|info> [args...]
 */
public class CasinoBlockCommand implements CommandExecutor, TabCompleter {
    
    private final Gambling plugin;
    private final CasinoBlockManager blockManager;
    private final PhysicalGameInterface physicalInterface;
    
    public CasinoBlockCommand(Gambling plugin, CasinoBlockManager blockManager, PhysicalGameInterface physicalInterface) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        this.physicalInterface = physicalInterface;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("gambling.admin.casinoblock")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage casino blocks.");
            return true;
        }
        
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            case "give":
                return handleGive(player, args);
            case "remove":
                return handleRemove(player, args);
            case "list":
                return handleList(player, args);
            case "info":
                return handleInfo(player, args);
            case "reload":
                return handleReload(player);
            case "setup":
                return handleSetup(player, args);
            default:
                sendUsage(player);
                return true;
        }
    }
    
    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 6) {
            player.sendMessage(ChatColor.RED + "Usage: /casinoblock create <gameType> <name> <minBet> <maxBet> <blockType>");
            player.sendMessage(ChatColor.YELLOW + "Example: /casinoblock create blackjack \"VIP Blackjack\" 100 5000 enchanting_table");
            return true;
        }
        
        // Get the block the player is looking at
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be looking at a block to create a casino block.");
            return true;
        }
        
        Location location = targetBlock.getLocation();
        
        // Check if there's already a casino block at this location
        if (blockManager.isCasinoBlock(location)) {
            player.sendMessage(ChatColor.RED + "There is already a casino block at this location.");
            return true;
        }
        
        // Parse arguments
        String gameTypeStr = args[1];
        String name = args[2].replace("\"", "");
        double minBet, maxBet;
        Material blockType;
        
        try {
            minBet = Double.parseDouble(args[3]);
            maxBet = Double.parseDouble(args[4]);
            blockType = Material.valueOf(args[5].toUpperCase());
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid bet amounts. Please use numbers.");
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid block type: " + args[5]);
            return true;
        }
        
        CasinoGameType gameType = CasinoGameType.fromString(gameTypeStr);
        if (gameType == null) {
            player.sendMessage(ChatColor.RED + "Invalid game type: " + gameTypeStr);
            player.sendMessage(ChatColor.YELLOW + "Available types: " + 
                             Arrays.stream(CasinoGameType.values())
                                   .map(Enum::name)
                                   .collect(Collectors.joining(", ")));
            return true;
        }
        
        if (minBet <= 0 || maxBet <= 0 || minBet > maxBet) {
            player.sendMessage(ChatColor.RED + "Invalid bet amounts. Min bet must be positive and less than max bet.");
            return true;
        }
        
        // Create the casino block
        boolean success = blockManager.registerCasinoBlock(location, gameType, name, minBet, maxBet, blockType);
        
        if (success) {
            // Set the block type
            targetBlock.setType(blockType);
            
            player.sendMessage(ChatColor.GREEN + "✓ Casino block created successfully!");
            player.sendMessage(ChatColor.YELLOW + "Game: " + gameType.getFormattedName());
            player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + name);
            player.sendMessage(ChatColor.YELLOW + "Min Bet: " + ChatColor.GREEN + Gambling.getEconomy().format(minBet));
            player.sendMessage(ChatColor.YELLOW + "Max Bet: " + ChatColor.GREEN + Gambling.getEconomy().format(maxBet));
            player.sendMessage(ChatColor.YELLOW + "Block Type: " + ChatColor.WHITE + blockType.name());
            
            // Set up physical interactions and visual effects
            CasinoBlock casinoBlock = blockManager.getCasinoBlock(location);
            if (casinoBlock != null) {
                if (gameType.supportsPhysicalInteractions()) {
                    physicalInterface.setupInteractionsForBlock(casinoBlock);
                    player.sendMessage(ChatColor.AQUA + "Physical interactions have been set up around the block!");
                }

                // Add visual effects
                Gambling.getVisualEffects().createHologram(casinoBlock);
                Gambling.getVisualEffects().startParticleEffects(casinoBlock);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Visual effects have been activated!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to create casino block.");
        }
        
        return true;
    }
    
    private boolean handleRemove(Player player, String[] args) {
        // Support: /casinoblock remove [number]
        if (args.length >= 2) {
            try {
                int index = Integer.parseInt(args[1]);
                java.util.List<CasinoBlock> blocks = new java.util.ArrayList<>(blockManager.getAllCasinoBlocks());
                if (index < 1 || index > blocks.size()) {
                    player.sendMessage(ChatColor.RED + "Invalid number. Use /casinoblock list to see indexes.");
                    return true;
                }
                CasinoBlock target = blocks.get(index - 1);
                Location location = target.getLocation();
                // Clear physical interactions and visual effects
                if (target.getGameType().supportsPhysicalInteractions()) {
                    physicalInterface.clearInteractionsForBlock(target);
                }
                Gambling.getVisualEffects().removeHologram(location);
                Gambling.getVisualEffects().stopParticleEffects(location);
                boolean success = blockManager.removeCasinoBlock(location);
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "✓ Casino block removed: " + target.getName());
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to remove casino block.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Usage: /casinoblock remove [number]");
                return true;
            }
        }

        // Fallback: remove the block you are looking at
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be looking at a casino block to remove it.");
            return true;
        }

        Location location = targetBlock.getLocation();
        CasinoBlock casinoBlock = blockManager.getCasinoBlock(location);

        if (casinoBlock == null) {
            player.sendMessage(ChatColor.RED + "There is no casino block at this location.");
            return true;
        }

        // Clear physical interactions and visual effects
        physicalInterface.clearInteractionsForBlock(casinoBlock);
        Gambling.getVisualEffects().removeHologram(location);
        Gambling.getVisualEffects().stopParticleEffects(location);

        // Remove the casino block
        boolean success = blockManager.removeCasinoBlock(location);

        if (success) {
            player.sendMessage(ChatColor.GREEN + "✓ Casino block removed successfully!");
            player.sendMessage(ChatColor.YELLOW + "Removed: " + casinoBlock.getName() +
                             " (" + casinoBlock.getGameType().getDisplayName() + ")");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to remove casino block.");
        }

        return true;
    }
    
    private boolean handleList(Player player, String[] args) {
        List<CasinoBlock> blocks = new ArrayList<>(blockManager.getAllCasinoBlocks());
        
        if (blocks.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No casino blocks found.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "  Casino Blocks (" + blocks.size() + " total)");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        
        for (int i = 0; i < blocks.size(); i++) {
            CasinoBlock block = blocks.get(i);
            Location loc = block.getLocation();
            
            player.sendMessage(ChatColor.WHITE + "" + (i + 1) + ". " + ChatColor.AQUA + block.getName());
            player.sendMessage(ChatColor.GRAY + "   Type: " + block.getGameType().getFormattedName());
            player.sendMessage(ChatColor.GRAY + "   Location: " + ChatColor.WHITE + 
                             loc.getWorld().getName() + " " + loc.getBlockX() + ", " + 
                             loc.getBlockY() + ", " + loc.getBlockZ());
            player.sendMessage(ChatColor.GRAY + "   Bets: " + ChatColor.GREEN + 
                             Gambling.getEconomy().format(block.getMinBet()) + " - " + 
                             Gambling.getEconomy().format(block.getMaxBet()));
            
            if (i < blocks.size() - 1) {
                player.sendMessage("");
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        return true;
    }
    
    private boolean handleInfo(Player player, String[] args) {
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be looking at a casino block to get its info.");
            return true;
        }
        
        Location location = targetBlock.getLocation();
        CasinoBlock casinoBlock = blockManager.getCasinoBlock(location);
        
        if (casinoBlock == null) {
            player.sendMessage(ChatColor.RED + "There is no casino block at this location.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "  Casino Block Information");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage(ChatColor.WHITE + "Name: " + ChatColor.AQUA + casinoBlock.getName());
        player.sendMessage(ChatColor.WHITE + "Game Type: " + casinoBlock.getGameType().getFormattedName());
        player.sendMessage(ChatColor.WHITE + "Block Type: " + ChatColor.YELLOW + casinoBlock.getBlockType().name());
        player.sendMessage(ChatColor.WHITE + "Min Bet: " + ChatColor.GREEN + Gambling.getEconomy().format(casinoBlock.getMinBet()));
        player.sendMessage(ChatColor.WHITE + "Max Bet: " + ChatColor.GREEN + Gambling.getEconomy().format(casinoBlock.getMaxBet()));
        player.sendMessage(ChatColor.WHITE + "Enabled: " + (casinoBlock.isEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        player.sendMessage(ChatColor.WHITE + "Physical Interactions: " + 
                         (casinoBlock.getGameType().supportsPhysicalInteractions() ? ChatColor.GREEN + "Yes" : ChatColor.GRAY + "No"));
        
        Location loc = casinoBlock.getLocation();
        player.sendMessage(ChatColor.WHITE + "Location: " + ChatColor.YELLOW + 
                         loc.getWorld().getName() + " " + loc.getBlockX() + ", " + 
                         loc.getBlockY() + ", " + loc.getBlockZ());
        player.sendMessage(ChatColor.WHITE + "ID: " + ChatColor.GRAY + casinoBlock.getId().toString());
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        
        return true;
    }
    
    private boolean handleReload(Player player) {
        try {
            // Reload the main configuration
            Gambling.getGamblingConfig().reloadConfig();

            // Reinitialize visual effects if settings changed
            if (Gambling.getVisualEffects() != null) {
                Gambling.getVisualEffects().cleanup();
                if (Gambling.getGamblingConfig().isHologramsEnabled() ||
                    Gambling.getGamblingConfig().isParticlesEnabled()) {
                    Gambling.getVisualEffects().initializeAllEffects(blockManager);
                }
            }

            player.sendMessage(ChatColor.GREEN + Gambling.getGamblingConfig().getMessage("admin-reload"));
            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to reload configuration: " + e.getMessage());
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleSetup(Player player, String[] args) {
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be looking at a casino block to set up interactions.");
            return true;
        }
        
        Location location = targetBlock.getLocation();
        CasinoBlock casinoBlock = blockManager.getCasinoBlock(location);
        
        if (casinoBlock == null) {
            player.sendMessage(ChatColor.RED + "There is no casino block at this location.");
            return true;
        }
        
        if (!casinoBlock.getGameType().supportsPhysicalInteractions()) {
            player.sendMessage(ChatColor.YELLOW + "This game type doesn't support physical interactions.");
            return true;
        }
        
        physicalInterface.setupInteractionsForBlock(casinoBlock);
        player.sendMessage(ChatColor.GREEN + "✓ Physical interactions set up for " + casinoBlock.getName());
        
        return true;
    }
    
    private void sendUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "  Casino Block Commands");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage(ChatColor.WHITE + "/casinoblock create <type> <name> <minBet> <maxBet> <blockType>");
        player.sendMessage(ChatColor.GRAY + "  Create a new casino block by replacing the block you're looking at");
        player.sendMessage(ChatColor.WHITE + "/casinoblock give <type> <name> <minBet> <maxBet> [blockType]");
        player.sendMessage(ChatColor.GRAY + "  Get a placeable Casino Block item (preferred)");
        player.sendMessage(ChatColor.WHITE + "/casinoblock remove [number]");
        player.sendMessage(ChatColor.GRAY + "  Remove the casino block you're looking at, or by list index");
        player.sendMessage(ChatColor.WHITE + "/casinoblock list");
        player.sendMessage(ChatColor.GRAY + "  List all casino blocks");
        player.sendMessage(ChatColor.WHITE + "/casinoblock info");
        player.sendMessage(ChatColor.GRAY + "  Get info about the casino block you're looking at");
        player.sendMessage(ChatColor.WHITE + "/casinoblock setup");
        player.sendMessage(ChatColor.GRAY + "  Set up physical interactions for a casino block");
        player.sendMessage(ChatColor.WHITE + "/casinoblock reload");
        player.sendMessage(ChatColor.GRAY + "  Reload casino blocks from configuration");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "remove", "list", "info", "setup", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            for (CasinoGameType type : CasinoGameType.values()) {
                completions.add(type.name().toLowerCase());
            }
        } else if (args.length == 6 && args[0].equalsIgnoreCase("create")) {
            // Block type suggestions
            completions.addAll(Arrays.asList("enchanting_table", "cauldron", "dispenser", "crafting_table", 
                                            "cartography_table", "smooth_stone_slab", "lectern", "redstone_block", 
                                            "hopper", "compass", "tnt", "gold_ingot"));
        }
        
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean handleGive(Player player, String[] args) {
        if (args.length < 5) {
            player.sendMessage(ChatColor.RED + "Usage: /casinoblock give <gameType> <name> <minBet> <maxBet> [blockType]");
            player.sendMessage(ChatColor.YELLOW + "Example: /casinoblock give blackjack \"VIP Blackjack\" 100 5000 enchanting_table");
            return true;
        }

        String gameTypeStr = args[1];
        String name = args[2].replace("\"", "");
        double minBet, maxBet;
        Material blockType = null;
        try {
            minBet = Double.parseDouble(args[3]);
            maxBet = Double.parseDouble(args[4]);
            if (args.length >= 6) {
                blockType = Material.valueOf(args[5].toUpperCase());
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid bet amounts. Please use numbers.");
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid block type: " + args[5]);
            return true;
        }

        CasinoGameType gameType = CasinoGameType.fromString(gameTypeStr);
        if (gameType == null) {
            player.sendMessage(ChatColor.RED + "Invalid game type: " + gameTypeStr);
            return true;
        }
        if (blockType == null) blockType = gameType.getDefaultBlock();

        // Create the item
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(blockType, 1);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(gameType.getFormattedName() + ChatColor.WHITE + " - " + name);
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(ChatColor.GRAY + "Min: " + ChatColor.GREEN + Gambling.getEconomy().format(minBet));
            lore.add(ChatColor.GRAY + "Max: " + ChatColor.GREEN + Gambling.getEconomy().format(maxBet));
            lore.add(ChatColor.DARK_GRAY + "Place to register casino block");
            meta.setLore(lore);

            // Tag with PDC so placement listener can register it
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey keyType = new org.bukkit.NamespacedKey(plugin, "casino_type");
            pdc.set(keyType, org.bukkit.persistence.PersistentDataType.STRING, gameType.name());
            pdc.set(new org.bukkit.NamespacedKey(plugin, "casino_name"), org.bukkit.persistence.PersistentDataType.STRING, name);
            pdc.set(new org.bukkit.NamespacedKey(plugin, "casino_min"), org.bukkit.persistence.PersistentDataType.DOUBLE, minBet);
            pdc.set(new org.bukkit.NamespacedKey(plugin, "casino_max"), org.bukkit.persistence.PersistentDataType.DOUBLE, maxBet);
            pdc.set(new org.bukkit.NamespacedKey(plugin, "casino_block"), org.bukkit.persistence.PersistentDataType.STRING, blockType.name());
            item.setItemMeta(meta);
        }

        // Give item
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "You received a placeable casino block for: " + gameType.getFormattedName());
        return true;
    }
}
