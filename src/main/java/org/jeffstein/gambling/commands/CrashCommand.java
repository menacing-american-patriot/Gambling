package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.Gambling;
import org.jeffstein.gambling.games.CrashGame;
import org.jeffstein.gambling.games.CrashGUI;
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
        
        // Create GUI for this player connected to the global game
        CrashGUI crashGUI = new CrashGUI(plugin, player, globalCrashGame);
        crashGUI.openInventory();
        
        return true;
    }
    
    public static CrashGame getGlobalCrashGame() {
        return globalCrashGame;
    }
}
