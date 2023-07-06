package logic.game;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import logic.deck.*;

public class CribbageAI {
    private static final int START_SIZE = 6;
    private static final int HAND_SIZE = 4;

    private List<Card> hand;
    private Deck personalDeck;
    
    public CribbageAI() {
        hand = new ArrayList<Card>();
        personalDeck = new Deck();
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public List<Card> getOptimalHand() {
        hand = maximizePoints(hand, new ArrayList<Card>(), 0, new HashMap<>());
        return hand;
    }

    private List<Card> maximizePoints(List<Card> original, 
            List<Card> soFar, int idx, Map<List<Card>, Double> savedCounts) {
        // If soFar represents a full hand, determine the expected number of 
        // points and save this information
        if (soFar.size() == HAND_SIZE) {
            double bestExpected = findBestPossibleCount(soFar);
            savedCounts.put(soFar, bestExpected);
            return soFar;
        }

        // If we have traversed the entire original hand, or if soFar won't be
        // able to reach a size of 4, create and save an empty list with a 
        // negative point value so that it always fails comparisons to other
        // lists (as seen below)
        if (idx == START_SIZE 
                || idx - soFar.size() > START_SIZE - HAND_SIZE) {
            List<Card> notApplicable = new ArrayList<Card>();
            savedCounts.put(notApplicable, -1.0);
            return notApplicable;
        }

        // Compare the expected points from including the card at this idx with
        // the expected points from ignoring it
        soFar.add(original.get(idx));
        List<Card> includeIdx = 
                maximizePoints(original, soFar, idx + 1, savedCounts);
        soFar.remove(original.get(idx));
        List<Card> excludeIdx = 
                maximizePoints(original, soFar, idx + 1, savedCounts);

        return savedCounts.get(includeIdx) >= savedCounts.get(excludeIdx) ?
                includeIdx : excludeIdx;
    }

    private double findBestPossibleCount(List<Card> hand) {
        Map<Integer, Integer> expectedPoints = new HashMap<Integer, Integer>();
        while (personalDeck.remainingCards() > 0) {
            Card next = personalDeck.takeTopCard();
            if (!hand.contains(next)) {
                int points = CribbageScoring.count15Combos(hand, next);
                points += CribbageScoring.countPairs(hand, next);
                points += CribbageScoring.countRuns(hand, next);
                points += CribbageScoring.countFlush(hand, next);
                points += CribbageScoring.countNobs(hand, next);
                expectedPoints.put(points, 
                        expectedPoints.getOrDefault(points, 0) + 1);
            }
        }

        double maxScore = 0.0;
        for (int points : expectedPoints.keySet()) {
            int occurrences = expectedPoints.get(points);
            double expected = points * ((double) occurrences / Deck.DECK_SIZE);
            maxScore = Math.max(maxScore, expected);
        }

        return maxScore;
    }
}