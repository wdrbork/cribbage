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
    private static final int CARDS_PER_SUIT = 13;
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    protected final int numPlayers;
    protected final Deck deck;
    protected final int[] gameScores;
    protected final List<List<Card>> hands;
    protected final List<Card> crib;
    protected final LinkedList<Card> cardStack;
    protected final List<List<Card>> playedCardsByPlayer;
    protected int nextToPlayCard;

    // The starter card drawn at the end of the first stage
    protected Card starterCard;  

    // Represents a player's ID (between 0 and numPlayers exclusive)
    protected int dealerId; 
    
    // Count for the second stage of play
    protected int count;         

    /**
     * Sets up a cribbage game using the given number of players
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     */
    public CribbageManager(int numPlayers) {
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

    /**************************************************************************
    * Setup Stage
    **************************************************************************/
    public Card pickCardForDealer() {
        return deck.pickRandomCard();
    }

    public void setDealer(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + "exclusive");
        } 
        
        this.dealerId = pid;
        nextToPlayCard = (dealerId + 1) % numPlayers;
    }

    /**************************************************************************
    * First Stage (Deal)
    **************************************************************************/
    /**
     * Shuffles the deck and deals out hands to each player in the game. 
     * Returns each player's hand
     * 
     * @return each player's hand (cannot be modified)
     * @throws IllegalArgumentException if a player or the crib still has cards
     * @throws IllegalStateException if no dealer has been declared
     */
    public List<List<Card>> dealHands() {
        if (!checkEmptyHands()) {
            throw new IllegalArgumentException("Players still have cards in their hands");
        } else if (!crib.isEmpty()) {
            throw new IllegalArgumentException("Crib still contains cards");
        } else if (this.dealerId == -1) {
            throw new IllegalStateException("Dealer not decided");
        }

        deck.resetDeck();
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

        return Collections.unmodifiableList(hands);
    }

    public List<Card> getCrib() {
        return Collections.unmodifiableList(crib);
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
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + "exclusive");
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

    public Card getStarterCard() {
        // All hands and the crib must be finalized before the starter 
        // card is drawn
        if (!checkStartingHands()) {
            throw new IllegalArgumentException("Not all hands have been finalized");
        }

        starterCard = deck.pickRandomCard();

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
    /**
     * Plays the passed-in card (if possible) and removes it from the player's 
     * hand. If the card cannot be played (i.e. if it causes the count to go 
     * above 31), -1 is returned. Otherwise, the total points earned from 
     * playing that card is added to the player's game score and returned.
     * 
     * @param pid the player's ID
     * @param card the card to be played
     * @return the total points earned from playing the card
     * @throws IllegalArgumentException if this player doesn't have the card in
     *                                  their hand, has already played the 
     *                                  card, or it is not this player's turn
     * @throws NullPointerException if the card is null
     * @throws IndexOutOfBoundsException if the player ID is invalid
     */
    public int playCard(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + "exclusive");
        } else if (card == null) {
           throw new NullPointerException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        } else if (playedCardsByPlayer.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player has already played this card");
        } else if (pid != nextToPlayCard) {
            throw new IllegalArgumentException("Not this player's turn");
        }
        

        int cardValue = card.getValue();
        if (count + cardValue > MAX_COUNT) {
            return -1;
        }

        count += cardValue;
        cardStack.addFirst(card);
        playedCardsByPlayer.get(pid).add(card);

        int totalPoints = 0;
        if (count == 15 || count == 31) {
            totalPoints += 2;
        }
        totalPoints += countPegPairs() + countPegRuns();
        addPoints(pid, totalPoints);
        determineNextPlayer();
        
        return totalPoints;
    }

    /**
     * Counts and returns the number of points earned via pairs from playing 
     * the most recently played card.
     * 
     * @return the number of points earned via pairs from playing the most 
     *         recently played card
     */
    public int countPegPairs() {
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
        return occurrences * (occurrences - 1);
    }

    /**
     * Counts and returns the number of points earned via runs from playing 
     * the most recently played card.
     * 
     * @return the number of points earned via runs from playing the most 
     *         recently played card
     */
    public int countPegRuns() {
        int totalCardsPlayed = cardStack.size();
        int longestRun = 0;
        int testRunLength = 3;
        
        while (testRunLength <= totalCardsPlayed) {
            // Get the last (testRunLength - 1) cards played
            List<Card> subList = 
                    new LinkedList<Card>(cardStack.subList(0, testRunLength));
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

        return longestRun;
    }

    /**
     * If no more cards can be played for the current round, the last player 
     * to play a card is awarded a point. 
     */
    public void awardPointsForGo() {
        if (inPlay()) {
            throw new IllegalStateException("Cards can still be played");
        }
        addPoints(nextToPlayCard, 1);
    }

    /**
     * Returns true if the count is currently 31, false otherwise. It is 
     * expected that if true is returned, the caller will subsequently call 
     * resetCount()
     * 
     * @return true if the count is currently 31, false otherwise
     */
    public boolean countIs31() {
        return count == MAX_COUNT;
    }

    public boolean inPlay() {
        for (int i = 0; i < numPlayers; i++) {
            if (hasPlayableCard(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the given player can play a card for the current round. 
     * If all their cards have been played, or they have no card that can be 
     * legally played, false is returned.
     * 
     * @param pid the player whose hand will be checked
     * @return true if the given player can currently play a card, false 
     *         otherwise
     */
    public boolean hasPlayableCard(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + "exclusive");
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
     * Returns true if every player in the game has played their entire hand 
     * for the second stage, false otherwise. 
     * 
     * @return true if all cards have been played in the second stage, false 
     *         otherwise
     */
    public boolean allCardsPlayed() {
        for (List<Card> playedCards : playedCardsByPlayer) {
            if (playedCards.size() < HAND_SIZE) {
                return false;
            }
        }

        return true;
    }

    public void resetCount() {
        count = 0;
        cardStack.clear();
        determineNextPlayer();
    }

    private void determineNextPlayer() {
        for (int i = (nextToPlayCard + 1) % numPlayers; i != nextToPlayCard; 
                i = (i + 1) % numPlayers) {
            if (hasPlayableCard(i)) {
                nextToPlayCard = i;
                return;
            }
        }
    }

    /**************************************************************************
    * Third Stage (Show)
    **************************************************************************/
    /**
     * Counts and returns the number of points present in the given hand in 
     * combination with the starter card.
     * 
     * @param hand the hand that will be counted up
     * @return the number of points present in the given hand
     */
    public int countHand(List<Card> hand) {
        int score = count15Combos(hand);
        score += countRuns(hand);
        score += countPairs(hand);
        score += countFlush(hand);
        score += countNobs(hand);
        return score;
    }

    /**
     * Counts and returns the number of points earned from combinations of 
     * cards that add up to 15 in a given hand along with the starter card.
     * 
     * @param hand the hand that will be searched
     * @return the number of points present in the given player's hand
     */
    public int count15Combos(List<Card> hand) {
        if (hand.isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hand.add(starterCard);
        int count = getCombos(hand, 0, 0);
        hand.remove(starterCard);
        return count * 2;
    }

    // Starting from the given idx, recursively searches through the given hand
    // in search of a subset that adds up to 15. Returns the number of such 
    // subsets. Note that this algorithm has a max runtime of O(2^n), but n  
    // should never be more than 5
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

    /**
     * Counts all points earned through runs in the given hand. A run is a 
     * sequence of consecutive numbers irrespective of suit (e.g. a 5 of clubs, 
     * 6 of spades, and 7 of diamonds form a run of three). The given hand 
     * must not be empty, and there must be a starter card to reference
     * 
     * @param hand the hand that will be searched
     * @return the number of points earned through runs in the given hand
     */
    public int countRuns(List<Card> hand) {
        if (hand.isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        hand.add(starterCard);
        Collections.sort(hand);
        
        // Count the number of times each number appears in this hand
        int[] occurrences = new int[CARDS_PER_SUIT];
        for (Card card : hand) {
            int value = card.getRankValue();
            occurrences[value]++;
        }

        int totalPoints = 0;
        int currentRun = 0;
        int prevValue = -1;

        // Used for situations in which a number appears
        // more than once in a single run (e.g. 7, 7, 8, 
        // 9 would produce a multiplier of 2 since 7 
        // appears twice)
        int multiplier = 1;  

        // Loop through the hand in search of runs
        int i = 0;
        while (i < hand.size()) {
            int currentValue = hand.get(i).getRankValue();
            assert(currentValue != prevValue) : "Incorrect traversal of hand for countRuns";

            // If the currentValue equals the previous value + 1, increase the 
            // counter for the current run
            if (currentValue == prevValue + 1) {
                currentRun++;
            } else {
                // Since the hand is sorted, this value must be greater than 
                // the previous value + 1, so determine if we had a valid run 
                // and reset the appropriate variables
                if (currentRun >= 3) {
                    totalPoints += currentRun * multiplier;
                }
                
                currentRun = 1;
                multiplier = 1;
            }

            multiplier *= occurrences[currentValue];
            prevValue = currentValue;
            i += occurrences[currentValue];
        }

        // If we had a run at the end of the hand, make sure this is counted
        if (currentRun >= 3) {
            totalPoints += currentRun * multiplier;
        }

        hand.remove(starterCard);
        return totalPoints;
    }

    /**
     * Counts all points earned through pairs in the given hand. A pair is 
     * worth 2 points.
     * 
     * @param hand the hand that will be searched
     * @return the number of points earned through pairs
     */
    public int countPairs(List<Card> hand) {
        if (hand.isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }
        hand.add(starterCard);
        Map<Integer, Integer> occurrences = new HashMap<Integer, Integer>();
        for (Card card : hand) {
            int value = card.getRankValue();
            occurrences.put(value, occurrences.getOrDefault(value, 0) + 1);
        }

        int totalPoints = 0;
        for (int valueOccurrence : occurrences.keySet()) {
            totalPoints += valueOccurrence * (valueOccurrence - 1);
        }

        hand.remove(starterCard);
        return totalPoints;
    }

    /**
     * Counts and returns points earned through flush. A flush is a hand in 
     * which all cards are of the same suit. If all four cards in a player's  
     * hand share the same suit, 4 points are earned. If the starter card is  
     * the same suit as these four cards, 5 points are earned.
     * 
     * @param hand the hand that will be searched
     * @return the number of points earned through flush
     */
    public int countFlush(List<Card> hand) {
        if (hand.isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        for (int i = 1; i < hand.size(); i++) {
            if (hand.get(i).getSuit() != hand.get(i - 1).getSuit()) {
                return 0;
            }
        }

        if (starterCard.getSuit() == hand.get(0).getSuit()) {
            return 5;
        }
        return 4;
    }

    /**
     * Returns 1 if this hand contains a jack that has the same suit as the 
     * starter card (formally called "one for his nob").
     * 
     * @param hand the hand that will be searched
     * @return
     */
    public int countNobs(List<Card> hand) {
        if (hand.isEmpty()) {
            throw new IllegalStateException("Player has no cards in their hand");
        } else if (starterCard == null) {
            throw new IllegalStateException("No starter card");
        }

        for (Card card : hand) {
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
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + "exclusive");
        }

        return gameScores[pid] >= MAX_SCORE;
    }

    public int getPlayerScore(int pid) { return gameScores[pid]; }

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
        nextToPlayCard = -1;
    }
}