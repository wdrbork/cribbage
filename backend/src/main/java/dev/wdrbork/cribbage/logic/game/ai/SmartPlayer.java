package dev.wdrbork.cribbage.logic.game.ai;

import dev.wdrbork.cribbage.logic.game.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import dev.wdrbork.cribbage.logic.deck.*;

/**
 * AI for a game of cribbage. Calculates the most optimal play at each stage 
 * of the game and suggests that option.
 */
public class SmartPlayer implements CribbageAI {
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    private CribbageManager gameState;
    private int pid;
    private List<Card> hand;
    
    public SmartPlayer(CribbageManager gameState, int pid) {
        int numPlayers = gameState.numPlayers();
        if (numPlayers != 2 && numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("PID is invalid, must be between 0 and " + numPlayers);
        }

        this.gameState = gameState;
        this.pid = pid;
        this.hand = gameState.getHand(pid);
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public List<Card> choosePlayingHand() {
        // Random rng = new Random();
        // if (this.hand == null) {
        //     throw new IllegalStateException("No hand set for AI with pid " + pid);
        // }

        // while (hand.size() > HAND_SIZE) {
        //     hand.remove(rng.nextInt(hand.size()));
        // }
        // return hand;

        if (this.hand == null) {
            throw new IllegalStateException("No hand set for AI with pid " + pid);
        }

        Map<List<Card>, Double> savedCounts = new HashMap<List<Card>, Double>();
        boolean isDealer = false;
        if (gameState.dealer() == pid) isDealer = true;
        hand = maximizePoints(hand, isDealer, 
                new ArrayList<Card>(), 0, savedCounts);
        return hand;
    }

    public Card chooseCard() {
        MCTSAgent agent = new MCTSAgent(gameState, pid);
        Card card = agent.selectCard();
        return card;
    }

    // Recursively finds the 4-card hand with the most expected points given 
    // a 6-card hand (6 choose 4). NOTE: Ignores suits in the calculation; see
    // the runtimes below to understand why this is necessary
    //
    // Total long runtime: 15 possible combos of 4 cards * 46 possible starter
    // cards * 1980 (45 * 44) possible cribs = 1,366,200 iterations
    // Total short runtime: 15 possible combos * 13 possible starter ranks
    // * 169 possible cribs (13 * 13) = 32,955 iterations
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

        int startSize;
        if (gameState.numPlayers() == 2) {
            startSize = TWO_PLAYER_START_SIZE;
        } else {
            startSize = THREE_PLAYER_START_SIZE;
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

        // Print the expected scores of the hands to be compared
        // System.out.println(includeIdx + " (expected = " + savedCounts.get(includeIdx) + 
        //         ") vs. " + excludeIdx + " (expected = " + savedCounts.get(excludeIdx) + 
        //         ")");      
        
        return savedCounts.get(includeIdx) >= savedCounts.get(excludeIdx) ?
                includeIdx : excludeIdx;
    }

    private double findBestPossibleCount(List<Card> hand, boolean ownsCrib) {
        // Use the given hand and the starting hand to infer which cards have 
        // been sent to the crib
        List<Card> sentToCrib = new ArrayList<Card>();
        for (Card card : this.hand) {
            if (!hand.contains(card)) {
                sentToCrib.add(card);
            }
        }

        double expected = 0.0;
        int[] counts = rankCounts();

        // Quick computation (ignores suits)
        for (int i = 1; i <= Deck.CARDS_PER_SUIT; i++) {
            Card next = new Card(Suit.SPADE, Card.getRankBasedOnValue(i));
            int points = CribbageScoring.count15Combos(hand, next);
            points += CribbageScoring.countPairs(hand, next);
            points += CribbageScoring.countRuns(hand, next);
            points += CribbageScoring.countFlush(hand, next);
            points += CribbageScoring.countNobs(hand, next);
            double cardProbability = (double) counts[i] / (Deck.DECK_SIZE - this.hand.size());
            expected += (double) points * cardProbability;

            if (ownsCrib) {
                expected += findBestCribScore(sentToCrib, next) * cardProbability;
            } else {
                expected -= findBestCribScore(sentToCrib, next) * cardProbability;
            }
        }

        return expected;
    }

    private double findBestCribScore(List<Card> sentToCrib, Card starterCard) {
        double expected = 0.0;
        int[] counts = rankCounts();
        counts[starterCard.getRankValue()]--;

        // Quick computation (ignores suits)
        for (int i = 1; i <= Deck.CARDS_PER_SUIT; i++) {
            Card thirdCard = new Card(Suit.SPADE, Card.getRankBasedOnValue(i));

            // Find the probability of a card of this rank ending up in the 
            // crib (e.g. if our 6-card hand contains 4 aces, we should not 
            // expect there to be another ace in the crib, so the probability 
            // would be zero)
            double thirdCardRankProbability = (double) counts[i] / 
                    (Deck.DECK_SIZE - this.hand.size() - 1);

            // Temporarily decrement the count for this rank
            counts[i]--;

            sentToCrib.add(thirdCard);
            for (int j = 1; j <= Deck.CARDS_PER_SUIT; j++) {
                Card fourthCard = new Card(Suit.SPADE, Card.getRankBasedOnValue(j));
                sentToCrib.add(fourthCard);

                // Find the expected points from this crib
                int points = CribbageScoring.count15Combos(sentToCrib, starterCard);
                points += CribbageScoring.countPairs(sentToCrib, starterCard);
                points += CribbageScoring.countRuns(sentToCrib, starterCard);

                // Find the probability of a card of this rank ending up in the
                // crib
                double fourthCardRankProbability = (double) counts[j] / 
                        (Deck.DECK_SIZE - this.hand.size() - 2);

                // Add the expected points from this crib to the overall total
                // (with respect to the probability of this crib occurring)
                expected += (double) points * 
                        thirdCardRankProbability * fourthCardRankProbability;

                sentToCrib.remove(fourthCard);
            }

            counts[i]++;
            sentToCrib.remove(thirdCard);
        }

        // If the two cards in the crib are of the same suit, a flush is 
        // possible, so increase the expected point total of the crib based 
        // on the probability of the flush occurring
        if (sentToCrib.get(0).getSuit() == sentToCrib.get(1).getSuit()) {
            Suit sharedSuit = sentToCrib.get(0).getSuit();
            int cardsOfSuitAvailable = Deck.CARDS_PER_SUIT;
            for (Card card : hand) {
                if (card.getSuit() == sharedSuit) {
                    cardsOfSuitAvailable--;
                }
            }

            double numerator = (double) cardsOfSuitAvailable * 
                    (cardsOfSuitAvailable - 1);
            double denominator = (Deck.DECK_SIZE - this.hand.size() - 1) * 
                    (Deck.DECK_SIZE - this.hand.size() - 2);
            double flushProbability = numerator / denominator;
                                    
            expected += 4 * flushProbability;
        }

        return expected;
    }

    private int[] rankCounts() {
        int[] counts = new int[Deck.CARDS_PER_SUIT + 1];
        Arrays.fill(counts, Deck.CARDS_PER_RANK);
        for (Card card : this.hand) {
            counts[card.getRankValue()]--;
        }

        return counts;
    }
}