package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.CrapsGUI;
import org.jeffstein.gambling.games.CrapsGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrapsCommand implements CommandExecutor {

    private final Gambling plugin;

    public CrapsCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        CrapsGame game = new CrapsGame(plugin, player);
        CrapsGUI crapsGUI = new CrapsGUI(plugin, player, game);
        crapsGUI.openInventory();
        return true;
    }
}
