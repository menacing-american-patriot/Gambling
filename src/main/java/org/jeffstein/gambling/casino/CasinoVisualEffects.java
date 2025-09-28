package org.jeffstein.gambling.casino;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jeffstein.gambling.Gambling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles visual effects for casino blocks including particles, holograms, and animations
 */
public class CasinoVisualEffects {
    
    private final Gambling plugin;
    private final Map<Location, ArmorStand> holograms;
    private final Map<Location, BukkitTask> particleTasks;
    private final Map<UUID, BukkitTask> playerEffectTasks;
    
    public CasinoVisualEffects(Gambling plugin) {
        this.plugin = plugin;
        this.holograms = new ConcurrentHashMap<>();
        this.particleTasks = new ConcurrentHashMap<>();
        this.playerEffectTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a hologram above a casino block
     */
    public void createHologram(CasinoBlock casinoBlock) {
        // Check if holograms are enabled
        if (!Gambling.getGamblingConfig().isHologramsEnabled()) {
            return;
        }

        double height = Gambling.getGamblingConfig().getHologramHeight();
        Location location = casinoBlock.getLocation().clone().add(0.5, height, 0.5);

        // Remove existing hologram if present
        removeHologram(casinoBlock.getLocation());

        ArmorStand hologram = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setCustomNameVisible(true);
        hologram.setMarker(true);
        hologram.setSmall(true);

        // Use configurable hologram format
        List<String> format = Gambling.getGamblingConfig().getHologramFormat();
        StringBuilder displayText = new StringBuilder();

        for (int i = 0; i < format.size(); i++) {
            String line = format.get(i)
                .replace("{game_name}", casinoBlock.getGameType().getFormattedName())
                .replace("{block_name}", casinoBlock.getName())
                .replace("{min_bet}", Gambling.getEconomy().format(casinoBlock.getMinBet()))
                .replace("{max_bet}", Gambling.getEconomy().format(casinoBlock.getMaxBet()));

            displayText.append(ChatColor.translateAlternateColorCodes('&', line));
            if (i < format.size() - 1) {
                displayText.append("\n");
            }
        }

        hologram.setCustomName(displayText.toString());

        holograms.put(casinoBlock.getLocation(), hologram);

        // Start floating animation if enabled
        if (Gambling.getGamblingConfig().isHologramFloatingEnabled()) {
            startFloatingAnimation(hologram, location);
        }
    }
    
    /**
     * Remove hologram for a casino block
     */
    public void removeHologram(Location blockLocation) {
        ArmorStand hologram = holograms.remove(blockLocation);
        if (hologram != null && !hologram.isDead()) {
            hologram.remove();
        }
    }
    
    /**
     * Start particle effects around a casino block
     */
    public void startParticleEffects(CasinoBlock casinoBlock) {
        // Check if particles are enabled
        if (!Gambling.getGamblingConfig().isParticlesEnabled()) {
            return;
        }

        Location center = casinoBlock.getLocation().clone().add(0.5, 1, 0.5);

        // Stop existing particle effects
        stopParticleEffects(casinoBlock.getLocation());

        int updateInterval = Gambling.getGamblingConfig().getParticleUpdateInterval();
        double density = Gambling.getGamblingConfig().getParticleDensity();

        BukkitTask task = new BukkitRunnable() {
            private double angle = 0;

            @Override
            public void run() {
                if (center.getWorld() == null) {
                    this.cancel();
                    return;
                }

                // Create different particle effects based on game type
                switch (casinoBlock.getGameType()) {
                    case BLACKJACK:
                        createBlackjackParticles(center, angle, density);
                        break;
                    case ROULETTE:
                        createRouletteParticles(center, angle, density);
                        break;
                    case SLOTS:
                        createSlotsParticles(center, angle, density);
                        break;
                    case POKER:
                        createPokerParticles(center, angle, density);
                        break;
                    case CRASH:
                        createCrashParticles(center, angle, density);
                        break;
                    case MINES:
                        createMinesParticles(center, angle, density);
                        break;
                    default:
                        createGenericParticles(center, angle, density);
                        break;
                }

                angle += 0.2;
                if (angle >= Math.PI * 2) {
                    angle = 0;
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);

        particleTasks.put(casinoBlock.getLocation(), task);
    }
    
    /**
     * Stop particle effects for a casino block
     */
    public void stopParticleEffects(Location blockLocation) {
        BukkitTask task = particleTasks.remove(blockLocation);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Show win effect for a player
     */
    public void showWinEffect(Player player, double winAmount) {
        Location playerLoc = player.getLocation().clone().add(0, 1, 0);
        
        // Stop existing player effects
        stopPlayerEffects(player);
        
        BukkitTask task = new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 60) { // 3 seconds
                    this.cancel();
                    return;
                }
                
                // Firework particles
                playerLoc.getWorld().spawnParticle(Particle.FIREWORK, 
                    playerLoc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5), 
                    3, 0.2, 0.2, 0.2, 0.1);
                
                // Gold particles
                playerLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                    playerLoc.clone().add(Math.random() - 0.5, Math.random(), Math.random() - 0.5),
                    2, 0.3, 0.3, 0.3, 0.05);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        playerEffectTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Show loss effect for a player
     */
    public void showLossEffect(Player player) {
        Location playerLoc = player.getLocation().clone().add(0, 1, 0);
        
        // Stop existing player effects
        stopPlayerEffects(player);
        
        BukkitTask task = new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 40) { // 2 seconds
                    this.cancel();
                    return;
                }
                
                // Smoke particles
                playerLoc.getWorld().spawnParticle(Particle.SMOKE,
                    playerLoc.clone().add(Math.random() - 0.5, Math.random() * 0.5, Math.random() - 0.5),
                    2, 0.2, 0.2, 0.2, 0.02);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        playerEffectTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stop all effects for a player
     */
    public void stopPlayerEffects(Player player) {
        BukkitTask task = playerEffectTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    // Game-specific particle effects
    private void createBlackjackParticles(Location center, double angle, double density) {
        // Spade and heart particles in a circle
        double x = Math.cos(angle) * 1.5;
        double z = Math.sin(angle) * 1.5;
        int count = Math.max(1, (int)(density * 1));
        center.getWorld().spawnParticle(Particle.ENCHANT,
            center.clone().add(x, 0.5, z), count, 0, 0, 0, 0.5);
    }

    private void createRouletteParticles(Location center, double angle, double density) {
        // Red and black particles spinning without requiring DustOptions
        double x = Math.cos(angle) * 2;
        double z = Math.sin(angle) * 2;
        boolean redPhase = (angle % (Math.PI) < Math.PI / 2);
        int count = Math.max(1, (int)(density * 1));
        if (redPhase) {
            center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, 0.5, z), count, 0, 0, 0, 0.01);
        } else {
            center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(x, 0.5, z), count, 0, 0, 0, 0.0);
        }
    }

    private void createSlotsParticles(Location center, double angle, double density) {
        // Golden particles going up and down
        double y = Math.sin(angle * 2) * 0.5;
        int count = Math.max(1, (int)(density * 2));
        center.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
            center.clone().add(0, y + 0.5, 0), count, 0.3, 0.1, 0.3, 0);
    }

    private void createPokerParticles(Location center, double angle, double density) {
        // Green particles in a square pattern
        double size = 1.2;
        int corners = Math.max(1, (int)(density * 4));
        for (int i = 0; i < corners; i++) {
            double cornerAngle = angle + (i * Math.PI / 2);
            double x = Math.cos(cornerAngle) * size;
            double z = Math.sin(cornerAngle) * size;
            center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                center.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
        }
    }

    private void createCrashParticles(Location center, double angle, double density) {
        // Explosive particles that get faster
        double speed = 1 + Math.sin(angle * 3) * 0.5;
        double x = Math.cos(angle * speed) * 1.8;
        double z = Math.sin(angle * speed) * 1.8;
        int count = Math.max(1, (int)(density * 1));
        center.getWorld().spawnParticle(Particle.FLAME,
            center.clone().add(x, 0.5, z), count, 0, 0, 0, 0.02);
    }

    private void createMinesParticles(Location center, double angle, double density) {
        // Danger particles - red dust in a grid pattern
        int gridDensity = Math.max(1, (int)(density * 3));
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if ((x + z + (int)(angle * 10)) % gridDensity == 0) {
                    center.getWorld().spawnParticle(Particle.SMOKE,
                        center.clone().add(x, 0.2, z), 1, 0, 0, 0, 0.0);
                }
            }
        }
    }

    private void createGenericParticles(Location center, double angle, double density) {
        // Simple spiral effect
        double x = Math.cos(angle) * 1.5;
        double z = Math.sin(angle) * 1.5;
        double y = Math.sin(angle * 2) * 0.3;
        int count = Math.max(1, (int)(density * 1));
        center.getWorld().spawnParticle(Particle.END_ROD,
            center.clone().add(x, y + 0.5, z), count, 0, 0, 0, 0);
    }
    
    private void startFloatingAnimation(ArmorStand hologram, Location baseLocation) {
        new BukkitRunnable() {
            private double offset = 0;
            
            @Override
            public void run() {
                if (hologram.isDead()) {
                    this.cancel();
                    return;
                }
                
                double y = Math.sin(offset) * 0.1;
                hologram.teleport(baseLocation.clone().add(0, y, 0));
                offset += 0.1;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
    
    /**
     * Initialize visual effects for all existing casino blocks
     */
    public void initializeAllEffects(CasinoBlockManager blockManager) {
        for (CasinoBlock block : blockManager.getAllCasinoBlocks()) {
            createHologram(block);
            startParticleEffects(block);
        }
    }
    
    /**
     * Clean up all visual effects
     */
    public void cleanup() {
        // Remove all holograms
        for (ArmorStand hologram : holograms.values()) {
            if (!hologram.isDead()) {
                hologram.remove();
            }
        }
        holograms.clear();
        
        // Cancel all particle tasks
        for (BukkitTask task : particleTasks.values()) {
            task.cancel();
        }
        particleTasks.clear();
        
        // Cancel all player effect tasks
        for (BukkitTask task : playerEffectTasks.values()) {
            task.cancel();
        }
        playerEffectTasks.clear();
    }
}
