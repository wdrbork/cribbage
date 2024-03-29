import "./App.css";
import Home from "./components/home";
import Game from "./components/game";
import { useState } from "react";

const NUM_PLAYERS_DEFAULT = 2;

function App() {
  const [gameStart, setGameStart] = useState(false);

  function startGame() {
    setGameStart(true);
  }

  return (
    <div className="App">
      <Game numPlayers={NUM_PLAYERS_DEFAULT} />
      {/* {gameStart ? (
        <Game numPlayers={NUM_PLAYERS_DEFAULT} />
      ) : (
        <Home handleClick={startGame} />
      )} */}
    </div>
  );
}

export default App;
