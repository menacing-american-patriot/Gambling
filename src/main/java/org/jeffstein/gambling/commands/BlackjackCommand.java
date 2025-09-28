package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.BlackjackGame;
import org.bukkit.ChatColor;
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
            if (games.containsKey(playerId)) {
                games.get(playerId).status();
            } else {
                player.sendMessage(ChatColor.YELLOW + "Usage: /blackjack <bet> | /blackjack hit | stand | double | status");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "hit":
                if (!games.containsKey(playerId)) {
                    player.sendMessage(ChatColor.RED + "No active blackjack game. Use /blackjack <bet> to start one.");
                    return true;
                }
                games.get(playerId).hit();
                return true;
            case "stand":
                if (!games.containsKey(playerId)) {
                    player.sendMessage(ChatColor.RED + "No active blackjack game. Use /blackjack <bet> to start one.");
                    return true;
                }
                games.get(playerId).stand();
                return true;
            case "double":
                if (!games.containsKey(playerId)) {
                    player.sendMessage(ChatColor.RED + "No active blackjack game. Use /blackjack <bet> to start one.");
                    return true;
                }
                games.get(playerId).doubleDown();
                return true;
            case "status":
                if (!games.containsKey(playerId)) {
                    player.sendMessage(ChatColor.RED + "No active blackjack game. Use /blackjack <bet> to start one.");
                    return true;
                }
                games.get(playerId).status();
                return true;
            case "help":
                player.sendMessage(ChatColor.YELLOW + "Blackjack commands:");
                player.sendMessage(ChatColor.GRAY + " /blackjack <bet> " + ChatColor.DARK_GRAY + "- start a new game");
                player.sendMessage(ChatColor.GRAY + " /blackjack hit " + ChatColor.DARK_GRAY + "- take another card");
                player.sendMessage(ChatColor.GRAY + " /blackjack stand " + ChatColor.DARK_GRAY + "- hold your total");
                player.sendMessage(ChatColor.GRAY + " /blackjack double " + ChatColor.DARK_GRAY + "- double wager, draw one card");
                player.sendMessage(ChatColor.GRAY + " /blackjack status " + ChatColor.DARK_GRAY + "- show table state");
                return true;
        }

        double bet;
        try {
            bet = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid bet amount.");
            return true;
        }

        if (games.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "You already have a game in progress. Use /blackjack hit or /blackjack stand to continue.");
            return true;
        }

        BlackjackGame game = new BlackjackGame(plugin, player, bet, games);
        games.put(playerId, game);
        boolean started = game.start();
        if (!started) {
            games.remove(playerId);
        }
            if (!games.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "Failed to start blackjack game.");
        }
        return true;
    }

    public Map<UUID, BlackjackGame> getGames() {
        return games;
    }
}
