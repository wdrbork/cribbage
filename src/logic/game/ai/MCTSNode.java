package logic.game.ai;

import logic.game.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

// Represents a node in a Monte Carlo search tree. Used when deciding what
// card to play during the second stage of Cribbage
public class MCTSNode {
    private static final double UCT_CONSTANT = .5;

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
            // If the constant is not 0, return the highest value possible
            // (we want to rollout from here)
            return constant == 0 ? 0 : Double.MAX_VALUE;
        }

        return pointsEarned / numRollouts + constant * 
                Math.sqrt(Math.log(parent.numRollouts) / numRollouts);
    }
}