package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DailyCommand implements CommandExecutor {

    private final Gambling plugin;
    private final FileConfiguration playerData;
    private final File playerDataFile;

    public DailyCommand(Gambling plugin) {
        this.plugin = plugin;
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerData = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastClaimed = playerData.getLong(playerId + ".lastClaimed", 0);

        if (currentTime - lastClaimed < 86400000) { // 24 hours in milliseconds
            player.sendMessage("You have already claimed your daily reward. Come back later!");
            return true;
        }

        double reward = 100.0; // The daily reward amount
        Gambling.getEconomy().depositPlayer(player, reward);
        playerData.set(playerId + ".lastClaimed", currentTime);
        savePlayerData();

        player.sendMessage("You have claimed your daily reward of " + Gambling.getEconomy().format(reward));

        return true;
    }

    private void savePlayerData() {
        try {
            playerData.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
