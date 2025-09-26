package org.jeffstein.gambling.commands;

import org.jeffstein.gambling.games.BlackjackGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlackjackTabCompleter implements TabCompleter {

    private final Map<UUID, BlackjackGame> games;

    public BlackjackTabCompleter(Map<UUID, BlackjackGame> games) {
        this.games = games;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (games.containsKey(player.getUniqueId())) {
                completions.add("hit");
                completions.add("stand");
            } else {
                completions.add("100");
                completions.add("1000");
                completions.add("10000");
            }
        }

        return completions;
    }
}
