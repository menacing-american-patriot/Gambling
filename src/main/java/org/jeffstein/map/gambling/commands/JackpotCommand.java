package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JackpotCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        double jackpot = Gambling.getJackpot().getJackpot();
        sender.sendMessage(ChatColor.GOLD + "The current jackpot is: " + Gambling.getEconomy().format(jackpot));
        return true;
    }
}
