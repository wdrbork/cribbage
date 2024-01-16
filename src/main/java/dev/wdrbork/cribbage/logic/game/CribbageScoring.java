package dev.wdrbork.cribbage.logic.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.wdrbork.cribbage.logic.deck.Card;
import dev.wdrbork.cribbage.logic.deck.Deck;
import dev.wdrbork.cribbage.logic.deck.Rank;

public class CribbageScoring {
    private static final int HAND_SIZE = 4;

    /**
     * Counts and returns the number of points earned from combinations of 
     * cards that add up to 15 in a given hand along with the starter card.
     * 
     * @param hand the hand that will be searched
     * @param starterCard the starter card for a round of cribbage
     * @return the number of points present in the given player's hand
     */
    public static int count15Combos(List<Card> hand, Card starterCard) {
        if (hand.size() != HAND_SIZE) {
            throw new IllegalStateException("Hand does not have 4 cards");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hand.add(starterCard);
        int count = getCombos(hand, 0, 0);
        hand.remove(starterCard);
        return count * 2;
    }

    // Starting from the given idx, recursively searches through the given hand
    // in search of a subset that adds up to 15. Returns the number of such 
    // subsets. Note that this algorithm has a runtime of O(2^n), but n  
    // should never be more than 5, so any optimizations would be trivial
    private static int getCombos(List<Card> hand, int idx, int soFar) {
        if (soFar == 15) {
            return 1;
        }

        if (idx == hand.size() || soFar > 15) {
            return 0;
        }

        // Ends up looking like a binary search tree, with one child including 
        // the value of the current card in soFar and the other skipping it
        return getCombos(hand, idx + 1, soFar + hand.get(idx).getValue())
                + getCombos(hand, idx + 1, soFar);
    }

    /**
     * Counts all points earned through runs in the given hand. A run is a 
     * sequence of consecutive numbers irrespective of suit (e.g. a 5 of clubs, 
     * 6 of spades, and 7 of diamonds form a run of three). The given hand 
     * must not be empty, and there must be a starter card to reference
     * 
     * @param hand the hand that will be searched
     * @param starterCard the starter card for a round of cribbage
     * @return the number of points earned through runs in the given hand
     */
    public static int countRuns(List<Card> hand, Card starterCard) {
        if (hand.size() != HAND_SIZE) {
            throw new IllegalStateException("Hand does not have 4 cards");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hand.add(starterCard);
        Collections.sort(hand);
        
        // Count the number of times each number appears in this hand
        int[] occurrences = new int[Deck.CARDS_PER_SUIT + 1];
        for (Card card : hand) {
            int value = card.getRankValue();
            occurrences[value]++;
        }

        int totalPoints = 0;
        int currentRun = 0;
        int prevValue = -1;

        // Used for situations in which a number appears
        // more than once in a single run (e.g. 7, 7, 8, 
        // 9 would produce a multiplier of 2 since 7 
        // appears twice)
        int multiplier = 1;  

        // Loop through the hand in search of runs
        int i = 0;
        while (i < hand.size()) {
            int currentValue = hand.get(i).getRankValue();
            assert(currentValue != prevValue) : "Incorrect traversal of hand for countRuns";

            // If the currentValue equals the previous value + 1, increase the 
            // counter for the current run
            if (currentValue == prevValue + 1) {
                currentRun++;
            } else {
                // Since the hand is sorted, this value must be greater than 
                // the previous value + 1, so determine if we had a valid run 
                // and reset the appropriate variables
                if (currentRun >= 3) {
                    totalPoints += currentRun * multiplier;
                }
                
                currentRun = 1;
                multiplier = 1;
            }

            multiplier *= occurrences[currentValue];
            prevValue = currentValue;
            i += occurrences[currentValue];
        }

        // If we had a run at the end of the hand, make sure this is counted
        if (currentRun >= 3) {
            totalPoints += currentRun * multiplier;
        }

        hand.remove(starterCard);
        return totalPoints;
    }

    /**
     * Counts all points earned through pairs in the given hand. A pair is 
     * worth 2 points.
     * 
     * @param hand the hand that will be searched
     * @param starterCard the starter card for a round of cribbage
     * @return the number of points earned through pairs
     */
    public static int countPairs(List<Card> hand, Card starterCard) {
        if (hand.size() != HAND_SIZE) {
            throw new IllegalStateException("Hand does not have 4 cards");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hand.add(starterCard);
        Map<Integer, Integer> occurrences = new HashMap<Integer, Integer>();
        for (Card card : hand) {
            int value = card.getRankValue();
            occurrences.put(value, occurrences.getOrDefault(value, 0) + 1);
        }

        int totalPoints = 0;
        for (int value : occurrences.keySet()) {
            totalPoints += occurrences.get(value) * (occurrences.get(value) - 1);
        }

        hand.remove(starterCard);
        return totalPoints;
    }

    /**
     * Counts and returns points earned through flush. A flush is a hand in 
     * which all cards are of the same suit. If all four cards in a player's  
     * hand share the same suit, 4 points are earned. If the starter card is  
     * the same suit as these four cards, 5 points are earned.
     * 
     * @param hand the hand that will be searched
     * @param starterCard the starter card for a round of cribbage
     * @return the number of points earned through flush
     */
    public static int countFlush(List<Card> hand, Card starterCard) {
        if (hand.size() != HAND_SIZE) {
            throw new IllegalStateException("Hand does not have 4 cards");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        for (int i = 1; i < hand.size(); i++) {
            if (hand.get(i).getSuit() != hand.get(i - 1).getSuit()) {
                return 0;
            }
        }

        if (starterCard.getSuit() == hand.get(0).getSuit()) {
            return 5;
        }
        return 4;
    }

    /**
     * Returns 1 if this hand contains a jack that has the same suit as the 
     * starter card (formally called "one for his nob").
     * 
     * @param hand the hand that will be searched
     * @param starterCard the starter card for a round of cribbage
     * @return a point if this hand has a jack with the same suit as the 
     *         starter card and 0 otherwise
     */
    public static int countNobs(List<Card> hand, Card starterCard) {
        if (hand.size() != HAND_SIZE) {
            throw new IllegalStateException("Hand does not have 4 cards");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        for (Card card : hand) {
            if (card.getRank() == Rank.JACK 
                    && starterCard.getSuit() == card.getSuit()) {
                return 1;
            }
        }

        return 0;
    }
}