package test.logic.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.CribbageManager;
import logic.deck.*;

import java.util.ArrayList;
import java.util.List;


public class TestSecondStage {
    private static final int NUM_PLAYERS = 2;
    private final CribbageManager man = new CribbageManager(NUM_PLAYERS);

    private class CribbageManagerTest extends CribbageManager {
        public CribbageManagerTest(int numPlayers) {
            super(numPlayers);
        }

        
    }

    @Test
    public void testSingleRound() {
        List<List<Card>> hands = setupHands(0);
        int currentPlayer = 0;
        while (man.inPlay()) {
            if (man.hasPlayableCard(currentPlayer)) {

            }

            currentPlayer = (currentPlayer + 1) % NUM_PLAYERS;
        }
    }

    // Assumes that all tests in TestGameSetup are passing
    private List<List<Card>> setupHands(int dealerId) {
        man.setDealer(dealerId);
        List<List<Card>> hands = man.dealHands();
        List<Card> crib = man.getCrib();
        while (crib.size() < 4) {
            for (int i = 0; i < NUM_PLAYERS; i++) {
                man.sendCardToCrib(i, hands.get(i).get(0));
            }
        }

        return hands;
    }
}