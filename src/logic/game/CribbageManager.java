package logic.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import logic.deck.Card;
import logic.deck.Deck;

/**
 * Manages a game of cribbage
 */
public class CribbageManager {
    private static final int MAX_COUNT = 31;
    private static final int MAX_SCORE = 121;
    private static final int CARDS_PER_RANK = 4;
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    private final int numPlayers;
    private final Deck deck;
    // private final int[] matchScores;  // Note: to be implemented later (only doing a single match at a time for now)
    private final int[] gameScores;
    private final List<List<Card>> hands;
    private final List<Card> crib;
    private final List<Card> cardStack;
    private final List<List<Card>> playedCardsByPlayer;

    // The starter card drawn at the end of the first stage
    private Card starterCard;  

    // Represents a player's ID (between 0 and numPlayers exclusive)
    private int dealerId; 
    
    // Count for the second stage of play
    private int count;         

    /**
     * Sets up a cribbage game using the given number of players
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     */
    public CribbageManager(int numPlayers) {
        if (numPlayers != 2 || numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (dealerId < 0 || dealerId >= numPlayers) {
            throw new IllegalArgumentException("Invalid ID for the dealer");
        }

        this.numPlayers = numPlayers;
        deck = new Deck();
        // matchScores = new int[numPlayers - 1];
        gameScores = new int[numPlayers - 1];
        this.dealerId = -1;

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
        cardStack = new ArrayList<Card>();
    }

    /**************************************************************************
    * Setup Stage
    **************************************************************************/
    public Card pickCardForDealer(int idx) {
        return deck.pickCard(idx);
    }

    public void setDealer(int pid) {
        this.dealerId = pid;
    }

    /**************************************************************************
    * First Stage (Deal)
    **************************************************************************/
    /**
     * Shuffles the deck and deals out hands to each player in the game. 
     * Returns each player's hand
     * 
     * @return each player's hand
     * @throws IllegalArgumentException if players still have cards, the crib 
     *                                  still has cards, or there's no dealer
     */
    public List<List<Card>> dealHands() {
        if (!checkEmptyHands()) {
            throw new IllegalArgumentException("Players still have cards in their hands");
        } else if (!crib.isEmpty()) {
            throw new IllegalArgumentException("Crib still contains cards");
        } else if (this.dealerId == -1) {
            throw new IllegalArgumentException("Dealer not decided");
        }

        deck.shuffle();

        int handSize = TWO_PLAYER_START_SIZE;
        if (this.numPlayers == 3) {
            handSize = THREE_PLAYER_START_SIZE;
        }

        /* Distribute cards to each player in the game. The player to the
           left of the dealer is dealt the first card. */ 
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

        return hands;
    }

    /**
     * Removes the card from the player's hand and puts it in the crib. 
     * 
     * @param pid a player ID
     * @param card a card held by the player with the above ID
     * @throws IllegalArgumentException if the ID is invalid or the player 
     *                                  does not have the passed-in card
     * @throws NullPointerException     if the card is null
     * @throws IllegalStateException    if the crib is full
     */
    public void sendCardToCrib(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID");
        } else if (card == null) {
            throw new NullPointerException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        } else if (crib.size() == 4) {
            throw new IllegalStateException("Crib is full");
        }

        hands.get(pid).remove(card);
        crib.add(card);
    }

    public Card getStarterCard(int idx) {
        // All hands and the crib must be finalized before the starter 
        // card is drawn
        if (!checkStartingHands()) {
            throw new IllegalArgumentException("Not all hands have been finalized");
        } else if (idx < 0 || idx >= deck.remainingCards()) {
            throw new IllegalArgumentException("Deck index is invalid");
        }

        Card starter = deck.pickCard(idx);
        starterCard = starter;
        return starter;
    }

    /**************************************************************************
    * Second Stage (Play)
    **************************************************************************/
    public boolean hasPlayableCard(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        }

        for (Card card : hands.get(pid)) {
            int cardValue = card.getValue();
            if (cardValue + count <= MAX_COUNT) {
                return true;
            }
        }

        return false;
    }

    /**
     * Play the passed-in card assuming that it is in the player's hand. If 
     * not, an exception is thrown. If the value of the card causes the count 
     * to go above MAX_COUNT (31), -1 is returned. Otherwise, the number of 
     * points earned from playing that card is returned
     * 
     * @param pid the player's ID
     * @param card the card to be played
     * @return the points earned from playing that card, or -1 if the card 
     *         can't be played
     * @throws IllegalArgumentException if this player doesn't have the card in
     *                                  their hand
     * @throws NullPointerException if the card is null
     * @throws IndexOutOfBoundsException if the player ID is invalid
     */
    public int playCard(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (card == null) {
           throw new NullPointerException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        }

        int cardValue = card.getValue();
        if (count + cardValue > MAX_COUNT) {
            return -1;
        }

        count += cardValue;
        hands.get(pid).remove(card);
        int pointsEarned = calculatePeg(card);
        cardStack.add(card);
        playedCardsByPlayer.get(pid).add(card);
        
        if (count == 31) resetCount();
        return pointsEarned;
    }

    private int calculatePeg(Card lastPlayed) {
        if (lastPlayed == null) {
            throw new NullPointerException("Card is null");
        }

        // This implementation separates the last played card from those 
        // already in the stack to make it easier to count points. Therefore, 
        // it is important that this card wasn't already put in the stack.
        assert(!cardStack.contains(lastPlayed)) : "lastPlayed already in cardStack";

        int points = 0;
        points += countPegPairs(lastPlayed);
        points += countPegRuns(lastPlayed);
        points += countIs15Or31();
        return points;
    }

    private int countPegPairs(Card lastPlayed) {
        int occurrences = 1;
        for (Card prev : cardStack) {
            if (prev.getRank() != lastPlayed.getRank()) {
                break;
            }
            occurrences++;
        }
        // 1 occurence = 0 pts, 2 occurrences = 2 pts, 
        // 3 occurences = 6 pts, 4 occurrences = 12 pts
        assert(occurrences <= CARDS_PER_RANK) : "More than 4 occurrences of a specific rank";
        return occurrences * (occurrences - 1);
    }

    private int countPegRuns(Card lastPlayed) {
        int totalCardsPlayed = cardStack.size();
        int longestRun = 0;
        int testRunLength = 3;
        
        while (testRunLength - 1 <= totalCardsPlayed) {
            // Get the last (testRunLength - 1) cards played
            List<Card> subList = 
                    cardStack.subList(totalCardsPlayed - testRunLength, 
                            totalCardsPlayed);

            // Add the most recently played cart to this list and sort it
            subList.add(lastPlayed);
            Collections.sort(subList);

            // Determine if a run is present in this sub list
            int i = 0;
            for (; i < subList.size() - 1; i++) {
                if (subList.get(i).getRankValue() + 1 
                        != subList.get(i + 1).getRankValue()) break;
            }

            // If all the numbers are consecutive, mark this as the longest run
            if (i == subList.size() - 1) {
                longestRun = testRunLength;
            }
            testRunLength++;
        }

        return longestRun;
    }

    private int countIs15Or31() {
        if (count == 15 || count == 31) {
            return 2;
        }
        return 0;
    }

    private boolean inPlay() {
        for (List<Card> hand : hands) {
            if (!hand.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public void resetCount() {
        count = 0;
        cardStack.clear();
    }

    /**************************************************************************
    * Third Stage (Show)
    **************************************************************************/
    public int countHand(List<Card> hand) {
        int score = count15Combos(hand);
        score += countRuns(hand);
        score += countPairs(hand);
        score += countFlush(hand);
        score += countNobs(hand);
        return score;
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

    public void addPoints(int pid, int total) {
        gameScores[pid] += total;

        // 
    }

    public boolean isWinner(int pid) {
        return false;
    }

    /* Checks to see if all hands are empty */
    private boolean checkEmptyHands() {
        for (int i = 0; i < hands.size(); i++) {
            if (!hands.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /* Checks to see if all hands as well as the crib are of size HAND_SIZE */
    private boolean checkStartingHands() {
        for (int i = 0; i < hands.size(); i++) {
            if (hands.get(i).size() != HAND_SIZE) {
                return false;
            }
        }

        if (crib.size() != HAND_SIZE) {
            return false;
        }

        return true;
    }
}