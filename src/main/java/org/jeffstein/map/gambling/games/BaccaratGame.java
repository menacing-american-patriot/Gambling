package org.jeffstein.map.gambling.games;

import java.util.ArrayList;
import java.util.List;

public class BaccaratGame {

    public enum Winner {
        PLAYER,
        BANKER,
        TIE
    }

    private final Deck deck = new Deck();
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> bankerHand = new ArrayList<>();

    private Card playerThirdCard;
    private Card bankerThirdCard;
    private boolean naturalRound;
    private Winner winner;

    public void beginRound() {
        playerHand.clear();
        bankerHand.clear();
        playerThirdCard = null;
        bankerThirdCard = null;
        naturalRound = false;
        winner = null;
        deck.shuffle();
    }

    public Card dealPlayerCard() {
        Card card = deck.deal();
        if (card != null) {
            playerHand.add(card);
        }
        return card;
    }

    public Card dealBankerCard() {
        Card card = deck.deal();
        if (card != null) {
            bankerHand.add(card);
        }
        return card;
    }

    public void evaluateNaturals() {
        int playerValue = getPlayerValue();
        int bankerValue = getBankerValue();
        naturalRound = playerValue >= 8 || bankerValue >= 8;
    }

    public boolean isNaturalRound() {
        return naturalRound;
    }

    public boolean shouldPlayerDrawThirdCard() {
        if (naturalRound) {
            return false;
        }
        return getPlayerValue() <= 5;
    }

    public Card drawPlayerThirdCard() {
        if (playerThirdCard != null) {
            return playerThirdCard;
        }
        playerThirdCard = dealPlayerCard();
        return playerThirdCard;
    }

    public boolean shouldBankerDrawThirdCard(Integer playerThirdValue) {
        if (naturalRound) {
            return false;
        }

        int bankerValue = getBankerValue();

        if (playerThirdValue == null) {
            return bankerValue <= 5;
        }

        if (bankerValue <= 2) {
            return true;
        }

        if (bankerValue == 3) {
            return playerThirdValue != 8;
        }

        if (bankerValue == 4) {
            return playerThirdValue >= 2 && playerThirdValue <= 7;
        }

        if (bankerValue == 5) {
            return playerThirdValue >= 4 && playerThirdValue <= 7;
        }

        if (bankerValue == 6) {
            return playerThirdValue == 6 || playerThirdValue == 7;
        }

        return false;
    }

    public Card drawBankerThirdCard() {
        if (bankerThirdCard != null) {
            return bankerThirdCard;
        }
        bankerThirdCard = dealBankerCard();
        return bankerThirdCard;
    }

    public Winner determineWinner() {
        int playerValue = getPlayerValue();
        int bankerValue = getBankerValue();

        if (playerValue > bankerValue) {
            winner = Winner.PLAYER;
        } else if (bankerValue > playerValue) {
            winner = Winner.BANKER;
        } else {
            winner = Winner.TIE;
        }

        return winner;
    }

    public Winner getWinner() {
        return winner;
    }

    public boolean playerDrewThirdCard() {
        return playerThirdCard != null;
    }

    public boolean bankerDrewThirdCard() {
        return bankerThirdCard != null;
    }

    public Card getPlayerThirdCard() {
        return playerThirdCard;
    }

    public Card getBankerThirdCard() {
        return bankerThirdCard;
    }

    public List<Card> getPlayerHand() {
        return playerHand;
    }

    public List<Card> getBankerHand() {
        return bankerHand;
    }

    public int getPlayerValue() {
        return getHandValue(playerHand);
    }

    public int getBankerValue() {
        return getHandValue(bankerHand);
    }

    public int getHandValue(List<Card> hand) {
        int value = 0;
        for (Card card : hand) {
            value += getBaccaratValue(card);
        }
        return value % 10;
    }

    public int getBaccaratValue(Card card) {
        if (card == null) {
            return 0;
        }
        if (card.getRank() == Card.Rank.ACE) {
            return 1;
        }
        int rankValue = card.getRank().getValue();
        return rankValue >= 10 ? 0 : rankValue;
    }
}
