package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.MinesGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MinesCommand implements CommandExecutor {

    private final Gambling plugin;

    public MinesCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("mines")) {
            player.sendMessage("Mines is currently disabled.");
            return true;
        }

        MinesGame minesGame = new MinesGame(plugin, player);

        // Optional fast-path: /mines <bet> [start]
        if (args.length >= 1) {
            try {
                double bet = Double.parseDouble(args[0]);
                minesGame.setBetAmount(bet);
                if (args.length >= 2 && args[1].equalsIgnoreCase("start")) {
                    // Open then immediately start so GUI shows animations/state
                    minesGame.openInventory();
                    minesGame.startGame();
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // fall back to GUI
            }
        }

        minesGame.openInventory();

        return true;
    }
}
