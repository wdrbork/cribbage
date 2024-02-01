package dev.wdrbork.cribbage.logic.game.ai;

import java.util.List;

import dev.wdrbork.cribbage.logic.cards.*;

/**
 * An interface for cribbage AI agents to implement from.
 */
public interface CribbageAI {
    /**
     * Sets the initial hand for this AI. 
     */
    public void setHand(List<Card> hand);

    /**
     * From the five or six cards dealt at the start of the round, choose four
     * cards that will be used in the second and third stages of the game. It 
     * is expected that the card(s) not selected will be sent to the crib.
     * 
     * @return the four-card hand that will be used for the rest of the round
     */
    public List<Card> choosePlayingHand();

    /**
     * Choose a card from the remaining cards in the AI's hand with the intent 
     * of playing it in the second stage.
     * 
     * @return the card that will be played
     */
    public Card chooseCard();
}