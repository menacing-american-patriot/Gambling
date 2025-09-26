package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlackjackGame {

    private final Gambling plugin;
    private final Player player;
    private final double bet;
    private final Economy economy;
    private final Deck deck;
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final Map<UUID, BlackjackGame> games;
    private boolean gameEnded = false;

    public BlackjackGame(Gambling plugin, Player player, double bet, Map<UUID, BlackjackGame> games) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
        this.economy = Gambling.getEconomy();
        this.deck = new Deck();
        this.games = games;
    }

    public List<Card> getPlayerHand() {
        return playerHand;
    }

    public List<Card> getDealerHand() {
        return dealerHand;
    }

    public void start() {
        // Check if player has enough money and withdraw the bet
        if (economy.getBalance(player) < bet) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to place that bet. You need " + economy.format(bet) + ".");
            games.remove(player.getUniqueId()); // Remove the game since it can't start
            return;
        }

        // Withdraw the bet amount upfront
        economy.withdrawPlayer(player, bet);
        player.sendActionBar(ChatColor.YELLOW + "Blackjack started! Bet: " + economy.format(bet));

        deck.shuffle();
        playerHand.add(deck.deal());
        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
        dealerHand.add(deck.deal());

        player.sendMessage(ChatColor.AQUA + "Your hand: " + getHandString(playerHand) + " (" + getHandValue(playerHand) + ")");
        player.sendMessage(ChatColor.GOLD + "Dealer's hand: " + dealerHand.get(0) + " and [HIDDEN]");

        if (getHandValue(playerHand) == 21) {
            stand();
        }
    }

    public void hit() {
        if (gameEnded) {
            player.sendMessage("The game is over.");
            return;
        }
        playerHand.add(deck.deal());
        player.sendMessage(ChatColor.GREEN + "Your hand: " + getHandString(playerHand) + " (" + getHandValue(playerHand) + ")");

        if (getHandValue(playerHand) > 21) {
            endGame(false);
        } else if (getHandValue(playerHand) == 21) {
            stand();
        }
    }

    public void stand() {
        if (gameEnded) {
            player.sendMessage("The game is over.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Dealer's hand: " + getHandString(dealerHand) + " (" + getHandValue(dealerHand) + ")");

        while (getHandValue(dealerHand) < 17) {
            dealerHand.add(deck.deal());
            player.sendMessage(ChatColor.GOLD + "Dealer hits. Dealer's hand: " + getHandString(dealerHand) + " (" + getHandValue(dealerHand) + ")");
        }

        if (getHandValue(dealerHand) > 21) {
            endGame(true);
        } else if (getHandValue(playerHand) > getHandValue(dealerHand)) {
            endGame(true);
        } else if (getHandValue(playerHand) < getHandValue(dealerHand)) {
            endGame(false);
        } else {
            endGame(null); // Push
        }
    }

    private void endGame(Boolean playerWon) {
        if (gameEnded) return;
        gameEnded = true;
        if (playerWon == null) {
            player.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + "PUSH!", ChatColor.GRAY + "Bet returned", 10, 40, 10);
            player.sendActionBar(ChatColor.YELLOW + "It's a tie! Your bet has been returned.");
            economy.depositPlayer(player, bet);
        } else if (playerWon) {
            player.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "BLACKJACK WIN!", ChatColor.GOLD + "+" + economy.format(bet * 2), 10, 40, 10);
            player.sendActionBar(ChatColor.GREEN + "Congratulations! You won " + economy.format(bet * 2));
            economy.depositPlayer(player, bet * 2);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), bet);
        } else {
            player.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "YOU LOST", ChatColor.GRAY + "-" + economy.format(bet), 10, 40, 10);
            player.sendActionBar(ChatColor.RED + "Better luck next time! You lost " + economy.format(bet));
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), bet);
        }
        games.remove(player.getUniqueId());
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
