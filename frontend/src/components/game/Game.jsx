import "./game.css";
import api from "../../api/axiosConfig.js";
import { useState, useEffect, useRef } from "react";

import { USER_ID, OPP_ID, DECK_SIZE } from "../../global/vars.js";

import {
  DRAW_DEALER,
  DEAL_CRIB,
  PLAY_ROUND,
  COUNT_HANDS,
  COUNT_CRIB,
} from "../../global/stages.js";

import Scoreboard from "../scoreboard";
import Message from "../message";
import Card from "../card";

import DrawDealer from "../stages/drawDealer";
import DealCrib from "../stages/dealCrib";
import PlayRound from "../stages/playRound";

const CARDS_PER_SUIT = 13;
const WINNING_SCORE = 121;

function Game({ numPlayers }) {
  const [currentStage, setCurrentStage] = useState(DRAW_DEALER);
  const [message, setMessage] = useState("");
  const [gameScores, setGameScores] = useState(Array(numPlayers));
  const [dealer, setDealer] = useState(-1);
  const [hands, setHands] = useState([]);
  const [crib, setCrib] = useState([]);
  const [starterCard, setStarterCard] = useState(null);
  const [winner, setWinner] = useState(-1);
  const cardsInPlay = useRef(0);

  // API CALLS
  const getScores = async () => {
    try {
      const promise = await api.get("game/scores");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const dealCards = async () => {
    try {
      const promise = await api.post("game/deal");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const resetGame = async () => {
    await api.post("game/reset_game");
  };

  // EFFECTS

  // For when the game stage changes
  useEffect(() => {
    getScores().then((response) => {
      setGameScores(response.data);
    });

    if (currentStage === DRAW_DEALER) {
      resetGame();
      setMessage(
        "Pick a card to decide who is the dealer. The player with the lowest card deals first."
      );
    } else if (currentStage === DEAL_CRIB) {
      dealCards().then((response) => {
        const hands = response.data;
        setHands(hands);
        cardsInPlay.current =
          hands[USER_ID].cards.length + hands[OPP_ID].cards.length;
      });

      if (dealer === USER_ID) {
        setMessage("Select two cards that will be sent to your crib.");
      } else {
        setMessage(
          "Select two cards that will be sent to your opponent's crib."
        );
      }
    } else if (currentStage === PLAY_ROUND) {
      if (dealer === USER_ID) {
        setMessage("It is your opponent's turn to select a card.");
      } else if (dealer === OPP_ID) {
        setMessage("It is your turn. Please select a card.");
      }
    }
  }, [currentStage]);

  // FUNCTIONALITY
  function displayDeck() {
    let deckCards = [];
    let i = 0;
    for (; i < DECK_SIZE - cardsInPlay.current; i++) {
      deckCards.push(<Card key={i} id={i} hidden />);
    }

    if (starterCard) {
      deckCards.push(
        <Card key={i} id={starterCard.cardId} cardInfo={starterCard} />
      );
    }

    return deckCards;
  }

  function stageSwitch() {
    switch (currentStage) {
      case DRAW_DEALER:
        return (
          <DrawDealer
            setMessage={setMessage}
            setDealer={setDealer}
            setStage={setCurrentStage}
            cardsInPlay={cardsInPlay}
          />
        );
      case DEAL_CRIB:
        return (
          <DealCrib
            dealer={dealer}
            hands={hands}
            crib={crib}
            starterCard={starterCard}
            setHands={setHands}
            setCrib={setCrib}
            setStarterCard={setStarterCard}
            setGameScores={setGameScores}
            setMessage={setMessage}
            setStage={setCurrentStage}
            displayDeck={displayDeck}
            cardsInPlay={cardsInPlay}
          />
        );
      case PLAY_ROUND:
        return (
          <PlayRound
            dealer={dealer}
            hands={hands}
            crib={crib}
            setHands={setHands}
            setGameScores={setGameScores}
            setMessage={setMessage}
            setStage={setCurrentStage}
            displayDeck={displayDeck}
          />
        );
      case COUNT_HANDS:
        return COUNT_HANDS;
      case COUNT_CRIB:
        return COUNT_CRIB;
      default:
        return null;
    }
  }

  return (
    <div className="Game">
      <div className="main-screen">{stageSwitch()}</div>
      <div className="right-bar">
        <Scoreboard gameScores={gameScores} dealer={dealer} />
        <Message message={message} />
      </div>
    </div>
  );
}

export default Game;
