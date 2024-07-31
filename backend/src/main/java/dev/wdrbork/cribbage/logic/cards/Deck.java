package dev.wdrbork.cribbage.logic.cards;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dev.wdrbork.cribbage.logic.game.CribbageScoring;

/**
 * Represents a deck of cards.
 */
public class Deck {
    public static final int CARDS_PER_RANK = 4;
    public static final int CARDS_PER_SUIT = 13;
    public static final int NUM_FACE_CARDS = 3;

    protected List<Card> cards;

    public Deck() {
        cards = new LinkedList<Card>();
    }

    public Deck(Set<Card> cards) {
        this.cards = new LinkedList<Card>();
        this.cards.addAll(cards);
    }

    public Deck(Deck copy) {
        this.cards = new LinkedList<Card>(copy.cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public boolean contains(Card card) {
        return cards.contains(card);
    }

    public void clearDeck() {
        cards.clear();
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public boolean retainAll(Deck hand) {
        return cards.retainAll(hand.getCards());
    }

    /**
     * Adds the given card to this Hand if it isn't already present
     * 
     * @param card the card to be added
     * @return true if the given card has been added to the Hand, false if 
     *         the card is already in the Hand
     */
    public boolean addCard(Card card) {
        if (cards.contains(card)) {
            return false;
        }

        cards.add(card);
        return true;
    }

    /**
     * Removes the given card from this Hand if it is present
     * 
     * @param card the card to be removed
     * @return true if the given card has been removed from this Hand, false 
     *         if the card is not in this hand
     */
    public boolean removeCard(Card card) {
        if (!cards.contains(card)) {
            return false;
        }

        cards.remove(card);
        return true;
    }

    public String toString() {
        return cards.toString();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public void sortDeck() {
        Collections.sort(cards);
    }

    /**
     * Returns a random card from the remaining cards in this deck, or null if 
     * there are no more cards. This card is removed from the deck.
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

    public Card getCard(int idx) {
        return cards.get(idx);
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

}