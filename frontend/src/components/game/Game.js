import './Game.css';
import Scoreboard from '../scoreboard/Scoreboard.js';
import { useState } from 'react';

function Game() {
  const testScores = [2, 6];

  return (
      <>
        <Scoreboard gameScores={testScores} />
      </>
  )
}

export default Game;