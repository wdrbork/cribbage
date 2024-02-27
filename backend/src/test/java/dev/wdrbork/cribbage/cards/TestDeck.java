package dev.wdrbork.cribbage.cards;

import org.junit.jupiter.api.Test;

import dev.wdrbork.cribbage.logic.cards.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestDeck {
    private static final Card TEST_CARD_1 = new Card(Suit.SPADE, Rank.ACE);
    private static final Card TEST_CARD_2 = new Card(Suit.DIAMOND, Rank.KING);
    private static final Card TEST_CARD_3 = new Card(Suit.CLUB, Rank.EIGHT);

    @Test
    public void testBasicOperations() {
        Deck hand = new Deck();
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
        Deck hand = new Deck();
        assertTrue(hand.addCard(TEST_CARD_1));
        assertTrue(hand.addCard(TEST_CARD_2));
        assertTrue(hand.addCard(TEST_CARD_3));

        Deck copy = new Deck();
        assertTrue(hand.getCards().equals(copy.getCards()));
        assertFalse(hand.getCards() == copy.getCards());
    }

    @Test
    public void testRetainAll() {
        Deck hand = new Deck();
        assertTrue(hand.addCard(TEST_CARD_1));
        assertTrue(hand.addCard(TEST_CARD_2));
        assertTrue(hand.addCard(TEST_CARD_3));

        Deck retain = new Deck();
        assertTrue(retain.addCard(TEST_CARD_1));
        assertTrue(retain.addCard(TEST_CARD_3));

        hand.retainAll(retain);
        assertTrue(hand.contains(TEST_CARD_1));
        assertFalse(hand.contains(TEST_CARD_2));
        assertTrue(hand.contains(TEST_CARD_3));

        hand.retainAll(new Deck());
        assertTrue(hand.isEmpty());
    }
    
}
