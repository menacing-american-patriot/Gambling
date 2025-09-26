package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RouletteGame {

    private final Gambling plugin;
    private final Player player;
    private final Map<String, Double> bets = new HashMap<>();
    private int winningNumber;

    public RouletteGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void spin() {
        this.winningNumber = new Random().nextInt(37);
    }

    public int getWinningNumber() {
        return winningNumber;
    }

    public void placeBet(String betType, double amount) {
        bets.put(betType, amount);
    }

    public Map<String, Double> getBets() {
        return bets;
    }
}
