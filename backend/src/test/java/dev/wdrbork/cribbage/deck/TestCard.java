package dev.wdrbork.cribbage.deck;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import dev.wdrbork.cribbage.logic.deck.Card;
import dev.wdrbork.cribbage.logic.deck.Rank;
import dev.wdrbork.cribbage.logic.deck.Suit;

public class TestCard {
    private static final int SAMPLE_SIZE = 13;
    private static List<Card> sampleCards = new ArrayList<Card>(SAMPLE_SIZE);
    private static final Card ACE_HEART = new Card(Suit.HEART, Rank.ACE);
    private static final Card TWO_DIAMOND = new Card(Suit.DIAMOND, Rank.TWO);
    private static final Card THREE_SPADE = new Card(Suit.SPADE, Rank.THREE);
    private static final Card FOUR_CLUB = new Card(Suit.CLUB, Rank.FOUR);
    private static final Card FIVE_HEART = new Card(Suit.HEART, Rank.FIVE);
    private static final Card SIX_DIAMOND = new Card(Suit.DIAMOND, Rank.SIX);
    private static final Card SEVEN_SPADE = new Card(Suit.SPADE, Rank.SEVEN);
    private static final Card EIGHT_CLUB = new Card(Suit.CLUB, Rank.EIGHT);
    private static final Card NINE_HEART = new Card(Suit.HEART, Rank.NINE);
    private static final Card TEN_DIAMOND = new Card(Suit.DIAMOND, Rank.TEN);
    private static final Card JACK_SPADE = new Card(Suit.SPADE, Rank.JACK);
    private static final Card QUEEN_CLUB = new Card(Suit.CLUB, Rank.QUEEN);
    private static final Card KING_HEART = new Card(Suit.HEART, Rank.KING);
    private static final Card NULL_CARD = null;

    @BeforeAll
    public static void initializeSample() throws Exception {
        sampleCards.add(ACE_HEART);
        sampleCards.add(TWO_DIAMOND);
        sampleCards.add(THREE_SPADE);
        sampleCards.add(FOUR_CLUB);
        sampleCards.add(FIVE_HEART);
        sampleCards.add(SIX_DIAMOND);
        sampleCards.add(SEVEN_SPADE);
        sampleCards.add(EIGHT_CLUB);
        sampleCards.add(NINE_HEART);
        sampleCards.add(TEN_DIAMOND);
        sampleCards.add(JACK_SPADE);
        sampleCards.add(QUEEN_CLUB);
        sampleCards.add(KING_HEART);
    }

    @Test
    public void testCorrectValues() {
        for(int i = 0; i < SAMPLE_SIZE; i++) {
            assertEquals(sampleCards.get(i).getValue(), Math.min(i + 1, 10));
        }
    }

    @Test
    public void testCorrectRankValues() {
        for(int i = 0; i < SAMPLE_SIZE; i++) {
            assertEquals(sampleCards.get(i).getRankValue(), i + 1);
        }
    }

    @Test
    public void testEquals() {
        // Test reference equality
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            assertTrue(sampleCards.get(i).equals(sampleCards.get(i)));
        }

        // Test null
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            assertFalse(sampleCards.get(i).equals(NULL_CARD));
        }

        // Test different classes
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            assertFalse(sampleCards.get(i).equals(new Object()));
        }

        // Test different reference, same card
        Card aceHeartTest = new Card(Suit.HEART, Rank.ACE);
        assertTrue(aceHeartTest.equals(ACE_HEART));

        // Test different cards
        for (int i = 1; i < SAMPLE_SIZE; i++) {
            assertFalse(sampleCards.get(i).equals(sampleCards.get(i - 1)));
        }

        // Test same suit, different rank
        assertFalse(ACE_HEART.equals(FIVE_HEART));

        // Test different suit, same rank
        Card aceSpade = new Card(Suit.SPADE, Rank.ACE);
        assertFalse(aceSpade.equals(ACE_HEART));
    }

    @Test
    public void testHashCode() {
        // The same card produces the same hash code
        Card aceHeart = new Card(Suit.HEART, Rank.ACE);
        assertEquals(ACE_HEART.hashCode(), aceHeart.hashCode());

        // Different cards produce different hash codes
        for (int i = 1; i < SAMPLE_SIZE; i++) {
            assertNotEquals(sampleCards.get(i).hashCode(), 
                    sampleCards.get(i - 1).hashCode()); 
        }

        // Test max possible hashcode (king of spades)
        Card kingSpade = new Card(Suit.SPADE, Rank.KING);
        try {
            kingSpade.hashCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompare() {
        // Test equal cards
        Card aceHeart = new Card(Suit.HEART, Rank.ACE);
        assertEquals(aceHeart.compareTo(ACE_HEART), 0);

        // Test different cards
        assertEquals(SEVEN_SPADE.compareTo(SIX_DIAMOND), 1);
        assertEquals(SIX_DIAMOND.compareTo(SEVEN_SPADE), -1);

        // Test same rank, different suit (from highest to lowest priority: 
        // spade, heart, diamond, club)
        Card aceSpade = new Card(Suit.SPADE, Rank.ACE);
        assertEquals(aceSpade.compareTo(ACE_HEART), 1);
        assertEquals(ACE_HEART.compareTo(aceSpade), -1);

        // Test lower suit but higher rank
        assertEquals(QUEEN_CLUB.compareTo(ACE_HEART), 1);
    }
}