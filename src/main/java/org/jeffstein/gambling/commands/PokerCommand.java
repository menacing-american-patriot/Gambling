package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.PokerGame;
import org.jeffstein.gambling.games.PokerManager;
import org.jeffstein.gambling.games.PokerPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PokerCommand implements CommandExecutor {

    private final Gambling plugin;
    private final PokerManager pokerManager;

    public PokerCommand(Gambling plugin) {
        this.plugin = plugin;
        this.pokerManager = PokerManager.getInstance(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /poker <join|leave|bet|check|fold>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("join")) {
            PokerGame game = pokerManager.getGame(player);
            if (game != null) {
                player.sendMessage("You are already in a game.");
                return true;
            }

            game = pokerManager.getGames().stream().filter(g -> g.getGameState() == PokerGame.GameState.WAITING).findFirst().orElse(null);
            if (game == null) {
                game = pokerManager.createGame();
            }

            game.addPlayer(new PokerPlayer(player));
            player.sendMessage("You have joined a poker game.");

        } else if (subCommand.equals("leave")) {
            PokerGame game = pokerManager.getGame(player);
            if (game == null) {
                player.sendMessage("You are not in a game.");
                return true;
            }

            game.removePlayer(game.getPlayers().stream().filter(p -> p.getPlayer().equals(player)).findFirst().get());
            player.sendMessage("You have left the poker game.");

            if (game.getPlayers().isEmpty()) {
                pokerManager.removeGame(game);
            }
        } else if (subCommand.equals("bet")) {
            PokerGame game = pokerManager.getGame(player);
            if (game == null) {
                player.sendMessage("You are not in a game.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("Usage: /poker bet <amount>");
                return true;
            }
            double amount = Double.parseDouble(args[1]);
            game.bet(player, amount);
        } else if (subCommand.equals("check")) {
            PokerGame game = pokerManager.getGame(player);
            if (game == null) {
                player.sendMessage("You are not in a game.");
                return true;
            }
            game.check(player);
        } else if (subCommand.equals("fold")) {
            PokerGame game = pokerManager.getGame(player);
            if (game == null) {
                player.sendMessage("You are not in a game.");
                return true;
            }
            game.fold(player);
        } else {
            player.sendMessage("Usage: /poker <join|leave|bet|check|fold>");
        }

        return true;
    }
}
