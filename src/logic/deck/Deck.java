package logic.deck;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a standard 52-card deck
 */
public class Deck {
    private static final int DECK_SIZE = 52;

    private final List<Card> cards;
    private int topCard;

    public Deck() {
        cards = new ArrayList<Card>(DECK_SIZE);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }

        topCard = 0;
    }
}