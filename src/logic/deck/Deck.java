package logic.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a standard 52-card deck.
 */
public class Deck {
    private static final int DECK_SIZE = 52;

    private final List<Card> cards;
    private int topCard;

    /**
     * Creates a new shuffled deck of cards.
     */
    public Deck() {
        cards = new ArrayList<Card>(DECK_SIZE);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards);

        topCard = 0;
    }

    /**
     * Shuffles the cards in this deck.
     */
    public void shuffle() {
        Collections.shuffle(cards);
        topCard = 0;
    }

    /**
     * Returns the top card of the deck, or null if there are no more cards in
     * the deck. 
     * 
     * Successive calls to takeTopCard() with no calls to shuffle() in between 
     * will always return a unique card. If null is returned, the caller is 
     * expected to call shuffle() so that the deck is restored.
     * 
     * @return the top card of the deck, or null if there are no more cards in
     * the deck. 
     */
    public Card takeTopCard() {
        if (topCard == DECK_SIZE) {
            return null;
        }

        Card nextCard = cards.get(topCard);
        topCard++;
        return nextCard;
    }

    /**
     * Returns a random card from the remaining cards in this deck, or null if 
     * there are no more cards.
     * @return
     */
    public Card pickRandomCard() {
        if (topCard == DECK_SIZE) {
            return null;
        }

        Random r = new Random();
        int remainingCards = DECK_SIZE - topCard - 1;
        int index = topCard + 
                (int) Math.round(r.nextDouble() * remainingCards);
        assert(index < DECK_SIZE) : "Index in pickRandomCard is an invalid value";
        
        return cards.get(index);
    }
}