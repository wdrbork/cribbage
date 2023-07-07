package logic.game;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Random;


import logic.deck.*;

public class CribbageAI {
    private static final int HAND_SIZE = 4;

    private int startSize;
    private List<Card> hand;
    private Deck personalDeck;
    private Random randomizer;
    
    public CribbageAI(int startSize) {
        this.startSize = startSize;
        hand = new ArrayList<Card>();
        personalDeck = new Deck();
        randomizer = new Random();
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public List<Card> getOptimalHand(boolean isDealer) {
        Map<List<Card>, Double> savedCounts = new HashMap<List<Card>, Double>();
        hand = maximizePoints(hand, isDealer, 
                new ArrayList<Card>(), 0, savedCounts);
        return hand;
    }

    // Total runtime: 15 possible combos of 4 cards * 46 possible starter cards
    // * 1980 (45 * 44) possible cribs = 1,366,200 iterations
    private List<Card> maximizePoints(List<Card> original, boolean isDealer,
            List<Card> soFar, int idx, Map<List<Card>, Double> savedCounts) {
        // If soFar represents a full hand, determine the expected number of 
        // points and save this information
        if (soFar.size() == HAND_SIZE) {
            double bestExpected = findBestPossibleCount(soFar, isDealer);
            List<Card> deepCopy = new ArrayList<Card>(soFar);
            savedCounts.put(deepCopy, bestExpected);
            return deepCopy;
        }

        // If we have traversed the entire original hand, or if soFar won't be
        // able to reach a size of 4, create and save an empty list with a 
        // negative point value so that it always fails comparisons to other
        // lists (as seen below)
        if (idx == startSize 
                || idx - soFar.size() > startSize - HAND_SIZE) {
            List<Card> notApplicable = new ArrayList<Card>();
            savedCounts.put(notApplicable, -Double.MAX_VALUE);
            return notApplicable;
        }

        // Compare the expected points from including the card at this idx with
        // the expected points from ignoring it
        soFar.add(original.get(idx));
        List<Card> includeIdx = 
                maximizePoints(original, isDealer, soFar, idx + 1, savedCounts);
        soFar.remove(original.get(idx));
        List<Card> excludeIdx = 
                maximizePoints(original, isDealer, soFar, idx + 1, savedCounts);

        System.out.println(includeIdx + " (expected = " + savedCounts.get(includeIdx) + 
                ") vs. " + excludeIdx + " (expected = " + savedCounts.get(excludeIdx) + 
                ")");      
        
        return savedCounts.get(includeIdx) >= savedCounts.get(excludeIdx) ?
                includeIdx : excludeIdx;
    }

    private double findBestPossibleCount(List<Card> hand, boolean ownsCrib) {
        List<Card> sentToCrib = new ArrayList<Card>();
        for (Card card : this.hand) {
            if (!hand.contains(card)) {
                sentToCrib.add(card);
            }
        }

        int totalPoints = 0;
        for (int i = 1; i < Deck.CARDS_PER_SUIT; i++) {
            Card next = new Card(Suit.GENERIC, Card.getRankBasedOnValue(i));
            
        }
        // while (personalDeck.remainingCards() > 0) {
        //     Card next = personalDeck.takeTopCard();
        //     if (!this.hand.contains(next)) {
        //         int points = CribbageScoring.count15Combos(hand, next);
        //         points += CribbageScoring.countPairs(hand, next);
        //         points += CribbageScoring.countRuns(hand, next);
        //         points += CribbageScoring.countFlush(hand, next);
        //         points += CribbageScoring.countNobs(hand, next);
        //         totalPoints += points;

        //         if (ownsCrib) {
        //             totalPoints += findBestCribScore(sentToCrib, next);
        //         } else {
        //             totalPoints -= findBestCribScore(sentToCrib, next);
        //         }
        //     }
        // }

        personalDeck.resetDeck();
        double expected = 
                (double) totalPoints / (Deck.DECK_SIZE - this.hand.size());

        return expected;
    }

    private double findBestCribScore(List<Card> sentToCrib, Card starterCard) {
        Deck firstDeck = new Deck();
        Deck secondDeck = new Deck();

        double totalPoints = 0.0;
        while (firstDeck.remainingCards() > 0) {
            Card next1 = firstDeck.takeTopCard();
            if (next1.equals(starterCard) || this.hand.contains(next1)) continue;

            sentToCrib.add(next1);
            int subPoints = 0;
            while (secondDeck.remainingCards() > 0) {
                Card next2 = secondDeck.takeTopCard();
                if (next2.equals(next1) || next2.equals(starterCard) 
                        || this.hand.contains(next1)) continue;

                sentToCrib.add(next2);
                int points = CribbageScoring.count15Combos(sentToCrib, starterCard);
                points += CribbageScoring.countPairs(sentToCrib, starterCard);
                points += CribbageScoring.countRuns(sentToCrib, starterCard);
                points += CribbageScoring.countFlush(sentToCrib, starterCard);
                points += CribbageScoring.countNobs(sentToCrib, starterCard);
                subPoints += points;

                sentToCrib.remove(next2);
            }

            totalPoints += 
                    (double) subPoints / (Deck.DECK_SIZE - this.hand.size() + 1);

            secondDeck.resetDeck();
            sentToCrib.remove(next1);
        }

        return totalPoints / (Deck.DECK_SIZE - this.hand.size());
    }
}