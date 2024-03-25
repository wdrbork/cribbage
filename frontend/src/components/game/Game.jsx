import "./game.css";
import api from "../../api/axiosConfig.js";
import Scoreboard from "../scoreboard";
import Card from "../card";
import { useState, useEffect } from "react";

// Game Stages
const DRAW_DEALER = 0;
const DEAL_CRIB = 1;
const PLAY_ROUND = 2;
const COUNT_HANDS = 3;
const COUNT_CRIB = 4;

const DECK_SIZE = 52;
const CARD_OFFSET = 20;

function Game({ numPlayers }) {
  const [currentStage, setCurrentStage] = useState(0);
  const [interactableDealerCards, setInteractableDealerCards] = useState(true);
  const [dealer, setDealer] = useState(-1);
  const [gameScores, setGameScores] = useState(Array(numPlayers));

  function stageSwitch() {
    switch (currentStage) {
      case DRAW_DEALER:
        return <div className="dealer-cards">{displayDealerCards()}</div>;
      case DEAL_CRIB:
        return DEAL_CRIB;
      case PLAY_ROUND:
        return PLAY_ROUND;
      case COUNT_HANDS:
        return COUNT_HANDS;
      case COUNT_CRIB:
        return COUNT_CRIB;
      default:
        return null;
    }
  }

  const getScores = async () => {
    try {
      const response = await api.get("/game/scores");
      setGameScores(response.data);
    } catch (err) {
      console.log(err);
    }
  };

  useEffect(() => {
    getScores();
  }, [currentStage]);

  function onDealerCardClick() {
    setInteractableDealerCards(false);
  }

  function displayDealerCards() {
    let dealerCards = [];
    for (let i = 0; i < DECK_SIZE; i++) {
      let offset = i * CARD_OFFSET;
      dealerCards.push(
        <Card
          key={i}
          cardInfo={null}
          offset={offset + "px"}
          interactable={interactableDealerCards}
          onClick={onDealerCardClick}
          hidden
        />
      );
    }

    return dealerCards;
  }

  const cardInfo = {
    suit: "DIAMOND",
    rank: "SIX",
    value: 6,
    rankValue: 6,
    suitValue: 2,
  };

  return (
    <div className="Game">
      <Scoreboard gameScores={gameScores} />
      {stageSwitch()}
    </div>
  );
}

export default Game;
