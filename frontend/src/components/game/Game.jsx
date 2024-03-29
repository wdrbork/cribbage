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
  const [aiDealerCard, setAiDealerCard] = useState(null);
  const [dealer, setDealer] = useState(-1);
  const [gameScores, setGameScores] = useState(Array(numPlayers));

  const pickedDealerCardId = useRef(-1);
  const aiDealerCardId = useRef(-1);

  function stageSwitch() {
    switch (currentStage) {
      case DRAW_DEALER:
        return (
          <>
            <div className="dealer-cards">{displayDealerCards()}</div>
            <div className="user-dealer-card">
              {userDealerCard && <Card cardInfo={userDealerCard} />}
            </div>
            <div className="ai-dealer-card">
              {aiDealerCard && <Card cardInfo={aiDealerCard} />}
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
      console.error(err);
    }
  };

  useEffect(() => {
    getScores();
  }, [currentStage]);

  useEffect(() => {
    if (!interactableDealerCards) {
      api
        .get("/game/dealer_card")
        .then((response) => {
          setUserDealerCard(response.data);
        })
        .catch((err) => {
          console.error(err);
        });
    }
  }, [interactableDealerCards]);

  useEffect(() => {
    if (userDealerCard) {
      api
        .get("/game/dealer_card")
        .then((response) => {
          if (response.data.rankValue === userDealerCard.rankValue) {
            resetDealerCards();
            return;
          }

          setAiDealerCard(response.data);
        })
        .catch((err) => {
          console.error(err);
        })
        .finally(function () {
          api.post("/game/reset_deck");
        });
    }
  }, [userDealerCard]);

  function onDealerCardClick(cardId) {
    setInteractableDealerCards(false);
    pickedDealerCardId.current = cardId;
  }

  function displayDealerCards() {
    console.log("rendering dealer cards");
    if (userDealerCard !== null && aiDealerCard === null) {
      aiDealerCardId.current = pickedDealerCardId.current;
      while (aiDealerCardId.current === pickedDealerCardId.current) {
        aiDealerCardId.current = Math.floor(Math.random() * DECK_SIZE);
      }
    }

    let dealerCards = [];
    let displayCard = true;
    for (let i = 0; i < DECK_SIZE; i++) {
      if (i === aiDealerCardId.current || i === pickedDealerCardId.current) {
        displayCard = false;
      }

      let offset = i * CARD_OFFSET;
      dealerCards.push(
        <Card
          key={i}
          id={i}
          offset={offset + "px"}
          interactable={interactableDealerCards}
          onClick={onDealerCardClick}
          display={displayCard}
          hidden
        />
      );
      displayCard = true;
    }

    return dealerCards;
  }

  function resetDealerCards() {
    setInteractableDealerCards(true);
    setUserDealerCard(null);
    pickedDealerCardId.current = -1;
  }

  return (
    <>
      <Scoreboard gameScores={gameScores} />
      {stageSwitch()}
    </>
  );
}

export default Game;
