package logic.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    protected final int numPlayers;
    protected final Deck deck;
    protected int[] gameScores;  // Set back to final once done with testing
    protected final List<List<Card>> hands;
    protected final List<Card> crib;
    protected LinkedList<Card> cardStack;  // Set back to final once done with testing
    protected List<List<Card>> playedCardsByPlayer;  // Set back to final once done with testing
    protected int lastToPlayCard;
    protected int nextToPlayCard;

    // The starter card drawn at the end of the first stage
    protected Card starterCard;  

    // Represents a player's ID (between 0 and numPlayers exclusive)
    protected int dealerId; 
    
    // Count for the second stage of play
    protected int count;         

    /**
     * Sets up a cribbage game using the given number of players.
     * 
     * @param numPlayers the number of players in the game (can only be 2 or 3)
     */
    public CribbageManager(int numPlayers) {
        if (numPlayers != 2 && numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        }

        this.numPlayers = numPlayers;
        deck = new Deck();
        gameScores = new int[numPlayers];
        lastToPlayCard = -1;
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

    /**
     * Creates a deep copy of an existing game.
     * 
     * @param copy the CribbageManager that will be copied over
     */
    public CribbageManager(CribbageManager copy) {
        if (copy == null) {
            throw new NullPointerException();
        }

        this.numPlayers = copy.numPlayers;
        this.deck = copy.deck;
        this.gameScores = copy.gameScores.clone();
        this.nextToPlayCard = copy.nextToPlayCard;
        this.dealerId = copy.dealerId;
        this.hands = copy.getAllHands();
        this.playedCardsByPlayer = copy.getPlayedCards();
        this.crib = new ArrayList<Card>(copy.crib);
        this.cardStack = new LinkedList<Card>(copy.cardStack);
        this.count = copy.count;
        this.starterCard = copy.starterCard;
    }

    // Getter functions
    public int numPlayers() { return numPlayers; }

    public int lastToPlayCard() { return lastToPlayCard; }

    public int nextToPlayCard() { return nextToPlayCard; }

    public int count() { return count; }

    public int dealer() { return dealerId; }

    public int getPlayerScore(int pid) { 
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return gameScores[pid]; 
    }

    public List<List<Card>> getAllHands() { 
        List<List<Card>> deepCopy = new ArrayList<List<Card>>();
        for (List<Card> hand : hands) {
            deepCopy.add(new ArrayList<Card>(hand));
        }
        return deepCopy;
    }

    public List<List<Card>> getPlayedCards() {
        List<List<Card>> deepCopy = new ArrayList<List<Card>>();
        for (List<Card> played : playedCardsByPlayer) {
            deepCopy.add(new ArrayList<Card>(played));
        }
        return deepCopy;
    }

    public List<Card> getHand(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return new ArrayList<Card>(hands.get(pid));
    }

    public List<Card> getCrib() {
        return Collections.unmodifiableList(crib);
    }

    public Card getLastPlayedCard() {
        if (cardStack.isEmpty()) {
            return null;
        }
        return cardStack.getFirst();
    }

    /**************************************************************************
    * Setup Stage
    **************************************************************************/
    public Card pickCardForDealer() {
        return deck.pickRandomCard();
    }

    public void setDealer(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        } 
        
        this.dealerId = pid;
        nextToPlayCard = (dealerId + 1) % numPlayers;
    }

    public void rotateDealer() {
        if (dealerId == -1) {
            throw new IllegalStateException("Must set dealer first");
        }

        dealerId = (dealerId + 1) % numPlayers;
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

    /**
     * Manually adds a card to the passed-in player's hand. Note: does not 
     * need to be used in combination with dealHands() since that method adds 
     * the cards to each hand by itself.
     * 
     * @param pid the ID of the player whose hand will be given the card
     * @param card the card to be added to the player's hand
     * @throws IllegalArgumentException - if the player's hand is full
     */
    public void addCardToHand(int pid, Card card) {
        if (hands.get(pid).size() == HAND_SIZE) {
            System.out.println(pid);
            System.out.println(hands);
            System.out.println(playedCardsByPlayer);
            throw new IllegalArgumentException("Can't add anymore cards to " + 
                    "this player's hand");
        }

        hands.get(pid).add(card);
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
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
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
     * @throws IllegalArgumentException if this player cannot play the card, 
     *                                  has already played the card, or it is 
     *                                  not this player's turn
     * @throws NullPointerException if the card is null
     * @throws IndexOutOfBoundsException if the player ID is invalid
     */
    public int playCard(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        } else if (card == null) {
            System.out.println(hands);
            System.out.println(playedCardsByPlayer);
            System.out.println("PID = " + pid);
            System.out.println("Desired PID = " + nextToPlayCard);
            System.out.println("Count = " + count);
           throw new NullPointerException("Card is null");
        } else if (cardAlreadyPlayed(card)) {
            throw new IllegalArgumentException("Player has already played this card");
        } else if (pid != nextToPlayCard) {
            throw new IllegalArgumentException("Not this player's turn");
        } else if (maxCountExceeded(card)) {
            throw new IllegalArgumentException("Card cannot be played");
        } else if (!hands.get(pid).contains(card)) {
            System.out.println("Count = " + count);
            System.out.println("Card = " + card);
            System.out.println("Hand = " + hands.get(pid));
            System.out.println("Played cards = " + playedCardsByPlayer.get(pid));
            throw new IllegalStateException("Player " + pid + " does not have card in their hand");
        }
    
        count += card.getValue();
        cardStack.addFirst(card);
        playedCardsByPlayer.get(pid).add(card);

        int totalPoints = 0;
        if (count == 15 || count == 31) {
            totalPoints += 2;
        }
        totalPoints += CribbagePegging.countPegPairs(cardStack) + 
                CribbagePegging.countPegRuns(cardStack);
        addPoints(pid, totalPoints);
        lastToPlayCard = pid;
        determineNextPlayer();

        // System.out.println("Player " + (pid + 1) + " plays a " + card + " to make the count " + count);
        // System.out.println("Points earned: " + totalPoints);
        
        return totalPoints;
    }

    /**
     * Check to see if the passed-in card can be played. A card can be played 
     * if it has not already been played and playing it wouldn't cause the
     * count to exceed the maxmimum value of 31. Returns true if the card
     * can be played, false otherwise
     * @param card a card
     * @return true if the card can be played, false otherwise
     */
    public boolean canPlayCard(Card card) {
        return !maxCountExceeded(card) && !cardAlreadyPlayed(card);
    }

    /**
     * Check to see if playing the passed-in card causes the count to exceed 
     * the maximum value of 31. Return true if this is the case, false
     * otherwise
     * @param card the card that will be played
     * @return true if playing the card would cause the count to exceed 31, 
     *         false otherwise
     */
    public boolean maxCountExceeded(Card card) {
        return count + card.getValue() > MAX_COUNT;
    }

    /**
     * If no more cards can be played for the current round, the last player 
     * to play a card is awarded a point. 
     */
    public void awardPointsForGo() {
        if (movePossible()) {
            System.out.println(count);
            System.out.println(lastToPlayCard);
            System.out.println(hands);
            System.out.println(playedCardsByPlayer);
            throw new IllegalStateException("Cards can still be played");
        }
        addPoints(lastToPlayCard, 1);
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

    /**
     * Checks to see if somebody in the game can play a card in the current
     * round. Returns true if a card can be played, false otherwise.
     * 
     * @return true if a card can be played, false otherwise
     */
    public boolean movePossible() {
        for (int i = 0; i < numPlayers; i++) {
            if (hasPlayableCard(i)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks to see if the passed-in card has already been played. Returns 
     * true if it has already been played, false otherwise.
     * 
     * @param card a card that may or may have not been played already
     * @return true if the passed-in card has already been played, false 
     *         otherwise
     */
    public boolean cardAlreadyPlayed(Card card) {
        for (List<Card> playedCards : playedCardsByPlayer) {
            if (playedCards.contains(card)) {
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
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
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

    public void resetCount() {
        count = 0;
        cardStack.clear();
        determineNextPlayer();
    }

    /**
     * Manually sets the next person to play a card, overriding the underlying 
     * function that determines the next player based on the cards in each 
     * player's hand. Note: should rarely be used for normal cribbage games.
     * 
     * @param pid the ID of the next player
     */
    public void setNextPlayer(int pid) {
        this.nextToPlayCard = pid;
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

    public boolean roundOver() {
        // If a player has won the game, return true
        if (gameOver()) return true;

        // If no more cards can be played, return true
        for (List<Card> playedCards : getPlayedCards()) {
            if (playedCards.size() != HAND_SIZE) {
                return false;
            }
        }
        return true;
    }

    /**************************************************************************
    * Third Stage (Show)
    **************************************************************************/
    /**
     * Counts and returns the number of points present in the given player's 
     * hand in combination with the starter card. This value is added to the 
     * player's overall total for the game.
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points present in the given player's hand
     */
    public int countHand(int pid) {
        return countHand(pid, true);
    }

    /**
     * Counts and returns the number of points present in the given player's 
     * hand in combination with the starter card. The number of points earned 
     * will be added to the player's total for this game if true is passed in.
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @param addToScore true if the score from the hand should be added to the 
     *                   player's total score for the game; false otherwise
     * @return the number of points present in the given player's hand
     */
    public int countHand(int pid, boolean addToScore) {
        int score = count15Combos(pid);
        score += countRuns(pid);
        score += countPairs(pid);
        score += countFlush(pid);
        score += countNobs(pid);

        if (addToScore) {
            addPoints(pid, score);
        }
        
        return score;
    }

    /**
     * Counts and returns the number of points present in the crib in 
     * combination with the starter card. Points earned from the crib are 
     * added to the current dealer's total.
     * 
     * @return the number of points present in the crib
     */
    public int countCrib() {
        if (dealerId < 0 || dealerId >= numPlayers) {
            throw new IllegalStateException("Dealer ID is invalid");
        }

        int score = CribbageScoring.count15Combos(crib, starterCard);
        score += CribbageScoring.countRuns(crib, starterCard);
        score += CribbageScoring.countPairs(crib, starterCard);
        score += CribbageScoring.countFlush(crib, starterCard);
        score += CribbageScoring.countNobs(crib, starterCard);
        addPoints(dealerId, score);

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
    public int count15Combos(int pid) {
        if (pid < 0 || pid > numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return CribbageScoring.count15Combos(hands.get(pid), starterCard);
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
    public int countRuns(int pid) {
        if (pid < 0 || pid > numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return CribbageScoring.countRuns(hands.get(pid), starterCard);
    }

    /**
     * Counts all points earned through pairs in the given hand. A pair is 
     * worth 2 points.
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return the number of points earned through pairs
     */
    public int countPairs(int pid) {
        if (pid < 0 || pid > numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

       return CribbageScoring.countPairs(hands.get(pid), starterCard);
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
    public int countFlush(int pid) {
        if (pid < 0 || pid > numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return CribbageScoring.countFlush(hands.get(pid), starterCard);
    }

    /**
     * Returns 1 if this hand contains a jack that has the same suit as the 
     * starter card (formally called "one for his nob").
     * 
     * @param pid the ID of the player whose hand will be counted up
     * @return
     */
    public int countNobs(int pid) {
        if (pid < 0 || pid > numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return CribbageScoring.countNobs(hands.get(pid), starterCard);
    }

    /**
     * Adds points to the designated player's total and returns the new total. 
     * Caps out at 121. It is expected that points earned from either pegging 
     * or the hand are added to a player's total in order to maintain the 
     * integrity of the cribbage game.
     * 
     * @param pid   the player's ID
     * @param total the number of points to be added
     */
    protected void addPoints(int pid, int total) {
        gameScores[pid] = Math.min(MAX_SCORE, gameScores[pid] + total);
    }

    public boolean isWinner(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return gameScores[pid] >= MAX_SCORE;
    }

    public boolean gameOver() {
        for (int i = 0; i < numPlayers; i++) {
            if (isWinner(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resets all state from a round, including hands, the crib, and played 
     * cards. Rotates the dealer as well.
     */
    public void clearRoundState() {
        resetCount();
        clearAllHands();

        for (List<Card> playedCards : playedCardsByPlayer) {
            playedCards.clear();
        }

        crib.clear();
        lastToPlayCard = -1;
        rotateDealer();
        determineNextPlayer();
    }

    /**
     * Clears the hands of all players in the game.
     */
    public void clearAllHands() {
        for (List<Card> hand : hands) {
            hand.clear();
        }
    }

    public void clearHand(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        hands.get(pid).clear();
    }

    public void clearHandOfUnplayedCards(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IndexOutOfBoundsException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        hands.get(pid).retainAll(playedCardsByPlayer.get(pid));
    }
}