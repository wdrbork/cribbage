import "./winnerModal.css";
import { USER_ID, OPP_ID } from "../../global/vars.js";

function WinnerModal({ winner, scores, onNewGame }) {
  return (
    <div className={winner !== -1 ? "WinnerModal show" : "WinnerModal"}>
      <div className="WinnerModal-content">
        <p className="winner">
          {winner === USER_ID ? "You win!" : "Your opponent has won."}
        </p>
        <p className="scores">
          Final Score: {scores[USER_ID]} - {scores[OPP_ID]}
        </p>
        <button onClick={onNewGame}>Start new game</button>
      </div>
    </div>
  );
}

export default WinnerModal;
