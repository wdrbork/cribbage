package dev.wdrbork.cribbage.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.wdrbork.cribbage.logic.cards.Card;
import dev.wdrbork.cribbage.logic.game.CribbageManager;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@CrossOrigin(origins = GameController.FRONTEND_URL)
@Controller
@RequestMapping("api/v1/game")
public class GameController {
    public static final String FRONTEND_URL = "http://localhost:3000";

    @Autowired
    private CribbageManager game;

    /**************************************************************************
    * GAME DATA
    **************************************************************************/
    @PostMapping("/getCount")
    public ResponseEntity<Object> getCount() {
        return new ResponseEntity<>(
            String.valueOf(game.count()), 
            HttpStatus.OK
        );
    }

    @PostMapping("/getAllHands")
    public ResponseEntity<Object> getAllHands() {
        return new ResponseEntity<>(game.getAllHands(), HttpStatus.OK);
    }

    @PostMapping("/getPlayedCards")
    public ResponseEntity<Object> getPlayedCards() {
        return new ResponseEntity<>(game.getPlayedCards(), HttpStatus.OK);
    }

    @PostMapping("/getNumberOfPlayers")
    public ResponseEntity<Object> numPlayers() {
        return new ResponseEntity<>(
            String.valueOf(game.numPlayers()), 
            HttpStatus.OK
        );
    }

    @PostMapping("/getPreviousPlayer")
    public ResponseEntity<Object> lastToPlayCard() {
        return new ResponseEntity<>(
            String.valueOf(game.lastToPlayCard()), 
            HttpStatus.OK
        );
    }

    @PostMapping("/getNextPlayer")
    public ResponseEntity<Object> nextToPlayCard() {
        return new ResponseEntity<>(
            game.nextToPlayCard(), 
            HttpStatus.OK
        );
    }

    @PostMapping("/getDealer")
    public ResponseEntity<Object> dealer() {
        return new ResponseEntity<>(
            String.valueOf(game.dealer()), 
            HttpStatus.OK
        );
    }

    @PostMapping("/getScores")
    public ResponseEntity<Object> gameScores() {
        return new ResponseEntity<>(
            game.gameScores(), 
            HttpStatus.OK
        );
    }

    @PostMapping("/getPlayerScore")
    public ResponseEntity<Object> playerScore(@RequestBody Map<String, Integer> json) {
        try {
            int pid = json.get("pid");
            return new ResponseEntity<>(
                String.valueOf(game.getPlayerScore(pid)), 
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/getPlayerHand")
    public ResponseEntity<Object> playerHand(@RequestBody Map<String, Integer> json) {
        try {
            int pid = json.get("pid");
            return new ResponseEntity<>(
                game.getHand(pid), 
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/getCrib")
    public ResponseEntity<Object> getCrib() {
        return new ResponseEntity<>(game.getCrib(), HttpStatus.OK);
    }

    @PostMapping("/getLastCard")
    public ResponseEntity<Object> lastPlayedCard() {
        return new ResponseEntity<>(game.getLastPlayedCard(), HttpStatus.OK);
    }

    /**************************************************************************
    * GAME FLOW
    **************************************************************************/
    @PostMapping("/pickDealerCard")
    public ResponseEntity<Object> pickCardForDealer() {
        return new ResponseEntity<>(game.pickCardForDealer(), HttpStatus.OK);
    }

    @PostMapping("/setDealer")
    public ResponseEntity<Object> setDealer(@RequestBody Map<String, Integer> json) {
        try {
            int pid = json.get("pid");
            game.setDealer(pid);
            return dealer();
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/dealHands")
    public ResponseEntity<Object> dealHands() {
        try {
            return new ResponseEntity<>(game.dealHands(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/selectAIHands")
    public ResponseEntity<Object> selectAIHands() {
        List<Card> cribCards = game.chooseAIPlayingHands();
        return new ResponseEntity<>(cribCards, HttpStatus.OK);
    }

    @PostMapping("/moveCardToCrib")
    public ResponseEntity<Object> cribCard(@RequestBody Map<String, Integer> json) {
        Card card;
        int pid = json.get("pid");
        int suitId = json.get("suitId");
        int rankId = json.get("rankId");
        try {
            card = new Card(
                Integer.valueOf(suitId), 
                Integer.valueOf(rankId)
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }

        try {
            game.sendCardToCrib(Integer.valueOf(pid), card);
            return new ResponseEntity<>(
                "Moved " + card + " to crib from player " + pid, 
                HttpStatus.OK
            );
        } catch (NullPointerException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/pickStarterCard")
    public ResponseEntity<Object> pickStarterCard() {
        try {
            return new ResponseEntity<>(game.pickStarterCard(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/playCard")
    public ResponseEntity<Object> playCard(@RequestBody Map<String, Integer> json) {
        Card card;
        int pid = json.get("pid");
        int suitId = json.get("suitId");
        int rankId = json.get("rankId");
        try {
            card = new Card(
                Integer.valueOf(suitId), 
                Integer.valueOf(rankId)
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }

        try {
            int[] points = game.playCard(Integer.valueOf(pid), card);
            return new ResponseEntity<>(new PlayResult(card, points), HttpStatus.OK);
        } catch (NullPointerException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.OK
            );
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/playAICard")
    public ResponseEntity<Object> playAICard(@RequestBody Map<String, Integer> json) {
        try {
            int pid = json.get("pid");
            Card card = game.chooseAICard(pid);
            int[] points = game.playCard(pid, card);
            return new ResponseEntity<>(new PlayResult(card, points), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/isMovePossible")
    public ResponseEntity<Object> movePossible() {
        return new ResponseEntity<>(game.movePossible(), HttpStatus.OK);
    }

    @PostMapping("/resetCount")
    public ResponseEntity<Object> resetCount() {
        game.resetCount();
        return new ResponseEntity<>(
            "Count has been reset to 0", 
            HttpStatus.OK
        );
    }

    @PostMapping("/isRoundOver")
    public ResponseEntity<Object> roundOver() {
        return new ResponseEntity<>(game.roundOver(), HttpStatus.OK);
    }

    @PostMapping("/countHand")
    public ResponseEntity<Object> countHand(@RequestBody Map<String, Integer> json) {
        try {
            int pid = json.get("pid");
            int[] scores = game.countHand(Integer.valueOf(pid));
            return new ResponseEntity<>(scores, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/countCrib")
    public ResponseEntity<Object> countCrib() {
        try {
            int[] scores = game.countCrib();
            return new ResponseEntity<>(scores, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/isWinner")
    public ResponseEntity<Object> winner(@RequestBody Map<String, Integer> json) {
        try {
            int pid = json.get("pid");
            return new ResponseEntity<>(
                game.isWinner(Integer.valueOf(pid)), 
                HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/clearRound")
    public ResponseEntity<Object> clearRoundState() {
        game.clearRoundState();
        return new ResponseEntity<>("Round state cleared", HttpStatus.OK);
    }

    @PostMapping("/resetDeck")
    public ResponseEntity<Object> resetDeck() {
        game.resetDeck();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/resetGame")
    public ResponseEntity<Object> resetGame() {
        game = new CribbageManager();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**************************************************************************
    * UTILITY CLASSES
    **************************************************************************/
    @AllArgsConstructor
    @EqualsAndHashCode
    private class PlayResult {
        @Getter
        private Card playedCard;

        @Getter
        private int[] pointsEarned;
    }
}