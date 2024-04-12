import "./playerRow.css";

function PlayerRow({ pid, score, dealer = false }) {
  return (
    <div className="PlayerRow">
      {pid === 0 ? (
        <div className="player">You {dealer ? " (dealer)" : ""}</div>
      ) : (
        <div className="player">Opponent {dealer ? " (dealer)" : ""}</div>
      )}
      <div className="score">{score}</div>
    </div>
  );
}

export default PlayerRow;
