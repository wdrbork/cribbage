package logic.deck;

/**
 * Represents a single card in a standard 52-card deck.
 */
public class Card {
    private final Suit suit;
    private final Rank rank;

    /**
     * Creates a card using the given suit and rank.
     * 
     * @param s the suit of this card
     * @param r the rank of this card
     */
    public Card(Suit s, Rank r) {
        suit = s;
        rank = r;
    }

    public Suit getSuit() {
        return this.suit;
    }

    public Rank getRank() {
        return this.rank;
    }

    /**
     * Returns the value of this card. If the rank is a number, that number 
     * is returned. Otherwise, aces return a value of 1, and face cards (jacks, 
     * queens, and kings) return a value of 10.
     * 
     * @return the value of this card
     */
    public int getValue() {
        return Math.min(rank.ordinal() + 1, 10);
    }

    /**
     * Returns the value of this card based on its rank. Unlike the above 
     * method, this one gives face cards unique values
     * 
     * @return the value of this card based on its rank
     */
    public int getRankValue() {
        return rank.ordinal() + 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !(other instanceof Card)) {
            return false;
        }

        Card otherCard = (Card) other;
        return this.suit == otherCard.suit && this.rank == otherCard.rank;
    }

    @Override
    public int hashCode() {
        int base = this.getRankValue();

        int shift = 0;
        if (suit == Suit.DIAMOND) {
            shift = 8;
        } else if (suit == Suit.HEART) {
            shift = 16;
        } else if (suit == Suit.SPADE) {
            shift = 24;
        }

        return base << shift;
    }
}