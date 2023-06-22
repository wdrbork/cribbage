package logic.game;

import java.util.ArrayList;
import java.util.List;
import logic.deck.Card;
import logic.deck.Deck;

/**
 * Manages a game of cribbage
 */
public class CribbageGame {
    private static final int MAX_COUNT = 31;

    private final int numPlayers;
    private final Deck deck;
    private final int[] matchScores;
    private final int[] gameScores;
    private final List<List<Card>> hands;
    private final List<Card> crib;

    private Card starterCard;  // The starter card drawn at the end of the first stage
    private int dealer;        // Represents a player's ID (between 0 and numPlayers exclusive)
    private int count;         // Count for the second stage of play

    /**
     * Sets up a cribbage game with a random dealer.
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     */
    public CribbageGame(int numPlayers) {
        this(numPlayers, (int) (Math.random() * numPlayers));
    }

    /**
     * Sets up a cribbage game. The ID of the dealer is passed in.
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     * @param dealer the ID of the dealer
     */
    public CribbageGame(int numPlayers, int dealer) {
        if (numPlayers != 2 || numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (dealer < 0 || dealer >= numPlayers) {
            throw new IllegalArgumentException("Invalid ID for the dealer");
        }

        this.numPlayers = numPlayers;
        deck = new Deck();
        matchScores = new int[numPlayers - 1];
        gameScores = new int[numPlayers - 1];
        hands = new ArrayList<List<Card>>();
        crib = new ArrayList<Card>();
        this.dealer = dealer;
    }

    public void dealHands() {

    }

    public void sendCardToCrib(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID");
        } else if (card == null) {
            throw new IllegalArgumentException("Card is null");
        } else if (crib.size() == 4) {
            throw new IllegalStateException("Crib is full");
        }

        crib.add(card);
    }

    public int countHand(List<Card> hand) {
        return -1;
        
    }

    private int countPairs(List<Card> hand) {
        return 0;
    }
}