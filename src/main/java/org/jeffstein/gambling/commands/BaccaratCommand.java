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
        org.jeffstein.gambling.listeners.BaccaratListener listener = org.jeffstein.gambling.listeners.BaccaratListener.getInstance();

        if (listener == null) {
            player.sendMessage("Baccarat is not available right now.");
            return true;
        }

        if (args.length == 0) {
            new BaccaratGUI(plugin, player).openInventory();
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "bet":
            case "play":
                if (args.length < 3) {
                    player.sendMessage("Usage: /baccarat bet <amount> <player|banker|tie>");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    listener.playCommand(player, args[2], amount);
                } catch (NumberFormatException e) {
                    player.sendMessage("Bet amount must be numeric.");
                }
                return true;
            case "gui":
            case "menu":
                new BaccaratGUI(plugin, player).openInventory();
                return true;
            default:
                break;
        }

        try {
            double amount = Double.parseDouble(args[0]);
            if (args.length < 2) {
                player.sendMessage("Usage: /baccarat <amount> <player|banker|tie>");
                return true;
            }
            listener.playCommand(player, args[1], amount);
        } catch (NumberFormatException ignored) {
            // Fallback to GUI if command pattern not matched
            new BaccaratGUI(plugin, player).openInventory();
        }
        return true;
    }
}
