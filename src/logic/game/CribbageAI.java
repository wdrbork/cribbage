package logic.game;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;

import logic.deck.*;

/**
 * AI for a game of cribbage. Calculates the most optimal play at each stage 
 * of the game and suggests that option. Currently supports two-player cribbage
 */
public class CribbageAI {
    private static final int HAND_SIZE = 4;
    private static final int MAX_COUNT = 31;
    private static final int ITERATIONS = 1000;

    private int pid;
    private int startSize;
    private List<Card> hand;
    private Deck personalDeck;

    // Represents a node in a Monte Carlo search tree. Used when deciding what
    // card to play during the second stage of Cribbage
    private class MCTSNode {
        private static final double CONSTANT = .5;

        // State fields
        public List<Card> hand;
        public int[] remainingCards;
        public int[] rankCounts;
        public int count;
        public LinkedList<Card> cardStack;
        public int pidTurn;

        // Node fields
        public int pointsEarned = 0;
        public int numRollouts = 0;
        public MCTSNode parent;
        public Map<Card, MCTSNode> children;

        public MCTSNode(MCTSNode parent, List<Card> currentHand) {
            this.parent = parent;
            hand = currentHand;
        }

        public double getUCTValue() {
            if (numRollouts == 0 || parent == null) {
                return 0;
            }

            return pointsEarned / numRollouts + CONSTANT * 
                    Math.sqrt(Math.log(parent.numRollouts) / numRollouts);
        }
    }
    
    public CribbageAI(int pid, int startSize) {
        this.pid = pid;
        this.startSize = startSize;
        hand = new ArrayList<Card>();
        personalDeck = new Deck();
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

        // Quick computation
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

        // // Go through every card in the deck and determine potential starter 
        // // cards
        // while (personalDeck.remainingCards() > 0) {
        //     Card next = personalDeck.takeTopCard();

        //     // If our hand contains this card, it cannot be a starter card
        //     if (!this.hand.contains(next)) {
        //         int points = CribbageScoring.count15Combos(hand, next);
        //         points += CribbageScoring.countPairs(hand, next);
        //         points += CribbageScoring.countRuns(hand, next);
        //         points += CribbageScoring.countFlush(hand, next);
        //         points += CribbageScoring.countNobs(hand, next);
        //         totalPoints += points;

        //         // Adjust the total points based on who owns the crib. If this 
        //         // AI owns the crib, it will earn whatever points were put in 
        //         // there. Otherwise, we want to minimize the number of points
        //         // provided to the opponent if they own the crib
        //         if (ownsCrib) {
        //             totalPoints += findBestCribScore(sentToCrib, next);
        //         } else {
        //             totalPoints -= findBestCribScore(sentToCrib, next);
        //         }
        //     }
        // }

        // personalDeck.resetDeck();
        // double expected = 
        //         (double) totalPoints / (Deck.DECK_SIZE - this.hand.size());

        return expected;
    }

    private double findBestCribScore(List<Card> sentToCrib, Card starterCard) {
        double expected = 0.0;
        int[] counts = rankCounts();
        counts[starterCard.getRankValue()]--;

        // Quick computation
        for (int i = 1; i <= Deck.CARDS_PER_SUIT; i++) {
            Card next1 = new Card(Suit.SPADE, Card.getRankBasedOnValue(i));
            double firstProbability = (double) counts[i] / 
                    (Deck.DECK_SIZE - this.hand.size() - 1);
            counts[i]--;
            sentToCrib.add(next1);
            for (int j = 1; j <= Deck.CARDS_PER_SUIT; j++) {
                Card next2 = new Card(Suit.SPADE, Card.getRankBasedOnValue(j));

                sentToCrib.add(next2);
                int points = CribbageScoring.count15Combos(sentToCrib, starterCard);
                points += CribbageScoring.countPairs(sentToCrib, starterCard);
                points += CribbageScoring.countRuns(sentToCrib, starterCard);
                double secondProbability = (double) counts[j] / 
                        (Deck.DECK_SIZE - this.hand.size() - 2);
                expected += (double) points * firstProbability * secondProbability;

                sentToCrib.remove(next2);
            }

            counts[i]++;
            sentToCrib.remove(next1);
        }

        return expected;

        // Deck firstDeck = new Deck();
        // Deck secondDeck = new Deck();

        // while (firstDeck.remainingCards() > 0) {
        //     Card next1 = firstDeck.takeTopCard();
        //     if (next1.equals(starterCard) || this.hand.contains(next1)) continue;

        //     sentToCrib.add(next1);
        //     int subPoints = 0;
        //     while (secondDeck.remainingCards() > 0) {
        //         Card next2 = secondDeck.takeTopCard();
        //         if (next2.equals(next1) || next2.equals(starterCard) 
        //                 || this.hand.contains(next1)) continue;

        //         sentToCrib.add(next2);
        //         int points = CribbageScoring.count15Combos(sentToCrib, starterCard);
        //         points += CribbageScoring.countPairs(sentToCrib, starterCard);
        //         points += CribbageScoring.countRuns(sentToCrib, starterCard);
        //         points += CribbageScoring.countFlush(sentToCrib, starterCard);
        //         points += CribbageScoring.countNobs(sentToCrib, starterCard);
        //         subPoints += points;

        //         sentToCrib.remove(next2);
        //     }

        //     totalPoints += 
        //             (double) subPoints / (Deck.DECK_SIZE - this.hand.size() + 1);

        //     secondDeck.resetDeck();
        //     sentToCrib.remove(next1);
        // }
    }

    public Card getOptimalCard(int count, LinkedList<Card> cardStack, 
            List<List<Card>> playedCardsByPlayer) {
        if (hand.size() > HAND_SIZE) {
            throw new IllegalStateException("Must send cards to crib first");
        }

        MCTSNode root = new MCTSNode(null, new ArrayList<Card>(hand));
        root.count = count;
        root.cardStack = cardStack;
        root.remainingCards = new int[playedCardsByPlayer.size()];
        Arrays.fill(root.remainingCards, HAND_SIZE);
        root.rankCounts = rankCounts();
        for (int i = 0; i < playedCardsByPlayer.size(); i++) {
            List<Card> playedCards = playedCardsByPlayer.get(i);
            root.remainingCards[i] = HAND_SIZE - playedCards.size();
            if (i == pid) continue;

            for (Card card : playedCards) {
                root.rankCounts[card.getRankValue()]--;
            }
        }
        root.hand = hand;
        root.pidTurn = pid;

        for (int i = 0; i < ITERATIONS; i++) {
            MCTSNode rolloutRoot = expand(root);
        }
        return null;
    }

    private MCTSNode expand(MCTSNode curr) {
        if (curr.numRollouts == 0) {
            if (curr.pidTurn == pid) {

            }
        }
    }

    private Map<Card, MCTSNode> createChildrenForSelf(List<Card> hand, int count) {
        for (Card card : hand) {
            
        }
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