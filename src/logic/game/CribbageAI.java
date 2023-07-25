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
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;
    private static final int MAX_COUNT = 31;
    private static final int ITERATIONS = 1000;

    private int pid;
    private int numPlayers;
    private int startSize;
    private List<Card> hand;

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
            this.hand = currentHand;
        }

        public double getUCTValue() {
            if (numRollouts == 0 || parent == null) {
                return 0;
            }

            return pointsEarned / numRollouts + CONSTANT * 
                    Math.sqrt(Math.log(parent.numRollouts) / numRollouts);
        }
    }
    
    public CribbageAI(int pid, int numPlayers) {
        if (numPlayers != 2 && numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("PID is invalid, must be between 0 and " + numPlayers);
        }

        this.pid = pid;
        this.numPlayers = numPlayers;

        if (numPlayers == 2) {
            startSize = TWO_PLAYER_START_SIZE;
        } else if (numPlayers == 3) {
            startSize = THREE_PLAYER_START_SIZE;
        }

        hand = new ArrayList<Card>();
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

    // Recursively finds the 4-card hand with the most expected points given 
    // a 6-card hand (6 choose 4). NOTE: Ignores suits in the calculation
    // Total long runtime: 15 possible combos of 4 cards * 46 possible starter
    // cards * 1980 (45 * 44) possible cribs = 1,366,200 iterations
    // Total short runtime: 15 possible combos * 13 possible starter cards
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

        // Debug statements
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

    // public Card getOptimalCard(int count, LinkedList<Card> cardStack, 
    //         List<List<Card>> playedCardsByPlayer) {
    //     if (hand.size() > HAND_SIZE) {
    //         throw new IllegalStateException("Must send cards to crib first");
    //     }

    //     MCTSNode root = new MCTSNode(null, new ArrayList<Card>(hand));
    //     root.count = count;
    //     root.cardStack = cardStack;
    //     root.remainingCards = new int[playedCardsByPlayer.size()];
    //     Arrays.fill(root.remainingCards, HAND_SIZE);
    //     root.rankCounts = rankCounts();
    //     for (int i = 0; i < playedCardsByPlayer.size(); i++) {
    //         List<Card> playedCards = playedCardsByPlayer.get(i);
    //         root.remainingCards[i] = HAND_SIZE - playedCards.size();
    //         if (i == pid) continue;

    //         for (Card card : playedCards) {
    //             root.rankCounts[card.getRankValue()]--;
    //         }
    //     }
    //     root.hand = hand;
    //     root.pidTurn = pid;

    //     for (int i = 0; i < ITERATIONS; i++) {
    //         MCTSNode rolloutRoot = expand(root);
    //     }
    //     return null;
    // }

    private int[] rankCounts() {
        int[] counts = new int[Deck.CARDS_PER_SUIT + 1];
        Arrays.fill(counts, Deck.CARDS_PER_RANK);
        for (Card card : this.hand) {
            counts[card.getRankValue()]--;
        }

        return counts;
    }
}