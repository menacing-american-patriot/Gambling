package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class CoinflipCommand implements CommandExecutor {

    // Get the Vault economy API instance from your main class
    private final Economy economy = Gambling.getEconomy();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Command can only be run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        // 2. Validate the command arguments
        if (args.length!= 1) {
            player.sendMessage(ChatColor.RED + "Usage: /coinflip <amount>");
            return true;
        }

        double betAmount;
        try {
            betAmount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Please enter a valid number.");
            return true;
        }

        if (betAmount <= 0) {
            player.sendMessage(ChatColor.RED + "You must bet a positive amount.");
            return true;
        }

        // 3. Check player's balance using Vault [1, 2]
        if (economy.getBalance(player) < betAmount) {
            player.sendMessage(ChatColor.RED + "You do not have enough money for that bet.");
            return true;
        }

        // 4. Withdraw the bet amount
        EconomyResponse withdrawal = economy.withdrawPlayer(player, betAmount);
        if (!withdrawal.transactionSuccess()) {
            player.sendMessage(ChatColor.RED + "An error occurred: " + withdrawal.errorMessage);
            return true;
        }

        // 5. Determine the outcome (50/50 chance) [3, 4]
        boolean won = ThreadLocalRandom.current().nextBoolean();

        // 6. Handle the result
        if (won) {
            double winnings = betAmount * 2;
            economy.depositPlayer(player, winnings);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), betAmount);
            player.sendMessage(ChatColor.GOLD + "Coinflip... " + ChatColor.GREEN + "You won! " +
                    ChatColor.GOLD + "You received " + economy.format(winnings) + ".");

            // Fun effects for winning [5, 6]
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), betAmount);
            player.sendMessage(ChatColor.GOLD + "Coinflip... " + ChatColor.RED + "You lost! " +
                    ChatColor.GOLD + "You lost " + economy.format(betAmount) + ".");

            // Effects for losing
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        return true;
    }
}
