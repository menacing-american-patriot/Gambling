package org.jeffstein.gambling.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jeffstein.gambling.Gambling;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all casino blocks in the world
 */
public class CasinoBlockManager {
    
    private final Gambling plugin;
    private final Map<Location, CasinoBlock> casinoBlocks;
    private final Map<UUID, Set<Location>> playerActiveSessions;
    private final File dataFile;
    private FileConfiguration config;
    
    public CasinoBlockManager(Gambling plugin) {
        this.plugin = plugin;
        this.casinoBlocks = new ConcurrentHashMap<>();
        this.playerActiveSessions = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "casino-blocks.yml");
        loadCasinoBlocks();
    }
    
    /**
     * Register a new casino block at the specified location
     */
    public boolean registerCasinoBlock(Location location, CasinoGameType gameType, 
                                     String name, double minBet, double maxBet, 
                                     Material blockType) {
        if (isCasinoBlock(location)) {
            return false; // Block already exists at this location
        }
        
        CasinoBlock casinoBlock = new CasinoBlock(location, gameType, name, minBet, maxBet, blockType);
        casinoBlocks.put(location.clone(), casinoBlock);
        saveCasinoBlocks();
        
        plugin.getLogger().info("Registered casino block: " + casinoBlock);
        return true;
    }
    
    /**
     * Remove a casino block at the specified location
     */
    public boolean removeCasinoBlock(Location location) {
        CasinoBlock removed = casinoBlocks.remove(location);
        if (removed != null) {
            saveCasinoBlocks();
            plugin.getLogger().info("Removed casino block: " + removed);
            return true;
        }
        return false;
    }
    
    /**
     * Check if a location has a casino block
     */
    public boolean isCasinoBlock(Location location) {
        return casinoBlocks.containsKey(location);
    }
    
    /**
     * Get the casino block at a specific location
     */
    public CasinoBlock getCasinoBlock(Location location) {
        return casinoBlocks.get(location);
    }
    
    /**
     * Get all casino blocks
     */
    public Collection<CasinoBlock> getAllCasinoBlocks() {
        return new ArrayList<>(casinoBlocks.values());
    }
    
    /**
     * Get all casino blocks of a specific game type
     */
    public List<CasinoBlock> getCasinoBlocksByType(CasinoGameType gameType) {
        return casinoBlocks.values().stream()
                .filter(block -> block.getGameType() == gameType)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Get casino blocks near a location within a radius
     */
    public List<CasinoBlock> getCasinoBlocksNear(Location center, double radius) {
        return casinoBlocks.values().stream()
                .filter(block -> block.getLocation().getWorld().equals(center.getWorld()))
                .filter(block -> block.getLocation().distance(center) <= radius)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Track that a player has an active session at a casino block
     */
    public void addPlayerSession(Player player, Location blockLocation) {
        playerActiveSessions.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                           .add(blockLocation.clone());
    }
    
    /**
     * Remove a player's session at a casino block
     */
    public void removePlayerSession(Player player, Location blockLocation) {
        Set<Location> sessions = playerActiveSessions.get(player.getUniqueId());
        if (sessions != null) {
            sessions.remove(blockLocation);
            if (sessions.isEmpty()) {
                playerActiveSessions.remove(player.getUniqueId());
            }
        }
    }
    
    /**
     * Check if a player has an active session at any casino block
     */
    public boolean hasActiveSession(Player player) {
        Set<Location> sessions = playerActiveSessions.get(player.getUniqueId());
        return sessions != null && !sessions.isEmpty();
    }
    
    /**
     * Get all active session locations for a player
     */
    public Set<Location> getPlayerSessions(Player player) {
        return new HashSet<>(playerActiveSessions.getOrDefault(player.getUniqueId(), new HashSet<>()));
    }
    
    /**
     * Load casino blocks from file
     */
    private void loadCasinoBlocks() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            return;
        }
        
        try {
            config = YamlConfiguration.loadConfiguration(dataFile);
            
            if (config.contains("casino-blocks")) {
                List<Map<?, ?>> blockList = config.getMapList("casino-blocks");
                for (Map<?, ?> blockData : blockList) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> blockMap = (Map<String, Object>) blockData;
                        CasinoBlock block = new CasinoBlock(blockMap);
                        casinoBlocks.put(block.getLocation(), block);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load casino block", e);
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + casinoBlocks.size() + " casino blocks");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load casino blocks", e);
        }
    }
    
    /**
     * Save casino blocks to file
     */
    private void saveCasinoBlocks() {
        try {
            if (config == null) {
                config = new YamlConfiguration();
            }
            
            List<Map<String, Object>> blockList = new ArrayList<>();
            for (CasinoBlock block : casinoBlocks.values()) {
                blockList.add(block.serialize());
            }
            
            config.set("casino-blocks", blockList);
            config.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save casino blocks", e);
        }
    }
    
    /**
     * Cleanup method to be called on plugin disable
     */
    public void shutdown() {
        saveCasinoBlocks();
        casinoBlocks.clear();
        playerActiveSessions.clear();
    }
}
