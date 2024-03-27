import "./game.css";
import api from "../../api/axiosConfig.js";
import Scoreboard from "../scoreboard";
import Card from "../card";
import { useState, useEffect, useRef } from "react";

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
  const [userDealerCard, setUserDealerCard] = useState(null);
  const [dealer, setDealer] = useState(-1);
  const [gameScores, setGameScores] = useState(Array(numPlayers));

  function stageSwitch() {
    switch (currentStage) {
      case DRAW_DEALER:
        return (
          <>
            <div className="dealer-cards">{displayDealerCards()}</div>
            <div className="user-dealer-card">
              {userDealerCard && (
                <Card cardInfo={userDealerCard} onClick={onDealerCardClick} />
              )}
            </div>
          </>
        );
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

  useEffect(() => {
    console.log("test1");
    if (!interactableDealerCards) {
      console.log("test2");
      api
        .get("/game/dealer_card")
        .then((response) => {
          console.log(response.data);
          setUserDealerCard(response.data);
        })
        .catch((err) => {
          console.err(err);
        });
    }
  }, [interactableDealerCards]);

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
          offset={offset + "px"}
          interactable={interactableDealerCards}
          onClick={onDealerCardClick}
          hidden
        />
      );
    }

    return dealerCards;
  }

  return (
    <>
      <Scoreboard gameScores={gameScores} />
      {stageSwitch()}
    </>
  );
}

export default Game;
