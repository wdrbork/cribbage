import "./card.css";
import { useState } from "react";

const CARD_WIDTH = 150;
const CARD_HEIGHT_RATIO = 1.452;
const PATH = "/images/PNG-cards-1.3/";
const CARD_BACK = PATH + "card_back.png";

function Card({ cardInfo, hidden, offset }) {
  const [onHover, setOnHover] = useState(false);

  const handleMouseOver = () => {
    setOnHover(true);
  };

  const handleMouseOut = () => {
    setOnHover(false);
  };

  const cardImage = hidden
    ? CARD_BACK
    : PATH +
      cardInfo.rankValue +
      "_of_" +
      cardInfo.suit.toLowerCase() +
      "s.png";

  let cardStyle = {
    position: "absolute",
    left: offset,
  };

  if (onHover) {
    cardStyle["top"] = "-25px";
  }

  return (
    <div
      className="Card"
      onMouseOver={handleMouseOver}
      onMouseOut={handleMouseOut}
    >
      <img
        src={cardImage}
        width={CARD_WIDTH}
        height={CARD_WIDTH * CARD_HEIGHT_RATIO}
        style={cardStyle}
        alt={
          hidden ? "Back of card" : cardInfo.rank + " of " + cardInfo.suit + "S"
        }
      />
    </div>
  );
}

export default Card;
