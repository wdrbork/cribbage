package logic.game;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import logic.deck.Card;
import logic.deck.Deck;

public class GameState {
    private static final int MAX_COUNT = 31;
    private static final int MAX_SCORE = 121;
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    private final int numPlayers;
    private final Deck deck;
    private final int[] gameScores;
    private final List<List<Card>> hands;
    private final List<Card> crib;
    private final LinkedList<Card> cardStack;
    private final List<List<Card>> playedCardsByPlayer;
    private int nextToPlayCard;
    private Card starterCard;  
    private int dealerId; 
    private int count; 

    public GameState(int numPlayers) {
        if (numPlayers != 2 && numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (dealerId < 0 || dealerId >= numPlayers) {
            throw new IllegalArgumentException("Invalid ID for the dealer");
        }

        this.numPlayers = numPlayers;
        deck = new Deck();
        gameScores = new int[numPlayers];
        nextToPlayCard = -1;
        dealerId = -1;

        /* Initialize the hands. The size of the starting hand is dependent on 
        the number of players in the game. 3 players = 5 cards,
        2 players = 6 cards. */
        int startSize = TWO_PLAYER_START_SIZE;
        if (numPlayers == 3) {
            startSize = THREE_PLAYER_START_SIZE;
        }

        hands = new ArrayList<List<Card>>(numPlayers);
        playedCardsByPlayer = new ArrayList<List<Card>>(numPlayers);
        for (int i = 0; i < numPlayers; i++) {
            hands.add(new ArrayList<Card>(startSize));
            playedCardsByPlayer.add(new ArrayList<Card>(HAND_SIZE));
        }

        crib = new ArrayList<Card>(HAND_SIZE);
        cardStack = new LinkedList<Card>();
    }

    
}