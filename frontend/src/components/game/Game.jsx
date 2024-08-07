import "./game.css";
import api from "../../api/axiosConfig.js";
import { useState, useEffect, useRef } from "react";

import { USER_ID, OPP_ID, DECK_SIZE } from "../../global/vars.js";

import {
  DRAW_DEALER,
  DEAL_HANDS,
  PLAY_ROUND,
  COUNT_HANDS,
} from "../../global/stages.js";

import Scoreboard from "../scoreboard";
import Message from "../message";
import Card from "../card";

import DrawDealer from "../stages/drawDealer";
import DealCrib from "../stages/dealHands";
import PlayRound from "../stages/playRound";
import CountHands from "../stages/countHands";

const CARDS_PER_SUIT = 13;
const WINNING_SCORE = 121;

// First index represents the dealer, second index represents the current shown score
const BUTTON_TEXT = [
  ["Opponent's score", "Your score", "Your crib", "Next round"],
  ["Your score, Opponent's score", "Opponent's crib", "Next round"],
];

function Game() {
  const [currentStage, setCurrentStage] = useState(DRAW_DEALER);
  const [message, setMessage] = useState("");
  const [gameScores, setGameScores] = useState([0, 0]);
  const [dealer, setDealer] = useState(-1);
  const [hands, setHands] = useState([]);
  const [crib, setCrib] = useState([]);
  const [starterCard, setStarterCard] = useState(null);
  const [shownScore, setShownScore] = useState(-1);
  const [winner, setWinner] = useState(-1);
  const cardsInPlay = useRef(0);

  const dealCards = async () => {
    try {
      const promise = await api.post("game/dealHands");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const resetGame = async () => {
    await api.post("game/resetGame");
  };

  // EFFECTS

  // For when the game stage changes
  useEffect(() => {
    if (currentStage === DRAW_DEALER) {
      resetGame();
      setMessage(
        "Pick a card to decide who is the dealer. The player with the lowest card deals first."
      );
    } else if (currentStage === DEAL_HANDS) {
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
    } else if (currentStage === COUNT_HANDS) {
      setShownScore(0);
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

  function previousScore() {
    setShownScore(Math.max(0, shownScore - 1));
  }

  function nextScore() {
    setShownScore(Math.min(2, shownScore + 1));
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
      case DEAL_HANDS:
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
            startingHands={structuredClone(hands)}
            crib={crib}
            setGameScores={setGameScores}
            setMessage={setMessage}
            setStage={setCurrentStage}
            displayDeck={displayDeck}
          />
        );
      case COUNT_HANDS:
        return (
          <CountHands
            dealer={dealer}
            hands={hands}
            crib={crib}
            setGameScores={setGameScores}
            setMessage={setMessage}
            setStage={setCurrentStage}
            displayDeck={displayDeck}
          />
        );
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
        {currentStage === COUNT_HANDS && (
          <div className="score-buttons">
            <button
              className={
                shownScore === 0 ? "left-button hidden" : "left-button"
              }
              onClick={previousScore}
            >
              {BUTTON_TEXT[dealer][shownScore - 1]}
            </button>
            <button className="right-button" onClick={nextScore}>
              {BUTTON_TEXT[dealer][shownScore + 1]}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default Game;
