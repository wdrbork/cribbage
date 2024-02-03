package dev.wdrbork.cribbage.logic.cards;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.wdrbork.cribbage.logic.game.CribbageScoring;


// Represents a hand of cards (is essentially an abstraction for a list of 
// Card objects)
public class Hand {
    public List<Card> cards;

    public Hand() {
        cards = new LinkedList<Card>();
    }

    public Hand(Set<Card> cards) {
        this.cards = new LinkedList<Card>();
        this.cards.addAll(cards);
    }

    public Hand(Hand copy) {
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

    public void clearHand() {
        cards.clear();
    }

    public List<Card> asList() {
        return Collections.unmodifiableList(cards);
    }

    public boolean retainAll(Hand hand) {
        return cards.retainAll(hand.asList());
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

    public String toString() {
        return cards.toString();
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

    public void removeCardByIndex(int idx) {
        cards.remove(idx);
    }

    public Card getCard(int idx) {
        return cards.get(idx);
    }

    public void sortHand() {
        Collections.sort(cards);
    }

    /**
     * Counts up the score of this hand based on standard cribbage scoring. 
     * This hand must contain exactly four cards for it to be counted up.
     * 
     * @param starterCard the starter card for a round of cribbage
     * @return the total score of this hand for a game of cribbage
     */
    public int countCribbageHand(Card starterCard) {
        int score = count15Combos(starterCard);
        score += countRuns(starterCard);
        score += countPairs(starterCard);
        score += countFlush(starterCard);
        score += countNobs(starterCard);

        return score;
    }

    /**
     * Counts and returns the number of points earned from combinations of 
     * cards that add up to 15 in a given player's hand along with the starter
     * card.
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points present in the given player's hand
     */
    private int count15Combos(Card starterCard) {
        return CribbageScoring.count15Combos(this, starterCard);
    }

    /**
     * Counts all points earned through runs in the given hand. A run is a 
     * sequence of consecutive numbers irrespective of suit (e.g. a 5 of clubs, 
     * 6 of spades, and 7 of diamonds form a run of three). The given hand 
     * must not be empty, and there must be a starter card to reference
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points earned through runs in the given hand
     */
    private int countRuns(Card starterCard) {
        return CribbageScoring.countRuns(this, starterCard);
    }

    /**
     * Counts all points earned through pairs in the given hand. A pair is 
     * worth 2 points.
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points earned through pairs
     */
    private int countPairs(Card starterCard) {
       return CribbageScoring.countPairs(this, starterCard);
    }

    /**
     * Counts and returns points earned through flush. A flush is a hand in 
     * which all cards are of the same suit. If all four cards in a player's  
     * hand share the same suit, 4 points are earned. If the starter card is  
     * the same suit as these four cards, 5 points are earned.
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points earned through flush
     */
    private int countFlush(Card starterCard) {
        return CribbageScoring.countFlush(this, starterCard);
    }

    /**
     * Returns 1 if this hand contains a jack that has the same suit as the 
     * starter card (formally called "one for his nob").
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points earned through nobs
     */
    private int countNobs(Card starterCard) {
        return CribbageScoring.countNobs(this, starterCard);
    }
}
