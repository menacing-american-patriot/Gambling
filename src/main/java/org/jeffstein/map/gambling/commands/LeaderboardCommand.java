package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LeaderboardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration leaderboardData = Gambling.getLeaderboard().getLeaderboardData();
        List<String> leaderboard = new ArrayList<>();

        for (String key : leaderboardData.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            double wins = leaderboardData.getDouble(key + ".wins", 0);
            leaderboard.add(Bukkit.getOfflinePlayer(playerId).getName() + ": " + wins);
        }

        leaderboard.sort(Comparator.comparingDouble(s -> Double.parseDouble(((String) s).split(": ")[1])).reversed());

        sender.sendMessage(ChatColor.GOLD + "--- Leaderboard ---");
        for (int i = 0; i < Math.min(10, leaderboard.size()); i++) {
            sender.sendMessage((i + 1) + ". " + leaderboard.get(i));
        }

        return true;
    }
}
