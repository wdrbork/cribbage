import "./playerRow.css";

function PlayerRow({ pid, score }) {
  return (
    <div className="PlayerRow">
      {pid === 0 ? (
        <div className="player">You</div>
      ) : (
        <div className="player">Opponent</div>
      )}
      <div className="score">{score}</div>
    </div>
  );
}

export default PlayerRow;
