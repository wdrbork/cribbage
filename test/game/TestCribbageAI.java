package test.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.ai.CribbageAI;
import logic.game.ai.RandomPlayer;
import logic.game.ai.SmartPlayer;
import logic.game.*;
import logic.deck.*;

import java.util.ArrayList;
import java.util.List;

public class TestCribbageAI {
    private static final int PID = 1;
    private static final int NUM_PLAYERS = 2;
    private static final int SMART_ID = 0;
    private static final int RANDOM_ID = 1;
    private static final int MAX_COUNT = 31;
    private static final int TEST_GAMES = 100;
    private static final double WIN_THRESHOLD = .8;

    // @Test
    public void analyzeRecommendedHands() {
        CribbageManager state = new CribbageManager(NUM_PLAYERS);
        CribbageAI ai = new SmartPlayer(state, PID);

        List<Card> hand = new ArrayList<Card>();
        hand.add(new Card(Suit.SPADE, Rank.TWO));
        hand.add(new Card(Suit.DIAMOND, Rank.TWO));
        hand.add(new Card(Suit.DIAMOND, Rank.FIVE));
        hand.add(new Card(Suit.SPADE, Rank.SIX));
        hand.add(new Card(Suit.SPADE, Rank.SEVEN));
        hand.add(new Card(Suit.SPADE, Rank.KING));

        ai.setHand(hand);
        state.setDealer(PID);
        List<Card> optimal = ai.choosePlayingHand();
        System.out.println(optimal);
    }

    // @Test 
    public void testSmartVersusSmart() {
        CribbageManager state = new CribbageManager(NUM_PLAYERS);
        CribbageAI playerOne = new SmartPlayer(state, 0);
        CribbageAI playerTwo = new SmartPlayer(state, 1);
        state.setDealer(1);
        state.dealHands();
        playerOne.setHand(state.getHand(0));
        playerTwo.setHand(state.getHand(1));
        List<Card> playerOneHand = playerOne.choosePlayingHand();
        for (Card card : state.getHand(0)) {
            if (!playerOneHand.contains(card)) {
                state.sendCardToCrib(0, card);
            }
        }

        List<Card> playerTwoHand = playerTwo.choosePlayingHand();
        for (Card card : state.getHand(1)) {
            if (!playerTwoHand.contains(card)) {
                state.sendCardToCrib(1, card);
            }
        }

        System.out.println("playerOne hand: " + state.getHand(0));
        System.out.println("playerTwo hand: " + state.getHand(1));

        while (!state.roundOver()) {
            Card optimalCard;
            if (state.nextPlayer() == 0) {
                optimalCard = playerOne.chooseCard();
                System.out.println("playerOne optimal card: " + optimalCard);
            } else {
                optimalCard = playerTwo.chooseCard();
                System.out.println("playerTwo optimal card: " + optimalCard);
            }
            System.out.println("Points earned: " + state.playCard(state.nextPlayer(), optimalCard));
            System.out.println("Count: " + state.count());
            if (!state.movePossible()) {
                if (state.count() != MAX_COUNT) {
                    state.awardPointsForGo();
                }
                state.resetCount();
            }
        }
    }

    @Test
    public void testSmartVersusRandom() {
        int smartWins = 0;
        int randomWins = 0;
        int gamesPlayed = 0;

        while (gamesPlayed < TEST_GAMES) {
            int winner = simulateTwoPlayerGame();
            if (winner == SMART_ID) {
                smartWins++;
            } else {
                randomWins++;
            }
            System.out.println("Game " + gamesPlayed + " winner PID: " + winner + "\n");
            gamesPlayed++;
        }
        
        double smartWinPct = (double) smartWins / gamesPlayed;
        double randomWinPct = (double) randomWins / gamesPlayed;
        System.out.println("Smart AI win %: " + smartWinPct * 100);
        System.out.println("Random AI win %: " + randomWinPct * 100);
        if (smartWinPct < WIN_THRESHOLD) {
            fail("Smart AI did not win enough games");
        } else {
            System.out.println("Test successful");
        }
    }

    private int simulateTwoPlayerGame() {
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

            List<Card> smartHand = smart.choosePlayingHand();
            for (Card card : game.getHand(SMART_ID)) {
                if (!smartHand.contains(card)) {
                    game.sendCardToCrib(SMART_ID, card);
                }
            }

            List<Card> randomHand = random.choosePlayingHand();
            for (Card card : game.getHand(RANDOM_ID)) {
                if (!randomHand.contains(card)) {
                    game.sendCardToCrib(RANDOM_ID, card);
                }
            }

            game.getStarterCard();
            //System.out.println("Begin playing round");
            playTwoPlayerRound(game, smart, random);
            if (game.dealer() == SMART_ID) {
                game.countHand(RANDOM_ID);
                game.countHand(SMART_ID);
            } else {
                game.countHand(SMART_ID);
                game.countHand(RANDOM_ID);
            }

            //System.out.println("Count crib");
            game.countCrib();
            
            // System.out.println("Clear round state");
            game.clearRoundState();
            rounds++;
        }

        return game.isWinner(SMART_ID) ? SMART_ID : RANDOM_ID;
    }

    private void playTwoPlayerRound(CribbageManager game, CribbageAI smart, 
            CribbageAI random) {
        while (!game.roundOver()) {
            Card playedCard;
            if (game.nextPlayer() == SMART_ID) {
                playedCard = smart.chooseCard();
            } else {
                playedCard = random.chooseCard();
            }
            game.playCard(game.nextPlayer(), playedCard);

            if (!game.movePossible()) {
                if (game.count() != MAX_COUNT) {
                    game.awardPointsForGo();
                }
                game.resetCount();
            }
        }
    }
}