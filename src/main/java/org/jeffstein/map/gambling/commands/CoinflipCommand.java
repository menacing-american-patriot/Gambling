package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
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

    // CASINO HOUSE EDGE SETTINGS
    private static final double WIN_CHANCE = 0.475;  // 47.5% chance to win (5% house edge)
    private static final double PAYOUT_MULTIPLIER = 1.95; // 1.95x payout instead of 2x

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Command can only be run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("coinflip")) {
            player.sendMessage(ChatColor.RED + "[COINFLIP] This game is currently disabled.");
            return true;
        }

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

        // 5. Determine the outcome with HOUSE EDGE
        // Casino gets 5% house edge: 47.5% win chance + 1.95x payout = profitable
        double randomValue = ThreadLocalRandom.current().nextDouble();
        boolean won = randomValue < WIN_CHANCE;

        // 6. Handle the result
        if (won) {
            // Payout is 1.95x instead of 2x to maintain house edge
            double winnings = betAmount * PAYOUT_MULTIPLIER;
            economy.depositPlayer(player, winnings);
            double profit = winnings - betAmount;
            Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);

            // Add chat message for visibility
            player.sendMessage(ChatColor.GREEN + "[COINFLIP] " + ChatColor.BOLD + "YOU WON! " +
                    ChatColor.GOLD + "+" + economy.format(profit) + " (1.95x payout)");

            // Fun effects for winning
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), betAmount);

            // Add chat message for visibility
            player.sendMessage(ChatColor.RED + "[COINFLIP] " + ChatColor.BOLD + "YOU LOST! " +
                    ChatColor.GRAY + "-" + economy.format(betAmount));

            // Effects for losing
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        return true;
    }
}
