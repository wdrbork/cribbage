package logic.game.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import logic.deck.Card;
import logic.deck.Deck;
import logic.deck.Rank;
import logic.deck.Suit;
import logic.game.*;

// Makes decisions for the AI using Monte Carlo tree search (for more 
// information on this algorithm, see the following link: 
// https://en.wikipedia.org/wiki/Monte_Carlo_tree_search). Currently used 
// for making decisions during the second stage of play, but may eventually
// be used for the first stage as well
public class MCTSAgent {
    private static final int ITERATIONS = 1000;
    private static final int MAX_COUNT = 31;
    private static final int HAND_SIZE = 4;

    private MCTSNode root;
    private int pid;

    /**
     * Constructs an MCTSAgent
     * @param currentState the current state of some cribbage game
     * @param pid the PID of the AI associated with this agent
     */
    public MCTSAgent(CribbageManager currentState, int pid) {
        // In order to avoid exceptions that are thrown when playing a 
        // card that isn't in a player's hand, we must clear all hands in 
        // the game state that are not this AI's. This will come in handy
        // when we are running simulations of different possible plays
        List<List<Card>> hands = currentState.getAllHands();
        for (int i = 0; i < currentState.numPlayers(); i++) {
            if (i != pid) {
                hands.get(i).clear();
            }
        }

        this.root = new MCTSNode();
        this.root.currentState = currentState;
        this.pid = pid;
    }

    public Card selectCard() {
        return null;
    }

    private MCTSNode nodeSelection() {
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

    private int rollout(CribbageManager state) {
        int pointDiff = 0;
        Random r = new Random();
        while (!isTerminalState(state)) {
            int next = state.nextPlayer();
            if (next == pid) {
                List<Card> hand = state.getHand(pid);
                List<Card> possibleCards = new ArrayList<Card>();
                for (Card card : hand) {
                    if (!state.cardAlreadyPlayed(pid, card)) {
                        possibleCards.add(card);
                    }
                }
                assert(possibleCards.size() > 0) : "Can't play anymore cards";

                int idx = r.nextInt(possibleCards.size());
                pointDiff += state.playCard(pid, possibleCards.get(idx));
                if (!state.canPlayCard()) {
                    // If the count is not 31 and nobody can play a card, give
                    // ourselves a point
                    if (state.count() != MAX_COUNT) {
                        pointDiff += 1;
                    }
                    state.resetCount();
                }
            } else {
                int value = r.nextInt(Deck.CARDS_PER_SUIT);
                Rank rank = Card.getRankBasedOnValue(value);

                int occurrences = 0;
                for (Card card : state.getPlayedCards().get(next)) {
                    if (card.getRank() == rank) {
                        occurrences++;
                    }
                }
                Suit suit = Suit.values()[occurrences];
                
                Card playedCard = new Card(suit, rank);
                pointDiff -= state.playCard(next, playedCard);
                if (!state.canPlayCard()) {
                    if (state.count() != MAX_COUNT) {
                        pointDiff -= 1;
                    }
                    state.resetCount();
                }
            }
        }

        return pointDiff;
    }

    private boolean expand(MCTSNode node) {
        CribbageManager currentState = node.currentState;

        // If no more cards can be played, return false
        if (isTerminalState(currentState)) return false;

        // If the next player to play a card is this AI, go through its 
        // hand and add nodes for cards that haven't already been played
        if (currentState.nextPlayer() == pid) {
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
            if (!nextState.canPlayCard()) {
                nextState.resetCount();
            }

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
        int otherPid = state.nextPlayer();
        List<Card> playedCards = state.getPlayedCards().get(otherPid);

        for (int i = 1; 
                i < Math.min(10, MAX_COUNT - state.count());
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
            if (!state.canPlayCard()) {
                state.resetCount();
            }

            MCTSNode child = new MCTSNode(parent);
            child.currentState = nextState;
            children.put(i, child);
        }

        return children;
    }

    private boolean isTerminalState(CribbageManager state) {
        // If a player has won the game, return true
        if (state.gameOver()) return true;

        // If no more cards can be played, return true
        for (List<Card> playedCards : state.getPlayedCards()) {
            if (playedCards.size() != HAND_SIZE) {
                return false;
            }
        }
        return true;
    }
}