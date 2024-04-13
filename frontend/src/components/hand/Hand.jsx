import "./hand.css";
import Card from "../card";
import { useState } from "react";

function Hand({ pid, cards, onCardClick = () => {}, selectedCards = [] }) {
  function extractCardComponents() {
    let cardComponents = [];
    cards.forEach((card) => {
      cardComponents.push(
        <Card
          key={card.cardId}
          id={card.cardId}
          cardInfo={card}
          onClick={onCardClick}
          selected={selectedCards.includes(card.cardId) ? true : false}
          interactable={pid === 0 ? true : false}
          hidden={pid === 1 ? true : false}
        />
      );
    });

    return cardComponents;
  }

  return <div className="Hand">{extractCardComponents()}</div>;
}

export default Hand;
