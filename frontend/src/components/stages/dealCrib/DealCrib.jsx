import "./dealCrib.css";
import api from "../../../api/axiosConfig.js";
import { useState, useEffect } from "react";

import { USER_ID, OPP_ID, PROCESS_DELAY_MS } from "../../../global/vars.js";

import { PLAY_ROUND } from "../../../global/stages.js";
import timeout from "../../../global/timeout.js";

import Card from "../../card";
import Hand from "../../hand";
import Crib from "../../crib";
import SendToCrib from "../../sendToCrib";

function DealCrib(props) {
  const [selectedCards, setSelectedCards] = useState([]);
  const [handsFinalized, setHandsFinalized] = useState(false);

  const moveToCrib = async (card) => {
    try {
      const promise = await api.post(
        `game/move/${USER_ID}/${card.suitValue}/${card.rankValue}`
      );
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const pickAIHand = async () => {
    try {
      const promise = await api.post("game/ai/hands");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const getStarterCard = async () => {
    try {
      const promise = await api.get("game/starter");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  // For when the user selects their cards for the crib
  useEffect(() => {
    if (props.crib.length === 2) {
      props.crib.forEach((card) => {
        moveToCrib(card);
      });

      pickAIHand().then(async (response) => {
        await timeout(PROCESS_DELAY_MS);
        let newHands = [...props.hands];
        let fullCrib = [...props.crib];

        response.data.forEach((card) => {
          newHands[OPP_ID].cards = newHands[OPP_ID].cards.filter(
            (cardInfo) => cardInfo.cardId !== card.cardId
          );
          fullCrib.push(card);
        });

        setHandsFinalized(true);
        props.setHands(newHands);
        props.setCrib(fullCrib);
      });
    }

    if (props.crib.length === 4 && !props.starterCard) {
      // Wait for the backend server to acknowledge the final hands
      while (!handsFinalized) {}
      getStarterCard().then(async (response) => {
        const card = response.data;
        props.cardsInPlay.current++;
        let newMessage = "";

        await timeout(PROCESS_DELAY_MS);
        props.setStarterCard(card);
        if (card.rankValue === 1 || card.rankValue === 8) {
          newMessage = `The starter card is an ${card.rank.toLowerCase()} 
              of ${card.suit.toLowerCase()}s.`;
        } else {
          newMessage = `The starter card is a ${card.rank.toLowerCase()} 
              of ${card.suit.toLowerCase()}s.`;
          if (card.rankValue === 11) {
            await timeout(PROCESS_DELAY_MS);
            newMessage += " Dealer gets two points from his heels.";
            props.getScores().then((response) => {
              props.setGameScores(response.data);
            });
          }
        }
        props.setMessage(newMessage);

        await timeout(PROCESS_DELAY_MS);
        props.setStage(PLAY_ROUND);
        const nextPlayer = (props.dealer + 1) % 2;
        props.setPlayerTurn(nextPlayer);
        if (nextPlayer === OPP_ID) {
          props.setMessage("It is your opponent's turn to select a card.");
        } else if (nextPlayer === USER_ID) {
          props.setMessage("It is your turn. Please select a card.");
        }
      });
    }
  }, [props.crib]);

  function onCribCardClick(cardId) {
    if (props.crib.length >= 2) return;

    let temp = [...selectedCards];

    if (selectedCards.length === 2 && !selectedCards.includes(cardId)) {
      props.setMessage(
        "Only two cards can be sent to the crib. " +
          "Please unselect another card and then select this card again."
      );
      return;
    }

    // Remove the card if it was already selected. Otherwise, add it to the array
    if (selectedCards.includes(cardId)) {
      temp = temp.filter((id) => id !== cardId);
      if (temp.length === 0) {
        if (props.dealer === USER_ID) {
          props.setMessage("Select two cards that will be sent to your crib.");
        } else {
          props.setMessage(
            "Select two cards that will be sent to your opponent's crib."
          );
        }
      } else {
        props.setMessage("Select one more card for the crib.");
      }
    } else {
      temp.push(cardId);
      if (temp.length === 1) {
        props.setMessage("Select one more card for the crib.");
      } else {
        props.setMessage(
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

    const selectedCardObjects = props.hands[USER_ID].cards.filter(
      (cardInfo) =>
        cardInfo.cardId === selectedCards[0] ||
        cardInfo.cardId === selectedCards[1]
    );
    props.setCrib([...selectedCardObjects]);

    let newHands = [...props.hands];
    newHands[USER_ID].cards = newHands[USER_ID].cards.filter(
      (cardInfo) =>
        cardInfo.cardId !== selectedCards[0] &&
        cardInfo.cardId !== selectedCards[1]
    );
    props.setHands(newHands);

    setSelectedCards([]);
    props.setMessage("Opponent currently selecting cards for the crib...");
  }

  return (
    <>
      <div className="top-row ai-hand">
        <div className="crib-container">
          {props.dealer === OPP_ID && <Crib cards={props.crib} />}
        </div>
        {props.hands.length !== 0 && (
          <Hand pid={OPP_ID} cards={props.hands[OPP_ID].cards} />
        )}
      </div>
      <div className="middle-row">
        <div className="deck-cards">{props.displayDeck()}</div>
      </div>
      <div className="bottom-row user-hand">
        <div className="crib-container">
          {props.dealer === USER_ID && <Crib cards={props.crib} />}
        </div>
        {props.hands.length !== 0 && (
          <Hand
            pid={USER_ID}
            cards={props.hands[USER_ID].cards}
            onCardClick={onCribCardClick}
            selectedCards={selectedCards}
          />
        )}
        <div className="crib-button-container">
          {props.crib.length < 2 && (
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

export default DealCrib;
