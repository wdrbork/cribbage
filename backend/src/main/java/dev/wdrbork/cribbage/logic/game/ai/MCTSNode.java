package dev.wdrbork.cribbage.logic.game.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import dev.wdrbork.cribbage.logic.cards.Card;

// Represents a node in a Monte Carlo search tree. Used when deciding what
// card to play during the second stage of Cribbage
public class MCTSNode {
    private static final double UCT_CONSTANT = 1.0;

    public Card playedCard;
    public int pidTurn;

    // Node fields
    public long pointsEarned = 0;
    public int numRollouts = 0;
    public MCTSNode parent;

    // Maps a card rank to the game state (i.e. the node) that follows from 
    // the playing of a card of that rank 
    public Set<MCTSNode> children;

    public MCTSNode() {
        this(null);
    }

    public MCTSNode(MCTSNode parent) {
        this.parent = parent;
        this.children = new HashSet<MCTSNode>();
    }

    public void addChildren(Set<MCTSNode> children) {
        this.children.addAll(children);
    }

    public MCTSNode chooseHighValueChild() {
        if (children.isEmpty()) {
            return null;
        }

        double maxValue = -Double.MAX_VALUE;
        List<MCTSNode> selections = new ArrayList<MCTSNode>();
        for (MCTSNode child : children) {
            double value = child.getUCTValue(UCT_CONSTANT);
            if (value > maxValue) {
                selections.clear();
                selections.add(child);
                maxValue = value;
            } else if (value == maxValue) {
                selections.add(child);
            }
        }

        Random r = new Random();
        int randomIdx = r.nextInt(selections.size());
        return selections.get(randomIdx);
    }

    public MCTSNode chooseMostExpandedChild() {
        if (children.isEmpty()) {
            return null;
        }

        int mostRollouts = 0;
        List<MCTSNode> selections = new ArrayList<MCTSNode>();
        for (MCTSNode child : children) {
            int value = child.numRollouts;
            System.out.print("Card = " + child.playedCard);
            System.out.print(", pointsEarned = " + child.pointsEarned);
            System.out.print(", numRollouts = " + child.numRollouts);
            System.out.print(", parentVisits = " + child.parent.numRollouts);
            System.out.println(", UCT = " + child.getUCTValue(UCT_CONSTANT));
            if (value > mostRollouts) {
                selections.clear();
                selections.add(child);
                mostRollouts = value;
            } else if (value == mostRollouts) {
                selections.add(child);
            }
        }

        Random r = new Random();
        int randomIdx = r.nextInt(selections.size());
        return selections.get(randomIdx);
    }

    public double getUCTValue(double constant) {
        if (numRollouts == 0) {
            // If the constant is not 0, return the highest value possible
            // (we want to rollout from here)
            return constant == 0 ? 0 : Double.MAX_VALUE;
        }

        double pointRatio = (double) pointsEarned / numRollouts;
        return pointRatio + constant * 
                Math.sqrt(Math.log(parent.numRollouts) / numRollouts);
    }
}