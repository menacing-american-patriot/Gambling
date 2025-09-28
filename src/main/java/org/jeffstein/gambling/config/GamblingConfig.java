package org.jeffstein.gambling.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.casino.CasinoGameType;

import java.util.List;
import java.util.logging.Level;

/**
 * Configuration manager for the Gambling plugin
 */
public class GamblingConfig {
    
    private final Gambling plugin;
    private FileConfiguration config;
    
    public GamblingConfig(Gambling plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Validate configuration
        validateConfig();
    }
    
    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    
    private void validateConfig() {
        // Validate critical settings and set defaults if missing
        if (!config.contains("casino-blocks.enabled")) {
            config.set("casino-blocks.enabled", true);
        }
        
        if (!config.contains("casino-blocks.defaults.min-bet")) {
            config.set("casino-blocks.defaults.min-bet", 10.0);
        }
        
        if (!config.contains("casino-blocks.defaults.max-bet")) {
            config.set("casino-blocks.defaults.max-bet", 1000.0);
        }
        
        plugin.saveConfig();
    }
    
    // Casino Block Settings
    public boolean isCasinoBlocksEnabled() {
        return config.getBoolean("casino-blocks.enabled", true);
    }
    
    public boolean isHologramsEnabled() {
        return config.getBoolean("casino-blocks.visual-effects.holograms.enabled", true);
    }
    
    public double getHologramHeight() {
        return config.getDouble("casino-blocks.visual-effects.holograms.height", 2.5);
    }
    
    public boolean isHologramFloatingEnabled() {
        return config.getBoolean("casino-blocks.visual-effects.holograms.floating-animation", true);
    }
    
    public int getHologramUpdateInterval() {
        return config.getInt("casino-blocks.visual-effects.holograms.update-interval", 2);
    }
    
    public boolean isParticlesEnabled() {
        return config.getBoolean("casino-blocks.visual-effects.particles.enabled", true);
    }
    
    public int getParticleUpdateInterval() {
        return config.getInt("casino-blocks.visual-effects.particles.update-interval", 3);
    }
    
    public double getParticleDensity() {
        return config.getDouble("casino-blocks.visual-effects.particles.density", 1.0);
    }
    
    public boolean isWinEffectsEnabled() {
        return config.getBoolean("casino-blocks.visual-effects.player-effects.win-effects", true);
    }
    
    public boolean isLossEffectsEnabled() {
        return config.getBoolean("casino-blocks.visual-effects.player-effects.loss-effects", true);
    }
    
    public int getEffectDuration() {
        return config.getInt("casino-blocks.visual-effects.player-effects.effect-duration", 60);
    }
    
    // Physical Interaction Settings
    public boolean isPhysicalInteractionsEnabled() {
        return config.getBoolean("casino-blocks.physical-interactions.enabled", true);
    }
    
    public boolean isAutoSetupEnabled() {
        return config.getBoolean("casino-blocks.physical-interactions.auto-setup", true);
    }
    
    public double getInteractionRange() {
        return config.getDouble("casino-blocks.physical-interactions.range", 2.0);
    }
    
    public Material getHitButtonMaterial() {
        String materialName = config.getString("casino-blocks.physical-interactions.materials.hit-button", "ACACIA_BUTTON");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid hit button material: " + materialName + ", using default");
            return Material.ACACIA_BUTTON;
        }
    }
    
    public Material getStandButtonMaterial() {
        String materialName = config.getString("casino-blocks.physical-interactions.materials.stand-button", "CRIMSON_BUTTON");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid stand button material: " + materialName + ", using default");
            return Material.CRIMSON_BUTTON;
        }
    }
    
    public Material getDoubleButtonMaterial() {
        String materialName = config.getString("casino-blocks.physical-interactions.materials.double-button", "BIRCH_BUTTON");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid double button material: " + materialName + ", using default");
            return Material.BIRCH_BUTTON;
        }
    }
    
    public Material getSpinLeverMaterial() {
        String materialName = config.getString("casino-blocks.physical-interactions.materials.spin-lever", "LEVER");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid spin lever material: " + materialName + ", using default");
            return Material.LEVER;
        }
    }
    
    public Material getCashoutBlockMaterial() {
        String materialName = config.getString("casino-blocks.physical-interactions.materials.cashout-block", "EMERALD_BLOCK");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid cashout block material: " + materialName + ", using default");
            return Material.EMERALD_BLOCK;
        }
    }
    
    public Material getBetPlateMaterial() {
        String materialName = config.getString("casino-blocks.physical-interactions.materials.bet-plate", "STONE_PRESSURE_PLATE");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid bet plate material: " + materialName + ", using default");
            return Material.STONE_PRESSURE_PLATE;
        }
    }
    
    // Default Settings
    public double getDefaultMinBet() {
        return config.getDouble("casino-blocks.defaults.min-bet", 10.0);
    }
    
    public double getDefaultMaxBet() {
        return config.getDouble("casino-blocks.defaults.max-bet", 1000.0);
    }
    
    public boolean getDefaultEnabled() {
        return config.getBoolean("casino-blocks.defaults.enabled", true);
    }
    
    public Material getDefaultBlockType(CasinoGameType gameType) {
        String path = "casino-blocks.defaults.block-types." + gameType.name().toLowerCase();
        String materialName = config.getString(path, gameType.getDefaultBlock().name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid default block type for " + gameType + ": " + materialName + ", using game default");
            return gameType.getDefaultBlock();
        }
    }
    
    // Game-Specific Settings
    public boolean isPhysicalControlsEnabled(CasinoGameType gameType) {
        String path = "games." + gameType.name().toLowerCase() + ".physical-controls";
        return config.getBoolean(path, gameType.supportsPhysicalInteractions());
    }
    
    public double getGameMinBet(CasinoGameType gameType) {
        String path = "games." + gameType.name().toLowerCase() + ".min-bet";
        return config.getDouble(path, getDefaultMinBet());
    }
    
    public double getGameMaxBet(CasinoGameType gameType) {
        String path = "games." + gameType.name().toLowerCase() + ".max-bet";
        return config.getDouble(path, getDefaultMaxBet());
    }
    
    public boolean isDoubleDownAllowed() {
        return config.getBoolean("games.blackjack.allow-double-down", true);
    }
    
    public boolean isSplitAllowed() {
        return config.getBoolean("games.blackjack.allow-split", false);
    }
    
    public int getAutoSpinDelay() {
        return config.getInt("games.roulette.auto-spin-delay", 100);
    }
    
    public boolean isPhysicalCashoutEnabled() {
        return config.getBoolean("games.crash.physical-cashout", true);
    }
    
    public int getPlinkoDropZones() {
        return config.getInt("games.plinko.drop-zones", 9);
    }
    
    public int getMinesGridSize() {
        return config.getInt("games.mines.grid-size", 3);
    }
    
    // Permission Settings
    public boolean isPerGamePermissionsEnabled() {
        return config.getBoolean("permissions.per-game-permissions", true);
    }
    
    public String getDefaultPermission() {
        return config.getString("permissions.default-permission", "gambling.use");
    }
    
    public String getAdminPermission() {
        return config.getString("permissions.admin-permission", "gambling.admin.casinoblock");
    }
    
    // Economy Settings
    public double getMinimumBalance() {
        return config.getDouble("economy.minimum-balance", 0.0);
    }
    
    public double getUsageFee() {
        return config.getDouble("economy.usage-fee", 0.0);
    }
    
    public String getFeeMessage() {
        return config.getString("economy.fee-message", "&7A small usage fee of &a{fee} &7has been charged.");
    }
    
    // Logging Settings
    public boolean isBlockManagementLoggingEnabled() {
        return config.getBoolean("logging.block-management", true);
    }
    
    public boolean isPlayerInteractionLoggingEnabled() {
        return config.getBoolean("logging.player-interactions", false);
    }
    
    public boolean isPhysicalInteractionLoggingEnabled() {
        return config.getBoolean("logging.physical-interactions", false);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("logging.debug", false);
    }
    
    // Performance Settings
    public int getMaxBlocksPerWorld() {
        return config.getInt("performance.max-blocks-per-world", 100);
    }
    
    public int getMaxActiveSessions() {
        return config.getInt("performance.max-active-sessions", 3);
    }
    
    public int getSessionTimeout() {
        return config.getInt("performance.session-timeout", 30);
    }
    
    public int getParticleDistance() {
        return config.getInt("performance.particle-distance", 32);
    }
    
    public int getHologramDistance() {
        return config.getInt("performance.hologram-distance", 64);
    }
    
    // Messages
    public String getMessage(String key) {
        return config.getString("messages." + key, "Message not found: " + key);
    }
    
    public List<String> getHologramFormat() {
        return config.getStringList("messages.hologram-format");
    }
    
    // Advanced Settings
    public boolean isDatabaseEnabled() {
        return config.getBoolean("advanced.use-database", false);
    }
    
    public String getDatabaseHost() {
        return config.getString("advanced.database.host", "localhost");
    }
    
    public int getDatabasePort() {
        return config.getInt("advanced.database.port", 3306);
    }
    
    public String getDatabaseName() {
        return config.getString("advanced.database.database", "gambling");
    }
    
    public String getDatabaseUsername() {
        return config.getString("advanced.database.username", "gambling_user");
    }
    
    public String getDatabasePassword() {
        return config.getString("advanced.database.password", "password");
    }
    
    public String getDatabaseTablePrefix() {
        return config.getString("advanced.database.table-prefix", "gambling_");
    }
    
    public boolean isWorldGuardIntegrationEnabled() {
        return config.getBoolean("advanced.integrations.worldguard", true);
    }
    
    public boolean isGriefPreventionIntegrationEnabled() {
        return config.getBoolean("advanced.integrations.griefprevention", true);
    }
    
    public boolean isPlotSquaredIntegrationEnabled() {
        return config.getBoolean("advanced.integrations.plotsquared", true);
    }
    
    public boolean isBackupEnabled() {
        return config.getBoolean("advanced.backup.enabled", true);
    }
    
    public int getBackupInterval() {
        return config.getInt("advanced.backup.interval", 24);
    }
    
    public int getKeepBackups() {
        return config.getInt("advanced.backup.keep-backups", 7);
    }
}
