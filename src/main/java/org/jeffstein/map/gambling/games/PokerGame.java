package org.jeffstein.map.gambling.games;

import org.jeffstein.map.gambling.Gambling;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PokerGame {

    private final Gambling plugin;
    private final List<PokerPlayer> players = new ArrayList<>();
    private final Deck deck;
    private final List<Card> communityCards = new ArrayList<>();
    private double pot;
    private GameState gameState;
    private int currentPlayerIndex;
    private double currentBet;

    public PokerGame(Gambling plugin) {
        this.plugin = plugin;
        this.deck = new Deck();
        this.pot = 0;
        this.gameState = GameState.WAITING;
        this.currentPlayerIndex = 0;
        this.currentBet = 0;
    }

    public void addPlayer(PokerPlayer player) {
        players.add(player);
    }

    public void removePlayer(PokerPlayer player) {
        players.remove(player);
    }

    public List<PokerPlayer> getPlayers() {
        return players;
    }

    public void start() {
        deck.shuffle();
        // Deal cards to players
        for (PokerPlayer player : players) {
            player.addCard(deck.deal());
            player.addCard(deck.deal());
        }
        gameState = GameState.PRE_FLOP;
        nextPlayer();
    }

    public void bet(Player player, double amount) {
        PokerPlayer pokerPlayer = getPokerPlayer(player);
        if (pokerPlayer != players.get(currentPlayerIndex)) {
            player.sendMessage("It's not your turn.");
            return;
        }
        if (amount < currentBet) {
            player.sendMessage("You must bet at least " + currentBet);
            return;
        }
        pokerPlayer.setBet(amount);
        currentBet = amount;
        pot += amount;
        nextPlayer();
    }

    public void check(Player player) {
        PokerPlayer pokerPlayer = getPokerPlayer(player);
        if (pokerPlayer != players.get(currentPlayerIndex)) {
            player.sendMessage("It's not your turn.");
            return;
        }
        if (pokerPlayer.getBet() < currentBet) {
            player.sendMessage("You cannot check, you must call or raise.");
            return;
        }
        nextPlayer();
    }

    public void fold(Player player) {
        PokerPlayer pokerPlayer = getPokerPlayer(player);
        if (pokerPlayer != players.get(currentPlayerIndex)) {
            player.sendMessage("It's not your turn.");
            return;
        }
        pokerPlayer.setFolded(true);
        nextPlayer();
    }

    private void nextPlayer() {
        currentPlayerIndex++;
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
            // End of betting round
            switch (gameState) {
                case PRE_FLOP:
                    gameState = GameState.FLOP;
                    communityCards.add(deck.deal());
                    communityCards.add(deck.deal());
                    communityCards.add(deck.deal());
                    broadcast("Flop: " + getHandString(communityCards));
                    break;
                case FLOP:
                    gameState = GameState.TURN;
                    communityCards.add(deck.deal());
                    broadcast("Turn: " + getHandString(communityCards));
                    break;
                case TURN:
                    gameState = GameState.RIVER;
                    communityCards.add(deck.deal());
                    broadcast("River: " + getHandString(communityCards));
                    break;
                case RIVER:
                    gameState = GameState.SHOWDOWN;
                    showdown();
                    break;
            }
            currentBet = 0;
            for (PokerPlayer player : players) {
                player.setBet(0);
            }
        }
        if (players.get(currentPlayerIndex).hasFolded()) {
            nextPlayer();
            return;
        }
        players.get(currentPlayerIndex).getPlayer().sendMessage("It's your turn.");
    }

    private void showdown() {
        PokerPlayer winner = null;
        HandEvaluator.HandRank bestRank = HandEvaluator.HandRank.HIGH_CARD;

        for (PokerPlayer player : players) {
            if (player.hasFolded()) continue;

            List<Card> allCards = new ArrayList<>(player.getHand());
            allCards.addAll(communityCards);
            List<Card> bestHand = getBestHand(allCards);
            HandEvaluator.HandRank rank = HandEvaluator.evaluateHand(bestHand);

            if (rank.ordinal() > bestRank.ordinal()) {
                bestRank = rank;
                winner = player;
            }
        }

        if (winner != null) {
            broadcast(winner.getPlayer().getName() + " wins with a " + bestRank);
            Gambling.getEconomy().depositPlayer(winner.getPlayer(), pot);
        }
    }

    private List<Card> getBestHand(List<Card> allCards) {
        List<Card> bestHand = new ArrayList<>();
        HandEvaluator.HandRank bestRank = HandEvaluator.HandRank.HIGH_CARD;

        for (int i = 0; i < allCards.size(); i++) {
            for (int j = i + 1; j < allCards.size(); j++) {
                for (int k = j + 1; k < allCards.size(); k++) {
                    for (int l = k + 1; l < allCards.size(); l++) {
                        for (int m = l + 1; m < allCards.size(); m++) {
                            List<Card> currentHand = new ArrayList<>();
                            currentHand.add(allCards.get(i));
                            currentHand.add(allCards.get(j));
                            currentHand.add(allCards.get(k));
                            currentHand.add(allCards.get(l));
                            currentHand.add(allCards.get(m));

                            HandEvaluator.HandRank currentRank = HandEvaluator.evaluateHand(currentHand);
                            if (currentRank.ordinal() > bestRank.ordinal()) {
                                bestRank = currentRank;
                                bestHand = currentHand;
                            }
                        }
                    }
                }
            }
        }
        return bestHand;
    }

    private void broadcast(String message) {
        for (PokerPlayer player : players) {
            player.getPlayer().sendMessage(message);
        }
    }

    private PokerPlayer getPokerPlayer(Player player) {
        return players.stream().filter(p -> p.getPlayer().equals(player)).findFirst().orElse(null);
    }

    private String getHandString(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i));
            if (i < hand.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public GameState getGameState() {
        return gameState;
    }

    public enum GameState {
        WAITING,
        PRE_FLOP,
        FLOP,
        TURN,
        RIVER,
        SHOWDOWN
    }
}
