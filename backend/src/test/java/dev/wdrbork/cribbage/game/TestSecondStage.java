package dev.wdrbork.cribbage.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.CribbageManager;
import dev.wdrbork.cribbage.logic.game.CribbagePegging;

import java.util.LinkedList;
import java.util.List;


public class TestSecondStage {
    private static final int PLAYER_ONE_ID = 0;
    private static final int PLAYER_TWO_ID = 1;
    private static final int PLAYER_THREE_ID = 2;

    private static final int TOTAL_POINTS_IDX = 0;
    private static final int RUNS_IDX = 1;
    private static final int PAIRS_IDX = 2;

    private CribbageManagerTest man;

    private class CribbageManagerTest extends CribbageManager {
        public CribbageManagerTest(int numPlayers) {
            super(numPlayers);
        }

        public void setHand(int pid, CribbageHand hand) {
            hands.set(pid, hand);
        }

        public int[] playCardByIndex(int pid, int idx) { 
            return playCard(pid, hands.get(pid).getCard(idx));
        }

        public void setCount(int val) { count = val; }

        public int getCount() { return count; }

        public LinkedList<Card> getCardStack() { return cardStack; }

        public List<Deck> getPlayedCards() { 
            return playedCardsByPlayer; 
        }

        public void setGameScore(int pid, int val) { gameScores[pid] = val; }

        public int[] getGameScores() { return gameScores; }
    }

    @Test
    public void testSingleRoundTwoPlayers() {
        man = new CribbageManagerTest(2);
        setupDecks(PLAYER_TWO_ID, 2);

        CribbageHand playerOneDeck = new CribbageHand();
        playerOneDeck.addCard(new Card(Suit.SPADE, Rank.EIGHT)); //
        playerOneDeck.addCard(new Card(Suit.HEART, Rank.EIGHT)); //
        playerOneDeck.addCard(new Card(Suit.SPADE, Rank.SEVEN)); //
        playerOneDeck.addCard(new Card(Suit.SPADE, Rank.SIX)); //
        man.setHand(PLAYER_ONE_ID, playerOneDeck);

        CribbageHand playerTwoDeck = new CribbageHand();
        playerTwoDeck.addCard(new Card(Suit.DIAMOND, Rank.KING)); //
        playerTwoDeck.addCard(new Card(Suit.DIAMOND, Rank.SEVEN)); //
        playerTwoDeck.addCard(new Card(Suit.CLUB, Rank.FOUR)); //
        playerTwoDeck.addCard(new Card(Suit.SPADE, Rank.FIVE)); //
        man.setHand(PLAYER_TWO_ID, playerTwoDeck);

        assertTrue(man.movePossible());

        // Player 1 plays the eight of spades
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        int[] points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 8);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the seven of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 2);
        assertEquals(currentCount, 15);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 2);

        // Player 1 can't play a card they have already played
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertThrows(IllegalArgumentException.class, () ->{
            man.playCardByIndex(PLAYER_ONE_ID, 0);
        });

        // Player 1 plays the six of spades
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 3);
        assertEquals(currentCount, 21);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 3);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // Player 2 plays the king of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 2);
        assertEquals(currentCount, 31);
        assertTrue(man.countIs31());
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        // The count is 31, so nobody should be able to play a card
        assertFalse(man.movePossible());
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_ONE_ID, 2);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_TWO_ID, 2);
        });

        man.resetCount();

        // It is player 1's turn, not player 2
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_TWO_ID, 2);
        });

        // Player 1 plays the eight of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 8);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // Player 2 plays the five of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 3);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 13);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        // Player 1 plays the seven of spades
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 20);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // assertThrows(IllegalStateException.class, () -> { 
        //     man.awardPointsForGo();
        // });

        // Player 2 plays their fourth and final card
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 1);
        assertEquals(currentCount, 24);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 5);

        // man.awardPointsForGo();
        // assertEquals(man.getGameScores()[PLAYER_TWO_ID], 5);
    }

    @Test
    public void testSingleRoundThreePlayers() {
        man = new CribbageManagerTest(3);
        setupDecks(PLAYER_THREE_ID, 3);

        CribbageHand playerOneDeck = new CribbageHand();
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.ACE)); //
        playerOneDeck.addCard(new Card(Suit.CLUB, Rank.ACE)); //
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.TWO)); // 
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.THREE)); //
        man.setHand(PLAYER_ONE_ID, playerOneDeck);

        CribbageHand playerTwoDeck = new CribbageHand();
        playerTwoDeck.addCard(new Card(Suit.CLUB, Rank.FIVE)); //
        playerTwoDeck.addCard(new Card(Suit.HEART, Rank.JACK)); //
        playerTwoDeck.addCard(new Card(Suit.HEART, Rank.QUEEN)); //
        playerTwoDeck.addCard(new Card(Suit.CLUB, Rank.QUEEN)); // 
        man.setHand(PLAYER_TWO_ID, playerTwoDeck);

        CribbageHand playerThreeDeck = new CribbageHand();
        playerThreeDeck.addCard(new Card(Suit.DIAMOND, Rank.KING)); // 
        playerThreeDeck.addCard(new Card(Suit.CLUB, Rank.SEVEN)); //
        playerThreeDeck.addCard(new Card(Suit.SPADE, Rank.NINE)); //
        playerThreeDeck.addCard(new Card(Suit.DIAMOND, Rank.NINE)); //
        man.setHand(PLAYER_THREE_ID, playerThreeDeck);

        Card falseCard = new Card(Suit.HEART, Rank.ACE);
        Card trueCard = new Card(Suit.DIAMOND, Rank.ACE);
        assertFalse(man.getHand(PLAYER_ONE_ID).contains(falseCard));
        assertTrue(man.getHand(PLAYER_ONE_ID).contains(trueCard));

        assertTrue(man.movePossible());

        // Player 1 plays the three of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        int[] points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        int currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 3);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the queen of clubs
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 3);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 13);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(CribbagePegging.countPegPairs(man.getCardStack()), 0);
        assertEquals(CribbagePegging.countPegRuns(man.getCardStack()), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 3 plays the king of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_THREE_ID);
        points = man.playCardByIndex(PLAYER_THREE_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 23);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 0);

        // Player 1 plays the two of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 25);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the five of clubs
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 30);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 5);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 3 can't play any cards
        assertFalse(man.hasPlayableCard(PLAYER_THREE_ID));

        // Player 1 plays the ace of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 2);
        assertEquals(currentCount, 31);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 6);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 2);

        // The count is 31, so nobody should be able to play a card
        assertFalse(man.movePossible());
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_ONE_ID, 0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_TWO_ID, 2);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_THREE_ID, 1);
        });

        man.resetCount();

        // It is player 2's turn, not player 3
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_THREE_ID, 1);
        });

        // Player 2 plays the jack of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 10);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 3 plays the seven of clubs
        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_THREE_ID);
        points = man.playCardByIndex(PLAYER_THREE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 17);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 0);

        // Player 1 plays the ace of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_ONE_ID);
        points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 18);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 2);

        // Player 2 plays the queen of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 1);
        assertEquals(currentCount, 28);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 1);
        assertFalse(man.hasPlayableCard(PLAYER_TWO_ID));

        // Neither player 3 nor player 1 can play a card
        assertFalse(man.hasPlayableCard(PLAYER_THREE_ID));
        assertFalse(man.hasPlayableCard(PLAYER_ONE_ID));

        // Player 2 pegs for go
        assertTrue(man.nextToPlayCard() == PLAYER_TWO_ID);
        // man.awardPointsForGo();
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 1);

        man.resetCount();

        // Player 3 plays their nine of spades and nine of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_THREE_ID);
        points = man.playCardByIndex(PLAYER_THREE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 9);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 0);

        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        assertTrue(man.nextToPlayCard() == PLAYER_THREE_ID);
        points = man.playCardByIndex(PLAYER_THREE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 3);
        assertEquals(currentCount, 18);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(points[PAIRS_IDX], 2);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 3);

        // Player 3 pegs for go
        assertTrue(man.nextToPlayCard() == PLAYER_THREE_ID);
        // man.awardPointsForGo();
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 3);
    }

    @Test
    public void testPairs() {
        man = new CribbageManagerTest(2);
        setupDecks(PLAYER_TWO_ID, 2);

        CribbageHand playerOneDeck = new CribbageHand();
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.ACE)); //
        playerOneDeck.addCard(new Card(Suit.CLUB, Rank.ACE)); //
        man.setHand(PLAYER_ONE_ID, playerOneDeck);

        CribbageHand playerTwoDeck = new CribbageHand();
        playerTwoDeck.addCard(new Card(Suit.HEART, Rank.ACE)); //
        playerTwoDeck.addCard(new Card(Suit.SPADE, Rank.ACE)); //
        man.setHand(PLAYER_TWO_ID, playerTwoDeck);

        // Player 1 plays the ace of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int[] points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 1);
        assertEquals(points[PAIRS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the ace of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 2);
        assertEquals(currentCount, 2);
        assertEquals(points[PAIRS_IDX], 2);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 2);

        // Player 1 plays the ace of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 6);
        assertEquals(currentCount, 3);
        assertEquals(points[PAIRS_IDX], 6);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 6);

        // Player 2 plays the ace of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 13);
        assertEquals(currentCount, 4);
        assertEquals(points[PAIRS_IDX], 12);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 15);
    }

    @Test
    public void testRunOf7() {
        man = new CribbageManagerTest(2);
        setupDecks(PLAYER_TWO_ID, 2);

        CribbageHand playerOneDeck = new CribbageHand();
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.TWO));
        playerOneDeck.addCard(new Card(Suit.CLUB, Rank.THREE));
        playerOneDeck.addCard(new Card(Suit.HEART, Rank.SIX));
        playerOneDeck.addCard(new Card(Suit.HEART, Rank.SEVEN));
        man.setHand(PLAYER_ONE_ID, playerOneDeck);

        CribbageHand playerTwoDeck = new CribbageHand();
        playerTwoDeck.addCard(new Card(Suit.HEART, Rank.FOUR));
        playerTwoDeck.addCard(new Card(Suit.SPADE, Rank.ACE));
        playerTwoDeck.addCard(new Card(Suit.SPADE, Rank.FIVE));
        man.setHand(PLAYER_TWO_ID, playerTwoDeck);

        // Player 1 plays the two of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int[] points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 2);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the four of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 6);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 1 plays the six of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 12);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the ace of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 13);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 1 plays the three of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 16);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the five of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 6);
        assertEquals(currentCount, 21);
        assertEquals(points[RUNS_IDX], 6);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 6);

        // Player 1 plays the seven of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 8);
        assertEquals(currentCount, 28);
        assertEquals(points[RUNS_IDX], 7);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 8);
    }

    @Test
    public void testRepeatedRuns() {
        man = new CribbageManagerTest(2);
        setupDecks(PLAYER_TWO_ID, 2);

        CribbageHand playerOneDeck = new CribbageHand();
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.TWO));
        playerOneDeck.addCard(new Card(Suit.CLUB, Rank.THREE));
        playerOneDeck.addCard(new Card(Suit.HEART, Rank.SIX));
        playerOneDeck.addCard(new Card(Suit.HEART, Rank.SEVEN));
        man.setHand(PLAYER_ONE_ID, playerOneDeck);

        CribbageHand playerTwoDeck = new CribbageHand();
        playerTwoDeck.addCard(new Card(Suit.HEART, Rank.FOUR));
        playerTwoDeck.addCard(new Card(Suit.SPADE, Rank.ACE));
        playerTwoDeck.addCard(new Card(Suit.SPADE, Rank.FIVE));
        man.setHand(PLAYER_TWO_ID, playerTwoDeck);

        // Player 1 plays the two of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int[] points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 2);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the four of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 0);
        assertEquals(currentCount, 6);
        assertEquals(points[RUNS_IDX], 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 1 plays the three of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 3);
        assertEquals(currentCount, 9);
        assertEquals(points[RUNS_IDX], 3);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // Player 2 plays the five of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 4);
        assertEquals(currentCount, 14);
        assertEquals(points[RUNS_IDX], 4);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        // Player 1 plays the six of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 5);
        assertEquals(currentCount, 20);
        assertEquals(points[RUNS_IDX], 5);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 8);

        // Player 2 plays the ace of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 6);
        assertEquals(currentCount, 21);
        assertEquals(points[RUNS_IDX], 6);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 10);

        // Player 1 plays the seven of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points[TOTAL_POINTS_IDX], 8);
        assertEquals(currentCount, 28);
        assertEquals(points[RUNS_IDX], 7);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 16);
    }

    @Test
    public void testWinnerFromPegging() {
        man = new CribbageManagerTest(2);
        setupDecks(PLAYER_TWO_ID, 2);

        CribbageHand playerOneDeck = new CribbageHand();
        playerOneDeck.addCard(new Card(Suit.DIAMOND, Rank.TWO));
        man.setHand(PLAYER_ONE_ID, playerOneDeck);

        man.setGameScore(PLAYER_ONE_ID, 120);
        man.setCount(13);

        assertFalse(man.isWinner(PLAYER_ONE_ID));
        man.playCardByIndex(PLAYER_ONE_ID, 0);
        assertTrue(man.isWinner(PLAYER_ONE_ID));

        assertTrue(man.gameOver());
    }

    // Assumes that all tests in TestGameSetup are passing
    private List<CribbageHand> setupDecks(int dealerId, int numPlayers) {
        man.setDealer(dealerId);
        List<CribbageHand> hands = man.dealHands();
        while (man.getCrib().size() < 4) {
            for (int i = 0; i < numPlayers; i++) {
                System.out.println("Player " + i + " sends card to crib");
                System.out.println(man.getCrib());
                man.sendCardToCrib(i, hands.get(i).getCard(0));
            }
        }

        return hands;
    }
}