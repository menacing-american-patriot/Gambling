package org.jeffstein.gambling.games;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HandEvaluator {

    public enum HandRank {
        HIGH_CARD,
        ONE_PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
    }

    public static HandRank evaluateHand(List<Card> hand) {
        if (isRoyalFlush(hand)) return HandRank.ROYAL_FLUSH;
        if (isStraightFlush(hand)) return HandRank.STRAIGHT_FLUSH;
        if (isFourOfAKind(hand)) return HandRank.FOUR_OF_A_KIND;
        if (isFullHouse(hand)) return HandRank.FULL_HOUSE;
        if (isFlush(hand)) return HandRank.FLUSH;
        if (isStraight(hand)) return HandRank.STRAIGHT;
        if (isThreeOfAKind(hand)) return HandRank.THREE_OF_A_KIND;
        if (isTwoPair(hand)) return HandRank.TWO_PAIR;
        if (isOnePair(hand)) return HandRank.ONE_PAIR;
        return HandRank.HIGH_CARD;
    }

    private static boolean isRoyalFlush(List<Card> hand) {
        return isStraightFlush(hand) && hand.stream().mapToInt(c -> c.getRank().getValue()).sum() == 60;
    }

    private static boolean isStraightFlush(List<Card> hand) {
        return isFlush(hand) && isStraight(hand);
    }

    private static boolean isFourOfAKind(List<Card> hand) {
        return hand.stream().map(Card::getRank).anyMatch(rank -> Collections.frequency(hand.stream().map(Card::getRank).collect(Collectors.toList()), rank) == 4);
    }

    private static boolean isFullHouse(List<Card> hand) {
        return isThreeOfAKind(hand) && isOnePair(hand);
    }

    private static boolean isFlush(List<Card> hand) {
        return hand.stream().map(Card::getSuit).distinct().count() == 1;
    }

    private static boolean isStraight(List<Card> hand) {
        List<Integer> ranks = hand.stream().map(c -> c.getRank().getValue()).sorted().collect(Collectors.toList());
        if (ranks.size() != 5) return false;
        if (ranks.get(4) == 14 && ranks.get(0) == 2 && ranks.get(1) == 3 && ranks.get(2) == 4 && ranks.get(3) == 5) return true; // Ace-low straight
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i + 1) - ranks.get(i)!= 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean isThreeOfAKind(List<Card> hand) {
        return hand.stream().map(Card::getRank).anyMatch(rank -> Collections.frequency(hand.stream().map(Card::getRank).collect(Collectors.toList()), rank) == 3);
    }

    private static boolean isTwoPair(List<Card> hand) {
        return hand.stream().map(Card::getRank).distinct().filter(rank -> Collections.frequency(hand.stream().map(Card::getRank).collect(Collectors.toList()), rank) == 2).count() == 2;
    }

    private static boolean isOnePair(List<Card> hand) {
        return hand.stream().map(Card::getRank).distinct().anyMatch(rank -> Collections.frequency(hand.stream().map(Card::getRank).collect(Collectors.toList()), rank) == 2);
    }
}
