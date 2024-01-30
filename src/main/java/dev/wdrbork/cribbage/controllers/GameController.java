package dev.wdrbork.cribbage.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.wdrbork.cribbage.logic.deck.Card;
import dev.wdrbork.cribbage.logic.deck.Rank;
import dev.wdrbork.cribbage.logic.deck.Suit;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

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
        return new ResponseEntity<>(String.valueOf(game.count()), HttpStatus.OK);
    }

    @GetMapping("/hands")
    public ResponseEntity<List<List<Card>>> getAllHands() {
        return new ResponseEntity<>(game.getAllHands(), HttpStatus.OK);
    }

    @GetMapping("/played_cards")
    public ResponseEntity<List<List<Card>>> getPlayedCards() {
        return new ResponseEntity<>(game.getPlayedCards(), HttpStatus.OK);
    }

    @GetMapping("/num_players")
    public ResponseEntity<String> numPlayers() {
        return new ResponseEntity<>(String.valueOf(game.numPlayers()), HttpStatus.OK);
    }

    @GetMapping("/last")
    public ResponseEntity<String> lastToPlayCard() {
        return new ResponseEntity<>(String.valueOf(game.lastToPlayCard()), HttpStatus.OK);
    }

    @GetMapping("/next")
    public ResponseEntity<String> nextToPlayCard() {
        return new ResponseEntity<>(String.valueOf(game.nextToPlayCard()), HttpStatus.OK);
    }

    @GetMapping("/dealer")
    public ResponseEntity<String> dealer() {
        return new ResponseEntity<>(String.valueOf(game.dealer()), HttpStatus.OK);
    }

    @GetMapping("/score/{pid}")
    public ResponseEntity<String> playerScore(@PathVariable String pid) {
        try {
            return new ResponseEntity<>(String.valueOf(game.getPlayerScore(Integer.valueOf(pid))), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid player ID", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/hand/{pid}")
    public ResponseEntity<List<Card>> playerHand(@PathVariable String pid) {
        try {
            return new ResponseEntity<>(game.getHand(Integer.valueOf(pid)), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(List.of(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/crib")
    public ResponseEntity<List<Card>> getCrib() {
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
            return new ResponseEntity<>("Invalid player ID", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/deal")
    public ResponseEntity<List<List<Card>>> dealHands() {
        try {
            return new ResponseEntity<>(game.dealHands(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(List.of(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/move/{pid}/{suit_id}/{rank_id}")
    public ResponseEntity<String> sendCardToCrib(@PathVariable String pid, 
                                                 @PathVariable String suit_id, 
                                                 @PathVariable String rank_id) {
        Suit suit;
        Rank rank;

        try {
            suit = Card.getSuitBasedOnValue(Integer.valueOf(suit_id));
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid suit ID", HttpStatus.BAD_REQUEST);
        }

        try {
            rank = Card.getRankBasedOnValue(Integer.valueOf(rank_id));
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid rank ID", HttpStatus.BAD_REQUEST);
        }

        Card card = new Card(suit, rank);

        try {
            game.sendCardToCrib(Integer.valueOf(pid), card);
            return new ResponseEntity<>("Moved " + card + " to crib from player " + pid, HttpStatus.OK);
        } catch (IndexOutOfBoundsException e) {
            return new ResponseEntity<>("Invalid player ID", HttpStatus.BAD_REQUEST);
        } catch (NullPointerException e) {
            return new ResponseEntity<>("Card is null", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Player " + pid + " does not have this card", HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>("Crib is full", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/starter")
    public ResponseEntity<Object> getStarterCard() {
        try {
            return new ResponseEntity<>(game.getStarterCard(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Card.class, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/play/{pid}/{suit_id}/{rank_id}")
    public ResponseEntity<String> playCard(@PathVariable String pid, 
                                           @PathVariable String suit_id,
                                           @PathVariable String rank_id) {
        Suit suit;
        Rank rank;

        try {
            suit = Card.getSuitBasedOnValue(Integer.valueOf(suit_id));
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid suit ID", HttpStatus.BAD_REQUEST);
        }

        try {
            rank = Card.getRankBasedOnValue(Integer.valueOf(rank_id));
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid rank ID", HttpStatus.BAD_REQUEST);
        }

        Card card = new Card(suit, rank);

        try {
            int points = game.playCard(Integer.valueOf(pid), card);
            return new ResponseEntity<>("Player " + pid + " played " + card + " for " + points + " points", HttpStatus.OK);
        } catch (IndexOutOfBoundsException e) {
            return new ResponseEntity<>("Invalid player ID", HttpStatus.BAD_REQUEST);
        } catch (NullPointerException e) {
            return new ResponseEntity<>("Card is null", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Player " + pid + " cannot play " + card + ". Try again.", HttpStatus.OK);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>("Player " + pid + " does not have card " + card, HttpStatus.BAD_REQUEST);
        }
    }

    

}