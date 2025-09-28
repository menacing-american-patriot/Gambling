package org.jeffstein.gambling.casino;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BlackjackGame;
import org.jeffstein.gambling.listeners.BlackjackBettingGUIListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles physical interactions around casino blocks (buttons, levers, pressure plates)
 * for game actions like hit/stand, betting, etc.
 */
public class PhysicalGameInterface implements Listener {
    
    private final Gambling plugin;
    private final CasinoBlockManager blockManager;
    
    // Maps to track physical interaction blocks around casino games
    private final Map<Location, PhysicalInteraction> physicalInteractions;
    private final Map<Location, Set<Location>> casinoBlockInteractions; // Casino block -> Set of interaction blocks
    
    public PhysicalGameInterface(Gambling plugin, CasinoBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        this.physicalInteractions = new ConcurrentHashMap<>();
        this.casinoBlockInteractions = new ConcurrentHashMap<>();
    }
    
    /**
     * Set up physical interactions around a casino block
     */
    public void setupInteractionsForBlock(CasinoBlock casinoBlock) {
        Location center = casinoBlock.getLocation();
        CasinoGameType gameType = casinoBlock.getGameType();
        
        // Clear existing interactions for this block
        clearInteractionsForBlock(casinoBlock);
        
        Set<Location> interactionLocations = new HashSet<>();
        
        switch (gameType) {
            case BLACKJACK:
                setupBlackjackInteractions(center, interactionLocations);
                break;
            case ROULETTE:
                setupRouletteInteractions(center, interactionLocations);
                break;
            case CRAPS:
                setupCrapsInteractions(center, interactionLocations);
                break;
            case CRASH:
                setupCrashInteractions(center, interactionLocations);
                break;
            case PLINKO:
                setupPlinkoInteractions(center, interactionLocations);
                break;
            case MINES:
                setupMinesInteractions(center, interactionLocations);
                break;
            default:
                return; // No physical interactions for this game type
        }
        
        casinoBlockInteractions.put(center, interactionLocations);
    }
    
    private void setupBlackjackInteractions(Location center, Set<Location> interactions) {
        // Get materials from config
        Material hitMaterial = Gambling.getGamblingConfig().getHitButtonMaterial();
        Material standMaterial = Gambling.getGamblingConfig().getStandButtonMaterial();
        Material doubleMaterial = Gambling.getGamblingConfig().getDoubleButtonMaterial();

        // Hit button (right side)
        Location hitButton = center.clone().add(1, 0, 0);
        createInteractionBlock(hitButton, hitMaterial, BlockFace.WEST,
                             new PhysicalInteraction(InteractionType.BLACKJACK_HIT, center, "Hit"));
        interactions.add(hitButton);

        // Stand button (left side)
        Location standButton = center.clone().add(-1, 0, 0);
        createInteractionBlock(standButton, standMaterial, BlockFace.EAST,
                             new PhysicalInteraction(InteractionType.BLACKJACK_STAND, center, "Stand"));
        interactions.add(standButton);

        // Double down button (front) - only if enabled in config
        if (Gambling.getGamblingConfig().isDoubleDownAllowed()) {
            Location doubleButton = center.clone().add(0, 0, 1);
            createInteractionBlock(doubleButton, doubleMaterial, BlockFace.NORTH,
                                 new PhysicalInteraction(InteractionType.BLACKJACK_DOUBLE, center, "Double Down"));
            interactions.add(doubleButton);
        }
    }
    
    private void setupRouletteInteractions(Location center, Set<Location> interactions) {
        // Get materials from config
        Material leverMaterial = Gambling.getGamblingConfig().getSpinLeverMaterial();
        Material plateMaterial = Gambling.getGamblingConfig().getBetPlateMaterial();

        // Spin lever
        Location spinLever = center.clone().add(0, 1, 1);
        createInteractionBlock(spinLever, leverMaterial, BlockFace.NORTH,
                             new PhysicalInteraction(InteractionType.ROULETTE_SPIN, center, "Spin Wheel"));
        interactions.add(spinLever);

        // Betting pressure plates around the wheel
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip center
                Location plateLocation = center.clone().add(x, 0, z);
                createInteractionBlock(plateLocation, plateMaterial, BlockFace.UP,
                                     new PhysicalInteraction(InteractionType.ROULETTE_BET, center, "Place Bet"));
                interactions.add(plateLocation);
            }
        }
    }
    
    private void setupCrapsInteractions(Location center, Set<Location> interactions) {
        // Roll dice button
        Location rollButton = center.clone().add(0, 0, 1);
        createInteractionBlock(rollButton, Material.STONE_BUTTON, BlockFace.NORTH,
                             new PhysicalInteraction(InteractionType.CRAPS_ROLL, center, "Roll Dice"));
        interactions.add(rollButton);
        
        // Pass line bet
        Location passButton = center.clone().add(-1, 0, 0);
        createInteractionBlock(passButton, Material.ACACIA_BUTTON, BlockFace.EAST,
                             new PhysicalInteraction(InteractionType.CRAPS_PASS_BET, center, "Pass Line"));
        interactions.add(passButton);
        
        // Don't pass bet
        Location dontPassButton = center.clone().add(1, 0, 0);
        createInteractionBlock(dontPassButton, Material.CRIMSON_BUTTON, BlockFace.WEST,
                             new PhysicalInteraction(InteractionType.CRAPS_DONT_PASS_BET, center, "Don't Pass"));
        interactions.add(dontPassButton);
    }
    
    private void setupCrashInteractions(Location center, Set<Location> interactions) {
        // Cash out button (will be activated when game is running)
        Location cashOutButton = center.clone().add(0, 1, 1);
        createInteractionBlock(cashOutButton, Material.EMERALD_BLOCK, BlockFace.UP,
                             new PhysicalInteraction(InteractionType.CRASH_CASHOUT, center, "Cash Out"));
        interactions.add(cashOutButton);
        
        // Bet amount adjustment buttons
        Location increaseBet = center.clone().add(1, 0, 0);
        createInteractionBlock(increaseBet, Material.ACACIA_BUTTON, BlockFace.WEST,
                             new PhysicalInteraction(InteractionType.CRASH_INCREASE_BET, center, "Increase Bet"));
        interactions.add(increaseBet);
        
        Location decreaseBet = center.clone().add(-1, 0, 0);
        createInteractionBlock(decreaseBet, Material.CRIMSON_BUTTON, BlockFace.EAST,
                             new PhysicalInteraction(InteractionType.CRASH_DECREASE_BET, center, "Decrease Bet"));
        interactions.add(decreaseBet);
    }
    
    private void setupPlinkoInteractions(Location center, Set<Location> interactions) {
        // Drop zones above the plinko board
        for (int x = -4; x <= 4; x++) {
            Location dropZone = center.clone().add(x, 2, 0);
            createInteractionBlock(dropZone, Material.STONE_PRESSURE_PLATE, BlockFace.UP,
                                 new PhysicalInteraction(InteractionType.PLINKO_DROP, center, "Drop Ball (Slot " + (x + 4) + ")"));
            interactions.add(dropZone);
        }
    }
    
    private void setupMinesInteractions(Location center, Set<Location> interactions) {
        // Cash out button
        Location cashOutButton = center.clone().add(0, 1, 1);
        createInteractionBlock(cashOutButton, Material.EMERALD_BLOCK, BlockFace.UP,
                             new PhysicalInteraction(InteractionType.MINES_CASHOUT, center, "Cash Out"));
        interactions.add(cashOutButton);
        
        // Grid of pressure plates for mine selection (3x3 grid around the center)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip center
                Location plateLocation = center.clone().add(x * 2, 0, z * 2);
                int position = (x + 1) * 3 + (z + 1);
                createInteractionBlock(plateLocation, Material.HEAVY_WEIGHTED_PRESSURE_PLATE, BlockFace.UP,
                                     new PhysicalInteraction(InteractionType.MINES_REVEAL, center, "Reveal Square " + position));
                interactions.add(plateLocation);
            }
        }
    }
    
    private void createInteractionBlock(Location location, Material material, BlockFace face, PhysicalInteraction interaction) {
        // Store the interaction data
        physicalInteractions.put(location, interaction);
        
        // Note: In a real implementation, you might want to actually place these blocks
        // or use a different approach like invisible armor stands with custom names
        // For now, we'll just track the locations and handle interactions
    }
    
    @EventHandler
    public void onPhysicalInteraction(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && 
            event.getAction() != Action.PHYSICAL) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        Location blockLocation = clickedBlock.getLocation();
        PhysicalInteraction interaction = physicalInteractions.get(blockLocation);
        
        if (interaction == null) {
            return;
        }
        
        event.setCancelled(true);
        Player player = event.getPlayer();
        
        // Check if player has an active session at this casino block
        if (!blockManager.getPlayerSessions(player).contains(interaction.getCasinoBlockLocation())) {
            player.sendActionBar(ChatColor.RED + "You need to start a game at the " + 
                               interaction.getDisplayName() + " table first!");
            return;
        }
        
        // Play interaction sound
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        
        // Handle the specific interaction
        handlePhysicalInteraction(player, interaction);
    }
    
    private void handlePhysicalInteraction(Player player, PhysicalInteraction interaction) {
        player.sendActionBar(ChatColor.GREEN + interaction.getDisplayName());
        
        switch (interaction.getType()) {
            case BLACKJACK_HIT:
                handleBlackjackHit(player);
                break;
            case BLACKJACK_STAND:
                handleBlackjackStand(player);
                break;
            case BLACKJACK_DOUBLE:
                handleBlackjackDouble(player);
                break;
            case ROULETTE_SPIN:
                handleRouletteSpin(player);
                break;
            case ROULETTE_BET:
                handleRouletteBet(player);
                break;
            case CRAPS_ROLL:
                handleCrapsRoll(player);
                break;
            case CRASH_CASHOUT:
                handleCrashCashout(player);
                break;
            case PLINKO_DROP:
                handlePlinkoDrop(player, interaction);
                break;
            case MINES_CASHOUT:
                handleMinesCashout(player);
                break;
            case MINES_REVEAL:
                handleMinesReveal(player, interaction);
                break;
            default:
                player.sendActionBar(ChatColor.YELLOW + "This interaction is not yet implemented!");
                break;
        }
    }
    
    // Game-specific interaction handlers
    private void handleBlackjackHit(Player player) {
        // Find the active blackjack game for this player
        BlackjackBettingGUIListener bettingListener = findBlackjackBettingListener();
        if (bettingListener != null) {
            BlackjackGame game = bettingListener.getGames().get(player.getUniqueId());
            if (game != null) {
                game.hit();
                player.sendActionBar(ChatColor.GREEN + "Hit! Drawing another card...");
            } else {
                player.sendActionBar(ChatColor.RED + "No active blackjack game found!");
            }
        }
    }
    
    private void handleBlackjackStand(Player player) {
        BlackjackBettingGUIListener bettingListener = findBlackjackBettingListener();
        if (bettingListener != null) {
            BlackjackGame game = bettingListener.getGames().get(player.getUniqueId());
            if (game != null) {
                game.stand();
                player.sendActionBar(ChatColor.YELLOW + "Stand! Dealer's turn...");
            } else {
                player.sendActionBar(ChatColor.RED + "No active blackjack game found!");
            }
        }
    }
    
    private void handleBlackjackDouble(Player player) {
        // Implementation for double down would go here
        player.sendActionBar(ChatColor.YELLOW + "Double down feature coming soon!");
    }
    
    private void handleRouletteSpin(Player player) {
        // Trigger roulette spin
        player.performCommand("roulette spin");
    }
    
    private void handleRouletteBet(Player player) {
        // Open roulette betting interface
        player.sendActionBar(ChatColor.YELLOW + "Use the roulette GUI to place your bets!");
    }
    
    private void handleCrapsRoll(Player player) {
        // Trigger craps roll
        player.performCommand("craps roll");
    }
    
    private void handleCrashCashout(Player player) {
        // Trigger crash cashout
        player.performCommand("crash cashout");
    }
    
    private void handlePlinkoDrop(Player player, PhysicalInteraction interaction) {
        // Extract slot number from display name and trigger plinko drop
        String displayName = interaction.getDisplayName();
        if (displayName.contains("Slot ")) {
            try {
                String slotStr = displayName.substring(displayName.indexOf("Slot ") + 5, displayName.indexOf(")"));
                int slot = Integer.parseInt(slotStr);
                player.performCommand("plinko drop " + slot);
            } catch (Exception e) {
                player.sendActionBar(ChatColor.RED + "Invalid drop slot!");
            }
        }
    }
    
    private void handleMinesCashout(Player player) {
        // Trigger mines cashout
        player.performCommand("mines cashout");
    }
    
    private void handleMinesReveal(Player player, PhysicalInteraction interaction) {
        // Extract position from display name and trigger mines reveal
        String displayName = interaction.getDisplayName();
        if (displayName.contains("Square ")) {
            try {
                String posStr = displayName.substring(displayName.indexOf("Square ") + 7);
                int position = Integer.parseInt(posStr);
                player.performCommand("mines reveal " + position);
            } catch (Exception e) {
                player.sendActionBar(ChatColor.RED + "Invalid square position!");
            }
        }
    }
    
    private BlackjackBettingGUIListener findBlackjackBettingListener() {
        // This is a helper method to find the blackjack betting listener
        // In a real implementation, you might want to store a reference to it
        // or use a different approach to access active games
        return null; // Placeholder - would need proper implementation
    }
    
    public void clearInteractionsForBlock(CasinoBlock casinoBlock) {
        Set<Location> interactions = casinoBlockInteractions.get(casinoBlock.getLocation());
        if (interactions != null) {
            for (Location loc : interactions) {
                physicalInteractions.remove(loc);
            }
            casinoBlockInteractions.remove(casinoBlock.getLocation());
        }
    }
    
    public void clearAllInteractions() {
        physicalInteractions.clear();
        casinoBlockInteractions.clear();
    }
}
