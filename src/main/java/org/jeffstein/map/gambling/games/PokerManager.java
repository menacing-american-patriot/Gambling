package org.jeffstein.map.gambling.games;

import org.jeffstein.map.gambling.Gambling;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PokerManager {

    private static PokerManager instance;
    private final Gambling plugin;
    private final List<PokerGame> games = new ArrayList<>();

    public PokerManager(Gambling plugin) {
        this.plugin = plugin;
    }

    public static PokerManager getInstance(Gambling plugin) {
        if (instance == null) {
            instance = new PokerManager(plugin);
        }
        return instance;
    }

    public PokerGame createGame() {
        PokerGame game = new PokerGame(plugin);
        games.add(game);
        return game;
    }

    public void removeGame(PokerGame game) {
        games.remove(game);
    }

    public PokerGame getGame(Player player) {
        for (PokerGame game : games) {
            for (PokerPlayer pokerPlayer : game.getPlayers()) {
                if (pokerPlayer.getPlayer().equals(player)) {
                    return game;
                }
            }
        }
        return null;
    }

    public List<PokerGame> getGames() {
        return games;
    }
}
