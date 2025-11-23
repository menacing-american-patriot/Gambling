package org.jeffstein.map.gambling.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CrapsGame {

    public enum GameState {
        COME_OUT,
        POINT
    }

    public static class RollOutcome {
        private final boolean passLineWins;
        private final boolean dontPassWins;
        private final boolean passLineLoses;
        private final boolean dontPassLoses;
        private final boolean dontPassPush;
        private final boolean continueRolling;
        private final boolean pointEstablished;
        private final boolean pointCleared;
        private final int pointValue;
        private final String message;

        public RollOutcome(boolean passLineWins,
                           boolean dontPassWins,
                           boolean passLineLoses,
                           boolean dontPassLoses,
                           boolean dontPassPush,
                           boolean continueRolling,
                           boolean pointEstablished,
                           boolean pointCleared,
                           int pointValue,
                           String message) {
            this.passLineWins = passLineWins;
            this.dontPassWins = dontPassWins;
            this.passLineLoses = passLineLoses;
            this.dontPassLoses = dontPassLoses;
            this.dontPassPush = dontPassPush;
            this.continueRolling = continueRolling;
            this.pointEstablished = pointEstablished;
            this.pointCleared = pointCleared;
            this.pointValue = pointValue;
            this.message = message;
        }

        public boolean passLineWins() { return passLineWins; }
        public boolean dontPassWins() { return dontPassWins; }
        public boolean passLineLoses() { return passLineLoses; }
        public boolean dontPassLoses() { return dontPassLoses; }
        public boolean dontPassPush() { return dontPassPush; }
        public boolean continueRolling() { return continueRolling; }
        public boolean pointEstablished() { return pointEstablished; }
        public boolean pointCleared() { return pointCleared; }
        public int pointValue() { return pointValue; }
        public String message() { return message; }
    }

    private double passLineBet;
    private double dontPassBet;
    private int point;
    private GameState state = GameState.COME_OUT;
    private int[] lastRoll;
    private final List<Integer> recentTotals = new ArrayList<>();

    public int[] rollDice() {
        int dieOne = ThreadLocalRandom.current().nextInt(1, 7);
        int dieTwo = ThreadLocalRandom.current().nextInt(1, 7);
        lastRoll = new int[]{dieOne, dieTwo};
        recordTotal(dieOne + dieTwo);
        return lastRoll;
    }

    private void recordTotal(int total) {
        recentTotals.add(0, total);
        if (recentTotals.size() > 6) {
            recentTotals.remove(recentTotals.size() - 1);
        }
    }

    public RollOutcome resolveRoll(int total) {
        if (state == GameState.COME_OUT) {
            if (total == 7 || total == 11) {
                clearPoint();
                return new RollOutcome(passLineBet > 0, false, false, dontPassBet > 0, false,
                        false, false, true, 0, "Natural " + total + "! Pass Line wins.");
            }

            if (total == 2 || total == 3) {
                clearPoint();
                return new RollOutcome(false, dontPassBet > 0, passLineBet > 0, false, false,
                        false, false, true, 0, "Craps " + total + "! Don't Pass wins.");
            }

            if (total == 12) {
                clearPoint();
                return new RollOutcome(false, false, passLineBet > 0, false, dontPassBet > 0,
                        false, false, true, 0, "Craps 12! Don't Pass pushes.");
            }

            point = total;
            state = GameState.POINT;
            return new RollOutcome(false, false, false, false, false,
                    true, true, false, point, "Point established at " + point + ".");
        }

        if (total == point) {
            int resolvedPoint = point;
            clearPoint();
            return new RollOutcome(passLineBet > 0, false, false, dontPassBet > 0, false,
                    false, false, true, resolvedPoint, "Point hit! Pass Line wins.");
        }

        if (total == 7) {
            int resolvedPoint = point;
            clearPoint();
            return new RollOutcome(false, dontPassBet > 0, passLineBet > 0, false, false,
                    false, false, true, resolvedPoint, "Seven out! Don't Pass wins.");
        }

        return new RollOutcome(false, false, false, false, false,
                true, false, false, point, "Rolling for point " + point + ".");
    }

    private void clearPoint() {
        point = 0;
        state = GameState.COME_OUT;
    }

    public void addPassLineBet(double amount) {
        passLineBet += amount;
    }

    public void addDontPassBet(double amount) {
        dontPassBet += amount;
    }

    public double getPassLineBet() {
        return passLineBet;
    }

    public double getDontPassBet() {
        return dontPassBet;
    }

    public void clearPassLineBet() {
        passLineBet = 0;
    }

    public void clearDontPassBet() {
        dontPassBet = 0;
    }

    public int getPoint() {
        return point;
    }

    public void resetForNewRound() {
        passLineBet = 0;
        dontPassBet = 0;
        point = 0;
        state = GameState.COME_OUT;
        lastRoll = null;
        recentTotals.clear();
    }

    public GameState getState() {
        return state;
    }

    public int[] getLastRoll() {
        return lastRoll;
    }

    public List<Integer> getRecentTotals() {
        return Collections.unmodifiableList(recentTotals);
    }
}
