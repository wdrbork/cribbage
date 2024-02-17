import "./game.css";
import Scoreboard from "../scoreboard";
import Card from "../card";

function Game({ numPlayers }) {
  const cardInfo = {
    suit: "DIAMOND",
    rank: "SIX",
    value: 6,
    rankValue: 6,
    suitValue: 2,
  };

  return (
    <div className="Game">
      <Scoreboard numPlayers={numPlayers} />
      <Card cardInfo={cardInfo} />
    </div>
  );
}

export default Game;
