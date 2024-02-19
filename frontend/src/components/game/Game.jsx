import "./game.css";
import api from "../../api/axiosConfig.js";
import Scoreboard from "../scoreboard";
import Card from "../card";
import { useState, useEffect } from "react";

// Game Stages
const DRAW_DEALER = 0;
const DEAL_CRIB = 1;
const PLAY_ROUND = 2;
const COUNT_HANDS = 3;
const COUNT_CRIB = 4;

function Game({ numPlayers }) {
  const [currentStage, setCurrentStage] = useState(0);
  const [dealer, setDealer] = useState(-1);
  const [gameScores, setGameScores] = useState(Array(numPlayers));

  const getScores = async () => {
    try {
      const response = await api.get("/game/scores");
      console.log(response);
      setGameScores(response.data);
    } catch (err) {
      console.log(err);
    }
  };

  useEffect(() => {
    getScores();
  }, [currentStage]);

  const cardInfo = {
    suit: "DIAMOND",
    rank: "SIX",
    value: 6,
    rankValue: 6,
    suitValue: 2,
  };

  return (
    <div className="Game">
      <Scoreboard gameScores={gameScores} />
      <Card cardInfo={cardInfo} />
    </div>
  );
}

export default Game;
