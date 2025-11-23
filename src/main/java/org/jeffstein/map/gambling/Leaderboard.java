package org.jeffstein.map.gambling;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Leaderboard {

    private final Gambling plugin;
    private final FileConfiguration leaderboardData;
    private final File leaderboardDataFile;

    public Leaderboard(Gambling plugin) {
        this.plugin = plugin;
        this.leaderboardDataFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        this.leaderboardData = YamlConfiguration.loadConfiguration(leaderboardDataFile);
    }

    public void addWin(UUID playerId, double amount) {
        double wins = leaderboardData.getDouble(playerId + ".wins", 0);
        leaderboardData.set(playerId + ".wins", wins + amount);
        saveLeaderboardData();
    }

    public void addLoss(UUID playerId, double amount) {
        double losses = leaderboardData.getDouble(playerId + ".losses", 0);
        leaderboardData.set(playerId + ".losses", losses + amount);
        saveLeaderboardData();
    }

    public double getWins(UUID playerId) {
        return leaderboardData.getDouble(playerId + ".wins", 0);
    }

    public double getLosses(UUID playerId) {
        return leaderboardData.getDouble(playerId + ".losses", 0);
    }

    public FileConfiguration getLeaderboardData() {
        return leaderboardData;
    }

    public void saveLeaderboardData() {
        try {
            leaderboardData.save(leaderboardDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
