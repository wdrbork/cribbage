package dev.wdrbork.cribbage.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

public class TestGameSetup {
    private static final int PLAYER_ONE_ID = 0;
    private static final int PLAYER_THREE_ID = 2;
    private static final int SETUP_TRIALS = 100;

    CribbageManager man;
    
    @Test
    public void testTwoPlayerSetup() {
        man = new CribbageManager(2);
        assertThrows(IllegalStateException.class, () -> {
            man.dealHands();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            man.setDealer(PLAYER_THREE_ID);
        });

        man.setDealer(PLAYER_ONE_ID);
        int dealerID = 0;

        for (int i = 0; i < SETUP_TRIALS; i++) {
            setupGame(2);
            man.clearRoundState();
            dealerID = (dealerID + 1) % 2;
            man.setDealer(dealerID);
        }
    }

    @Test
    public void testThreePlayerSetup() {
        man = new CribbageManager(3);
        assertThrows(IllegalStateException.class, () -> {
            man.dealHands();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            man.setDealer(-1);
        });

        man.setDealer(PLAYER_ONE_ID);
        int dealerID = 0;

        for (int i = 0; i < SETUP_TRIALS; i++) {
            setupGame(3);
            man.clearRoundState();
            dealerID = (dealerID + 1) % 3;
            man.setDealer(dealerID);
        }
    }

    @Test
    public void testHeelsAwarded() {
        man = new CribbageManager(2);
        man.setDealer(PLAYER_ONE_ID);
        setupGame(2);

        while(man.pickStarterCard().getRank() != Rank.JACK) {
        }

        assertEquals(man.getPlayerScore(PLAYER_ONE_ID), 2);
    }

    private void setupGame(int numPlayers) {
        List<List<Card>> hands = man.dealHands();
        assertThrows(UnsupportedOperationException.class, () -> {
            hands.add(new ArrayList<Card>());
        });

        Set<Card> dealtCards = new HashSet<Card>();
        for (List<Card> hand : hands) {
            if (numPlayers == 2) {
                assertEquals(hand.size(), 6);
            } else {
                assertEquals(hand.size(), 5);
            }
            
            System.out.println(hand);
            for (Card card : hand) {
                assertFalse(dealtCards.contains(card));
                dealtCards.add(card);
            }
        }

        List<Card> crib = man.getCrib();
        if (numPlayers == 2) {
            assertEquals(crib.size(), 0);
            man.sendCardToCrib(0, hands.get(0).get(0));
            man.sendCardToCrib(0, hands.get(0).get(0));
            man.sendCardToCrib(1, hands.get(1).get(0));
            man.sendCardToCrib(1, hands.get(1).get(0));
        } else {
            assertEquals(crib.size(), 1);
            man.sendCardToCrib(0, hands.get(0).get(0));
            man.sendCardToCrib(1, hands.get(1).get(0));
            man.sendCardToCrib(2, hands.get(2).get(0));
        }

        crib = man.getCrib();
        System.out.println("Crib = " + crib);
        assertEquals(crib.size(), 4);
        for (List<Card> hand : hands) {
            assertEquals(hand.size(), 4);
        }
    }
}