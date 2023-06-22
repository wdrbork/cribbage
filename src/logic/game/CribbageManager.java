package logic.game;

import java.util.ArrayList;
import java.util.List;
import logic.deck.Card;
import logic.deck.Deck;

/**
 * Manages a game of cribbage
 */
public class CribbageManager {
    private static final int MAX_COUNT = 31;
    private static final int MAX_SCORE = 121;
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    private final int numPlayers;
    private final Deck deck;
    private final int[] matchScores;
    private final int[] gameScores;
    private final List<List<Card>> hands;
    private final List<Card> crib;

    // The starter card drawn at the end of the first stage
    private Card starterCard;  

    // Represents a player's ID (between 0 and numPlayers exclusive)
    private int dealerId; 
    
    // Count for the second stage of play
    private int count;         

    /**
     * Sets up a cribbage game with a random dealer.
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     */
    public CribbageManager(int numPlayers) {
        this(numPlayers, (int) (Math.random() * numPlayers));
    }

    /**
     * Sets up a cribbage game. The ID of the dealer is passed in.
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     * @param dealer the ID of the dealer
     */
    public CribbageManager(int numPlayers, int dealerId) {
        if (numPlayers != 2 || numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (dealerId < 0 || dealerId >= numPlayers) {
            throw new IllegalArgumentException("Invalid ID for the dealer");
        }

        this.numPlayers = numPlayers;
        deck = new Deck();
        matchScores = new int[numPlayers - 1];
        gameScores = new int[numPlayers - 1];
        hands = new ArrayList<List<Card>>(numPlayers);

        /* The size of the starting hand is dependent on the number of players
        in the game. 3 players = 5 cards, 2 players = 6 cards. */
        int startSize = TWO_PLAYER_START_SIZE;
        if (numPlayers == 3) {
            startSize = THREE_PLAYER_START_SIZE;
        }

        /* Initialize the hands */
        for (int i = 0; i < numPlayers; i++) {
            hands.add(new ArrayList<Card>(startSize));
        }

        crib = new ArrayList<Card>(HAND_SIZE);
        this.dealerId = dealerId;
    }

    /**************************************************************************
    * First Stage (Deal)
    **************************************************************************/
    public void dealHands() {
        checkEmptyHands();

        int handSize = TWO_PLAYER_START_SIZE;
        if (this.numPlayers == 3) {
            handSize = THREE_PLAYER_START_SIZE;
        }

        /* Shuffle the deck and distribute cards to each player in the game. 
           The player to the left of the dealer is dealt the first card */ 
        deck.shuffle();
        for (int i = 0; i < handSize; i++) {
            for (int j = 1; j <= this.numPlayers; j++) {
                Card next = deck.takeTopCard();
                assert(next != null) : "Deck is empty";
                hands.get((j + dealerId) % numPlayers).add(next);
            }
        }

        /* If there are three players, the crib starts out with one card. */
        if (this.numPlayers == 3) {
            Card next = deck.takeTopCard();
            assert(next != null) : "Deck is empty";
            crib.add(next);
        }
    }

    public void sendCardToCrib(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID");
        } else if (card == null) {
            throw new IllegalArgumentException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        } else if (crib.size() == 4) {
            throw new IllegalStateException("Crib is full");
        }

        hands.get(pid).remove(card);
        crib.add(card);
    }

    public Card getStarterCard() {
        // All hands and the crib must be finalized before the starter 
        // card is drawn
        checkFullHands();

        Card starter = deck.takeTopCard();
        assert(starter != null) : "Deck is empty";
        starterCard = starter;
        return starter;
    }

    /**************************************************************************
    * Second Stage (Play)
    **************************************************************************/
    public void playCard(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID");
        } else if (card == null) {
            throw new IllegalArgumentException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        }


    }

    private boolean playOver() {
        for (List<Card> hand : hands) {
            if (!hand.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**************************************************************************
    * Third Stage (Show)
    **************************************************************************/
    public int countHand(List<Card> hand) {
        return count15Combos(hand) + countRuns(hand) + countPairs(hand)
                + countFlush(hand) + countNobs(hand);
    }

    public void addPoints(int pid, int total) {
        gameScores[pid] += total;

        // 
    }

    public boolean isWinner(int pid) {

    }

    private int count15Combos(List<Card> hand) {
        return -1;
    }

    private int countRuns(List<Card> hand) {
        return -1;
    }

    private int countPairs(List<Card> hand) {
        return -1;
    }

    private int countFlush(List<Card> hand) {
        return -1;
    }

    private int countNobs(List<Card> hand) {
        return -1;
    }

    /* Ensures that all hands as well as the crib are empty */
    private void checkEmptyHands() {
        for (int i = 0; i < hands.size(); i++) {
            if (!hands.get(i).isEmpty()) {
                throw new IllegalStateException("Player(s) still have cards in their hand");
            }
        }

        if (!crib.isEmpty()) {
            throw new IllegalStateException("Crib still has cards");
        }
    }

    /* Ensures that all hands as well as the crib are of size HAND_SIZE */
    private void checkFullHands() {
        for (int i = 0; i < hands.size(); i++) {
            if (hands.get(i).size() != HAND_SIZE) {
                throw new IllegalStateException("Hands are not finalized");
            }
        }

        if (crib.size() != HAND_SIZE) {
            throw new IllegalStateException("Crib does not have four cards");
        }
    }
}