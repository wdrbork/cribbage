package test.logic.game;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import logic.game.CribbageManager;
import logic.deck.*;
import java.util.List;


public class TestSecondStage {
    private static final int NUM_PLAYERS = 2;
    private final CribbageManager man = new CribbageManager(NUM_PLAYERS);

    

    private List<List<Card>> setupTest(int dealerId) {
        man.setDealer(dealerId);
        List<List<Card>> hands = man.dealHands();
        List<Card> crib = man.getCrib();
        while (crib.size() < 4) {
            for (int i = 0; i < NUM_PLAYERS; i++) {
                man.sendCardToCrib(i, hands.get(i).get(0));
            }
        }

        return hands;
    }


}