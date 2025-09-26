package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.games.SlotMachine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SlotsCommand implements CommandExecutor {

    private final Gambling plugin;

    public SlotsCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        SlotMachine slotMachine = new SlotMachine(plugin, player);
        slotMachine.openInventory();

        return true;
    }
}