package logic.game.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final int GO_KEY = 0;

    private CribbageManager gameState;
    private CribbageManager simulator;
    private int[] lowestPlayableCards;
    private MCTSNode root;
    private int pid;

    // Debug fields
    private int numberOfNodes;
    private int loops = 0;

    /**
     * Constructs an MCTSAgent.
     * 
     * @param currentState the current state of some cribbage game
     * @param pid the PID of the AI associated with this agent
     */
    public MCTSAgent(CribbageManager currentState, int pid) {
        gameState = currentState;
        lowestPlayableCards = new int[gameState.numPlayers()];
        this.root = new MCTSNode();
        root.playedCard = gameState.getLastPlayedCard();
        root.pidTurn = gameState.lastToPlayCard();
        this.pid = pid;

        this.numberOfNodes = 1;
    }

    public Card selectCard() {
        if (gameState.gameOver()) {
            return null;
        }

        search();
        MCTSNode bestMove = root.chooseMostExpandedChild();
        return bestMove.playedCard;
    }

    private void search() {
        int searches = 0;

        while (searches < ITERATIONS) {
            loops = 0;
            Arrays.fill(lowestPlayableCards, 1);
            MCTSNode selection = nodeSelection();
            int pointsEarned = rollout();
            backup(selection, pointsEarned);
            searches++;
        }
    }

    private MCTSNode nodeSelection() {
        MCTSNode curr = root;
        simulator = new CribbageManager(gameState);
        
        // Clear hands that are not this AI's
        for (int i = 0; i < simulator.numPlayers(); i++) {
            if (i != pid) {
                simulator.clearHand(i);
            }
        }

        // Stop searching once we find a leaf node
        while (!curr.children.isEmpty()) {
            // Find node with the highest UCT value
            curr = curr.chooseHighValueChild();
            assert(curr != null);
            simulator.setNextPlayer(curr.pidTurn);
            playCardInSimulation(curr);

            // If this node has not been expanded, select it for rollout
            if (curr.numRollouts == 0) return curr;
        }

        // Generate children for this leaf node, if possible, and select 
        // one of the children for the rollout
        if (expandSelection(curr)) {
            curr = curr.chooseHighValueChild();
            simulator.setNextPlayer(curr.pidTurn);
            playCardInSimulation(curr);
        }

        return curr;
    }

    private int rollout() {
        int pointsEarned = 0;
        Random r = new Random();

        // Fill opponent hands with random cards
        for (int i = 0; i < simulator.numPlayers(); i++) {
            if (i == pid) continue;

            // System.out.println("Original hand: " + simulator.getHand(i));

            while (simulator.getHand(i).size() < HAND_SIZE) {
                loops++;
                if (loops == 1000) {
                    System.out.println(i);
                    System.out.println(simulator.getAllHands());
                    System.out.println(simulator.getPlayedCards());
                    System.out.println(simulator.count());
                    throw new IllegalStateException("Too many loops");
                }

                int val = r.nextInt(Deck.CARDS_PER_SUIT) + 1;
                Rank rank = Card.getRankBasedOnValue(val);
                Suit suit = getPossibleSuit(rank);

                // If there is no available suit for this rank, try again
                if (suit == null) {
                    continue; 
                }

                Card card = new Card(suit, rank);
                simulator.addCardToHand(i, card);
            }
        }

        // Make sure that every hand has 4 cards
        for (int i = 0; i < simulator.numPlayers(); i++) {
            assert(simulator.getHand(i).size() == HAND_SIZE);
        }

        while (!simulator.roundOver()) {
            loops++;
            if (loops == 1000) {
                throw new IllegalStateException("Too many loops");
            }

            if (!simulator.movePossible()) {
                // If the count is not 31 and nobody can play a card, give 
                // the last player to play a card a single point
                if (!simulator.countIs31()) {
                    simulator.awardPointsForGo();

                    // If this AI was the last to play a card, include the 
                    // point as part of the rollout
                    if (simulator.nextToPlayCard() == pid) {
                        pointsEarned++;
                    } else {
                        pointsEarned--;
                    }
                }
                simulator.resetCount();
            }

            int nextPlayer = simulator.nextToPlayCard();
            List<Card> hand = simulator.getHand(nextPlayer);
            List<Card> possibleCards = new ArrayList<Card>();
            for (Card card : hand) {
                if (simulator.canPlayCard(card)) {
                    possibleCards.add(card);
                }
            }

            // If this player can't play anymore cards, skip to the next player
            if (possibleCards.isEmpty()) {
                nextPlayer = (nextPlayer + 1) % simulator.numPlayers();
                simulator.setNextPlayer(nextPlayer);
                continue;
            }

            int idx = r.nextInt(possibleCards.size());
            int points = simulator.playCard(nextPlayer, possibleCards.get(idx));
            if (nextPlayer == pid) {
                pointsEarned += points;
            } else {
                pointsEarned -= points;
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
        if (simulator.roundOver()) return false;

        // If this AI was the last player to play a card, expand the hand of 
        // another player
        if (simulator.nextToPlayCard() == pid) {
            node.addChildren(expandOwnHand(node));
        } else {
            // Expand the tree using this AI's hand
            node.addChildren(expandOtherHand(node));
        }

        return true;
    }

    private Map<Integer, MCTSNode> expandOwnHand(MCTSNode parent) {
        Map<Integer, MCTSNode> children = new HashMap<Integer, MCTSNode>();
        List<Card> hand = simulator.getHand(pid);

        for (Card card : hand) {
            if (!simulator.canPlayCard(card)) {
                continue;
            }

            int rank = card.getRankValue();
            MCTSNode child = new MCTSNode(parent);
            child.playedCard = card;
            child.pidTurn = pid;
            children.put(rank, child);
        }

        // If this AI can't play a card, add a node indicating a go
        if (children.isEmpty()) {
            MCTSNode child = new MCTSNode(parent);
            child.playedCard = null;
            child.pidTurn = pid;
            children.put(GO_KEY, child);
        }

        this.numberOfNodes += children.size();
        return children;
    }

    private Map<Integer, MCTSNode> expandOtherHand(MCTSNode parent) {
        Map<Integer, MCTSNode> children = new HashMap<Integer, MCTSNode>();
        int nextPid = (parent.pidTurn + 1) % simulator.numPlayers();
        int maxCardPossible = Math.min(10, MAX_COUNT - simulator.count());

        // If we can play a face card, adjust the max card possible so that 
        // face cards are included in the expansion
        if (maxCardPossible == 10) maxCardPossible = 13;

        if (simulator.getHand(nextPid).size() > HAND_SIZE) {
            System.out.println(simulator.count());
            System.out.println(nextPid);
            System.out.println(simulator.getAllHands());
            System.out.println(simulator.getPlayedCards());
            throw new IllegalStateException("Hand has more than 4 cards");
        }
        if (simulator.getHand(nextPid).size() < HAND_SIZE) {
            // System.out.println("Expansion lower bound = " + lowestPlayableCards[nextPid]);
            for (int i = lowestPlayableCards[nextPid]; i <= maxCardPossible; i++) {
                Rank rank = Card.getRankBasedOnValue(i);
                
                // Find a possible suit for a card of this rank. If there is 
                // no available suit, try again
                Suit suit = getPossibleSuit(rank);
                if (suit == null) continue;

                Card possibleCard = new Card(suit, rank);

                // If this card can't be played, skip it
                if (!simulator.canPlayCard(possibleCard)) {
                    continue;
                }

                MCTSNode child = new MCTSNode(parent);
                child.playedCard = possibleCard;
                child.pidTurn = nextPid;
                children.put(i, child);
            }
        }

        // If there are cards that cannot be played because they would exceed 
        // the max count of 31, it is possible for this player to call go, so 
        // add a node that signifies this
        if (simulator.getHand(nextPid).size() == HAND_SIZE
                || maxCardPossible != 13) {
            MCTSNode child = new MCTSNode(parent);
            child.playedCard = null;
            child.pidTurn = nextPid;
            children.put(GO_KEY, child);
        }

        this.numberOfNodes += children.size();
        return children;
    }

    private Suit getPossibleSuit(Rank rank) {
        for (Suit suit : Suit.values()) {
            Card testCard = new Card(suit, rank);
            if (!simulator.cardAlreadyPlayed(testCard)
                    && !simulator.getHand(pid).contains(testCard)) {
                return suit;
            }
        }

        return null;
    }

    private int playCardInSimulation(MCTSNode curr) {
        int points = 0;

        // System.out.println("Play card " + curr.playedCard + " with count " + simulator.count());

        // If the played card is null, that indicates that this player 
        // called go, so no card is played
        if (curr.playedCard == null) {
            int lowestPlayable = lowestPlayableCards[curr.pidTurn];
            lowestPlayableCards[curr.pidTurn] = Math.max(lowestPlayable, 
                    MAX_COUNT - simulator.count() + 1);
            // System.out.println(lowestPlayableCards[curr.pidTurn]);
            
            if (canResetCount(curr)) {
                // System.out.println("Last played card = " + simulator.getLastPlayedCard());
                // System.out.println("pidTurn = " + curr.pidTurn);
                // System.out.println("Lowest playable card = " + lowestPlayableCards[curr.pidTurn]);
                if (!simulator.countIs31()) {
                    points++;
                    simulator.awardPointsForGo();
                }
                simulator.resetCount();
                System.out.println("Reset count");
            }
        } else if (curr.playedCard != null) {
            // If it is not this player's turn, manually add the card 
            // to their hand
            if (curr.pidTurn != pid) {
                simulator.addCardToHand(curr.pidTurn, curr.playedCard);
            }
            assert(simulator.canPlayCard(curr.playedCard));
            points = simulator.playCard(curr.pidTurn, curr.playedCard);
            if (simulator.countIs31()) {
                simulator.resetCount();
            }
        }            

        int nextPlayer = (curr.pidTurn + 1) % simulator.numPlayers();
        simulator.setNextPlayer(nextPlayer);
        return points;
    }

    private boolean canResetCount(MCTSNode node) {
        if (simulator.countIs31()) {
            return true;
        }

        // If a card was played for this node, return false
        if (node.playedCard != null) {
            return false;
        }

        // If the parent to the passed-in node is the root, or if there are 
        // three players and the parent to the passed-in node's parent is the 
        // root, special rules apply
        MCTSNode parent = node.parent;
        if (parent == root || (simulator.numPlayers() == 3
                && parent.parent == root)) {
            // Note that the pidTurn field in root represents the last player
            // to play a card. If this is equal to the pidTurn of this node, 
            // that means nobody can play a card, so a reset is possible
            return root.pidTurn == node.pidTurn;
        }



        // If every player in the game has called go, a reset is possible.
        return parent.playedCard == null && (simulator.numPlayers() == 2 
            || parent.parent.playedCard == null);
    }
}