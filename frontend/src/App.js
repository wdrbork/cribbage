import './App.css';
import Game from './components/game/Game.js'
import { useState } from 'react';

function App() {
  const [gameStart, setGameStart] = useState(false);

  function handleClick() {
    setGameStart(true);
  }

  return (
    <div className="App">
      {gameStart ? (
        <Game />
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
