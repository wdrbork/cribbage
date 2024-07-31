import "./drawDealer.css";
import api from "../../../api/axiosConfig.js";
import Card from "../../card";
import { useState, useEffect, useRef } from "react";

import {
  USER_ID,
  OPP_ID,
  DECK_SIZE,
  PROCESS_DELAY_MS,
} from "../../../global/vars.js";

import { DEAL_HANDS } from "../../../global/stages.js";
import timeout from "../../../global/timeout.js";

function DrawDealer({ setMessage, setDealer, setStage, cardsInPlay }) {
  const [userDealerCard, setUserDealerCard] = useState(null);
  const [aiDealerCard, setAiDealerCard] = useState(null);
  const [interactableDealerCards, setInteractableDealerCards] = useState(true);

  const aiDealerCardId = useRef(-1);
  const pickedDealerCardId = useRef(-1);

  // API CALLS
  const getDealerCard = async () => {
    try {
      const promise = await api.post("/game/pickDealerCard");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const postDealer = async (dealer) => {
    try {
      const promise = await api.post("game/setDealer", {
        pid: dealer,
      });
      setDealer(dealer);
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const resetDeck = async () => {
    await api.post("/game/resetDeck");
  };

  // EFFECTS

  // For when the user selects a dealer card
  useEffect(() => {
    if (!interactableDealerCards) {
      getDealerCard().then((response) => {
        const card = response.data;
        if (card.rankValue === 1 || card.rankValue === 8) {
          setMessage(`You drew an ${card.rank.toLowerCase()}.`);
        } else {
          setMessage(`You drew a ${card.rank.toLowerCase()}.`);
        }
        setUserDealerCard(card);
      });
    }
  }, [interactableDealerCards]);

  // For when the user dealer card is retrieved from the backend
  useEffect(() => {
    if (userDealerCard) {
      getDealerCard()
        .then(async (response) => {
          let card = response.data;
          await timeout(PROCESS_DELAY_MS);

          let newMessage = "";
          if (card.rankValue === 1 || card.rankValue === 8) {
            newMessage = `Your opponent drew an ${card.rank.toLowerCase()}. `;
          } else {
            newMessage = `Your opponent drew a ${card.rank.toLowerCase()}. `;
          }

          if (card.rankValue === userDealerCard.rankValue) {
            newMessage += "There was a tie; please draw again.";
            resetDealerCards();
            card = null;
          } else if (card.rankValue < userDealerCard.rankValue) {
            newMessage += "Your opponent will deal first.";
            postDealer(OPP_ID);
          } else {
            newMessage += "You will deal first.";
            postDealer(USER_ID);
          }

          setMessage(newMessage);
          setAiDealerCard(card);
        })
        .finally(() => {
          resetDeck();
        });
    }
  }, [userDealerCard]);

  // For when both the user and AI have selected a dealer card
  useEffect(() => {
    if (aiDealerCard && userDealerCard) {
      const cleanupDealerCards = async () => {
        await timeout(PROCESS_DELAY_MS);
        resetDealerCards();
        setStage(DEAL_HANDS);
      };

      cleanupDealerCards();
    }
  }, [aiDealerCard, userDealerCard]);

  // INTERACTION
  function onDealerCardClick(cardId) {
    setInteractableDealerCards(false);
    pickedDealerCardId.current = cardId;
  }

  function resetDealerCards() {
    setInteractableDealerCards(true);
    setUserDealerCard(null);
    setAiDealerCard(null);
    pickedDealerCardId.current = -1;
    aiDealerCardId.current = -1;
  }

  function displayDealerCards() {
    if (
      userDealerCard !== null &&
      aiDealerCard !== null &&
      aiDealerCardId.current === -1
    ) {
      aiDealerCardId.current = pickedDealerCardId.current;
      while (aiDealerCardId.current === pickedDealerCardId.current) {
        aiDealerCardId.current = Math.floor(Math.random() * DECK_SIZE);
      }
    }

    let dealerCards = [];
    let displayCard = true;
    for (let i = 0; i < DECK_SIZE - cardsInPlay.current; i++) {
      if (i === aiDealerCardId.current || i === pickedDealerCardId.current) {
        displayCard = false;
      }

      dealerCards.push(
        <Card
          key={i}
          id={i}
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

  return (
    <>
      <div className="top-row ai-dealer-card">
        {aiDealerCard && <Card cardInfo={aiDealerCard} />}
      </div>
      <div className="middle-row">
        <div className="dealer-cards">{displayDealerCards()}</div>
      </div>
      <div className="bottom-row user-dealer-card">
        {userDealerCard && <Card cardInfo={userDealerCard} />}
      </div>
    </>
  );
}

export default DrawDealer;
