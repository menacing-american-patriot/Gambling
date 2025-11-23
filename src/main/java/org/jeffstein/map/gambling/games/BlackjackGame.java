package org.jeffstein.map.gambling.games;

import org.jeffstein.map.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlackjackGame {

    private enum Outcome {
        PLAYER_BLACKJACK,
        PLAYER_WIN,
        DEALER_WIN,
        PUSH
    }

    private final Gambling plugin;
    private final Player player;
    private final double bet;
    private final Economy economy;
    private final Deck deck;
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final Map<UUID, BlackjackGame> games;

    private double wager;
    private boolean doubled;
    private boolean gameEnded = false;

    public BlackjackGame(Gambling plugin, Player player, double bet, Map<UUID, BlackjackGame> games) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
        this.economy = Gambling.getEconomy();
        this.deck = new Deck();
        this.games = games;
    }

    public boolean start() {
        if (bet < 10.0) {
            player.sendMessage(ChatColor.RED + "Minimum bet is " + economy.format(10.0) + ".");
            games.remove(player.getUniqueId());
            return false;
        }

        if (economy.getBalance(player) < bet) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to place that bet. You need " + economy.format(bet) + ".");
            games.remove(player.getUniqueId());
            return false;
        }

        economy.withdrawPlayer(player, bet);
        wager = bet;
        deck.shuffle();

        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());

        player.sendActionBar(ChatColor.YELLOW + "Blackjack started! Bet: " + economy.format(bet));
        showTable(false);
        showOptions();
        checkNaturals();
        return true;
    }

    public void hit() {
        if (gameEnded) {
            player.sendMessage(ChatColor.GRAY + "The game is already over.");
            return;
        }

        playerHand.add(deck.deal());
        player.sendMessage(ChatColor.GREEN + "You draw: " + formatCard(playerHand.get(playerHand.size() - 1)));
        showTable(false);

        int total = getHandValue(playerHand);
        if (total > 21) {
            finishRound(Outcome.DEALER_WIN);
        } else if (total == 21) {
            player.sendMessage(ChatColor.YELLOW + "You reached 21! Dealer's turn.");
            stand();
        } else {
            showOptions();
        }
    }

    public void stand() {
        if (gameEnded) {
            player.sendMessage(ChatColor.GRAY + "The game is already over.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Dealer reveals the hidden card...");
        showTable(true);

        while (getHandValue(dealerHand) < 17) {
            Card card = deck.deal();
            dealerHand.add(card);
            player.sendMessage(ChatColor.GOLD + "Dealer draws: " + formatCard(card));
            showTable(true);
        }

        int playerTotal = getHandValue(playerHand);
        int dealerTotal = getHandValue(dealerHand);

        if (dealerTotal > 21 || playerTotal > dealerTotal) {
            finishRound(Outcome.PLAYER_WIN);
        } else if (playerTotal < dealerTotal) {
            finishRound(Outcome.DEALER_WIN);
        } else {
            finishRound(Outcome.PUSH);
        }
    }

    public void doubleDown() {
        if (gameEnded) {
            player.sendMessage(ChatColor.GRAY + "The game is already over.");
            return;
        }
        if (doubled) {
            player.sendMessage(ChatColor.RED + "You have already doubled down.");
            return;
        }
        if (playerHand.size() != 2) {
            player.sendMessage(ChatColor.RED + "Double down is only available on your first decision.");
            return;
        }
        if (economy.getBalance(player) < bet) {
            player.sendMessage(ChatColor.RED + "You need " + economy.format(bet) + " more to double down.");
            return;
        }

        economy.withdrawPlayer(player, bet);
        wager += bet;
        doubled = true;
        player.sendMessage(ChatColor.YELLOW + "Double down! Bet is now " + economy.format(wager) + ".");

        Card card = deck.deal();
        playerHand.add(card);
        player.sendMessage(ChatColor.GREEN + "You draw: " + formatCard(card));
        showTable(false);

        int total = getHandValue(playerHand);
        if (total > 21) {
            finishRound(Outcome.DEALER_WIN);
        } else {
            stand();
        }
    }

    public void status() {
        showTable(gameEnded);
        if (!gameEnded) {
            showOptions();
        }
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public List<Card> getPlayerHand() {
        return new ArrayList<>(playerHand);
    }

    public List<Card> getDealerHand() {
        return new ArrayList<>(dealerHand);
    }

    private void checkNaturals() {
        int playerTotal = getHandValue(playerHand);
        int dealerTotal = getHandValue(dealerHand);

        if (playerTotal == 21 || dealerTotal == 21) {
            showTable(true);

            if (playerTotal == 21 && dealerTotal == 21) {
                player.sendMessage(ChatColor.YELLOW + "Both you and the dealer have blackjack! It's a push.");
                finishRound(Outcome.PUSH);
            } else if (playerTotal == 21) {
                player.sendMessage(ChatColor.GREEN + "Blackjack! You are paid 3:2.");
                finishRound(Outcome.PLAYER_BLACKJACK);
            } else {
                player.sendMessage(ChatColor.RED + "Dealer has blackjack.");
                finishRound(Outcome.DEALER_WIN);
            }
        }
    }

    private void finishRound(Outcome outcome) {
        if (gameEnded) return;
        gameEnded = true;
        showTable(true);

        double payout = 0.0;
        double profit = 0.0;

        switch (outcome) {
            case PLAYER_BLACKJACK:
                payout = bet * 2.5; // Original stake + 3:2 profit
                profit = payout - bet;
                economy.depositPlayer(player, payout);
                player.sendTitle(ChatColor.GREEN + "BLACKJACK!", ChatColor.GOLD + "+" + economy.format(profit), 10, 60, 20);
                player.sendActionBar(ChatColor.GREEN + "Blackjack pays " + economy.format(profit) + "!");
                Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);
                break;
            case PLAYER_WIN:
                payout = wager * 2;
                profit = payout - wager;
                economy.depositPlayer(player, payout);
                player.sendTitle(ChatColor.GREEN + "YOU WIN", ChatColor.GOLD + "+" + economy.format(profit), 10, 60, 20);
                player.sendActionBar(ChatColor.GREEN + "You profit " + economy.format(profit) + ".");
                Gambling.getLeaderboard().addWin(player.getUniqueId(), profit);
                break;
            case DEALER_WIN:
                profit = -wager;
                player.sendTitle(ChatColor.RED + "YOU LOST", ChatColor.GRAY + "-" + economy.format(wager), 10, 60, 20);
                player.sendActionBar(ChatColor.RED + "Better luck next time.");
                Gambling.getLeaderboard().addLoss(player.getUniqueId(), wager);
                break;
            case PUSH:
                payout = wager;
                economy.depositPlayer(player, payout);
                player.sendTitle(ChatColor.YELLOW + "PUSH", ChatColor.GRAY + "Bet returned", 10, 60, 20);
                player.sendActionBar(ChatColor.YELLOW + "No profit, no loss.");
                break;
        }

        games.remove(player.getUniqueId());
    }

    private void showTable(boolean revealDealer) {
        String bar = ChatColor.DARK_GRAY + "┌────────────── BLACKJACK ──────────────┐";
        player.sendMessage(bar);
        player.sendMessage(ChatColor.YELLOW + "Dealer: " + formatHand(dealerHand, revealDealer) + ChatColor.GRAY + " ("
                + (revealDealer ? getHandValue(dealerHand) : "??") + ")");
        player.sendMessage(ChatColor.GREEN + "Player: " + formatHand(playerHand, true) + ChatColor.GRAY + " ("
                + getHandValue(playerHand) + ")");
        player.sendMessage(ChatColor.GRAY + "Stake: " + ChatColor.GOLD + economy.format(wager)
                + (doubled ? ChatColor.YELLOW + " (doubled)" : ""));
        player.sendMessage(ChatColor.DARK_GRAY + "└────────────────────────────────────────┘");
    }

    private void showOptions() {
        StringBuilder options = new StringBuilder();
        options.append(ChatColor.GRAY).append("Options: ")
                .append(ChatColor.GREEN).append("/blackjack hit")
                .append(ChatColor.GRAY).append(", ")
                .append(ChatColor.RED).append("/blackjack stand");
        if (canDoubleDown()) {
            options.append(ChatColor.GRAY).append(", ")
                    .append(ChatColor.GOLD).append("/blackjack double");
        }
        options.append(ChatColor.GRAY).append(", ")
                .append(ChatColor.AQUA).append("/blackjack status");
        player.sendMessage(options.toString());
    }

    private boolean canDoubleDown() {
        return !gameEnded && !doubled && playerHand.size() == 2 && economy.getBalance(player) >= bet;
    }

    private String formatHand(List<Card> hand, boolean revealAll) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            if (!revealAll && i > 0) {
                parts.add(ChatColor.DARK_GRAY + "??" + ChatColor.RESET);
            } else {
                parts.add(formatCard(hand.get(i)));
            }
        }
        return String.join(ChatColor.GRAY + " | " + ChatColor.RESET, parts);
    }

    private String formatCard(Card card) {
        String rank;
        switch (card.getRank()) {
            case ACE: rank = "A"; break;
            case KING: rank = "K"; break;
            case QUEEN: rank = "Q"; break;
            case JACK: rank = "J"; break;
            case TEN: rank = "10"; break;
            case NINE: rank = "9"; break;
            case EIGHT: rank = "8"; break;
            case SEVEN: rank = "7"; break;
            case SIX: rank = "6"; break;
            case FIVE: rank = "5"; break;
            case FOUR: rank = "4"; break;
            case THREE: rank = "3"; break;
            case TWO: rank = "2"; break;
            default: rank = card.getRank().toString();
        }

        String suit;
        ChatColor color;
        switch (card.getSuit()) {
            case HEARTS:
                suit = "♥";
                color = ChatColor.RED;
                break;
            case DIAMONDS:
                suit = "♦";
                color = ChatColor.RED;
                break;
            case CLUBS:
                suit = "♣";
                color = ChatColor.WHITE;
                break;
            case SPADES:
                suit = "♠";
                color = ChatColor.WHITE;
                break;
            default:
                suit = card.getSuit().toString();
                color = ChatColor.WHITE;
        }
        return color + rank + suit + ChatColor.RESET;
    }

    private int getHandValue(List<Card> hand) {
        int value = 0;
        int aceCount = 0;
        for (Card card : hand) {
            value += card.getValue();
            if (card.getRank() == Card.Rank.ACE) {
                aceCount++;
            }
        }
        while (value > 21 && aceCount > 0) {
            value -= 10;
            aceCount--;
        }
        return value;
    }
}
