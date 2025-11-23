package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.BlackjackBettingGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlackjackGUICommand implements CommandExecutor {

    private final Gambling plugin;

    public BlackjackGUICommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("blackjack")) {
            player.sendMessage("Blackjack is currently disabled.");
            return true;
        }
        BlackjackBettingGUI bettingGUI = new BlackjackBettingGUI(plugin, player);
        bettingGUI.openInventory();
        return true;
    }
}
