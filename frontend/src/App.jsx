import './App.css';
import Scoreboard from './components/scoreboard'
import { useState } from 'react';

const NUM_PLAYERS_DEFAULT = 2;

function App() {
  const [gameStart, setGameStart] = useState(false);
  const numPlayers = NUM_PLAYERS_DEFAULT;

  function handleClick() {
    setGameStart(true);
  }

  return (
    <div className="App">
      {gameStart ? (
        <div className="Game">
            <Scoreboard numPlayers={numPlayers} />
        </div>
      ) : (
        <>
          <h1>Cribbage</h1>
          <button onClick={handleClick}>Start Game</button>
        </>
      )}
    </div>
  );
}

export default App;