package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.WheelOfFortuneGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WheelOfFortuneCommand implements CommandExecutor {

    private final Gambling plugin;

    public WheelOfFortuneCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        
        // Create a new Wheel of Fortune game for this player
        WheelOfFortuneGame wheelGame = new WheelOfFortuneGame(plugin, player);
        wheelGame.openInventory();
        
        return true;
    }
}
