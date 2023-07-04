package test.logic.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import logic.game.CribbageManager;
import logic.deck.*;

public class TestScoring {
    private static final int NUM_PLAYERS = 2;
    private static final int PLAYER_ONE_ID = 0;
    private static final int PLAYER_TWO_ID = 1;
    private static final int TRIALS = 1000000;

    private CribbageManagerScoreTest man;

    private class CribbageManagerScoreTest extends CribbageManager {
        public CribbageManagerScoreTest(int numPlayers) {
            super(numPlayers);
        }

        public void setHand(int pid, List<Card> hand) { hands.set(pid, hand); }

        public void setStarterCard(Card card) { starterCard = card; }
    }

    @Test
    public void test29Hand() {
        man = new CribbageManagerScoreTest(NUM_PLAYERS);
        List<Card> hand = new LinkedList<Card>();
        hand.add(new Card(Suit.HEART, Rank.JACK));
        hand.add(new Card(Suit.DIAMOND, Rank.FIVE));
        hand.add(new Card(Suit.SPADE, Rank.FIVE));
        hand.add(new Card(Suit.CLUB, Rank.FIVE));
        man.setStarterCard(new Card(Suit.HEART, Rank.FIVE));
        man.setHand(PLAYER_ONE_ID, hand);

        assertEquals(man.count15Combos(PLAYER_ONE_ID), 16);
        assertEquals(man.countRuns(PLAYER_ONE_ID), 0);
        assertEquals(man.countFlush(PLAYER_ONE_ID), 0);
        assertEquals(man.countNobs(PLAYER_ONE_ID), 1);
        assertEquals(man.countPairs(PLAYER_ONE_ID), 12);
        assertEquals(man.countHand(PLAYER_ONE_ID), 29);
    }

    @Test
    public void testNoImpossibleScore() {
        // A score from a hand can never be 19, 25, 26, or 27
        man = new CribbageManagerScoreTest(NUM_PLAYERS);
        man.setDealer(PLAYER_ONE_ID);
        for (int i = 0; i < TRIALS; i++) {
            List<List<Card>> hands = man.dealHands();
            man.setStarterCard(man.pickCardForDealer());
            for (int j = 0; j < hands.size(); j++) {
                List<Card> hand = hands.get(j);
                man.sendCardToCrib(j, hand.get(0));
                man.sendCardToCrib(j, hand.get(1));

                int score = man.countHand(j);
                assertNotEquals(score, 19);
                assertNotEquals(score, 25);
                assertNotEquals(score, 26);
                assertNotEquals(score, 27);
            }

            man.clearRoundState();
        }
    }

    @Test
    public void testCorrectCalculationOfSpecificHands() {
        man = new CribbageManagerScoreTest(NUM_PLAYERS);
        man.setDealer(PLAYER_ONE_ID);

        man.setStarterCard(new Card(Suit.HEART, Rank.TWO));

        List<Card> playerOneHand = new LinkedList<Card>();
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.ACE));
        playerOneHand.add(new Card(Suit.CLUB, Rank.FOUR));
        playerOneHand.add(new Card(Suit.SPADE, Rank.TEN));
        playerOneHand.add(new Card(Suit.HEART, Rank.KING));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID), 4);

        List<Card> playerTwoHand = new LinkedList<Card>();
        playerTwoHand.add(new Card(Suit.SPADE, Rank.ACE));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.FOUR));
        playerTwoHand.add(new Card(Suit.HEART, Rank.FOUR));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.QUEEN));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID), 6);

        man.clearRoundState();
        man.setStarterCard(new Card(Suit.CLUB, Rank.NINE));

        playerOneHand.add(new Card(Suit.CLUB, Rank.FOUR));
        playerOneHand.add(new Card(Suit.SPADE, Rank.FIVE));
        playerOneHand.add(new Card(Suit.HEART, Rank.SIX));
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.SIX));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID), 16);

        playerTwoHand.add(new Card(Suit.HEART, Rank.THREE));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.THREE));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.SEVEN));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.KING));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID), 4);

        man.clearRoundState();
        man.setStarterCard(new Card(Suit.SPADE, Rank.JACK));

        playerOneHand.add(new Card(Suit.SPADE, Rank.ACE));
        playerOneHand.add(new Card(Suit.HEART, Rank.FOUR));
        playerOneHand.add(new Card(Suit.CLUB, Rank.FIVE));
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.KING));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID), 8);

        playerTwoHand.add(new Card(Suit.HEART, Rank.THREE));
        playerTwoHand.add(new Card(Suit.CLUB, Rank.FOUR));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.FIVE));
        playerTwoHand.add(new Card(Suit.HEART, Rank.QUEEN));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID), 7);

        man.clearRoundState();
        man.setStarterCard(new Card(Suit.SPADE, Rank.SEVEN));

        playerOneHand.add(new Card(Suit.CLUB, Rank.FIVE));
        playerOneHand.add(new Card(Suit.SPADE, Rank.SIX));
        playerOneHand.add(new Card(Suit.CLUB, Rank.SIX));
        playerOneHand.add(new Card(Suit.HEART, Rank.SEVEN));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID), 16);

        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.ACE));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.TWO));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.NINE));
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.JACK));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID), 4);
    }
}