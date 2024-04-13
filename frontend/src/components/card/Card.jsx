import "./card.css";
import { useState } from "react";

const CARD_WIDTH = 150;
const CARD_HEIGHT_RATIO = 1.452;
const PATH = "/images/PNG-cards-1.3/";
const CARD_BACK = PATH + "card_back.png";

function Card({
  id,
  cardInfo = null,
  interactable = false,
  onClick = () => {},
  selected = false,
  display = true,
  hidden = false,
}) {
  const [onHover, setOnHover] = useState(false);

  if (!hidden && !cardInfo) {
    console.error("Cannot show card; no card info provided");
    return;
  }

  const cardImage = hidden
    ? CARD_BACK
    : PATH +
      cardInfo.rankValue +
      "_of_" +
      cardInfo.suit.toLowerCase() +
      "s.png";

  let cardStyle = {
    position: "absolute",
  };

  const handleMouseOver = () => {
    interactable && setOnHover(true);
  };

  const handleMouseOut = () => {
    interactable && setOnHover(false);
  };

  const handleClick = () => {
    if (interactable) {
      onClick(id);
    }
  };

  if (interactable && (onHover || selected)) {
    if (selected) {
      cardStyle["top"] = "-20px";
    } else if (onHover) {
      cardStyle["top"] = "-5px";
    }
  }

  if (!display) {
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
