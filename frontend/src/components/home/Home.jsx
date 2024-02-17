import "./home.css";

function Home({ handleClick }) {
  return (
    <div className="Home">
      <h1>Cribbage</h1>
      <button onClick={handleClick}>Start Game</button>
    </div>
  );
}

export default Home;
