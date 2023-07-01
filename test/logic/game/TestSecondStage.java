package test.logic.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.CribbageManager;
import logic.deck.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class TestSecondStage {
    private static final int NUM_PLAYERS = 2;

    private CribbageManagerTest man;

    private class CribbageManagerTest extends CribbageManager {
        public CribbageManagerTest(int numPlayers) {
            super(numPlayers);
        }

        public void setHand(int pid, List<Card> hand) {
            hands.set(pid, hand);
        }

        public int playCardByIndex(int pid, int idx) { 
            return playCard(pid, hands.get(pid).get(idx));
        }

        public int getCount() { return count; }

        public LinkedList<Card> getCardStack() { return cardStack; }

        public List<List<Card>> getPlayedCards() { 
            return playedCardsByPlayer; 
        }

        public int[] getGameScores() { return gameScores; }
    }

    @Test
    public void testSingleRoundTwoPlayers() {
        man = new CribbageManagerTest(2);
        setupHands(0);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.SPADE, Rank.EIGHT));
        playerOneHand.add(new Card(Suit.HEART, Rank.EIGHT));
        playerOneHand.add(new Card(Suit.SPADE, Rank.SEVEN));
        playerOneHand.add(new Card(Suit.SPADE, Rank.SIX));
        man.setHand(0, playerOneHand);

        List<Card> playerTwoHand = new ArrayList<Card>();
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.KING));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.SEVEN));
        playerTwoHand.add(new Card(Suit.CLUB, Rank.FOUR));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.FIVE));
        man.setHand(1, playerTwoHand);

        assertTrue(man.inPlay());

        assertTrue(man.hasPlayableCard(0));
        int count = man.playCardByIndex(0, 0);
        int currentCount = man.getCount();
        assertNotEquals(count, -1);
        assertEquals(currentCount, 8);
        assertEquals(man.getPlayedCards().get(0).size(), 1);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[0], 0);

        assertTrue(man.hasPlayableCard(1));
        count = man.playCardByIndex(1, 1);
        currentCount = man.getCount();
        assertNotEquals(count, 2);
        assertEquals(currentCount, 15);
        assertEquals(man.getPlayedCards().get(0).size(), 1);
        assertEquals(man.getCardStack().size(), 1);
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