import "./scoreboard.css";
import api from "../../api/axiosConfig.js";
import PlayerRow from "./playerRow";
import { useState, useEffect } from "react";

function Scoreboard({ numPlayers }) {
  const [gameScores, setGameScores] = useState([0, 0]);

  const getScores = async () => {
    try {
      const response = await api.get("/game/scores");
      setGameScores(response.data);
    } catch (err) {
      console.log(err);
    }
  };

  useEffect(() => {
    getScores();
  }, []);

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
