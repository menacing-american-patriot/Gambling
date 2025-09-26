package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BlackjackGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlackjackCommand implements CommandExecutor {

    private final Gambling plugin;
    private final Map<UUID, BlackjackGame> games = new HashMap<>();

    public BlackjackCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (args.length == 0) {
            // Start a new game
            if (games.containsKey(playerId)) {
                player.sendMessage("You already have a game in progress.");
                return true;
            }
            player.sendMessage("Usage: /blackjack <bet>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("hit")) {
            if (!games.containsKey(playerId)) {
                player.sendMessage("You don't have a game in progress. Use /blackjack <bet> to start one.");
                return true;
            }
            games.get(playerId).hit();
        } else if (subCommand.equals("stand")) {
            if (!games.containsKey(playerId)) {
                player.sendMessage("You don't have a game in progress. Use /blackjack <bet> to start one.");
                return true;
            }
            games.get(playerId).stand();
        } else {
            // Assume the argument is a bet amount
            double bet;
            try {
                bet = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid bet amount.");
                return true;
            }

            if (games.containsKey(playerId)) {
                player.sendMessage("You already have a game in progress. Use /blackjack hit or /blackjack stand to continue.");
                return true;
            }

            BlackjackGame game = new BlackjackGame(plugin, player, bet);
            games.put(playerId, game);
            game.start();
        }
        return true;
    }
}
