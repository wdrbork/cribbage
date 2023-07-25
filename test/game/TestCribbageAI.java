package test.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.*;
import logic.deck.*;

import java.util.ArrayList;
import java.util.List;

public class TestCribbageAI {
    private static final int PID = 1;
    private static final int NUM_PLAYERS = 2;

    CribbageAI ai = new CribbageAI(PID, NUM_PLAYERS);

    @Test
    public void analyzeRecommendedHands() {
        List<Card> hand = new ArrayList<Card>();
        hand.add(new Card(Suit.SPADE, Rank.TWO));
        hand.add(new Card(Suit.DIAMOND, Rank.TWO));
        hand.add(new Card(Suit.DIAMOND, Rank.FIVE));
        hand.add(new Card(Suit.SPADE, Rank.SIX));
        hand.add(new Card(Suit.SPADE, Rank.SEVEN));
        hand.add(new Card(Suit.SPADE, Rank.KING));

        ai.setHand(hand);
        List<Card> optimal = ai.getOptimalHand(true);
        // System.out.println(optimal);
    }
}