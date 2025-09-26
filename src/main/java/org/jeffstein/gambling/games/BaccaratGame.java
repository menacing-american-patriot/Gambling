package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BaccaratGame {

    private final Gambling plugin;
    private final Player player;
    private final Economy economy;
    private final Deck deck;
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> bankerHand = new ArrayList<>();

    public BaccaratGame(Gambling plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.economy = Gambling.getEconomy();
        this.deck = new Deck();
    }

    public void deal() {
        deck.shuffle();
        playerHand.add(deck.deal());
        bankerHand.add(deck.deal());
        playerHand.add(deck.deal());
        bankerHand.add(deck.deal());
    }

    public int getHandValue(List<Card> hand) {
        int value = 0;
        for (Card card : hand) {
            if (card.getRank().getValue() >= 10) {
                value += 0;
            } else {
                value += card.getRank().getValue();
            }
        }
        return value % 10;
    }

    public List<Card> getPlayerHand() {
        return playerHand;
    }

    public List<Card> getBankerHand() {
        return bankerHand;
    }
}
