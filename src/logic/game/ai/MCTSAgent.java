package logic.game.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import logic.deck.*;
import logic.game.*;

// Makes decisions for the AI using Monte Carlo tree search (for more 
// information on this algorithm, see the following link: 
// https://en.wikipedia.org/wiki/Monte_Carlo_tree_search). Currently used 
// for making decisions during the second stage of play, but may eventually
// be used for the first stage as well
public class MCTSAgent {
    private static final int ITERATIONS = 10000;
    private static final int MAX_COUNT = 31;
    private static final int HAND_SIZE = 4;

    private CribbageManager gameState;
    private CribbageManager selectedState;
    private MCTSNode root;
    private int pid;

    // Debug field
    private int numberOfNodes;

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
        gameState = new CribbageManager(currentState);
        List<List<Card>> hands = gameState.getAllHands();
        for (int i = 0; i < gameState.numPlayers(); i++) {
            if (i != pid) {
                hands.get(i).clear();
            }
        }

        this.root = new MCTSNode();
        this.pid = pid;

        this.numberOfNodes = 1;
    }

    public Card selectCard() {
        if (gameState.gameOver()) {
            return null;
        }

        search();
        MCTSNode bestMove = root.chooseMostExpandedChild();
        System.out.println(numberOfNodes);
        return bestMove.playedCard;
    }

    private void search() {
        int searches = 0;

        while (searches < ITERATIONS) {
            // System.out.println("Search " + searches);
            MCTSNode selection = nodeSelection();
            int pointsEarned = rollout();
            backup(selection, pointsEarned);
            searches++;
        }
    }

    private MCTSNode nodeSelection() {
        MCTSNode curr = root;
        selectedState = new CribbageManager(gameState);

        // Stop searching once we find a leaf node
        while (!curr.children.isEmpty()) {
            // Find node with the highest UCT value
            curr = curr.chooseHighValueChild();
            assert(curr != null);
            assert(selectedState.nextPlayer() == curr.pidTurn);
            assert(selectedState.canPlayCard(curr.playedCard));
            // System.out.println(curr.playedCard);
            selectedState.playCard(curr.pidTurn, curr.playedCard);
            if (!selectedState.movePossible()) {
                selectedState.resetCount();
            }

            // If this node has not been expanded, select it for rollout
            if (curr.numRollouts == 0) return curr;
        }

        // Generate children for this leaf node, if possible, and select 
        // one of the children for the rollout
        if (expandSelection(curr)) {
            curr = curr.chooseHighValueChild();
            assert(selectedState.canPlayCard(curr.playedCard));
            selectedState.playCard(curr.pidTurn, curr.playedCard);
            if (!selectedState.movePossible()) {
                selectedState.resetCount();
            }
        }

        return curr;
    }

    private int rollout() {
        int pointsEarned = 0;
        Random r = new Random();
        while (!isTerminalState(selectedState)) {
            if (!selectedState.movePossible()) {
                // If the count is not 31 and nobody can play a card, give 
                // the last player to play a card a single point
                if (selectedState.count() != MAX_COUNT) {
                    selectedState.awardPointsForGo();

                    // If this AI was the last to play a card, include the 
                    // point as part of the rollout
                    if (selectedState.nextPlayer() == pid) pointsEarned++;
                }
                selectedState.resetCount();
            }

            int next = selectedState.nextPlayer();
            if (next == pid) {
                List<Card> hand = selectedState.getHand(pid);
                List<Card> possibleCards = new ArrayList<Card>();
                for (Card card : hand) {
                    if (!selectedState.cardAlreadyPlayed(pid, card)) {
                        possibleCards.add(card);
                    }
                }
                assert(possibleCards.size() > 0) : "Can't play anymore cards";

                int idx = r.nextInt(possibleCards.size());
                if (!selectedState.canPlayCard(possibleCards.get(idx))) {
                    continue;
                }
                pointsEarned +=selectedState.playCard(pid, possibleCards.get(idx));
            } else {
                // Get a value between 1 and 13
                int value = r.nextInt(Deck.CARDS_PER_SUIT - 1) + 1;
                Rank rank = Card.getRankBasedOnValue(value);

                // If all 4 cards of this rank have been played, try again
                int occurrences = knownRankCount(rank);
                if (occurrences == 4) continue;
                Suit suit = Suit.values()[occurrences];
                
                Card playedCard = new Card(suit, rank);
                if (!selectedState.canPlayCard(playedCard)) {
                    continue;
                }
                selectedState.playCard(next, playedCard);
            }
        }

        return pointsEarned;
    }

    private void backup(MCTSNode curr, int points) {
        if (curr == null) {
            return;
        }

        curr.numRollouts++;
        if (curr.pidTurn == pid) {
            curr.pointsEarned += points;
        }
        backup(curr.parent, points);
    }

    private boolean expandSelection(MCTSNode node) {
        // If no more cards can be played, return false
        if (isTerminalState(selectedState)) return false;

        // If the next player to play a card is this AI, go through its 
        // hand and add nodes for cards that haven't already been played
        if (selectedState.nextPlayer() == pid) {
            node.addChildren(expandOwnHand(node));
        } else {
            // Add nodes to the tree using cards that could possibly be 
            // played
            node.addChildren(expandOtherHand(node));
        }

        return true;
    }

    private Map<Integer, MCTSNode> expandOwnHand(MCTSNode parent) {
        Map<Integer, MCTSNode> children = new HashMap<Integer, MCTSNode>();
        List<Card> hand = selectedState.getHand(pid);

        for (Card card : hand) {
            if (selectedState.cardAlreadyPlayed(pid, card) 
                    || !selectedState.canPlayCard(card)) {
                continue;
            }

            int rank = card.getRankValue();
            MCTSNode child = new MCTSNode(parent);
            child.playedCard = card;
            child.pidTurn = pid;
            children.put(rank, child);
        }

        this.numberOfNodes += children.size();
        return children;
    }

    private Map<Integer, MCTSNode> expandOtherHand(MCTSNode parent) {
        Map<Integer, MCTSNode> children = new HashMap<Integer, MCTSNode>();
        int otherPid = selectedState.nextPlayer();
        List<Card> playedCards = selectedState.getPlayedCards().get(otherPid);

        for (int i = 1; 
                i <= Math.min(10, MAX_COUNT - selectedState.count());
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
            // If all 4 cards of this rank have been played, skip it
            if (occurrences == 4) continue;
            Suit suit = Suit.values()[occurrences];

            Card possibleCard = new Card(suit, rank);
            MCTSNode child = new MCTSNode(parent);
            child.playedCard = possibleCard;
            child.pidTurn = otherPid;
            children.put(i, child);
        }

        this.numberOfNodes += children.size();
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

    private int knownRankCount(Rank rank) {
        int occurrences = 0;

        // Search through our own hand first
        List<Card> hand = gameState.getHand(pid);
        for (Card card : hand) {
            if (card.getRank() == rank) {
                occurrences++;
            }
        }

        // Search through cards that have been played by other players
        List<List<Card>> allPlayedCards = selectedState.getPlayedCards();
        for (int i = 0; i < selectedState.numPlayers(); i++) {
            if (i == pid) continue;

            List<Card> pidPlayedCards = allPlayedCards.get(i);
            for (Card card : pidPlayedCards) {
                if (card.getRank() == rank) {
                    occurrences++;
                }
            }
        }
        if (occurrences > Deck.CARDS_PER_RANK) {
            throw new IllegalStateException(occurrences + " instances of " + rank);
        }
        assert(occurrences <= Deck.CARDS_PER_RANK);
        return occurrences;
    }
}