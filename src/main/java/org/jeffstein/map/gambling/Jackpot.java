package org.jeffstein.map.gambling;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Jackpot {

    private final Gambling plugin;
    private final FileConfiguration jackpotData;
    private final File jackpotDataFile;
    private double jackpot;

    public Jackpot(Gambling plugin) {
        this.plugin = plugin;
        this.jackpotDataFile = new File(plugin.getDataFolder(), "jackpot.yml");
        this.jackpotData = YamlConfiguration.loadConfiguration(jackpotDataFile);
        this.jackpot = jackpotData.getDouble("jackpot", 10000.0); // Default jackpot
    }

    public double getJackpot() {
        return jackpot;
    }

    public void addToJackpot(double amount) {
        jackpot += amount;
        saveJackpotData();
    }

    public void resetJackpot() {
        jackpot = 10000.0; // Reset to default
        saveJackpotData();
    }

    public void saveJackpotData() {
        jackpotData.set("jackpot", jackpot);
        try {
            jackpotData.save(jackpotDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
