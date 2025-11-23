package org.jeffstein.map.gambling.casino;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jeffstein.map.gambling.Gambling;

/**
 * Handles player interactions with casino blocks
 */
public class CasinoBlockListener implements Listener {
    
    private final Gambling plugin;
    private final CasinoBlockManager blockManager;
    
    public CasinoBlockListener(Gambling plugin, CasinoBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        Location blockLocation = clickedBlock.getLocation();
        CasinoBlock casinoBlock = blockManager.getCasinoBlock(blockLocation);
        
        if (casinoBlock == null) {
            return;
        }
        
        event.setCancelled(true); // Prevent normal block interactions
        
        Player player = event.getPlayer();
        
        // Check if block is enabled
        if (!casinoBlock.isEnabled()) {
            player.sendActionBar(ChatColor.RED + Gambling.getGamblingConfig().getMessage("game-disabled"));
            return;
        }

        // Check if player has permission
        String defaultPerm = Gambling.getGamblingConfig().getDefaultPermission();
        String gamePerm = defaultPerm + "." + casinoBlock.getGameType().name().toLowerCase();

        if (Gambling.getGamblingConfig().isPerGamePermissionsEnabled()) {
            if (!player.hasPermission(defaultPerm) && !player.hasPermission(gamePerm)) {
                player.sendActionBar(ChatColor.RED + Gambling.getGamblingConfig().getMessage("no-permission"));
                return;
            }
        } else {
            if (!player.hasPermission(defaultPerm)) {
                player.sendActionBar(ChatColor.RED + Gambling.getGamblingConfig().getMessage("no-permission"));
                return;
            }
        }

        // Check if player has enough money for minimum bet
        double balance = Gambling.getEconomy().getBalance(player);
        double minimumBalance = Gambling.getGamblingConfig().getMinimumBalance();

        if (balance < minimumBalance) {
            player.sendActionBar(ChatColor.RED + "You need at least " +
                               Gambling.getEconomy().format(minimumBalance) + " to use casino games.");
            return;
        }

        if (balance < casinoBlock.getMinBet()) {
            String message = Gambling.getGamblingConfig().getMessage("insufficient-funds")
                           .replace("{min_bet}", Gambling.getEconomy().format(casinoBlock.getMinBet()));
            player.sendActionBar(ChatColor.RED + message);
            return;
        }

        // Charge usage fee if configured
        double usageFee = Gambling.getGamblingConfig().getUsageFee();
        if (usageFee > 0) {
            if (balance >= usageFee) {
                Gambling.getEconomy().withdrawPlayer(player, usageFee);
                String feeMessage = Gambling.getGamblingConfig().getFeeMessage()
                                  .replace("{fee}", Gambling.getEconomy().format(usageFee));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', feeMessage));
            } else {
                player.sendActionBar(ChatColor.RED + "Insufficient funds for usage fee.");
                return;
            }
        }
        
        // Play interaction sound
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
        
        // Show game info
        showGameInfo(player, casinoBlock);
        
        // Handle the interaction based on game type
        handleGameInteraction(player, casinoBlock);
    }
    
    private void showGameInfo(Player player, CasinoBlock casinoBlock) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "  " + casinoBlock.getGameType().getFormattedName());
        player.sendMessage(ChatColor.GRAY + "  " + casinoBlock.getName());
        player.sendMessage("");
        player.sendMessage(ChatColor.WHITE + "  Min Bet: " + ChatColor.GREEN + Gambling.getEconomy().format(casinoBlock.getMinBet()));
        player.sendMessage(ChatColor.WHITE + "  Max Bet: " + ChatColor.GREEN + Gambling.getEconomy().format(casinoBlock.getMaxBet()));
        player.sendMessage(ChatColor.WHITE + "  Your Balance: " + ChatColor.YELLOW + Gambling.getEconomy().format(Gambling.getEconomy().getBalance(player)));
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  " + casinoBlock.getGameType().getFormattedDescription());
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════════");
        player.sendMessage("");
    }
    
    private void handleGameInteraction(Player player, CasinoBlock casinoBlock) {
        CasinoGameType gameType = casinoBlock.getGameType();
        
        // Add player session tracking
        blockManager.addPlayerSession(player, casinoBlock.getLocation());
        
        // Show action bar with game starting message
        player.sendActionBar(ChatColor.GREEN + "Starting " + gameType.getDisplayName() + "...");
        
        // Delay the command execution slightly to allow the action bar to show
        new BukkitRunnable() {
            @Override
            public void run() {
                executeGameCommand(player, casinoBlock);
            }
        }.runTaskLater(plugin, 5L); // 5 ticks = 0.25 seconds
    }
    
    private void executeGameCommand(Player player, CasinoBlock casinoBlock) {
        String command;

        switch (casinoBlock.getGameType()) {
            case BLACKJACK:
                // Start blackjack immediately with min bet
                command = "blackjack " + (int) casinoBlock.getMinBet();
                break;
            case ROULETTE:
                // Start roulette immediately with a default bet type (black) using min bet
                String betType = String.valueOf(casinoBlock.getGameSetting("defaultBetType", "black"));
                command = "roulette " + (int) casinoBlock.getMinBet() + " " + betType;
                break;
            case COINFLIP:
                command = "coinflip " + (int) casinoBlock.getMinBet();
                break;
            case POKER:
                command = "poker join";
                break;
            default:
                command = casinoBlock.getGameType().getCommand();
                break;
        }

        // Execute the command as the player
        player.performCommand(command);

        // For games that support physical interactions, set up the interaction area
        if (casinoBlock.getGameType().supportsPhysicalInteractions()) {
            setupPhysicalInteractions(player, casinoBlock);
        }
    }
    
    private void setupPhysicalInteractions(Player player, CasinoBlock casinoBlock) {
        // This will be expanded in the next phase to create physical buttons/levers
        // around the casino block for game actions like hit/stand, bet adjustments, etc.
        
        switch (casinoBlock.getGameType()) {
            case BLACKJACK:
                player.sendMessage(ChatColor.YELLOW + "💡 Look for buttons around the table for Hit/Stand actions!");
                break;
            case ROULETTE:
                player.sendMessage(ChatColor.YELLOW + "💡 Use the betting interface to place your bets!");
                break;
            case CRAPS:
                player.sendMessage(ChatColor.YELLOW + "💡 Look for dice and betting areas around the table!");
                break;
            case CRASH:
                player.sendMessage(ChatColor.YELLOW + "💡 Watch for the cash-out button when the game starts!");
                break;
            case PLINKO:
                player.sendMessage(ChatColor.YELLOW + "💡 Look for drop zones above the Plinko board!");
                break;
            case MINES:
                player.sendMessage(ChatColor.YELLOW + "💡 Right-click squares to reveal them!");
                break;
            default:
                break;
        }
    }
    
    /**
     * Handle when a player leaves the game area or logs out
     */
    public void handlePlayerLeave(Player player, Location blockLocation) {
        blockManager.removePlayerSession(player, blockLocation);
        
        // Clean up any physical interactions for this player
        // This could include removing temporary blocks, stopping particle effects, etc.
    }

    // Auto-cleanup: remove casino block entry if the block is broken (e.g., in creative)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCasinoBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        CasinoBlock casinoBlock = blockManager.getCasinoBlock(loc);
        if (casinoBlock != null) {
            // Clear effects and interactions
            Gambling.getVisualEffects().removeHologram(loc);
            Gambling.getVisualEffects().stopParticleEffects(loc);
            if (casinoBlock.getGameType().supportsPhysicalInteractions()) {
                // Best-effort clear; safe if not present
                try { Gambling.getPhysicalGameInterface().clearInteractionsForBlock(casinoBlock); } catch (Exception ignored) {}
            }
            blockManager.removeCasinoBlock(loc);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Casino block removed.");
        }
    }

    // Allow placing preconfigured casino block items instead of replacing a looked-at block
    @EventHandler(priority = EventPriority.HIGH)
    public void onCasinoBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey keyType = new NamespacedKey(plugin, "casino_type");
        if (!pdc.has(keyType, PersistentDataType.STRING)) return;

        // Extract data
        String typeStr = pdc.get(keyType, PersistentDataType.STRING);
        String name = pdc.getOrDefault(new NamespacedKey(plugin, "casino_name"), PersistentDataType.STRING, "Casino Game");
        Double minBet = pdc.getOrDefault(new NamespacedKey(plugin, "casino_min"), PersistentDataType.DOUBLE, 100.0);
        Double maxBet = pdc.getOrDefault(new NamespacedKey(plugin, "casino_max"), PersistentDataType.DOUBLE, 1000.0);
        String blockMat = pdc.getOrDefault(new NamespacedKey(plugin, "casino_block"), PersistentDataType.STRING, Material.ENCHANTING_TABLE.name());

        CasinoGameType gameType = CasinoGameType.fromString(typeStr);
        if (gameType == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "Invalid casino block item.");
            return;
        }

        Location loc = event.getBlockPlaced().getLocation();
        boolean ok = blockManager.registerCasinoBlock(loc, gameType, name, minBet, maxBet, Material.valueOf(blockMat));
        if (ok) {
            // Ensure the world block matches our configured type
            event.getBlockPlaced().setType(Material.valueOf(blockMat));
            // Effects
            CasinoBlock cb = blockManager.getCasinoBlock(loc);
            if (cb != null) {
                if (cb.getGameType().supportsPhysicalInteractions()) {
                    Gambling.getPhysicalGameInterface().setupInteractionsForBlock(cb);
                }
                Gambling.getVisualEffects().createHologram(cb);
                Gambling.getVisualEffects().startParticleEffects(cb);
            }
            event.getPlayer().sendMessage(ChatColor.GREEN + "Placed casino block: " + gameType.getFormattedName());
        } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "A casino block already exists here.");
        }
    }
}

