package org.jeffstein.gambling.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a casino block in the world that players can interact with to play games
 */
public class CasinoBlock implements ConfigurationSerializable {
    
    private final UUID id;
    private final Location location;
    private final CasinoGameType gameType;
    private final String name;
    private final double minBet;
    private final double maxBet;
    private final Material blockType;
    private final boolean enabled;
    private final Map<String, Object> gameSpecificSettings;
    
    public CasinoBlock(Location location, CasinoGameType gameType, String name, 
                      double minBet, double maxBet, Material blockType) {
        this.id = UUID.randomUUID();
        this.location = location.clone();
        this.gameType = gameType;
        this.name = name;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.blockType = blockType;
        this.enabled = true;
        this.gameSpecificSettings = new HashMap<>();
    }
    
    // Constructor for deserialization
    public CasinoBlock(Map<String, Object> map) {
        this.id = UUID.fromString((String) map.get("id"));
        this.location = (Location) map.get("location");
        this.gameType = CasinoGameType.valueOf((String) map.get("gameType"));
        this.name = (String) map.get("name");
        this.minBet = (Double) map.get("minBet");
        this.maxBet = (Double) map.get("maxBet");
        this.blockType = Material.valueOf((String) map.get("blockType"));
        this.enabled = (Boolean) map.getOrDefault("enabled", true);
        Object settingsObj = map.getOrDefault("gameSpecificSettings", new HashMap<String, Object>());
        if (settingsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) settingsObj;
            this.gameSpecificSettings = settings;
        } else {
            this.gameSpecificSettings = new HashMap<>();
        }
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id.toString());
        map.put("location", location);
        map.put("gameType", gameType.name());
        map.put("name", name);
        map.put("minBet", minBet);
        map.put("maxBet", maxBet);
        map.put("blockType", blockType.name());
        map.put("enabled", enabled);
        map.put("gameSpecificSettings", gameSpecificSettings);
        return map;
    }
    
    // Getters
    public UUID getId() { return id; }
    public Location getLocation() { return location.clone(); }
    public CasinoGameType getGameType() { return gameType; }
    public String getName() { return name; }
    public double getMinBet() { return minBet; }
    public double getMaxBet() { return maxBet; }
    public Material getBlockType() { return blockType; }
    public boolean isEnabled() { return enabled; }
    public Map<String, Object> getGameSpecificSettings() { return new HashMap<>(gameSpecificSettings); }
    
    // Game-specific setting helpers
    public void setGameSetting(String key, Object value) {
        gameSpecificSettings.put(key, value);
    }
    
    public Object getGameSetting(String key) {
        return gameSpecificSettings.get(key);
    }
    
    public Object getGameSetting(String key, Object defaultValue) {
        return gameSpecificSettings.getOrDefault(key, defaultValue);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CasinoBlock that = (CasinoBlock) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("CasinoBlock{name='%s', gameType=%s, location=%s}", 
                           name, gameType, location);
    }
}
