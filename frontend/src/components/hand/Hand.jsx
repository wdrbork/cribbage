import "./hand.css";
import Card from "../card";
import { useState } from "react";

import { USER_ID, OPP_ID } from "../../global/vars.js";

function Hand({
  pid,
  cards,
  interactable = pid === USER_ID,
  hidden = pid === OPP_ID,
  onCardClick = () => {},
  selectedCards = [],
}) {
  function extractCardComponents() {
    let cardComponents = [];
    cards.forEach((card, i) => {
      cardComponents.push(
        <Card
          key={pid === 0 ? card.cardId : i}
          id={card.cardId}
          cardInfo={pid === 0 || !hidden ? card : null}
          onClick={onCardClick}
          selected={selectedCards.includes(card.cardId) ? true : false}
          interactable={interactable}
          hidden={hidden}
        />
      );
    });

    return cardComponents;
  }

  return <div className="Hand">{extractCardComponents()}</div>;
}

export default Hand;
