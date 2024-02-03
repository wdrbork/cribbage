package dev.wdrbork.cribbage.logic.game.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

public class RandomPlayer implements CribbageAI {
    private static final int TWO_PLAYER_START_SIZE = 6;
    private static final int THREE_PLAYER_START_SIZE = 5;
    private static final int HAND_SIZE = 4;

    private CribbageManager gameState;
    private Random rng;
    private int pid;
    private Hand hand;

    public RandomPlayer(CribbageManager gameState, int pid) {
        int numPlayers = gameState.numPlayers();
        if (numPlayers != 2 && numPlayers != 3) {
            throw new IllegalArgumentException("Must have either 2 or 3 players");
        } else if (pid < 0 || pid >= numPlayers) {
            throw new IllegalArgumentException("PID is invalid, must be between 0 and " + numPlayers);
        }

        this.gameState = gameState;
        rng = new Random();
        this.pid = pid;
        this.hand = gameState.getHand(pid);
    }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

    public Hand choosePlayingHand() {
        if (this.hand == null) {
            throw new IllegalStateException("No hand set for AI with pid " + pid);
        }

        while (hand.size() > HAND_SIZE) {
            hand.removeCardByIndex(rng.nextInt(hand.size()));
        }
        return hand;
    }

    public Card chooseCard() {
        Hand playedCards = gameState.getPlayedCards().get(pid);
        List<Card> availableCards = new ArrayList<Card>();
        for (Card card : hand.asList()) {
            if (!playedCards.contains(card)) {
                availableCards.add(card);
            }
        }

        Card card = availableCards.get(rng.nextInt(availableCards.size()));
        while (!gameState.canPlayCard(card)) {
            card = availableCards.get(rng.nextInt(availableCards.size()));
        }
        return card;
    }
}