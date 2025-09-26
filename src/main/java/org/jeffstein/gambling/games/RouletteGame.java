package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RouletteGame {

    private final Gambling plugin;
    private final Player player;
    private final Map<String, Double> bets = new HashMap<>();
    private int winningNumber;

    private static final List<Integer> RED_NUMBERS = Arrays.asList(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    private static final List<Integer> BLACK_NUMBERS = Arrays.asList(2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35);

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

    public boolean isRed(int number) {
        return RED_NUMBERS.contains(number);
    }

    public boolean isBlack(int number) {
        return BLACK_NUMBERS.contains(number);
    }
}
