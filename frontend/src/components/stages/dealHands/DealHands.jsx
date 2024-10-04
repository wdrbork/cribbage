import "./dealHands.css";
import api from "../../../api/axiosConfig.js";
import { useState, useEffect } from "react";

import { USER_ID, OPP_ID, PROCESS_DELAY_MS } from "../../../global/vars.js";

import { PLAY_ROUND } from "../../../global/stages.js";
import timeout from "../../../global/timeout.js";

import Hand from "../../hand/index.js";
import Crib from "../../crib/index.js";
import SendToCrib from "../../sendToCrib/index.js";

function DealHands({
  dealer,
  hands,
  crib,
  starterCard,
  setHands,
  setCrib,
  setStarterCard,
  setGameScores,
  setMessage,
  setStage,
  displayDeck,
  cardsInPlay,
}) {
  const [selectedCards, setSelectedCards] = useState([]);

  const getScores = async () => {
    try {
      const promise = await api.post("game/getScores");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const moveToCrib = async (card) => {
    try {
      const promise = await api.post("/game/moveCardToCrib", {
        pid: USER_ID,
        suitId: card.suitValue,
        rankId: card.rankValue,
      });
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const pickAIHand = async () => {
    try {
      const promise = await api.post("game/selectAIHands");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const getStarterCard = async () => {
    try {
      const promise = await api.post("game/pickStarterCard");
      return promise;
    } catch (err) {
      console.error(err);
      await timeout(200);
      getStarterCard();
    }
  };

  // For when the user selects their cards for the crib
  useEffect(() => {
    if (crib.length === 2) {
      crib.forEach((card) => {
        moveToCrib(card);
      });

      pickAIHand().then(async (response) => {
        await timeout(PROCESS_DELAY_MS);
        let newHands = [...hands];
        let fullCrib = [...crib];

        response.data.forEach((card) => {
          newHands[OPP_ID].cards = newHands[OPP_ID].cards.filter(
            (cardInfo) => cardInfo.cardId !== card.cardId
          );
          fullCrib.push(card);
        });

        setHands(newHands);
        setCrib(fullCrib);
      });
    }

    if (crib.length === 4 && !starterCard) {
      getStarterCard().then(async (response) => {
        const card = response.data;
        cardsInPlay.current++;
        let newMessage = "";

        await timeout(PROCESS_DELAY_MS);
        setStarterCard(card);
        if (card.rankValue === 1 || card.rankValue === 8) {
          newMessage = `The starter card is an ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`;
        } else {
          newMessage = `The starter card is a ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`;
          if (card.rankValue === 11) {
            await timeout(PROCESS_DELAY_MS);
            newMessage += " Dealer gets two points from his heels.";
            getScores().then((response) => {
              setGameScores(response.data);
            });
          }
        }
        setMessage(newMessage);

        await timeout(PROCESS_DELAY_MS);
        setStage(PLAY_ROUND);
      });
    }
  }, [crib]);

  function onCribCardClick(cardId) {
    if (crib.length >= 2) return;

    let temp = [...selectedCards];

    if (selectedCards.length === 2 && !selectedCards.includes(cardId)) {
      setMessage(
        "Only two cards can be sent to the crib. " +
          "Please unselect another card and then select this card again."
      );
      return;
    }

    // Remove the card if it was already selected. Otherwise, add it to the array
    if (selectedCards.includes(cardId)) {
      temp = temp.filter((id) => id !== cardId);
      if (temp.length === 0) {
        if (dealer === USER_ID) {
          setMessage("Select two cards that will be sent to your crib.");
        } else {
          setMessage(
            "Select two cards that will be sent to your opponent's crib."
          );
        }
      } else {
        setMessage("Select one more card for the crib.");
      }
    } else {
      temp.push(cardId);
      if (temp.length === 1) {
        setMessage("Select one more card for the crib.");
      } else {
        setMessage(
          'Click the "Send to Crib" button to move the selected cards to the crib.'
        );
      }
    }

    setSelectedCards(temp);
  }

  function onCribButtonClick() {
    if (selectedCards.length !== 2) {
      return;
    }

    const selectedCardObjects = hands[USER_ID].cards.filter(
      (cardInfo) =>
        cardInfo.cardId === selectedCards[0] ||
        cardInfo.cardId === selectedCards[1]
    );
    setCrib([...selectedCardObjects]);

    let newHands = [...hands];
    newHands[USER_ID].cards = newHands[USER_ID].cards.filter(
      (cardInfo) =>
        cardInfo.cardId !== selectedCards[0] &&
        cardInfo.cardId !== selectedCards[1]
    );
    setHands(newHands);

    setSelectedCards([]);
    setMessage("Opponent currently selecting cards for the crib...");
  }

  return (
    <>
      <div className="top-row ai-hand">
        <div className="crib-container">
          {dealer === OPP_ID && <Crib cards={crib} />}
        </div>
        {hands.length !== 0 && (
          <Hand pid={OPP_ID} cards={hands[OPP_ID].cards} />
        )}
      </div>
      <div className="middle-row">
        <div className="deck-cards">{displayDeck()}</div>
      </div>
      <div className="bottom-row user-hand">
        <div className="crib-container">
          {dealer === USER_ID && <Crib cards={crib} />}
        </div>
        {hands.length !== 0 && (
          <Hand
            pid={USER_ID}
            cards={hands[USER_ID].cards}
            onCardClick={onCribCardClick}
            selectedCards={selectedCards}
          />
        )}
        <div className="crib-button-container">
          {crib.length < 2 && (
            <SendToCrib
              selectedCards={selectedCards}
              onClick={onCribButtonClick}
            />
          )}
        </div>
      </div>
    </>
  );
}

export default DealHands;
