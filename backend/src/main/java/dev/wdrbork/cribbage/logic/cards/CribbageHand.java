package dev.wdrbork.cribbage.logic.cards;

import dev.wdrbork.cribbage.logic.game.CribbageScoring;

public class CribbageHand extends Deck {
    private static final int POINT_CATEGORIES = 6;
    private static final int TOTAL_POINTS = 0;
    private static final int RUNS = 1;
    private static final int PAIRS = 2;
    private static final int FIFTEEN = 3;
    private static final int FLUSH = 4;
    private static final int NOBS = 5;

    public CribbageHand() {
        super();
    }

    public CribbageHand(CribbageHand copy) {
        super(copy);
    }

    /**
     * Counts up the score of this hand based on standard cribbage scoring. 
     * This hand must contain exactly four cards for it to be counted up.
     * 
     * @param starterCard the starter card for a round of cribbage
     * @param isCrib true if this deck represents a crib, false otherwise
     * @return the total score of this hand for a game of cribbage
     */
    public int[] countCribbageHand(Card starterCard, boolean isCrib) {
        int[] scores = new int[POINT_CATEGORIES];
        scores[FIFTEEN] = count15Combos(starterCard);
        scores[RUNS] = countRuns(starterCard);
        scores[PAIRS] = countPairs(starterCard);
        scores[FLUSH] = countFlush(starterCard, isCrib);
        scores[NOBS] = countNobs(starterCard);

        int totalScore = 0;
        for (int score : scores) totalScore += score;
        scores[TOTAL_POINTS] = totalScore;

        return scores;
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
    private int countFlush(Card starterCard, boolean isCrib) {
        return CribbageScoring.countFlush(this, starterCard, isCrib);
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
