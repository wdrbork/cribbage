import "./playedCards.css";
import Card from "../card";
// import { useState } from "react";

function PlayedCards({ cards, oldCards }) {
  function extractCardComponents() {
    let playedCards = [];

    oldCards.forEach((card) => {
      playedCards.push(
        <Card key={card.cardId} id={card.cardId} cardInfo={card} shaded />
      );
    });

    cards.forEach((card) => {
      playedCards.push(
        <Card key={card.cardId} id={card.cardId} cardInfo={card} />
      );
    });

    // oldCards.forEach((player) => {
    //   player.forEach((card) => {
    //     playedCards.push(
    //       <Card key={card.cardId} id={card.cardId} cardInfo={card} shaded />
    //     );
    //   });
    // });

    // cards.forEach((player) => {
    //   player.forEach((card) => {
    //     playedCards.push(
    //       <Card key={card.cardId} id={card.cardId} cardInfo={card} />
    //     );
    //   });
    // });

    return playedCards;
  }

  return <div className="played-cards">{extractCardComponents()}</div>;
}

export default PlayedCards;
