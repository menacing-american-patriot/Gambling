package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.listeners.CrapsListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrapsCommand implements CommandExecutor {

    public CrapsCommand(org.jeffstein.map.gambling.Gambling plugin) {}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("craps")) {
            player.sendMessage("Craps is currently disabled.");
            return true;
        }

        CrapsListener listener = CrapsListener.getInstance();

        if (listener == null) {
            player.sendMessage("Craps is not available right now.");
            return true;
        }

        if (args.length == 0) {
            listener.openTable(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "roll":
                listener.rollCommand(player);
                return true;
            case "status":
                listener.statusCommand(player);
                return true;
            case "reset":
            case "clear":
                listener.resetCommand(player);
                return true;
            case "table":
                listener.openTable(player);
                return true;
            default:
                break;
        }

        try {
            double amount = Double.parseDouble(args[0]);
            if (args.length < 2) {
                player.sendMessage("Usage: /craps <amount> <pass|dontpass> [roll]");
                return true;
            }

            String betType = args[1].toLowerCase();
            boolean passLine;
            if (betType.equals("pass")) {
                passLine = true;
            } else if (betType.equals("dontpass") || betType.equals("don'tpass") || betType.equals("dont")) {
                passLine = false;
            } else {
                player.sendMessage("Bet type must be pass or dontpass.");
                return true;
            }

            listener.placeBetCommand(player, amount, passLine);

            if (args.length >= 3 && args[2].equalsIgnoreCase("roll")) {
                listener.rollCommand(player);
            }
        } catch (NumberFormatException ignored) {
            player.sendMessage("Usage: /craps roll | /craps status | /craps <amount> <pass|dontpass> [roll]");
        }
        return true;
    }
}
