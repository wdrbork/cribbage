package dev.wdrbork.cribbage.cards;

import org.junit.jupiter.api.Test;

import dev.wdrbork.cribbage.logic.cards.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestHand {
    private static final Card TEST_CARD_1 = new Card(Suit.SPADE, Rank.ACE);
    private static final Card TEST_CARD_2 = new Card(Suit.DIAMOND, Rank.KING);
    private static final Card TEST_CARD_3 = new Card(Suit.CLUB, Rank.EIGHT);

    @Test
    public void testBasicOperations() {
        Hand hand = new Hand();
        assertTrue(hand.isEmpty());
        assertEquals(hand.size(), 0);

        assertTrue(hand.addCard(TEST_CARD_1));
        assertFalse(hand.isEmpty());
        assertEquals(hand.size(), 1);
        assertTrue(hand.contains(TEST_CARD_1));

        assertTrue(hand.removeCard(TEST_CARD_1));
        assertTrue(hand.isEmpty());
        assertEquals(hand.size(), 0);
        assertFalse(hand.contains(TEST_CARD_1));
    }

    @Test
    public void testCopyConstructor() {
        Hand hand = new Hand();
        assertTrue(hand.addCard(TEST_CARD_1));
        assertTrue(hand.addCard(TEST_CARD_2));
        assertTrue(hand.addCard(TEST_CARD_3));

        Hand copy = new Hand(hand);
        assertTrue(hand.asList().equals(copy.asList()));
        assertFalse(hand.asList() == copy.asList());
    }

    @Test
    public void testRetainAll() {
        Hand hand = new Hand();
        assertTrue(hand.addCard(TEST_CARD_1));
        assertTrue(hand.addCard(TEST_CARD_2));
        assertTrue(hand.addCard(TEST_CARD_3));

        Hand retain = new Hand();
        assertTrue(retain.addCard(TEST_CARD_1));
        assertTrue(retain.addCard(TEST_CARD_3));

        hand.retainAll(retain);
        assertTrue(hand.contains(TEST_CARD_1));
        assertFalse(hand.contains(TEST_CARD_2));
        assertTrue(hand.contains(TEST_CARD_3));

        hand.retainAll(new Hand());
        assertTrue(hand.isEmpty());
    }
    
}
