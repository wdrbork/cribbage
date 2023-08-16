package test.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.ai.CribbageAI;
import logic.game.*;
import logic.deck.*;

import java.util.ArrayList;
import java.util.List;

public class TestCribbageAI {
    private static final int PID = 1;
    private static final int NUM_PLAYERS = 2;

    @Test
    public void analyzeRecommendedHands() {
        CribbageManager state = new CribbageManager(NUM_PLAYERS);
        CribbageAI ai = new CribbageAI(state, PID);

        List<Card> hand = new ArrayList<Card>();
        hand.add(new Card(Suit.SPADE, Rank.TWO));
        hand.add(new Card(Suit.DIAMOND, Rank.TWO));
        hand.add(new Card(Suit.DIAMOND, Rank.FIVE));
        hand.add(new Card(Suit.SPADE, Rank.SIX));
        hand.add(new Card(Suit.SPADE, Rank.SEVEN));
        hand.add(new Card(Suit.SPADE, Rank.KING));

        ai.setHand(hand);
        state.setDealer(PID);
        List<Card> optimal = ai.getOptimalHand();
        System.out.println(optimal);
    }

    @Test 
    public void testTwoAI() {
        CribbageManager state = new CribbageManager(NUM_PLAYERS);
        CribbageAI playerOne = new CribbageAI(state, 0);
        CribbageAI playerTwo = new CribbageAI(state, 1);
        state.setDealer(1);
        state.dealHands();
        playerOne.setHand(state.getHand(0));
        playerTwo.setHand(state.getHand(1));
        List<Card> playerOneHand = playerOne.getOptimalHand();
        for (Card card : state.getHand(0)) {
            if (!playerOneHand.contains(card)) {
                state.sendCardToCrib(0, card);
            }
        }

        List<Card> playerTwoHand = playerTwo.getOptimalHand();
        for (Card card : state.getHand(1)) {
            if (!playerTwoHand.contains(card)) {
                state.sendCardToCrib(1, card);
            }
        }

        System.out.println("playerOne hand: " + state.getHand(0));
        System.out.println("playerTwo hand: " + state.getHand(1));

        while (state.movePossible()) {
            Card optimalCard;
            if (state.nextPlayer() == 0) {
                optimalCard = playerOne.getOptimalCard();
                System.out.println("playerOne optimal card: " + optimalCard);
            } else {
                optimalCard = playerTwo.getOptimalCard();
                System.out.println("playerTwo optimal card: " + optimalCard);
            }
            System.out.println("Points earned: " + state.playCard(state.nextPlayer(), optimalCard));
        }
    }
}