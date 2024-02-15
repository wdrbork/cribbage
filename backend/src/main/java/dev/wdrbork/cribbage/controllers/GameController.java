package dev.wdrbork.cribbage.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.wdrbork.cribbage.logic.cards.Card;
import dev.wdrbork.cribbage.logic.cards.Hand;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("api/v1/game")
public class GameController {
    @Autowired
    private CribbageManager game;

    /**************************************************************************
    * GAME DATA
    **************************************************************************/
    @GetMapping("/count")
    public ResponseEntity<String> count() {
        return new ResponseEntity<>(
            String.valueOf(game.count()), 
            HttpStatus.OK
        );
    }

    @GetMapping("/hands")
    public ResponseEntity<List<Hand>> getAllHands() {
        return new ResponseEntity<>(game.getAllHands(), HttpStatus.OK);
    }

    @GetMapping("/played_cards")
    public ResponseEntity<List<Hand>> getPlayedCards() {
        return new ResponseEntity<>(game.getPlayedCards(), HttpStatus.OK);
    }

    @GetMapping("/num_players")
    public ResponseEntity<String> numPlayers() {
        return new ResponseEntity<>(
            String.valueOf(game.numPlayers()), 
            HttpStatus.OK
        );
    }

    @GetMapping("/last")
    public ResponseEntity<String> lastToPlayCard() {
        return new ResponseEntity<>(
            String.valueOf(game.lastToPlayCard()), 
            HttpStatus.OK
        );
    }

    @GetMapping("/next")
    public ResponseEntity<String> nextToPlayCard() {
        return new ResponseEntity<>(
            String.valueOf(game.nextToPlayCard()), 
            HttpStatus.OK
        );
    }

    @GetMapping("/dealer")
    public ResponseEntity<String> dealer() {
        return new ResponseEntity<>(
            String.valueOf(game.dealer()), 
            HttpStatus.OK
        );
    }

    @GetMapping("/scores")
    public ResponseEntity<int[]> gameScores() {
        return new ResponseEntity<>(
            game.gameScores(), 
            HttpStatus.OK
        );
    }

    @GetMapping("/score/{pid}")
    public ResponseEntity<String> playerScore(@PathVariable String pid) {
        try {
            return new ResponseEntity<>(
                String.valueOf(game.getPlayerScore(Integer.valueOf(pid))), 
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/hand/{pid}")
    public ResponseEntity<Hand> playerHand(@PathVariable String pid) {
        try {
            return new ResponseEntity<>(
                game.getHand(Integer.valueOf(pid)), 
                HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                new Hand(false), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/crib")
    public ResponseEntity<Hand> getCrib() {
        return new ResponseEntity<>(game.getCrib(), HttpStatus.OK);
    }

    @GetMapping("/last_card")
    public ResponseEntity<Card> lastPlayedCard() {
        return new ResponseEntity<>(game.getLastPlayedCard(), HttpStatus.OK);
    }

    @GetMapping("/dealer_card")
    public ResponseEntity<Card> pickCardForDealer() {
        return new ResponseEntity<>(game.pickCardForDealer(), HttpStatus.OK);
    }

    /**************************************************************************
    * GAME FLOW
    **************************************************************************/
    @PostMapping("/dealer/{pid}")
    public ResponseEntity<String> setDealer(@PathVariable String pid) {
        try {
            game.setDealer(Integer.valueOf(pid));
            return dealer();
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @PostMapping("/deal")
    public ResponseEntity<List<Hand>> dealHands() {
        try {
            return new ResponseEntity<>(game.dealHands(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(List.of(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/move/{pid}/{suit_id}/{rank_id}")
    public ResponseEntity<String> cribCard(@PathVariable String pid, 
                                           @PathVariable String suit_id, 
                                           @PathVariable String rank_id) {
        Card card;
        try {
            card = new Card(
                Integer.valueOf(suit_id), 
                Integer.valueOf(rank_id)
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

    @GetMapping("/starter")
    public ResponseEntity<Object> pickStarterCard() {
        try {
            return new ResponseEntity<>(game.pickStarterCard(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Card.class, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/play/{pid}/{suit_id}/{rank_id}")
    public ResponseEntity<String> playCard(@PathVariable String pid, 
                                           @PathVariable String suit_id,
                                           @PathVariable String rank_id) {
        Card card;
        try {
            card = new Card(
                Integer.valueOf(suit_id), 
                Integer.valueOf(rank_id)
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }

        try {
            int points = game.playCard(Integer.valueOf(pid), card);
            return new ResponseEntity<>(
                "Player " + pid + " played " + card + " for " + points + 
                    " points", 
                HttpStatus.OK);
        } catch (NullPointerException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                "Player " + pid + " cannot play " + card + ". Try again.", 
                HttpStatus.OK
            );
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/move_possible")
    public ResponseEntity<Boolean> movePossible() {
        return new ResponseEntity<>(game.movePossible(), HttpStatus.OK);
    }

    @PostMapping("/reset_count")
    public ResponseEntity<String> resetCount() {
        game.resetCount();
        return new ResponseEntity<>(
            "Count has been reset to 0", 
            HttpStatus.OK
        );
    }

    @GetMapping("/round_over")
    public ResponseEntity<Boolean> roundOver() {
        return new ResponseEntity<>(game.roundOver(), HttpStatus.OK);
    }

    @GetMapping("/hand/score/{pid}")
    public ResponseEntity<String> countHand(@PathVariable String pid) {
        try {
            int score = game.countHand(Integer.valueOf(pid));
            return new ResponseEntity<>(String.valueOf(score), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/crib/score")
    public ResponseEntity<String> countCrib() {
        try {
            int score = game.countCrib();
            return new ResponseEntity<>(String.valueOf(score), HttpStatus.OK);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(
                e.getMessage(), 
                HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("/winner/{pid}")
    public ResponseEntity<Boolean> winner(@PathVariable String pid) {
        try {
            return new ResponseEntity<>(
                game.isWinner(Integer.valueOf(pid)), 
                HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/clear_round")
    public ResponseEntity<String> clearRoundState() {
        game.clearRoundState();
        return new ResponseEntity<>("Round state cleared", HttpStatus.OK);
    }

}