import "./playerRow.css";

function PlayerRow({ pid, score }) {
  return (
    <div className="PlayerRow">
      <div className="player">Player {pid + 1}</div>
      <div className="score">{score}</div>
    </div>
  );
}

export default PlayerRow;
