import "./scoreboard.css";
import PlayerRow from "./playerRow";
import { useState, useEffect } from "react";

function Scoreboard({ gameScores }) {
  const scoreElements = gameScores.map((score, pid) => {
    return <PlayerRow key={pid} pid={pid} score={score} />;
  });

  return (
    <div className="Scoreboard">
      <div className="header">
        <h3>Scores</h3>
      </div>
      <div className="scores">{scoreElements}</div>
    </div>
  );
}

export default Scoreboard;
