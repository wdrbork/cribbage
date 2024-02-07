package dev.wdrbork.cribbage.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import dev.wdrbork.cribbage.logic.game.ai.CribbageAI;
import dev.wdrbork.cribbage.logic.game.ai.RandomPlayer;
import dev.wdrbork.cribbage.logic.game.ai.SmartPlayer;
import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

public class TestCribbageAI {
    private static final int PID = 1;
    private static final int NUM_PLAYERS = 2;
    private static final int SMART_ID = 0;
    private static final int RANDOM_ID = 1;
    private static final int TEST_GAMES = 1;
    private static final double WIN_THRESHOLD = .5;

    private static int scoreDiff = 0;

    // private class CribbageManagerTest extends CribbageManager {
    //     public CribbageManagerTest(int numPlayers) {
    //         super(numPlayers);
    //     }

    //     public void setPlayedCards(List<List<Card>> playedCards) {
    //         this.playedCardsByPlayer = playedCards;
    //     }

    //     public void setHand(int pid, List<Card> hand) { hands.set(pid, hand); }

    //     public void setCount(int count) { this.count = count; }

    //     public void setGameScores(int[] gameScores) {
    //         this.gameScores = gameScores.clone();
    //     }

    //     public void setCardStack(LinkedList<Card> cardStack) {
    //         this.cardStack = cardStack;
    //     }
    // }

    @Test
    public void analyzeRecommendedHands() {
        CribbageManager state = new CribbageManager(NUM_PLAYERS);
        CribbageAI ai = new SmartPlayer(state, PID);

        Hand hand = new Hand(false);
        hand.addCard(new Card(Suit.CLUB, Rank.ACE));
        hand.addCard(new Card(Suit.CLUB, Rank.SEVEN));
        hand.addCard(new Card(Suit.SPADE, Rank.EIGHT));
        hand.addCard(new Card(Suit.DIAMOND, Rank.TEN));
        hand.addCard(new Card(Suit.HEART, Rank.QUEEN));
        hand.addCard(new Card(Suit.DIAMOND, Rank.QUEEN));

        ai.setHand(hand);
        state.setDealer(PID);
        Hand optimal = ai.choosePlayingHand();
        System.out.println(optimal);
    }

    @Test 
    public void testSmartVersusSmart() {
        CribbageManager state = new CribbageManager(NUM_PLAYERS);
        CribbageAI playerZero = new SmartPlayer(state, 0);
        CribbageAI playerOne = new SmartPlayer(state, 1);
        state.setDealer(1);
        state.dealHands();
        playerZero.setHand(state.getHand(0));
        playerOne.setHand(state.getHand(1));
        Hand playerZeroHand = playerZero.choosePlayingHand();
        for (Card card : state.getHand(0).asList()) {
            if (!playerZeroHand.contains(card)) {
                state.sendCardToCrib(0, card);
            }
        }

        Hand playerOneHand = playerOne.choosePlayingHand();
        for (Card card : state.getHand(1).asList()) {
            if (!playerOneHand.contains(card)) {
                state.sendCardToCrib(1, card);
            }
        }

        System.out.println("playerZero hand: " + state.getHand(0));
        System.out.println("playerOne hand: " + state.getHand(1) + "\n");

        while (!state.roundOver()) {
            Card optimalCard;
            System.out.println("Player " + (state.nextToPlayCard()) + "'s turn");
            if (state.nextToPlayCard() == 0) {
                optimalCard = playerZero.chooseCard();
                System.out.println("playerZero optimal card: " + optimalCard);
            } else {
                optimalCard = playerOne.chooseCard();
                System.out.println("playerOne optimal card: " + optimalCard);
            }
            System.out.println("Points earned: " + state.playCard(state.nextToPlayCard(), optimalCard));
            System.out.println("Count: " + state.count());
            if (!state.movePossible()) {
                // if (state.count() != MAX_COUNT) {
                //     System.out.println("Award point for go");
                //     state.awardPointsForGo();
                // }
                state.resetCount();
            }
            System.out.println();
        }
    }

    @Test
    public void testSmartVersusRandom() {
        int smartWins = 0;
        int randomWins = 0;
        int gamesPlayed = 0;

        while (gamesPlayed < TEST_GAMES) {
            int winner = simulateSmartVsRandom();
            if (winner == SMART_ID) {
                smartWins++;
            } else {
                randomWins++;
            }
            System.out.println("Game " + (gamesPlayed + 1) + " winner PID: " + winner + "\n");
            gamesPlayed++;
        }
        
        double smartWinPct = (double) smartWins / gamesPlayed;
        double randomWinPct = (double) randomWins / gamesPlayed;
        System.out.println("Smart AI win %: " + smartWinPct * 100);
        System.out.println("Random AI win %: " + randomWinPct * 100);
        System.out.println("Average score difference: " + 
                (double) scoreDiff / TEST_GAMES);
        if (smartWinPct >= WIN_THRESHOLD) {
            System.out.println("Test successful");
        } else {
            fail("Smart AI did not win enough games");
        }
    }

    private int simulateSmartVsRandom() {
        CribbageManager game = new CribbageManager(NUM_PLAYERS);
        CribbageAI smart = new SmartPlayer(game, SMART_ID);
        CribbageAI random = new RandomPlayer(game, RANDOM_ID);
        Card smartDraw = game.pickCardForDealer();
        Card randomDraw = game.pickCardForDealer();
        if (smartDraw.compareTo(randomDraw) > 0) {
            game.setDealer(SMART_ID);
        } else {
            game.setDealer(RANDOM_ID);
        }

        int rounds = 0;
        while (!game.gameOver()) {
            System.out.println("Round " + rounds + ", smart AI score = " + game.getPlayerScore(SMART_ID) + ", random AI score = " + game.getPlayerScore(RANDOM_ID));
            game.dealHands();
            smart.setHand(game.getHand(SMART_ID));
            random.setHand(game.getHand(RANDOM_ID));

            Hand smartHand = smart.choosePlayingHand();
            for (Card card : game.getHand(SMART_ID).asList()) {
                if (!smartHand.contains(card)) {
                    game.sendCardToCrib(SMART_ID, card);
                }
            }

            Hand randomHand = random.choosePlayingHand();
            for (Card card : game.getHand(RANDOM_ID).asList()) {
                if (!randomHand.contains(card)) {
                    game.sendCardToCrib(RANDOM_ID, card);
                }
            }

            System.out.println("Smart hand = " + smartHand);
            System.out.println("Random hand = " + randomHand);
            game.pickStarterCard();
            playTwoPlayerRound(game, smart, random);
            // if (game.dealer() == SMART_ID) {
            //     game.countHand(RANDOM_ID);
            //     game.countHand(SMART_ID);
            // } else {
            //     game.countHand(SMART_ID);
            //     game.countHand(RANDOM_ID);
            // }
            // game.countCrib();
            game.clearRoundState();
            rounds++;
        }

        scoreDiff += game.getPlayerScore(SMART_ID) - game.getPlayerScore(RANDOM_ID);
        return game.isWinner(SMART_ID) ? SMART_ID : RANDOM_ID;
    }

    private void playTwoPlayerRound(CribbageManager game, CribbageAI smart, 
            CribbageAI random) {
        while (!game.roundOver()) {
            Card playedCard;
            if (game.nextToPlayCard() == SMART_ID) {
                playedCard = smart.chooseCard();
            } else {
                playedCard = random.chooseCard();
            }
            // System.out.println(game.nextToPlayCard() + " plays " + playedCard);
            game.playCard(game.nextToPlayCard(), playedCard);

            if (!game.movePossible()) {
                // if (game.count() != MAX_COUNT) {
                //     game.awardPointsForGo();
                // }
                game.resetCount();
            }
        }
    }
}