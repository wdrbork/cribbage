package logic.game;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

import logic.deck.*;

/**
 * AI for a game of cribbage. Calculates the most optimal play at each stage 
 * of the game and suggests that option.
 */
public class CribbageAI {
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;
    private static final int MAX_COUNT = 31;
    private static final int ITERATIONS = 1000;
    private static final double UCT_CONSTANT = .5;

    private CribbageManager gameState;
    private int pid;
    private int numPlayers;
    private List<Card> hand;

    // Makes decisions for the AI using Monte Carlo tree search (for more 
    // information on this algorithm, see the following link: 
    // https://en.wikipedia.org/wiki/Monte_Carlo_tree_search). Currently used 
    // for making decisions during the second stage of play, but may eventually
    // be used for the first stage as well
    private class MCTSAgent {
        private MCTSNode root;

        public MCTSAgent(CribbageManager currentState) {
            // In order to avoid exceptions that are thrown when playing a 
            // card that isn't in a player's hand, we must clear all hands in 
            // the game state that are not this AI's. This will come in handy
            // when we are running simulations of different possible plays
            for (int i = 0; i < currentState.numPlayers; i++) {
                if (i != pid) {
                    currentState.hands.get(i).clear();
                }
            }

            this.root = new MCTSNode();
            this.root.currentState = currentState;
        }

        public MCTSNode nodeSelection() {
            MCTSNode curr = root;

            // Stop searching once we find a leaf node
            while (!curr.children.isEmpty()) {
                // Find node with the highest UCT value
                curr = curr.chooseHighValueChild();
                assert(curr != null);

                // If this node has not been expanded, select it for rollout
                if (curr.numRollouts == 0) return curr;
            }

            // Generate children for this leaf node, if possible, and select 
            // one of the children for the rollout
            if (expand(curr)) {
                curr = curr.chooseHighValueChild();
            }

            return curr;
        }

        private int rollout(CribbageManager initialState) {
            
        }

        private boolean expand(MCTSNode node) {
            CribbageManager currentState = node.currentState;

            // If no more cards can be played, return false
            if (isTerminalState(currentState)) return false;

            // If the next player to play a card is this AI, go through its 
            // hand and add nodes for cards that haven't already been played
            if (currentState.nextToPlayCard == pid) {
                node.addChildren(expandOwnHand(node));
            } else {
                // Add nodes to the tree using cards that could possibly be 
                // played
                node.addChildren(expandOtherHand(node));
            }

            return true;
        }

        private Map<Integer, MCTSNode> expandOwnHand(
                MCTSNode parent) {
            Map<Integer, MCTSNode> children = new HashMap<Integer, MCTSNode>();
            CribbageManager state = parent.currentState;
            List<Card> hand = state.getHand(pid);

            for (Card card : hand) {
                if (state.cardAlreadyPlayed(pid, card)) {
                    continue;
                }

                // Create a deep copy of the parent state and play 
                // the current card
                CribbageManager nextState = new CribbageManager(state);
                nextState.playCard(pid, card);

                int rank = card.getRankValue();
                MCTSNode child = new MCTSNode(parent);
                child.currentState = nextState;
                children.put(rank, child);
            }

            return children;
        }

        private Map<Integer, MCTSNode> expandOtherHand(MCTSNode parent) {
            Map<Integer, MCTSNode> children = new HashMap<Integer, MCTSNode>();
            CribbageManager state = parent.currentState;
            int otherPid = state.nextToPlayCard;
            List<Card> playedCards = state.playedCardsByPlayer.get(otherPid);

            for (int i = 1; 
                    i < Math.min(10, MAX_COUNT - state.count);
                    i++) {
                Rank rank = Card.getRankBasedOnValue(i);

                // In order to prevent duplicate cards from being played 
                // by the same player, change the suit based on the number
                // of times this rank has been played by this player already
                int occurrences = 0;
                for (Card card : playedCards) {
                    if (card.getRank() == rank) {
                        occurrences++;
                    }
                }
                Suit suit = Suit.values()[occurrences];

                // Create a deep copy of the parent state and play 
                // the possible card
                Card possibleCard = new Card(suit, rank);
                CribbageManager nextState = new CribbageManager(state);
                state.playCard(otherPid, possibleCard);

                MCTSNode child = new MCTSNode(parent);
                child.currentState = nextState;
                children.put(i, child);
            }

            return children;
        }

        private boolean isTerminalState(CribbageManager state) {
            // If no more cards can be played, return false
            for (List<Card> hand : state.playedCardsByPlayer) {
                if (hand.size() != HAND_SIZE) {
                    return false;
                }
            }
            return true;
        }
    }

    // Represents a node in a Monte Carlo search tree. Used when deciding what
    // card to play during the second stage of Cribbage
    private class MCTSNode {
        // // State fields
        // public List<Card> hand;
        // public int[] remainingCards;
        // public int[] rankCounts;
        // public int count;
        // public LinkedList<Card> cardStack;
        // public int pidTurn;

        public CribbageManager currentState;

        // Node fields
        public int pointsEarned = 0;
        public int numRollouts = 0;
        public MCTSNode parent;

        // Maps a card rank to the game state (i.e. the node) that follows from 
        // the playing of a card of that rank 
        public Map<Integer, MCTSNode> children;

        public MCTSNode() {
            this(null);
        }

        public MCTSNode(MCTSNode parent) {
            this.parent = parent;
        }

        public void addChildren(Map<Integer, MCTSNode> children) {
            for (int rank : children.keySet()) {
                this.children.put(rank, children.get(rank));
            }
        }

        public MCTSNode chooseHighValueChild() {
            if (children.isEmpty()) {
                return null;
            }

            double maxValue = 0;
            List<MCTSNode> selections = new ArrayList<MCTSNode>();
            for (MCTSNode child : children.values()) {
                double value = child.getUCTValue(UCT_CONSTANT);
                if (value > maxValue) {
                    selections.clear();
                    selections.add(child);
                    maxValue = value;
                } else if (value == maxValue) {
                    selections.add(child);
                }
            }

            int randomIdx = (int) Math.random() * selections.size();
            return selections.get(randomIdx);
        }

        public double getUCTValue(double constant) {
            if (numRollouts == 0) {
                // If the constant is 0
                return constant == 0 ? 0 : Double.MAX_VALUE;
            }

            return pointsEarned / numRollouts + constant * 
                    Math.sqrt(Math.log(parent.numRollouts) / numRollouts);
        }
    }
    
    public CribbageAI(CribbageManager gameState, int pid) {
        this.numPlayers = gameState.numPlayers;
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

    public List<Card> getOptimalHand(boolean isDealer) {
        Map<List<Card>, Double> savedCounts = new HashMap<List<Card>, Double>();
        hand = maximizePoints(hand, isDealer, 
                new ArrayList<Card>(), 0, savedCounts);
        return hand;
    }

    // Recursively finds the 4-card hand with the most expected points given 
    // a 6-card hand (6 choose 4). NOTE: Ignores suits in the calculation
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
        if (numPlayers == 2) {
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