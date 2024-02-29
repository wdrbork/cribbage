import "./card.css";

const CARD_WIDTH = 150;
const CARD_HEIGHT_RATIO = 1.452;
const PATH = "/images/PNG-cards-1.3/";
const CARD_BACK = PATH + "card_back.png";

function Card({ cardInfo, hidden }) {
  const cardImage = hidden
    ? CARD_BACK
    : PATH +
      cardInfo.rankValue +
      "_of_" +
      cardInfo.suit.toLowerCase() +
      "s.png";

  return (
    <div className="Card">
      <img
        src={cardImage}
        width={CARD_WIDTH}
        height={CARD_WIDTH * CARD_HEIGHT_RATIO}
        alt={
          hidden ? "Back of card" : cardInfo.rank + " of " + cardInfo.suit + "S"
        }
      />
    </div>
  );
}

export default Card;
