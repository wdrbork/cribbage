package dev.wdrbork.cribbage.logic.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.wdrbork.cribbage.logic.cards.Card;
import dev.wdrbork.cribbage.logic.cards.Deck;
import dev.wdrbork.cribbage.logic.cards.Rank;
import dev.wdrbork.cribbage.logic.cards.StandardDeck;
import dev.wdrbork.cribbage.logic.cards.CribbageHand;
import dev.wdrbork.cribbage.logic.game.ai.CribbageAI;
import dev.wdrbork.cribbage.logic.game.ai.SmartPlayer;

/**
 * Manages a game of cribbage. The caller is largely in charge of maintaining 
 * the correct order of operations for a cribbage game (e.g. they should not 
 * be adding points in a hand until after the second stage). The caller should 
 * also keep track of which player is assigned to which ID so that points are
 * not being added to the wrong player.
 */
@Service
public class CribbageManager {
    private static final int MAX_COUNT = 31;
    private static final int MAX_SCORE = 121;
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    private static final int ROUND_POINT_CATEGORIES = 4;
    private static final int TOTAL_POINTS = 0;
    private static final int RUNS = 1;
    private static final int PAIRS = 2;
    private static final int SPECIAL = 3;

    protected final int numPlayers;
    protected final StandardDeck deck;
    protected final int[] gameScores;
    protected final CribbageAI[] ai;
    protected final List<CribbageHand> hands;
    protected final CribbageHand crib;
    protected LinkedList<Card> cardStack;
    protected List<Deck> playedCardsByPlayer;
    protected int lastToPlayCard;
    protected int nextToPlayCard;

    // The starter card drawn at the end of the first stage
    protected Card starterCard;

    // Represents a player's ID (between 0 and numPlayers exclusive)
    protected int dealerId;
    
    // Count for the second stage of play
    protected int count;

    /**
     * Sets up a default cribbage game with two players.
     */
    public CribbageManager() {
        this(2);
    }
    
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
        deck = new StandardDeck();
        gameScores = new int[numPlayers];
        lastToPlayCard = -1;
        nextToPlayCard = -1;
        dealerId = -1;

        ai = new SmartPlayer[numPlayers - 1];
        for (int i = 0; i < ai.length; i++) {
            ai[0] = new SmartPlayer(this, i + 1);
        }

        hands = new ArrayList<CribbageHand>(numPlayers);
        playedCardsByPlayer = new ArrayList<Deck>(numPlayers);
        for (int i = 0; i < numPlayers; i++) {
            hands.add(new CribbageHand());
            playedCardsByPlayer.add(new CribbageHand());
        }

        crib = new CribbageHand();
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
        this.ai = copy.ai.clone();
        this.gameScores = copy.gameScores.clone();
        this.nextToPlayCard = copy.nextToPlayCard;
        this.dealerId = copy.dealerId;
        this.hands = copy.getAllHands();
        this.playedCardsByPlayer = copy.getPlayedCards();
        this.crib = new CribbageHand(copy.crib);
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

    public Card starterCard() { return starterCard; }

    public int[] gameScores() { return gameScores.clone(); }

    public int getPlayerScore(int pid) { 
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return gameScores[pid]; 
    }

    public List<CribbageHand> getAllHands() { 
        List<CribbageHand> deepCopy = new ArrayList<CribbageHand>();
        for (CribbageHand hand : hands) {
            deepCopy.add(new CribbageHand(hand));
        }
        return deepCopy;
    }

    public List<Deck> getPlayedCards() {
        List<Deck> deepCopy = new ArrayList<Deck>();
        for (Deck played : playedCardsByPlayer) {
            deepCopy.add(new Deck(played));
        }
        return deepCopy;
    }

    public CribbageHand getHand(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        return new CribbageHand(hands.get(pid));
    }

    public CribbageHand getCrib() {
        return new CribbageHand(crib);
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

    private void rotateDealer() {
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
    public List<CribbageHand> dealHands() {
        if (!checkEmptyHands()) {
            throw new IllegalArgumentException("Players still have cards in their hands");
        } else if (!crib.isEmpty()) {
            throw new IllegalArgumentException("Crib still contains cards");
        } else if (this.dealerId == -1) {
            throw new IllegalStateException("Dealer not decided");
        }

        resetDeck();
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
                hands.get((j + dealerId) % numPlayers).addCard(next);
            }
        }

        /* If there are three players, the crib starts out with one card. */
        if (this.numPlayers == 3) {
            Card next = deck.takeTopCard();
            assert(next != null) : "Deck is empty";
            crib.addCard(next);
        }

        // Rearrange the decks so that the cards increase in rank from left to right
        for (CribbageHand hand : hands) {
            hand.sortDeck();
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

        hands.get(pid).addCard(card);
    }

    /**
     * Selects the playing hand for each AI opponent and sends the appropriate
     * cards to the crib.
     */
    public List<Card> chooseAIPlayingHands() {
        List<Card> cribCards = new LinkedList<Card>();
        for (int i = 0; i < ai.length; i++) {
            CribbageHand currentHand = getHand(i + 1);
            CribbageHand playingHand = ai[i].choosePlayingHand();
            for (Card card : currentHand.getCards()) {
                if (!playingHand.contains(card)) {
                    cribCards.add(card);
                    sendCardToCrib(i + 1, card);
                }
            }
        }

        return cribCards;
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
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        } else if (card == null) {
            throw new NullPointerException("Card is null");
        } else if (!hands.get(pid).contains(card)) {
            throw new IllegalArgumentException("Player does not have this card");
        } else if (crib.size() == 4) {
            throw new IllegalStateException("Crib is full");
        }

        hands.get(pid).removeCard(card);
        crib.addCard(card);
    }

    public Card pickStarterCard() {
        // All hands and the crib must be finalized before the starter 
        // card is drawn
        // if (!checkStartingHands()) {
        //     throw new IllegalArgumentException("Not all hands have been finalized");
        // }

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
     * playing that card is added to the player's game score, and an array 
     * consisting of the total points (index 0), points earned from runs (index
     * 1), points earned from pairs (index 2), and points earned from go's and
     * getting the count to 15/31 (index 3) is returned.
     * 
     * @param pid the player's ID
     * @param card the card to be played
     * @return an array consisting of the total points earned as well as where
     *         those points came from
     * @throws IllegalArgumentException if this player cannot play the card, 
     *                                  has already played the card, or it is 
     *                                  not this player's turn
     * @throws NullPointerException if the card is null
     * @throws IllegalArgumentException if the player ID is invalid
     */
    public int[] playCard(int pid, Card card) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        } else if (card == null) {
            // System.out.println(hands);
            // System.out.println(playedCardsByPlayer);
            // System.out.println("PID = " + pid);
            // System.out.println("Desired PID = " + nextToPlayCard);
            // System.out.println("Count = " + count);
           throw new NullPointerException("Card is null");
        } else if (pid != nextToPlayCard) {
            System.out.println(dealerId);
            System.out.println(nextToPlayCard);
            throw new IllegalArgumentException("Not this player's turn");
        } else if (!canPlayCard(card)) {
            throw new IllegalArgumentException("Card cannot be played");
        } else if (!hands.get(pid).contains(card)) {
            // System.out.println("Count = " + count);
            // System.out.println("Card = " + card);
            // System.out.println("Hand = " + hands.get(pid));
            // System.out.println("Played cards = " + playedCardsByPlayer.get(pid));
            throw new IllegalStateException("Player " + pid + " does not have card in their hand");
        }
    
        count += card.getValue();
        cardStack.addFirst(card);
        playedCardsByPlayer.get(pid).addCard(card);

        int[] pointCategories = new int[ROUND_POINT_CATEGORIES];
        
        pointCategories[PAIRS] = CribbagePegging.countPegPairs(cardStack);
        pointCategories[RUNS] = CribbagePegging.countPegRuns(cardStack);
        if (count == 15 || count == 31) {
            pointCategories[SPECIAL] = 2;
        }

        int totalPoints = pointCategories[PAIRS] + 
                pointCategories[RUNS] + 
                pointCategories[SPECIAL];
        pointCategories[TOTAL_POINTS] = totalPoints;

        addPoints(pid, totalPoints);
        lastToPlayCard = pid;
        if (!movePossible() && count != 31) {
            awardPointsForGo();
            pointCategories[SPECIAL]++;
            pointCategories[TOTAL_POINTS]++;
        }
        determineNextPlayer();

        // System.out.println("Player " + (pid + 1) + " plays a " + card + " to make the count " + count);
        // System.out.println("Points earned: " + totalPoints);
        
        return pointCategories;
    }

    /**
     * Selects a card from the given AI's hand that should be played. The 
     * chosen card is returned.
     * @param pid the AI's ID
     * @return the card selected by the AI
     */
    public Card chooseAICard(int pid) {
        if (pid < 1 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid AI ID of " + 
                    pid + "; must be between 1 and " + numPlayers + " exclusive");
        }

        return ai[pid - 1].chooseCard();
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
            System.out.println(Arrays.toString(gameScores));
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
        for (Deck playedCards : playedCardsByPlayer) {
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
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        Deck playedCards = playedCardsByPlayer.get(pid);
        if (playedCards.size() == HAND_SIZE) {
            // All cards have been played, so return false
            return false;
        }

        for (Card card : hands.get(pid).getCards()) {
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
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        this.nextToPlayCard = pid;
    }

    private void determineNextPlayer() {
        for (
            int i = (nextToPlayCard + 1) % numPlayers;
            i != nextToPlayCard; 
            i = (i + 1) % numPlayers
        ) {
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
        for (Deck playedCards : getPlayedCards()) {
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
     * @return the number of points present in the given player's hand, split 
     *         into categories
     */
    public int[] countHand(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

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
    public int[] countHand(int pid, boolean addToScore) {
        int[] scores = hands.get(pid).countCribbageHand(starterCard, false);
        if (addToScore) {
            addPoints(pid, scores[TOTAL_POINTS]);
        }
        
        return scores;
    }

    /**
     * Counts and returns the number of points present in the crib in 
     * combination with the starter card. Points earned from the crib are 
     * added to the current dealer's total.
     * 
     * @return the number of points present in the crib, split into categories
     */
    public int[] countCrib() {
        if (dealerId < 0 || dealerId >= numPlayers) {
            throw new IllegalStateException("Dealer ID is invalid");
        } else if (crib.size() != 4) {
            throw new IllegalStateException("Crib does not have four cards");
        }

        int[] scores = crib.countCribbageHand(starterCard, true);
        addPoints(dealerId, scores[TOTAL_POINTS]);

        return scores;
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
            throw new IllegalArgumentException("Invalid player ID of " + 
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

        for (Deck playedCards : playedCardsByPlayer) {
            playedCards.clearDeck();
        }

        crib.clearDeck();
        lastToPlayCard = -1;
        rotateDealer();
        nextToPlayCard = (dealerId + 1) % numPlayers;
    }

    /**
     * Clears the hands of all players in the game.
     */
    public void clearAllHands() {
        for (CribbageHand hand : hands) {
            hand.clearDeck();
        }
    }

    public void clearHand(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        hands.get(pid).clearDeck();
    }

    public void clearHandOfUnplayedCards(int pid) {
        if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("Invalid player ID of " + 
                    pid + "; must be between 0 and " + numPlayers + " exclusive");
        }

        hands.get(pid).retainAll(playedCardsByPlayer.get(pid));
    }

    public void resetDeck() {
        deck.resetDeck();
    }
}