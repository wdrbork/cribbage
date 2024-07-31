package dev.wdrbork.cribbage.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.CribbageManager;
import dev.wdrbork.cribbage.logic.game.CribbageScoring;

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

        public void setHand(int pid, CribbageHand hand) { hands.set(pid, hand); }

        public void setStarterCard(Card card) { starterCard = card; }
    }

    @Test
    public void test29Hand() {
        man = new CribbageManagerScoreTest(NUM_PLAYERS);
        CribbageHand hand = new CribbageHand();
        hand.addCard(new Card(Suit.HEART, Rank.JACK));
        hand.addCard(new Card(Suit.DIAMOND, Rank.FIVE));
        hand.addCard(new Card(Suit.SPADE, Rank.FIVE));
        hand.addCard(new Card(Suit.CLUB, Rank.FIVE));
        Card starter = new Card(Suit.HEART, Rank.FIVE);
        man.setStarterCard(starter);
        man.setHand(PLAYER_ONE_ID, hand);

        assertEquals(CribbageScoring.count15Combos(hand, starter), 16);
        assertEquals(CribbageScoring.countRuns(hand, starter), 0);
        assertEquals(CribbageScoring.countFlush(hand, starter, false), 0);
        assertEquals(CribbageScoring.countNobs(hand, starter), 1);
        assertEquals(CribbageScoring.countPairs(hand, starter), 12);
        assertEquals(man.countHand(PLAYER_ONE_ID, false)[0], 29);
    }

    @Test
    public void testNoImpossibleScore() {
        // A score from a hand can never be 19, 25, 26, or 27
        man = new CribbageManagerScoreTest(NUM_PLAYERS);
        man.setDealer(PLAYER_ONE_ID);
        for (int i = 0; i < TRIALS; i++) {
            List<CribbageHand> hands = man.dealHands();
            man.setStarterCard(man.pickCardForDealer());
            for (int j = 0; j < hands.size(); j++) {
                CribbageHand hand = hands.get(j);
                man.sendCardToCrib(j, hand.getCard(0));
                man.sendCardToCrib(j, hand.getCard(1));

                int[] scores = man.countHand(j, false);
                assertNotEquals(scores[0], 19);
                assertNotEquals(scores[0], 25);
                assertNotEquals(scores[0], 26);
                assertNotEquals(scores[0], 27);
            }

            man.clearRoundState();
        }
    }

    @Test
    public void testCorrectCalculationOfSpecificHands() {
        man = new CribbageManagerScoreTest(NUM_PLAYERS);
        man.setDealer(PLAYER_ONE_ID);

        man.setStarterCard(new Card(Suit.HEART, Rank.TWO));

        CribbageHand playerOneHand = new CribbageHand();
        playerOneHand.addCard(new Card(Suit.DIAMOND, Rank.ACE));
        playerOneHand.addCard(new Card(Suit.CLUB, Rank.FOUR));
        playerOneHand.addCard(new Card(Suit.SPADE, Rank.TEN));
        playerOneHand.addCard(new Card(Suit.HEART, Rank.KING));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID, false)[0], 4);

        CribbageHand playerTwoHand = new CribbageHand();
        playerTwoHand.addCard(new Card(Suit.SPADE, Rank.ACE));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.FOUR));
        playerTwoHand.addCard(new Card(Suit.HEART, Rank.FOUR));
        playerTwoHand.addCard(new Card(Suit.SPADE, Rank.QUEEN));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID, false)[0], 6);

        man.clearRoundState();
        man.setStarterCard(new Card(Suit.CLUB, Rank.NINE));

        playerOneHand.addCard(new Card(Suit.CLUB, Rank.FOUR));
        playerOneHand.addCard(new Card(Suit.SPADE, Rank.FIVE));
        playerOneHand.addCard(new Card(Suit.HEART, Rank.SIX));
        playerOneHand.addCard(new Card(Suit.DIAMOND, Rank.SIX));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID, false)[0], 16);

        playerTwoHand.addCard(new Card(Suit.HEART, Rank.THREE));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.THREE));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.SEVEN));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.KING));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID, false)[0], 4);

        man.clearRoundState();
        man.setStarterCard(new Card(Suit.SPADE, Rank.JACK));

        playerOneHand.addCard(new Card(Suit.SPADE, Rank.ACE));
        playerOneHand.addCard(new Card(Suit.HEART, Rank.FOUR));
        playerOneHand.addCard(new Card(Suit.CLUB, Rank.FIVE));
        playerOneHand.addCard(new Card(Suit.DIAMOND, Rank.KING));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID, false)[0], 8);

        playerTwoHand.addCard(new Card(Suit.HEART, Rank.THREE));
        playerTwoHand.addCard(new Card(Suit.CLUB, Rank.FOUR));
        playerTwoHand.addCard(new Card(Suit.SPADE, Rank.FIVE));
        playerTwoHand.addCard(new Card(Suit.HEART, Rank.QUEEN));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID, false)[0], 7);

        man.clearRoundState();
        man.setStarterCard(new Card(Suit.SPADE, Rank.SEVEN));

        playerOneHand.addCard(new Card(Suit.CLUB, Rank.FIVE));
        playerOneHand.addCard(new Card(Suit.SPADE, Rank.SIX));
        playerOneHand.addCard(new Card(Suit.CLUB, Rank.SIX));
        playerOneHand.addCard(new Card(Suit.HEART, Rank.SEVEN));
        man.setHand(PLAYER_ONE_ID, playerOneHand);
        assertEquals(man.countHand(PLAYER_ONE_ID, false)[0], 16);

        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.ACE));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.TWO));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.NINE));
        playerTwoHand.addCard(new Card(Suit.DIAMOND, Rank.JACK));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);
        assertEquals(man.countHand(PLAYER_TWO_ID, false)[0], 4);
    }
}