package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.SlotMachine;
import org.bukkit.ChatColor;
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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!Gambling.getGamblingConfig().isGameEnabled("slots")) {
            player.sendMessage(ChatColor.RED + "[SLOTS] This game is currently disabled.");
            return true;
        }

        // Optional fast-path: /slots <bet>
        if (args.length >= 1) {
            try {
                double bet = Double.parseDouble(args[0]);
                SlotMachine slotMachine = new SlotMachine(plugin, player, bet);
                slotMachine.openInventory();
                // Auto-spin once with the provided bet
                slotMachine.spin();
                return true;
            } catch (NumberFormatException ignored) {
                // fall back to GUI
            }
        }

        SlotMachine slotMachine = new SlotMachine(plugin, player);
        slotMachine.openInventory();

        return true;
    }
}