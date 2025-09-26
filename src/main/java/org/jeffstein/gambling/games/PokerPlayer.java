package org.jeffstein.gambling.games;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PokerPlayer {

    private final Player player;
    private final List<Card> hand = new ArrayList<>();
    private double bet;
    private boolean folded;

    public PokerPlayer(Player player) {
        this.player = player;
        this.bet = 0;
        this.folded = false;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
    }

    public double getBet() {
        return bet;
    }

    public void setBet(double bet) {
        this.bet = bet;
    }

    public boolean hasFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }
}
