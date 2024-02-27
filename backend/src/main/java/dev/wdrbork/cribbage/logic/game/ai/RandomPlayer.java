package dev.wdrbork.cribbage.logic.game.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.wdrbork.cribbage.logic.cards.*;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

public class RandomPlayer implements CribbageAI {
    private static final int HAND_SIZE = 4;

    private CribbageManager gameState;
    private Random rng;
    private int pid;
    private Deck hand;

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

    public void setHand(Deck hand) {
        this.hand = hand;
    }

    public Deck choosePlayingHand() {
        if (this.hand == null) {
            throw new IllegalStateException("No hand set for AI with pid " + pid);
        }

        while (hand.size() > HAND_SIZE) {
            hand.pickCard(rng.nextInt(hand.size()));
        }
        return hand;
    }

    public Card chooseCard() {
        Deck playedCards = gameState.getPlayedCards().get(pid);
        List<Card> availableCards = new ArrayList<Card>();
        for (Card card : hand.getCards()) {
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