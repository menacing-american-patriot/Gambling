package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.PlinkoGame;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlinkoCommand implements CommandExecutor {

    private final Gambling plugin;

    public PlinkoCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("plinko")) {
            player.sendMessage(ChatColor.RED + "[PLINKO] This game is currently disabled.");
            return true;
        }
        PlinkoGame plinkoGame = new PlinkoGame(plugin, player);

        if (args.length == 0) {
            plinkoGame.openInventory();
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "gui":
            case "menu":
                plinkoGame.openInventory();
                return true;
            case "drop":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /plinko drop <amount> <column 1-9>");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    int column = Integer.parseInt(args[2]);
                    plinkoGame.dropBallCommand(amount, column);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "[PLINKO] Usage: /plinko drop <amount> <column>");
                }
                return true;
            default:
                break;
        }

        try {
            double bet = Double.parseDouble(args[0]);
            plinkoGame.setBetAmount(bet);
            plinkoGame.openInventory();
            if (args.length >= 2) {
                try {
                    int pos = Integer.parseInt(args[1]);
                    plinkoGame.dropBall(pos - 1);
                } catch (NumberFormatException ignored) {
                    player.sendMessage(ChatColor.RED + "[PLINKO] Column must be a number 1-9.");
                }
            }
        } catch (NumberFormatException ignored) {
            plinkoGame.openInventory();
        }

        return true;
    }
}
