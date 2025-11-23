package org.jeffstein.map.gambling.commands;

import org.jeffstein.map.gambling.Gambling;
import org.jeffstein.map.gambling.games.KenoGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KenoCommand implements CommandExecutor {

    private final Gambling plugin;

    public KenoCommand(Gambling plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!Gambling.getGamblingConfig().isGameEnabled("keno")) {
            player.sendMessage("Keno is currently disabled.");
            return true;
        }

        // Optional fast-path: /keno <amount>
        if (args.length >= 1) {
            try {
                double amount = Double.parseDouble(args[0]);
                org.jeffstein.map.gambling.listeners.KenoListener listener = org.jeffstein.map.gambling.listeners.KenoListener.getInstance();
                if (listener != null) listener.setBetForPlayer(player, amount);
            } catch (NumberFormatException ignored) {}
        }

        KenoGUI gui = new KenoGUI(plugin, player);
        gui.openInventory();
        return true;
    }
}
