package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.WheelOfFortuneGame;
import org.bukkit.Bukkit;
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
        if (!Gambling.getGamblingConfig().isGameEnabled("wheel")) {
            player.sendMessage("Wheel of Fortune is currently disabled.");
            return true;
        }

        WheelOfFortuneGame wheelGame = new WheelOfFortuneGame(plugin, player);

        if (args.length >= 2) {
            try {
                double amount = Double.parseDouble(args[0]);
                String betType = parseBetType(args[1]);
                if (betType == null) {
                    player.sendMessage("Usage: /wheel <amount> <numbers|lose|jackpot>");
                    wheelGame.openInventory();
                    return true;
                }

                wheelGame.openInventory();
                if (wheelGame.placeBet(betType, amount)) {
                    Bukkit.getScheduler().runTaskLater(plugin, wheelGame::spinWheel, 1L);
                    return true;
                }
            } catch (NumberFormatException ignored) {
                player.sendMessage("Bet amount must be numeric.");
                wheelGame.openInventory();
                return true;
            }
        } else if (args.length == 1) {
            try {
                double amount = Double.parseDouble(args[0]);
                wheelGame.openInventory();
                if (wheelGame.placeBet("Numbers", amount)) {
                    Bukkit.getScheduler().runTaskLater(plugin, wheelGame::spinWheel, 1L);
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // fall-through to GUI open below
            }
        }

        wheelGame.openInventory();

        return true;
    }

    private String parseBetType(String raw) {
        if (raw == null) {
            return null;
        }
        String lower = raw.toLowerCase();
        switch (lower) {
            case "numbers":
            case "number":
            case "num":
                return "Numbers";
            case "lose":
                return "LOSE";
            case "jackpot":
                return "JACKPOT";
            default:
                if (lower.endsWith("x")) {
                    return "Numbers";
                }
                return null;
        }
    }
}
