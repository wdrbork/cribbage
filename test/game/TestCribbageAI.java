package test.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.*;
import logic.deck.*;

import java.util.ArrayList;
import java.util.List;

public class TestCribbageAI {
    private static final int START_SIZE = 6;

    CribbageAI ai = new CribbageAI(START_SIZE);

    @Test
    public void analyzeRecommendedHands() {
        List<Card> hand = new ArrayList<Card>();
        hand.add(new Card(Suit.HEART, Rank.ACE));
        hand.add(new Card(Suit.DIAMOND, Rank.FIVE));
        hand.add(new Card(Suit.HEART, Rank.SEVEN));
        hand.add(new Card(Suit.CLUB, Rank.SEVEN));
        hand.add(new Card(Suit.SPADE, Rank.TEN));
        hand.add(new Card(Suit.CLUB, Rank.TEN));

        ai.setHand(hand);
        List<Card> optimal = ai.getOptimalHand(false);
        System.out.println(optimal);
    }
}