import "./card.css";

const CARD_WIDTH = 150;
const CARD_HEIGHT_RATIO = 1.452;
const PATH = "/images/PNG-cards-1.3/";
const CARD_BACK = PATH + "card_back.png";

function Card({
  id,
  cardInfo,
  interactable = false,
  onClick = () => {},
  selected = false,
  display = true,
  hidden = false,
  shaded = false,
}) {
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

  const handleClick = () => {
    if (interactable) {
      onClick(id);
    }
  };

  let className = interactable ? "Card interactable" : "Card";

  if (interactable && selected) {
    className += " selected";
  }

  if (!display) {
    className += " no-display";
  }

  if (shaded) {
    className += " shaded";
  }

  return (
    <div className="card-container">
      <img
        className={className}
        src={cardImage}
        width={CARD_WIDTH}
        height={CARD_WIDTH * CARD_HEIGHT_RATIO}
        onClick={handleClick}
      />
    </div>
  );
}

export default Card;
