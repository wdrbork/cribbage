package dev.wdrbork.cribbage.logic.game.ai;

import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

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
    
    public SmartPlayer(CribbageManager gameState, int pid) {
        int numPlayers = gameState.numPlayers();
        if (numPlayers != 2 && numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("PID is invalid, must be between 0 and " + numPlayers);
        }

        this.gameState = gameState;
        this.pid = pid;
    }

    public CribbageHand choosePlayingHand() {
        CribbageHand currentHand = gameState.getHand(pid);
        if (currentHand.size() < 5) {
            // Playing hand has already been chosen
            return currentHand;
        }

        Map<CribbageHand, Double> savedCounts = new HashMap<CribbageHand, Double>();
        boolean isDealer = false;
        if (gameState.dealer() == pid) isDealer = true;
        CribbageHand playingHand = maximizePoints(currentHand, isDealer, 
                new CribbageHand(), 0, savedCounts);
        return playingHand;
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
    private CribbageHand maximizePoints(CribbageHand original, boolean isDealer,
            CribbageHand soFar, int idx, Map<CribbageHand, Double> savedCounts) {
        // If soFar represents a full hand, determine the expected number of 
        // points and save this information
        if (soFar.size() == HAND_SIZE) {
            double bestExpected = findBestPossibleCount(soFar, isDealer);
            CribbageHand deepCopy = new CribbageHand(soFar);
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
            CribbageHand notApplicable = new CribbageHand();
            savedCounts.put(notApplicable, -Double.MAX_VALUE);
            return notApplicable;
        }

        // Compare the expected points from including the card at this idx with
        // the expected points from ignoring it
        soFar.addCard(original.getCard(idx));
        CribbageHand includeIdx = 
                maximizePoints(original, isDealer, soFar, idx + 1, savedCounts);
        soFar.removeCard(original.getCard(idx));
        CribbageHand excludeIdx = 
                maximizePoints(original, isDealer, soFar, idx + 1, savedCounts);

        // Print the expected scores of the hands to be compared
        // System.out.println(includeIdx + " (expected = " + savedCounts.get(includeIdx) + 
        //         ") vs. " + excludeIdx + " (expected = " + savedCounts.get(excludeIdx) + 
        //         ")");      
        
        return savedCounts.get(includeIdx) >= savedCounts.get(excludeIdx) ?
                includeIdx : excludeIdx;
    }

    private double findBestPossibleCount(CribbageHand hand, boolean ownsCrib) {
        // Use the given hand and the starting hand to infer which cards have 
        // been sent to the crib
        CribbageHand currentHand = gameState.getHand(pid);
        Deck sentToCrib = new Deck();
        for (Card card : currentHand.getCards()) {
            if (!hand.contains(card)) {
                sentToCrib.addCard(card);
            }
        }

        double expected = 0.0;
        int[] counts = rankCounts();

        // Quick computation (ignores suits)
        for (int i = 1; i <= Deck.CARDS_PER_SUIT; i++) {
            Card starter = new Card(Suit.SPADE, Card.getRankBasedOnValue(i));
            for (Suit suit : Suit.values()) {
                starter = new Card(suit, Card.getRankBasedOnValue(i));
                if (!hand.contains(starter) && !sentToCrib.contains(starter)) {
                    break;
                }
            }
            int points = CribbageScoring.count15Combos(hand, starter);
            points += CribbageScoring.countPairs(hand, starter);
            points += CribbageScoring.countRuns(hand, starter);
            points += CribbageScoring.countFlush(hand, starter, false);
            points += CribbageScoring.countNobs(hand, starter);
            double cardProbability = (double) counts[i] / 
                    (StandardDeck.DECK_SIZE - currentHand.size());
            expected += (double) points * cardProbability;

            if (ownsCrib) {
                expected += findBestCribScore(sentToCrib, starter) * cardProbability;
            } else {
                expected -= findBestCribScore(sentToCrib, starter) * cardProbability;
            }
        }

        return expected;
    }

    private double findBestCribScore(Deck sentToCrib, Card starterCard) {
        assert(sentToCrib.size() == 2);
        CribbageHand currentHand = gameState.getHand(pid);
        double expected = 0.0;
        int[] counts = rankCounts();
        counts[starterCard.getRankValue()]--;

        // Quick computation (ignores suits)
        for (int i = 1; i <= Deck.CARDS_PER_SUIT; i++) {
            Card thirdCard = new Card(1, 1);
            for (Suit suit : Suit.values()) {
                thirdCard = new Card(suit, Card.getRankBasedOnValue(i));
                if (!thirdCard.equals(starterCard) && 
                        sentToCrib.addCard(thirdCard)) {
                    break;
                }
            }

            // Find the probability of a card of this rank ending up in the 
            // crib (e.g. if our 6-card hand contains 4 aces, we should not 
            // expect there to be another ace in the crib, so the probability 
            // would be zero)
            double thirdCardRankProbability = (double) counts[i] / 
                    (StandardDeck.DECK_SIZE - currentHand.size() - 1);

            // Temporarily decrement the count for this rank
            counts[i]--;

            for (int j = 1; j <= Deck.CARDS_PER_SUIT; j++) {
                Card fourthCard = new Card(1, 1);
                for (Suit suit : Suit.values()) {
                    fourthCard = new Card(suit, Card.getRankBasedOnValue(j));
                    if (!fourthCard.equals(starterCard) && 
                            sentToCrib.addCard(fourthCard)) {
                        break;
                    }
                }

                // If we are already using all four cards of this rank value, \
                // skip to the next rank
                if (sentToCrib.size() != 4) continue;

                // Find the expected points from this crib
                int points = CribbageScoring.count15Combos(sentToCrib, starterCard);
                points += CribbageScoring.countPairs(sentToCrib, starterCard);
                points += CribbageScoring.countRuns(sentToCrib, starterCard);

                // Find the probability of a card of this rank ending up in the
                // crib
                double fourthCardRankProbability = (double) counts[j] / 
                        (StandardDeck.DECK_SIZE - currentHand.size() - 2);

                // Add the expected points from this crib to the overall total
                // (with respect to the probability of this crib occurring)
                expected += (double) points * 
                        thirdCardRankProbability * fourthCardRankProbability;

                sentToCrib.removeCard(fourthCard);
            }

            counts[i]++;
            sentToCrib.removeCard(thirdCard);
        }

        // If the two cards in the crib are of the same suit, a flush is 
        // possible, so increase the expected point total of the crib based 
        // on the probability of the flush occurring
        if (sentToCrib.getCard(0).getSuit() == sentToCrib.getCard(1).getSuit()) {
            Suit sharedSuit = sentToCrib.getCard(0).getSuit();
            int cardsOfSuitAvailable = Deck.CARDS_PER_SUIT;
            for (Card card : currentHand.getCards()) {
                if (card.getSuit() == sharedSuit) {
                    cardsOfSuitAvailable--;
                }
            }

            double numerator = (double) cardsOfSuitAvailable * 
                    (cardsOfSuitAvailable - 1);
            double denominator = (StandardDeck.DECK_SIZE - currentHand.size() - 1) * 
                    (StandardDeck.DECK_SIZE - currentHand.size() - 2);
            double flushProbability = numerator / denominator;
                                    
            expected += 4 * flushProbability;
        }

        return expected;
    }

    private int[] rankCounts() {
        int[] counts = new int[Deck.CARDS_PER_SUIT + 1];
        Arrays.fill(counts, Deck.CARDS_PER_RANK);
        for (Card card : gameState.getHand(pid).getCards()) {
            counts[card.getRankValue()]--;
        }

        return counts;
    }
}