package logic.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logic.deck.Card;
import logic.deck.Deck;
import logic.deck.Rank;

/**
 * Manages a game of cribbage. The caller is largely in charge of maintaining 
 * the correct order of operations for a cribbage game (e.g. they should not 
 * be adding points in a hand until after the second stage). The caller should 
 * also keep track of which player is assigned to which ID so that points are
 * not being added to the wrong player.
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
    private final int[] gameScores;
    private final List<List<Card>> hands;
    private final List<Card> crib;
    private final LinkedList<Card> cardStack;
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
        cardStack = new LinkedList<Card>();
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
     * Removes the card from the player's hand and puts it in the crib. The 
     * caller must have already dealt cards to each player, and the crib must 
     * not already be full
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

        starterCard = deck.pickCard(idx);

        // If the starter card is a jack, the dealer gets two points (heels)
        if (starterCard.getRank() == Rank.JACK) {
            addPoints(dealerId, 2);
        }

        return starterCard;
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

    /**************************************************************************
    * Second Stage (Play)
    **************************************************************************/
    public boolean hasPlayableCard(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        }

        List<Card> playedCards = playedCardsByPlayer.get(pid);
        if (playedCards.size() == HAND_SIZE) {
            // All cards have been played, so return false
            return false;
        }

        for (Card card : hands.get(pid)) {
            int cardValue = card.getValue();
            if (!playedCards.contains(card) && cardValue + count <= MAX_COUNT) {
                return true;
            }
        }

        return false;
    }

    /**
     * Play the passed-in card assuming that it is in the player's hand. If 
     * not, an exception is thrown. If the value of the card causes the count 
     * to go above MAX_COUNT (31), false is returned. Otherwise, the card is 
     * played and true is returned.
     * 
     * @param pid the player's ID
     * @param card the card to be played
     * @return the points earned from playing that card, or -1 if the card 
     *         can't be played
     * @throws IllegalArgumentException if this player doesn't have the card in
     *                                  their hand, has already played the 
     *                                  card, or the count is already 31
     * @throws NullPointerException if the card is null
     * @throws IndexOutOfBoundsException if the player ID is invalid
     */
    public boolean playCard(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (card == null) {
           throw new NullPointerException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        } else if (playedCardsByPlayer.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player has already played this card");
        } else if (count == MAX_COUNT) {
            throw new IllegalArgumentException("Count is already at 31");
        }

        int cardValue = card.getValue();
        if (count + cardValue > MAX_COUNT) {
            return false;
        }

        count += cardValue;
        cardStack.addFirst(card);
        playedCardsByPlayer.get(pid).add(card);
        
        return true;
    }

    /**
     * Counts and returns the number of points earned via pairs from playing 
     * the most recently played card. Points earned are added to the passed-in
     * player's total
     * 
     * @param pid the ID of the player who we will give points to
     * @return the number of points earned via pairs from playing the most 
     *         recently played card
     */
    public int countPegPairs(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        }

        if (cardStack.isEmpty()) {
            return 0;
        }

        Card mostRecentlyPlayed = cardStack.getFirst();
        int occurrences = 0;

        // Since new cards are added to the front of the list, we can iterate 
        // through the list starting from the front in the same way we would 
        // with a stack
        for (Card prev : cardStack) {
            if (prev.getRank() != mostRecentlyPlayed.getRank()) {
                break;
            }
            occurrences++;
        }
        assert(occurrences <= CARDS_PER_RANK) : "More than 4 occurrences of a specific rank";
        assert(occurrences != 0) : "Most recently played card not counted";

        // 1 occurence = 0 pts, 2 occurrences = 2 pts, 
        // 3 occurences = 6 pts, 4 occurrences = 12 pts
        int pointsEarned = occurrences * (occurrences - 1);
        addPoints(pid, pointsEarned);
        return pointsEarned;
    }

    /**
     * Counts and returns the number of points earned via runs from playing 
     * the most recently played card. Points earned are added to the passed-in
     * player's total
     * 
     * @param pid the ID of the player who we will give points to
     * @return the number of points earned via runs from playing the most 
     *         recently played card
     */
    public int countPegRuns(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        }

        int totalCardsPlayed = cardStack.size();
        int longestRun = 0;
        int testRunLength = 3;
        
        while (testRunLength <= totalCardsPlayed) {
            // Get the last (testRunLength - 1) cards played
            List<Card> subList = 
                    cardStack.subList(totalCardsPlayed - testRunLength, 
                            totalCardsPlayed);

            // Add the most recently played cart to this list and sort it
            Collections.sort(subList);

            // Determine if a run is present in this sub list
            int i = 0;
            for (; i < subList.size() - 1; i++) {
                if (subList.get(i).getRankValue() + 1 
                        != subList.get(i + 1).getRankValue()) break;
            }

            // If we reached the end of the sub list, then all the numbers are
            // consecutive, so set the longest run to this value
            if (i == subList.size() - 1) {
                longestRun = testRunLength;
            }
            testRunLength++;
        }

        addPoints(pid, longestRun);
        return longestRun;
    }

    /**
     * If the count is currently 15, the player with the passed-in ID is given 
     * 2 points and true is returned. Otherwise, no points are given and false 
     * is returned
     * 
     * @param pid the ID of the player who we will give points to
     * @return true if the count is currently 15, false otherwise
     */
    public boolean countIs15(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        }

        if (count == 15) {
            addPoints(pid, 2);
            return true;
        }
        return false;
    }

    /**
     * If the count is currently 31, the player with the passed-in ID is given 
     * 2 points and true is returned. Otherwise, no points are given and false 
     * is returned. It is expected that if true is returned, the caller will 
     * subsequently call resetCount()
     * 
     * @param pid the ID of the player who we will give points to
     * @return true if the count is currently 31, false otherwise
     */
    public boolean countIs31(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        }

        if (count == MAX_COUNT) {
            addPoints(pid, 2);
            return true;
        }
        return false;
    }

    public boolean inPlay() {
        for (int i = 0; i < numPlayers; i++) {
            if (hasPlayableCard(i)) {
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
    /**
     * Counts and returns the number of points present in the player's hand in 
     * combination with the starter card.
     * 
     * @param pid the ID of the player whose hand we will use for the count
     * @return the number of points present in the given player's hand
     */
    public int countHand(int pid) {
        int score = count15Combos(pid);
        score += countRuns(pid);
        score += countPairs(pid);
        score += countFlush(pid);
        score += countNobs(pid);
        return score;
    }

    /**
     * Counts and returns the number of points earned from combinations of 
     * cards that add up to 15 in a given player's hand along with the starter
     * card.
     * 
     * @param pid the ID of the player whose hand we will use for the count
     * @return the number of points present in the given player's hand
     */
    public int count15Combos(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (hands.get(pid).isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hands.get(pid).add(starterCard);
        int count = getCombos(hands.get(pid), 0, 0);
        hands.get(pid).remove(starterCard);
        return count * 2;
    }

    // Starting from the given idx, recursively searches through the given hand
    // in search of a subset that adds up to 15. Returns the number of such 
    // subsets. Note that this algorithm has a max runtime of O(2^n), but n  
    // should never be more than 6 (the starting hand size for a two-player 
    // game).
    private int getCombos(List<Card> hand, int idx, int soFar) {
        if (soFar == 15) {
            return 1;
        }

        if (idx == hand.size() || soFar > 15) {
            return 0;
        }

        // Ends up looking like a binary search tree, with one child including 
        // the value of the current card in soFar and the other skipping it
        return getCombos(hand, idx + 1, soFar + hand.get(idx).getValue())
                + getCombos(hand, idx + 1, soFar);
    }

    public int countRuns(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (hands.get(pid).isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hands.get(pid).add(starterCard);
        Collections.sort(hands.get(pid));
        int totalPoints = getRuns()
        hands.get(pid).remove(starterCard);
    }

    private int getRuns(List<Card> hand, int idx, int prevValue, int currentRun) {
        int currentValue = hand.get(idx).getRankValue();
        if (idx == hand.size()) {
            return (currentRun >= 3) ? currentRun : 0;
        }


    }

    public int countPairs(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (hands.get(pid).isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        return -1;
    }

    public int countFlush(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (hands.get(pid).isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        return -1;
    }

    public int countNobs(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID");
        } else if (hands.get(pid).isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        for (Card card : hands.get(pid)) {
            if (card.getRank() == Rank.JACK 
                    && starterCard.getSuit() == card.getSuit()) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Adds points to the designated player's total and returns the new total. 
     * Caps out at 121. It is expected that points earned from either pegging 
     * or the hand are added to a player's total in order to maintain the 
     * integrity of the cribbage game. It is the duty of the caller to ensure 
     * that points are added in the correct fashion (i.e. to the right player 
     * with the correct amount)
     * 
     * @param pid   the player's ID
     * @param total the number of points to be added
     */
    private void addPoints(int pid, int total) {
        gameScores[pid] = Math.min(MAX_SCORE, gameScores[pid] + total);
    }

    public boolean isWinner(int pid) {
        return gameScores[pid] == MAX_SCORE;
    }

    /**
     * Resets all state from a round, including hands, the crib, and played 
     * cards.
     */
    public void clearRoundState() {
        resetCount();

        for (List<Card> hand : hands) {
            hand.clear();
        }

        for (List<Card> playedCards : playedCardsByPlayer) {
            playedCards.clear();
        }

        crib.clear();
    }
}