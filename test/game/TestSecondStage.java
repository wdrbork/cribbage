package test.logic.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.CribbageManager;
import logic.deck.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class TestSecondStage {
    private static final int PLAYER_ONE_ID = 0;
    private static final int PLAYER_TWO_ID = 1;
    private static final int PLAYER_THREE_ID = 2;

    private CribbageManagerTest man;

    private class CribbageManagerTest extends CribbageManager {
        public CribbageManagerTest(int numPlayers) {
            super(numPlayers);
        }

        public void setHand(int pid, List<Card> hand) {
            hands.set(pid, hand);
        }

        public int playCardByIndex(int pid, int idx) { 
            return playCard(pid, hands.get(pid).get(idx));
        }

        public void setCount(int val) { count = val; }

        public int getCount() { return count; }

        public LinkedList<Card> getCardStack() { return cardStack; }

        public List<List<Card>> getPlayedCards() { 
            return playedCardsByPlayer; 
        }

        public void setGameScore(int pid, int val) { gameScores[pid] = val; }

        public int[] getGameScores() { return gameScores; }
    }

    @Test
    public void testSingleRoundTwoPlayers() {
        man = new CribbageManagerTest(2);
        setupHands(PLAYER_TWO_ID, 2);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.SPADE, Rank.EIGHT)); //
        playerOneHand.add(new Card(Suit.HEART, Rank.EIGHT)); //
        playerOneHand.add(new Card(Suit.SPADE, Rank.SEVEN)); //
        playerOneHand.add(new Card(Suit.SPADE, Rank.SIX)); //
        man.setHand(PLAYER_ONE_ID, playerOneHand);

        List<Card> playerTwoHand = new ArrayList<Card>();
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.KING)); //
        playerTwoHand.add(new Card(Suit.DIAMOND, Rank.SEVEN)); //
        playerTwoHand.add(new Card(Suit.CLUB, Rank.FOUR)); //
        playerTwoHand.add(new Card(Suit.SPADE, Rank.FIVE)); //
        man.setHand(PLAYER_TWO_ID, playerTwoHand);

        assertTrue(man.inPlay());

        // Player 1 plays the eight of spades
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 8);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the seven of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 2);
        assertEquals(currentCount, 15);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 2);

        // Player 1 can't play a card they have already played
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        assertThrows(IllegalArgumentException.class, () ->{
            man.playCardByIndex(PLAYER_ONE_ID, 0);
        });

        // Player 1 plays the six of spades
        points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points, 3);
        assertEquals(currentCount, 21);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 3);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // Player 2 plays the king of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 2);
        assertEquals(currentCount, 31);
        assertTrue(man.countIs31());
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        // The count is 31, so nobody should be able to play a card
        assertFalse(man.inPlay());
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_ONE_ID, 2);
        });
        assertEquals(man.playCardByIndex(PLAYER_TWO_ID, 2), -1);

        man.resetCount();

        // It is player 1's turn, not player 2
        assertThrows(IllegalArgumentException.class, () -> {
            man.playCardByIndex(PLAYER_TWO_ID, 2);
        });

        // Player 1 plays the eight of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 8);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // Player 2 plays the five of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 3);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 13);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        // Player 1 plays the seven of spades
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 20);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        assertThrows(IllegalStateException.class, () -> { 
            man.awardPointsForGo();
        });

        // Player 2 plays their fourth and final card
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 24);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        man.awardPointsForGo();
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 5);
    }

    @Test
    public void testSingleRoundThreePlayers() {
        man = new CribbageManagerTest(3);
        setupHands(PLAYER_THREE_ID, 3);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.ACE)); //
        playerOneHand.add(new Card(Suit.CLUB, Rank.ACE)); //
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.TWO)); // 
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.THREE)); //
        man.setHand(PLAYER_ONE_ID, playerOneHand);

        List<Card> playerTwoHand = new ArrayList<Card>();
        playerTwoHand.add(new Card(Suit.CLUB, Rank.FIVE)); //
        playerTwoHand.add(new Card(Suit.HEART, Rank.JACK)); //
        playerTwoHand.add(new Card(Suit.HEART, Rank.QUEEN)); //
        playerTwoHand.add(new Card(Suit.CLUB, Rank.QUEEN)); // 
        man.setHand(PLAYER_TWO_ID, playerTwoHand);

        List<Card> playerThreeHand = new ArrayList<Card>();
        playerThreeHand.add(new Card(Suit.DIAMOND, Rank.KING)); // 
        playerThreeHand.add(new Card(Suit.CLUB, Rank.SEVEN)); //
        playerThreeHand.add(new Card(Suit.SPADE, Rank.NINE)); //
        playerThreeHand.add(new Card(Suit.DIAMOND, Rank.NINE)); //
        man.setHand(PLAYER_THREE_ID, playerThreeHand);

        assertTrue(man.inPlay());

        // Player 1 plays the three of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        int currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 3);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the queen of clubs
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 3);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 13);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 3 plays the king of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        points = man.playCardByIndex(PLAYER_THREE_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 23);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 1);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 0);

        // Player 1 plays the two of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 25);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the five of clubs
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 30);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 5);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 3 can't play any cards
        assertFalse(man.hasPlayableCard(PLAYER_THREE_ID));

        // Player 1 plays the ace of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 2);
        assertEquals(currentCount, 31);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 6);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 2);

        // The count is 31, so nobody should be able to play a card
        assertFalse(man.inPlay());
        assertEquals(man.playCardByIndex(PLAYER_ONE_ID, 0), -1);
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
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 10);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 3 plays the seven of clubs
        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        points = man.playCardByIndex(PLAYER_THREE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 17);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 2);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 0);

        // Player 1 plays the ace of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 18);
        assertEquals(man.getPlayedCards().get(PLAYER_ONE_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 3);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 2);

        // Player 2 plays the queen of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 28);
        assertEquals(man.getPlayedCards().get(PLAYER_TWO_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 4);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);
        assertFalse(man.hasPlayableCard(PLAYER_TWO_ID));

        // Neither player 3 nor player 1 can play a card
        assertFalse(man.hasPlayableCard(PLAYER_THREE_ID));
        assertFalse(man.hasPlayableCard(PLAYER_ONE_ID));

        // Player 2 pegs for go
        man.awardPointsForGo();
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 1);

        man.resetCount();

        // Player 3 plays their nine of spades and nine of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        points = man.playCardByIndex(PLAYER_THREE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 9);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 3);
        assertEquals(man.getCardStack().size(), 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 0);

        assertTrue(man.hasPlayableCard(PLAYER_THREE_ID));
        points = man.playCardByIndex(PLAYER_THREE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points, 2);
        assertEquals(currentCount, 18);
        assertEquals(man.getPlayedCards().get(PLAYER_THREE_ID).size(), 4);
        assertEquals(man.getCardStack().size(), 2);
        assertEquals(man.countPegPairs(), 2);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 2);

        // Player 3 pegs for go
        man.awardPointsForGo();
        assertEquals(man.getGameScores()[PLAYER_THREE_ID], 3);
    }

    @Test
    public void testPairs() {
        man = new CribbageManagerTest(2);
        setupHands(PLAYER_TWO_ID, 2);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.ACE)); //
        playerOneHand.add(new Card(Suit.CLUB, Rank.ACE)); //
        man.setHand(PLAYER_ONE_ID, playerOneHand);

        List<Card> playerTwoHand = new ArrayList<Card>();
        playerTwoHand.add(new Card(Suit.HEART, Rank.ACE)); //
        playerTwoHand.add(new Card(Suit.SPADE, Rank.ACE)); //
        man.setHand(PLAYER_TWO_ID, playerTwoHand);

        // Player 1 plays the ace of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 1);
        assertEquals(man.countPegPairs(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the ace of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 2);
        assertEquals(currentCount, 2);
        assertEquals(man.countPegPairs(), 2);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 2);

        // Player 1 plays the ace of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 6);
        assertEquals(currentCount, 3);
        assertEquals(man.countPegPairs(), 6);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 6);

        // Player 2 plays the ace of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 12);
        assertEquals(currentCount, 4);
        assertEquals(man.countPegPairs(), 12);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 14);
    }

    @Test
    public void testRunOf7() {
        man = new CribbageManagerTest(2);
        setupHands(PLAYER_TWO_ID, 2);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.TWO));
        playerOneHand.add(new Card(Suit.CLUB, Rank.THREE));
        playerOneHand.add(new Card(Suit.HEART, Rank.SIX));
        playerOneHand.add(new Card(Suit.HEART, Rank.SEVEN));
        man.setHand(PLAYER_ONE_ID, playerOneHand);

        List<Card> playerTwoHand = new ArrayList<Card>();
        playerTwoHand.add(new Card(Suit.HEART, Rank.FOUR));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.ACE));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.FIVE));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);

        // Player 1 plays the two of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 2);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the four of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 6);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 1 plays the six of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 12);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the ace of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 13);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 1 plays the three of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 16);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the five of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 6);
        assertEquals(currentCount, 21);
        assertEquals(man.countPegRuns(), 6);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 6);

        // Player 1 plays the seven of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points, 7);
        assertEquals(currentCount, 28);
        assertEquals(man.countPegRuns(), 7);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 7);
    }

    @Test
    public void testRepeatedRuns() {
        man = new CribbageManagerTest(2);
        setupHands(PLAYER_TWO_ID, 2);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.TWO));
        playerOneHand.add(new Card(Suit.CLUB, Rank.THREE));
        playerOneHand.add(new Card(Suit.HEART, Rank.SIX));
        playerOneHand.add(new Card(Suit.HEART, Rank.SEVEN));
        man.setHand(PLAYER_ONE_ID, playerOneHand);

        List<Card> playerTwoHand = new ArrayList<Card>();
        playerTwoHand.add(new Card(Suit.HEART, Rank.FOUR));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.ACE));
        playerTwoHand.add(new Card(Suit.SPADE, Rank.FIVE));
        man.setHand(PLAYER_TWO_ID, playerTwoHand);

        // Player 1 plays the two of diamonds
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        int points = man.playCardByIndex(PLAYER_ONE_ID, 0);
        int currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 2);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 0);

        // Player 2 plays the four of hearts
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 0);
        currentCount = man.getCount();
        assertEquals(points, 0);
        assertEquals(currentCount, 6);
        assertEquals(man.countPegRuns(), 0);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 0);

        // Player 1 plays the three of clubs
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 3);
        assertEquals(currentCount, 9);
        assertEquals(man.countPegRuns(), 3);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 3);

        // Player 2 plays the five of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 4);
        assertEquals(currentCount, 14);
        assertEquals(man.countPegRuns(), 4);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 4);

        // Player 1 plays the six of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 2);
        currentCount = man.getCount();
        assertEquals(points, 5);
        assertEquals(currentCount, 20);
        assertEquals(man.countPegRuns(), 5);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 8);

        // Player 2 plays the ace of spades
        assertTrue(man.hasPlayableCard(PLAYER_TWO_ID));
        points = man.playCardByIndex(PLAYER_TWO_ID, 1);
        currentCount = man.getCount();
        assertEquals(points, 6);
        assertEquals(currentCount, 21);
        assertEquals(man.countPegRuns(), 6);
        assertEquals(man.getGameScores()[PLAYER_TWO_ID], 10);

        // Player 1 plays the seven of hearts
        assertTrue(man.hasPlayableCard(PLAYER_ONE_ID));
        points = man.playCardByIndex(PLAYER_ONE_ID, 3);
        currentCount = man.getCount();
        assertEquals(points, 7);
        assertEquals(currentCount, 28);
        assertEquals(man.countPegRuns(), 7);
        assertEquals(man.getGameScores()[PLAYER_ONE_ID], 15);
    }

    @Test
    public void testWinnerFromPegging() {
        man = new CribbageManagerTest(2);
        setupHands(PLAYER_TWO_ID, 2);

        List<Card> playerOneHand = new ArrayList<Card>();
        playerOneHand.add(new Card(Suit.DIAMOND, Rank.TWO));
        man.setHand(PLAYER_ONE_ID, playerOneHand);

        man.setGameScore(PLAYER_ONE_ID, 120);
        man.setCount(13);

        assertFalse(man.isWinner(PLAYER_ONE_ID));
        man.playCardByIndex(PLAYER_ONE_ID, 0);
        assertTrue(man.isWinner(PLAYER_ONE_ID));
    }

    // Assumes that all tests in TestGameSetup are passing
    private List<List<Card>> setupHands(int dealerId, int numPlayers) {
        man.setDealer(dealerId);
        List<List<Card>> hands = man.dealHands();
        List<Card> crib = man.getCrib();
        while (crib.size() < 4) {
            for (int i = 0; i < numPlayers; i++) {
                man.sendCardToCrib(i, hands.get(i).get(0));
            }
        }

        return hands;
    }
}