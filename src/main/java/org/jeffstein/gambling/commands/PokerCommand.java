package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.PokerGame;
import org.jeffstein.gambling.games.PokerManager;
import org.jeffstein.gambling.games.PokerPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PokerCommand implements CommandExecutor {

    private final Gambling plugin;

    public PokerCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("poker")) {
            player.sendMessage(ChatColor.RED + "[POKER] This game is currently disabled.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /poker <join|leave|bet|check|fold>");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("join")) {
            PokerGame game = Gambling.getPokerManager().getGame(player);
            if (game != null) {
                player.sendMessage("You are already in a game.");
                return true;
            }

            game = Gambling.getPokerManager().getGames().stream().filter(g -> g.getGameState() == PokerGame.GameState.WAITING).findFirst().orElse(null);
            if (game == null) {
                game = Gambling.getPokerManager().createGame();
            }

            game.addPlayer(new PokerPlayer(player));
            player.sendMessage(ChatColor.GREEN + "[POKER] You joined a poker table! Players: " + game.getPlayers().size() + "/6");

        } else if (subCommand.equals("leave")) {
            PokerGame game = Gambling.getPokerManager().getGame(player);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "[POKER] You are not in a poker game.");
                return true;
            }

            game.removePlayer(game.getPlayers().stream().filter(p -> p.getPlayer().equals(player)).findFirst().get());
            player.sendMessage(ChatColor.YELLOW + "[POKER] You left the poker game.");

            if (game.getPlayers().isEmpty()) {
                Gambling.getPokerManager().removeGame(game);
            }
        } else if (subCommand.equals("bet")) {
            PokerGame game = Gambling.getPokerManager().getGame(player);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "[POKER] You are not in a poker game.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "[POKER] Usage: /poker bet <amount>");
                return true;
            }
            try {
                double amount = Double.parseDouble(args[1]);
                game.bet(player, amount);
                player.sendMessage(ChatColor.GREEN + "[POKER] You bet " + Gambling.getEconomy().format(amount));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "[POKER] Invalid bet amount!");
            }
        } else if (subCommand.equals("check")) {
            PokerGame game = Gambling.getPokerManager().getGame(player);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "[POKER] You are not in a poker game.");
                return true;
            }
            game.check(player);
            player.sendMessage(ChatColor.YELLOW + "[POKER] You checked.");
        } else if (subCommand.equals("fold")) {
            PokerGame game = Gambling.getPokerManager().getGame(player);
            if (game == null) {
                player.sendMessage(ChatColor.RED + "[POKER] You are not in a poker game.");
                return true;
            }
            game.fold(player);
            player.sendMessage(ChatColor.RED + "[POKER] You folded.");
        } else {
            player.sendMessage("Usage: /poker <join|leave|bet|check|fold>");
        }

        return true;
    }
}
