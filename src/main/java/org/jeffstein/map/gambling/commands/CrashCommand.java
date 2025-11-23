package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.CrashGame;
import org.jeffstein.map.gambling.games.CrashGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrashCommand implements CommandExecutor {

    private final Gambling plugin;
    private static CrashGame globalCrashGame; // Shared game instance for all players

    public CrashCommand(Gambling plugin) {
        this.plugin = plugin;
        if (globalCrashGame == null) {
            globalCrashGame = new CrashGame(plugin);
            // Start the first round
            globalCrashGame.initializeFirstRound();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("crash")) {
            player.sendMessage(ChatColor.RED + "[CRASH] This game is currently disabled.");
            return true;
        }
        CrashGame game = getGlobalCrashGame();

        if (args.length == 0) {
            new CrashGUI(plugin, player, game).openInventory();
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "bet":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /crash bet <amount> [auto]");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[1]);
                    boolean placed = game.placeBet(player, amount);
                    if (placed && args.length >= 3) {
                        try {
                            double auto = Double.parseDouble(args[2]);
                            game.setAutoCashout(player, auto);
                        } catch (NumberFormatException ignored) {
                            player.sendMessage(ChatColor.RED + "[CRASH] Auto value must be a number.");
                        }
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "[CRASH] Bet amount must be numeric.");
                }
                return true;
            case "auto":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /crash auto <multiplier>");
                    return true;
                }
                try {
                    double autoValue = Double.parseDouble(args[1]);
                    if (!game.setAutoCashout(player, autoValue)) {
                        player.sendMessage(ChatColor.RED + "[CRASH] Set a bet first before auto-cashout.");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "[CRASH] Auto multiplier must be numeric.");
                }
                return true;
            case "cashout":
            case "cash":
                if (!game.cashOut(player)) {
                    player.sendMessage(ChatColor.RED + "[CRASH] Unable to cash out right now.");
                }
                return true;
            case "status":
                sendStatus(player, game);
                return true;
            case "gui":
            case "menu":
                new CrashGUI(plugin, player, game).openInventory();
                return true;
            case "help":
                sendHelp(player);
                return true;
            default:
                break;
        }

        // Fast path: /crash <bet> [auto]
        try {
            double bet = Double.parseDouble(args[0]);
            boolean placed = game.placeBet(player, bet);
            if (placed && args.length >= 2) {
                try {
                    double auto = Double.parseDouble(args[1]);
                    game.setAutoCashout(player, auto);
                } catch (NumberFormatException ignored) {
                    player.sendMessage(ChatColor.RED + "[CRASH] Auto value must be a number.");
                }
            }
        } catch (NumberFormatException ignored) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /crash bet <amount> [auto] | /crash cashout | /crash status");
        }

        return true;
    }

    public static CrashGame getGlobalCrashGame() {
        return globalCrashGame;
    }

    private void sendStatus(Player player, CrashGame game) {
        boolean betting = game.isBettingPhase();
        boolean running = game.isGameRunning();
        player.sendMessage(ChatColor.YELLOW + "[CRASH] " + (betting ? "Betting phase" : running ? "Game running" : "Waiting"));
        if (running) {
            player.sendMessage(ChatColor.YELLOW + "[CRASH] Current multiplier: " + String.format("%.2fx", game.getCurrentMultiplier()));
        }

        Double bet = game.getPlayerBets().get(player.getUniqueId());
        if (bet != null) {
            player.sendMessage(ChatColor.GREEN + "[CRASH] Your bet: " + Gambling.getEconomy().format(bet));
            Double auto = game.getPlayerAutoCashout(player.getUniqueId());
            if (auto != null) {
                player.sendMessage(ChatColor.GREEN + "[CRASH] Auto cashout: " + String.format("%.2fx", auto));
            }
            if (game.getCashedOutPlayers().contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "[CRASH] You have already cashed out this round.");
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "[CRASH] No active bet.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "[CRASH] Command help:");
        player.sendMessage(ChatColor.GRAY + "/crash bet <amount> [auto]" + ChatColor.DARK_GRAY + " - place a bet");
        player.sendMessage(ChatColor.GRAY + "/crash auto <multiplier>" + ChatColor.DARK_GRAY + " - set auto cashout");
        player.sendMessage(ChatColor.GRAY + "/crash cashout" + ChatColor.DARK_GRAY + " - cash out immediately");
        player.sendMessage(ChatColor.GRAY + "/crash status" + ChatColor.DARK_GRAY + " - view current round");
        player.sendMessage(ChatColor.GRAY + "/crash" + ChatColor.DARK_GRAY + " - open the GUI");
    }
}
