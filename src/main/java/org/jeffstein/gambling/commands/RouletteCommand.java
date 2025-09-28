package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.RouletteGUI;
import org.jeffstein.gambling.games.RouletteBettingGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RouletteCommand implements CommandExecutor {

    private final Gambling plugin;

    public RouletteCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        org.jeffstein.gambling.listeners.RouletteListener listener = org.jeffstein.gambling.listeners.RouletteListener.getInstance();
        if (listener == null) {
            player.sendMessage("Roulette is not available right now.");
            return true;
        }

        if (args.length == 0) {
            RouletteBettingGUI gui = new RouletteBettingGUI(plugin, player);
            gui.openInventory();
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "roll":
            case "spin":
                listener.spinCommand(player);
                return true;
            case "status":
                listener.statusCommand(player);
                return true;
            case "clear":
                listener.clearBetsCommand(player, true);
                return true;
            case "table":
                RouletteBettingGUI gui = new RouletteBettingGUI(plugin, player);
                gui.openInventory();
                return true;
            default:
                break;
        }

        try {
            double amount = Double.parseDouble(args[0]);
            if (args.length < 2) {
                player.sendMessage("Usage: /roulette <amount> <bet> [roll]");
                return true;
            }

            String betType = args[1];
            listener.placeBetCommand(player, betType, amount);

            if (args.length >= 3 && args[2].equalsIgnoreCase("roll")) {
                listener.spinCommand(player);
            }
        } catch (NumberFormatException ignored) {
            player.sendMessage("Usage: /roulette roll | /roulette status | /roulette <amount> <bet> [roll]");
        }
        return true;
    }
}
