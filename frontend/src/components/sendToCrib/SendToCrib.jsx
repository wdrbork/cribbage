import "./sendToCrib.css";
import { useState } from "react";

function sendToCrib({ selectedCards, onClick }) {
  return (
    <button
      className={
        selectedCards.length === 2 ? "crib-button" : "crib-button discreet"
      }
      onClick={onClick}
    >
      Send to Crib
    </button>
  );
}

export default sendToCrib;
