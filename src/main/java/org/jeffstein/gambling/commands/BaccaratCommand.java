package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BaccaratGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BaccaratCommand implements CommandExecutor {

    private final Gambling plugin;

    public BaccaratCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        BaccaratGUI gui = new BaccaratGUI(plugin, player);
        gui.openInventory();
        return true;
    }
}
