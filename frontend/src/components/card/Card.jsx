import "./card.css";
import { useState } from "react";

const CARD_WIDTH = 150;
const CARD_HEIGHT_RATIO = 1.452;
const PATH = "/images/PNG-cards-1.3/";
const CARD_BACK = PATH + "card_back.png";

function Card({
  id,
  cardInfo = null,
  offset = 0,
  interactable = false,
  onClick = (cardId) => {},
  display = true,
  hidden = false,
}) {
  const [onHover, setOnHover] = useState(false);
  const [clicked, setClicked] = useState(false);

  const handleMouseOver = () => {
    interactable && setOnHover(true);
  };

  const handleMouseOut = () => {
    interactable && setOnHover(false);
  };

  const handleClick = () => {
    if (interactable) {
      setClicked(true);
      onClick(id);
    }
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
    cardStyle["top"] = "-5px";
  }

  if (!display || clicked) {
    cardStyle["display"] = "none";
  }

  return (
    <div
      className="Card"
      onMouseOver={handleMouseOver}
      onMouseOut={handleMouseOut}
      onClick={handleClick}
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
