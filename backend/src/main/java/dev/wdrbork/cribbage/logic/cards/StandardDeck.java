package dev.wdrbork.cribbage.logic.cards;

import java.util.ArrayList;
import java.util.Random;

/**
 * Represents a standard deck that contains 52 cards.
 */
public class StandardDeck extends Deck {
    public static final int DECK_SIZE = 52;
    private static final int TOP_CARD = 0;

    /**
     * Creates a new deck of cards. The deck is unshuffled by default
     */
    public StandardDeck() {
        this(false);
    }

    public StandardDeck(boolean shuffle) {
        cards = new ArrayList<Card>(DECK_SIZE);
        resetDeck();
        if (shuffle) shuffle();
    }

    /** 
     * Puts all cards back into the deck. Note that the returned deck is 
     * unshuffled.
     */
    public void resetDeck() {
        clearDeck();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    /**
     * Returns the top card of the deck, or null if there are no more cards in
     * the deck. 
     * 
     * Successive calls to takeTopCard() with no calls to shuffle() in between 
     * will always return a unique card. If null is returned, the caller is 
     * expected to call shuffle() so that the deck is restored.
     * 
     * @return the top card of the deck, or null if the deck is empty
     */
    public Card takeTopCard() {
        return pickCard(TOP_CARD);
    }

    /**
     * Returns a random card. The returned card is not bound to any instance 
     * of the Deck class.
     * 
     * @return a random card
     */
    public static Card getRandomCard() {
        Random r = new Random();
        int idx = (int) Math.floor(r.nextDouble() * Deck.CARDS_PER_RANK);
        Suit suit = Suit.values()[idx];

        idx = (int) Math.floor(r.nextDouble() * Deck.CARDS_PER_SUIT);
        Rank rank = Rank.values()[idx];

        return new Card(suit, rank);
    }
    
}
