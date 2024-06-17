import "./hand.css";
import Card from "../card";
import { useState } from "react";

function Hand({ pid, cards, onCardClick = () => {}, selectedCards = [] }) {
  function extractCardComponents() {
    let cardComponents = [];
    cards.forEach((card, i) => {
      cardComponents.push(
        <Card
          key={pid === 0 ? card.cardId : i}
          id={card.cardId}
          cardInfo={pid === 0 ? card : null}
          onClick={onCardClick}
          selected={selectedCards.includes(card.cardId) ? true : false}
          interactable={pid === 0 ? true : false}
          hidden={pid === 0 ? false : true}
        />
      );
    });

    return cardComponents;
  }

  return <div className="Hand">{extractCardComponents()}</div>;
}

export default Hand;
