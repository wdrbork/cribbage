import "./card.css";

function Card({ cardInfo }) {
  const cardImage =
    "/images/PNG-cards-1.3/" +
    cardInfo.rankValue +
    "_of_" +
    cardInfo.suit.toLowerCase() +
    "s.png";

  return (
    <div className="Card">
      <img src={cardImage} alt={cardImage} />
    </div>
  );
}

export default Card;
