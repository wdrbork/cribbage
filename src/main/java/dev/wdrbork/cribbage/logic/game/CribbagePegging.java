package dev.wdrbork.cribbage.logic.game;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import dev.wdrbork.cribbage.logic.deck.Card;
import dev.wdrbork.cribbage.logic.deck.Deck;

public class CribbagePegging {
    /**
     * Counts and returns the number of points earned via pairs from playing 
     * the most recently played card.
     * 
     * @return the number of points earned via pairs from playing the most 
     *         recently played card
     */
    public static int countPegPairs(LinkedList<Card> cardStack) {
        if (cardStack.isEmpty()) {
            return 0;
        }

        Card mostRecentlyPlayed = cardStack.getFirst();
        int occurrences = 0;

        // Since new cards are added to the front of the list, we can iterate 
        // through the list starting from the front in the same way we would 
        // with a stack
        for (Card prev : cardStack) {
            if (prev.getRank() != mostRecentlyPlayed.getRank()) {
                break;
            }
            occurrences++;
        }
        assert(occurrences <= Deck.CARDS_PER_RANK) : "More than 4 occurrences of a specific rank";
        assert(occurrences != 0) : "Most recently played card not counted";

        // 1 occurence = 0 pts, 2 occurrences = 2 pts, 
        // 3 occurences = 6 pts, 4 occurrences = 12 pts
        return occurrences * (occurrences - 1);
    }
    
    /**
     * Counts and returns the number of points earned via runs from playing 
     * the most recently played card.
     * 
     * @return the number of points earned via runs from playing the most 
     *         recently played card
     */
    public static int countPegRuns(LinkedList<Card> cardStack) {
        int totalCardsPlayed = cardStack.size();
        int longestRun = 0;
        int testRunLength = 3;
        
        while (testRunLength <= totalCardsPlayed) {
            // Get the last (testRunLength - 1) cards played
            List<Card> subList = 
                    new LinkedList<Card>(cardStack.subList(0, testRunLength));
            Collections.sort(subList);

            // Determine if a run is present in this sub list
            int i = 0;
            for (; i < subList.size() - 1; i++) {
                if (subList.get(i).getRankValue() + 1 
                        != subList.get(i + 1).getRankValue()) break;
            }

            // If we reached the end of the sub list, then all the numbers are
            // consecutive, so set the longest run to this value
            if (i == subList.size() - 1) {
                longestRun = testRunLength;
            }
            testRunLength++;
        }

        return longestRun;
    }
    
}