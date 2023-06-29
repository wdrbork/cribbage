package test.logic.deck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import logic.deck.Deck;
import logic.deck.Card;

public class TestDeck {
    private static final int DECK_SIZE = 52;
    private static final int TOP_CARD = 0;

    @Test
    public void testDeckCreation() {
        Deck deck = new Deck();
        assertEquals(deck.remainingCards(), DECK_SIZE);
    }

    @Test
    public void testShuffle() {
        Deck deck = new Deck();
        for (int i = 0; i < 100; i++) {
            deck.shuffle();
            assertEquals(deck.remainingCards(), DECK_SIZE);
            deck.resetDeck();
        }
    }

    @Test
    public void testRandomCard() {
        Deck deck = new Deck();
        for (int i = 0; i < 1000; i++) {
            while (deck.remainingCards() > 0) {
                assertNotEquals(deck.pickRandomCard(), null);
            }
            assertEquals(deck.takeTopCard(), null);
            deck.resetDeck();
            deck.shuffle();
        }
    }

    @Test
    public void testEmptyDeck() {
        Deck deck = new Deck();
        while (deck.remainingCards() > 0) {
            assertNotEquals(deck.takeTopCard(), null);
        }

        assertEquals(deck.takeTopCard(), null);
        assertEquals(deck.pickCard(TOP_CARD), null);
    }

    @Test
    public void testNoDuplicateCards() {
        Deck deck = new Deck();
        Set<Card> pickedCards = new HashSet<Card>();

        for (int i = 0; i < 10; i++) {
            deck.shuffle();
            while (deck.remainingCards() > 0) {
                Card card = deck.takeTopCard();
                assertFalse(pickedCards.contains(card));
                pickedCards.add(card);
            }
            deck.resetDeck();
            pickedCards.clear();
        }
    }

    @Test
    public void testInvalidOffsets() {
        Deck deck = new Deck();
        try {
            deck.pickCard(-1);
        } catch (Exception e) {
            System.out.println("Caught exception for negative input");
        }

        try {
            deck.pickCard(100);
        } catch (Exception e) {
            System.out.println("Caught exception for high input");
        }

        assertEquals(deck.remainingCards(), DECK_SIZE);
        for (int i = 0; i < 5; i++) {
            deck.shuffle();
            for (int j = DECK_SIZE - 1; j >= 0; j--) {
                assertNotEquals(deck.takeTopCard(), null);
                try {
                    deck.pickCard(j);
                } catch (Exception e) {
                    System.out.println("Trial " + i + 
                            ": Caught exception for offset " + j);
                }
            }
            deck.resetDeck();
        }
    }
}