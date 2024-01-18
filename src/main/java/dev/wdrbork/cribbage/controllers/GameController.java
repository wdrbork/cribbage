package dev.wdrbork.cribbage.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.wdrbork.cribbage.logic.deck.Card;
import dev.wdrbork.cribbage.logic.game.CribbageManager;

@RestController
@RequestMapping("api/v1/game")
public class GameController {
    @Autowired
    private CribbageManager game;

    @GetMapping("/count")
    public ResponseEntity<String> count() {
        return new ResponseEntity<>(String.valueOf(game.count()), HttpStatus.OK);
    }

    @GetMapping("/dealer_card")
    public ResponseEntity<Card> pickCardForDealer() {
        return new ResponseEntity<>(game.pickCardForDealer(), HttpStatus.OK);
    }

    @GetMapping("/hands")
    public ResponseEntity<List<List<Card>>> test() {
        return new ResponseEntity<>(game.getAllHands(), HttpStatus.OK);
    }

}