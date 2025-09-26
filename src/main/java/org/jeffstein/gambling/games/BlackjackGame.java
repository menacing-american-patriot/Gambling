package org.jeffstein.gambling.games;

import org.jeffstein.gambling.Gambling;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BlackjackGame {

    private final Gambling plugin;
    private final Player player;
    private final double bet;
    private final Economy economy;
    private final Deck deck;
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();

    public BlackjackGame(Gambling plugin, Player player, double bet) {
        this.plugin = plugin;
        this.player = player;
        this.bet = bet;
        this.economy = Gambling.getEconomy();
        this.deck = new Deck();
    }

    public void start() {
        deck.shuffle();
        playerHand.add(deck.deal());
        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
        dealerHand.add(deck.deal());

        player.sendMessage(ChatColor.GREEN + "Your hand: " + getHandString(playerHand) + " (" + getHandValue(playerHand) + ")");
        player.sendMessage(ChatColor.GREEN + "Dealer's hand: " + dealerHand.get(0) + " and [HIDDEN]");

        if (getHandValue(playerHand) == 21) {
            stand();
        }
    }

    public void hit() {
        playerHand.add(deck.deal());
        player.sendMessage(ChatColor.GREEN + "Your hand: " + getHandString(playerHand) + " (" + getHandValue(playerHand) + ")");

        if (getHandValue(playerHand) > 21) {
            endGame(false);
        }
    }

    public void stand() {
        player.sendMessage(ChatColor.GREEN + "Dealer's hand: " + getHandString(dealerHand) + " (" + getHandValue(dealerHand) + ")");

        while (getHandValue(dealerHand) < 17) {
            dealerHand.add(deck.deal());
            player.sendMessage(ChatColor.GREEN + "Dealer hits. Dealer's hand: " + getHandString(dealerHand) + " (" + getHandValue(dealerHand) + ")");
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
        if (playerWon == null) {
            player.sendMessage(ChatColor.YELLOW + "Push! Your bet has been returned.");
            economy.depositPlayer(player, bet);
        } else if (playerWon) {
            player.sendMessage(ChatColor.GOLD + "You win! You won " + economy.format(bet * 2));
            economy.depositPlayer(player, bet * 2);
            Gambling.getLeaderboard().addWin(player.getUniqueId(), bet);
        } else {
            player.sendMessage(ChatColor.RED + "You lose! You lost " + economy.format(bet));
            Gambling.getLeaderboard().addLoss(player.getUniqueId(), bet);
        }
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
