import "./crib.css";
import Card from "../card";
import { useState } from "react";

function Crib({ cards }) {
  function extractCardComponents() {
    let cardComponents = [];
    cards.forEach((card, i) => {
      cardComponents.push(
        <Card key={i} id={card.cardId} cardInfo={card} hidden />
      );
    });

    return cardComponents;
  }

  return <div className="Crib">{extractCardComponents()}</div>;
}

export default Crib;
