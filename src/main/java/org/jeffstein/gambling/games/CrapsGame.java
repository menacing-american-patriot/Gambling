package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CrapsGame {

    private final Gambling plugin;
    private final Player player;
    private final Map<String, Double> bets = new HashMap<>();
    private int point;
    private GameState gameState;

    public enum GameState {
        COME_OUT,
        POINT
    }

    public CrapsGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.gameState = GameState.COME_OUT;
    }

    public int[] rollDice() {
        Random random = new Random();
        int[] dice = new int[2];
        dice[0] = random.nextInt(6) + 1;
        dice[1] = random.nextInt(6) + 1;
        return dice;
    }

    public void placeBet(String betType, double amount) {
        bets.put(betType, amount);
    }

    public Map<String, Double> getBets() {
        return bets;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
}
