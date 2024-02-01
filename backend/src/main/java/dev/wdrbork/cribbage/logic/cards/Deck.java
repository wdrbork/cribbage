package dev.wdrbork.cribbage.logic.cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a standard 52-card deck.
 */
public class Deck {
    public static final int DECK_SIZE = 52;
    public static final int CARDS_PER_RANK = 4;
    public static final int CARDS_PER_SUIT = 13;
    public static final int NUM_FACE_CARDS = 3;
    private static final int TOP_CARD = 0;

    private final List<Card> cards;

    /**
     * Creates a new deck of cards. The deck is unshuffled by default
     */
    public Deck() {
        this(false);
    }

    public Deck(boolean shuffle) {
        cards = new ArrayList<Card>(DECK_SIZE);
        resetDeck();
        if (shuffle) shuffle();
    }

    /** 
     * Puts all cards back into the deck. Note that the returned deck is 
     * unshuffled.
     */
    public void resetDeck() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    public int remainingCards() {
        return cards.size();
    }

    public void shuffle() {
        Collections.shuffle(cards);
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
     * Returns a random card from the remaining cards in this deck, or null if 
     * there are no more cards.
     * 
     * @return a random card from the deck, or null if the deck is empty
     */
    public Card pickRandomCard() {
        if (cards.isEmpty()) {
            return null;
        }

        Random r = new Random();
        int offset = (int) Math.round(r.nextDouble() * (cards.size() - 1));
        return pickCard(offset);
    }

    /**
     * Picks out a card from the deck using the given offset. An offset of 0 
     * indicates the top card
     * 
     * @param offset the location of the card to choose from
     * @return the card at the given offset, or null if the deck is emprty
     */
    public Card pickCard(int offset) {
        if (offset < 0 || offset > cards.size()) {
            throw new IllegalArgumentException("Card offset is invalid");
        }

        if (cards.isEmpty()) {
            return null;
        }

        Card nextCard = cards.remove(offset);
        return nextCard;
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