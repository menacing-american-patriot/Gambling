package org.jeffstein.map.gambling.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KenoGame {

    private final List<Integer> playerNumbers;
    private final List<Integer> drawnNumbers = new ArrayList<>();
    private int matches = 0;

    public KenoGame(List<Integer> playerNumbers) {
        this.playerNumbers = playerNumbers;
    }

    public void drawNumbers() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            numbers.add(i);
        }
        Collections.shuffle(numbers);
        for (int i = 0; i < 20; i++) {
            drawnNumbers.add(numbers.get(i));
        }

        for (int playerNum : playerNumbers) {
            if (drawnNumbers.contains(playerNum)) {
                matches++;
            }
        }
    }

    public List<Integer> getDrawnNumbers() {
        return drawnNumbers;
    }

    public int getMatches() {
        return matches;
    }

    public double getPayout(double betAmount) {
        int numbersPicked = playerNumbers.size();
        // Payout table - can be adjusted
        if (numbersPicked >= 4) {
            if (matches == 4) return betAmount * 2;
            if (matches == 5) return betAmount * 10;
            if (matches == 6) return betAmount * 50;
            if (matches == 7) return betAmount * 100;
            if (matches == 8) return betAmount * 500;
            if (matches == 9) return betAmount * 1000;
            if (matches == 10) return betAmount * 5000;
        }
        return 0;
    }
}
