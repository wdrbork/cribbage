import "./game.css";
import api from "../../api/axiosConfig.js";
import { useState, useEffect, useRef } from "react";

import {
  USER_ID,
  OPP_ID,
  DECK_SIZE,
  PROCESS_DELAY_MS,
} from "../../global/vars.js";

import {
  DRAW_DEALER,
  DEAL_CRIB,
  PLAY_ROUND,
  COUNT_HANDS,
  COUNT_CRIB,
} from "../../global/stages.js";

import timeout from "../../global/timeout.js";

import Scoreboard from "../scoreboard";
import Message from "../message";
import Card from "../card";
import Hand from "../hand";
import Crib from "../crib";
import PlayedCards from "../playedCards";

import DrawDealer from "../stages/drawDealer";
import DealCrib from "../stages/dealCrib";

const CARDS_PER_SUIT = 13;
const MAX_COUNT = 31;
const WINNING_SCORE = 121;

const ROUND_POINT_CATEGORIES = 4;
const TOTAL_POINTS = 0;
const RUNS = 1;
const PAIRS = 2;
const SPECIAL = 3;

function Game({ numPlayers }) {
  const [currentStage, setCurrentStage] = useState(DRAW_DEALER);
  const [message, setMessage] = useState("");
  const [gameScores, setGameScores] = useState(Array(numPlayers));
  const [dealer, setDealer] = useState(-1);
  const [hands, setHands] = useState([]);
  const [selectedCards, setSelectedCards] = useState([]);
  const [crib, setCrib] = useState([]);
  const [starterCard, setStarterCard] = useState(null);
  const [playerTurn, setPlayerTurn] = useState(-1);
  const [count, setCount] = useState(0);
  const [playedCards, setPlayedCards] = useState([]);
  const [oldPlayedCards, setOldPlayedCards] = useState([]);
  const [winner, setWinner] = useState(-1);

  const cardsInPlay = useRef(0);
  const playerOnGo = useRef(false);

  // API CALLS
  const getScores = async () => {
    try {
      const promise = await api.get("game/scores");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const dealCards = async () => {
    try {
      const promise = await api.post("game/deal");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const playCard = async (cardInfo) => {
    try {
      const suitValue = cardInfo["suitValue"];
      const rankValue = cardInfo["rankValue"];
      const promise = await api.post(
        `game/play/${USER_ID}/${suitValue}/${rankValue}`
      );
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const playAICard = async () => {
    try {
      const promise = await api.post(`game/ai/play/${OPP_ID}`);
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const getNextPlayer = async () => {
    try {
      const promise = await api.get(`game/next`);
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const movePossible = async () => {
    try {
      const promise = await api.get(`game/move_possible`);
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const resetCount = async () => {
    try {
      const promise = await api.post(`game/reset_count`);
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const resetDeck = async () => {
    await api.post("/game/reset_deck");
  };

  const resetGame = async () => {
    await api.post("game/reset_game");
  };

  // EFFECTS

  // For when the game stage changes
  useEffect(() => {
    getScores().then((response) => {
      setGameScores(response.data);
    });

    if (currentStage === DRAW_DEALER) {
      resetGame();
      setMessage(
        "Pick a card to decide who is the dealer. The player with the lowest card deals first."
      );
    } else if (currentStage === DEAL_CRIB) {
      dealCards().then((response) => {
        const hands = response.data;
        setHands(hands);
        cardsInPlay.current =
          hands[USER_ID].cards.length + hands[OPP_ID].cards.length;
      });

      if (dealer === USER_ID) {
        setMessage("Select two cards that will be sent to your crib.");
      } else {
        setMessage(
          "Select two cards that will be sent to your opponent's crib."
        );
      }
    }
  }, [currentStage]);

  // For after when a card has been played
  useEffect(() => {
    if (playerTurn === -1 || playedCards.length + oldPlayedCards.length === 0)
      return;

    if (hands[USER_ID].cards.length + hands[OPP_ID].cards.length === 0) {
      const endRound = async () => {
        setMessage("The round is over.");
        await timeout(PROCESS_DELAY_MS);
        setCurrentStage(COUNT_HANDS);
      };

      endRound();
      return;
    }

    Promise.all([getNextPlayer(), movePossible()]).then(async (responses) => {
      let nextPlayer = responses[0].data;
      const movePossible = responses[1].data;
      const otherPlayer = (nextPlayer + 1) % 2;

      // Set message box if a player calls go but the other player can still play a card
      // (if the server says that the next player is the same one that played the most
      // recent card, then the other player must not be able to play a card)
      if (
        nextPlayer === playerTurn &&
        hands[otherPlayer].cards.length > 0 &&
        !playerOnGo.current &&
        movePossible
      ) {
        playerOnGo.current = true;
        if (playerTurn === USER_ID) {
          setMessage("Your opponent calls go.");
        } else {
          setMessage("You cannot play a card, so you call go.");
        }
        await timeout(PROCESS_DELAY_MS);
        return;
      }

      if (!movePossible) {
        if (count !== 31) {
          playerOnGo.current = false;
          let newGameScores = [...gameScores];
          newGameScores[nextPlayer]++;
          setGameScores(newGameScores);
          if (playerTurn === USER_ID) {
            setMessage(
              "Your opponent earns 1 point for playing the last card."
            );
          } else {
            setMessage("You earn 1 point for playing the last card.");
          }
          await timeout(PROCESS_DELAY_MS);
        }

        resetCount().then(() => {
          let oldCards = [...oldPlayedCards];
          oldCards.push(...playedCards);
          setOldPlayedCards(oldCards);
          setPlayedCards([]);
        });

        setCount(0);
        if (hands[otherPlayer].length > 0) {
          nextPlayer = otherPlayer;
        }
      }

      setPlayerTurn(nextPlayer);
      if (nextPlayer === OPP_ID) {
        setMessage("It is your opponent's turn to select a card.");
      } else if (nextPlayer === USER_ID) {
        setMessage("It is your turn. Please select a card.");
      }
    });
  }, [playedCards]);

  // For when it is the AI's turn to play a card
  useEffect(() => {
    if (playerTurn !== OPP_ID) return;

    playAICard().then(async (response) => {
      await timeout(PROCESS_DELAY_MS);

      let playedCard = response.data.playedCard;
      const newHands = [...hands];
      newHands[OPP_ID].cards = newHands[OPP_ID].cards.filter(
        (cardInfo) => cardInfo.cardId !== playedCard.cardId
      );
      setHands(newHands);

      const newPlayedCards = [...playedCards];
      newPlayedCards.push(playedCard);
      setPlayedCards(newPlayedCards);

      let newMessage;
      if (playedCard.rankValue === 1 || playedCard.rankValue === 8) {
        newMessage = `Your opponent played an ${playedCard.rank.toLowerCase()} of ${playedCard.suit.toLowerCase()}s.`;
      } else {
        newMessage = `Your opponent played a ${playedCard.rank.toLowerCase()} of ${playedCard.suit.toLowerCase()}s.`;
      }

      const pointCategories = response.data.pointsEarned;

      if (pointCategories[RUNS] > 0) {
        newMessage += `\n\nYour opponent earned ${pointCategories[RUNS]} points for the run.`;
      }

      if (pointCategories[PAIRS] === 2) {
        newMessage += `\n\nYour opponent earned ${pointCategories[PAIRS]} points for the pair.`;
      } else if (pointCategories[PAIRS] === 6) {
        newMessage += `\n\nYour opponent earned ${pointCategories[PAIRS]} points for the pair royal.`;
      } else if (pointCategories[PAIRS] === 12) {
        newMessage += `\n\nYour opponent earned ${pointCategories[PAIRS]} points for the double pair royal.`;
      }

      if (pointCategories[SPECIAL] >= 2) {
        newMessage += `\n\nYour opponent earned 2 points for making the count ${
          count + playedCard.value
        }.`;
      }

      if (pointCategories[SPECIAL] % 2 === 1) {
        if (hands[USER_ID].cards.length > 0) {
          pointCategories[TOTAL_POINTS]--;
          pointCategories[SPECIAL]--;
        } else if (hands[OPP_ID].cards.length === 0) {
          newMessage += `\n\nYour opponent earned 1 point for playing the last card.`;
        }
      }

      if (pointCategories[TOTAL_POINTS] > 0) {
        let newGameScores = [...gameScores];
        newGameScores[OPP_ID] += pointCategories[TOTAL_POINTS];
        setGameScores(newGameScores);
      }

      setMessage(newMessage);

      setCount(count + playedCard.value);

      await timeout(PROCESS_DELAY_MS);
    });
  }, [playerTurn]);

  // INTERACTION
  function onHandCardClick(cardId) {
    if (playerTurn !== USER_ID) {
      return;
    }

    const card = hands[USER_ID].cards.find(
      (cardInfo) => cardInfo.cardId === cardId
    );

    if (count + card.rankValue > MAX_COUNT) {
      setMessage(
        "This card cannot be played because it would cause the " +
          "count to exceed 31. Please select another card."
      );
      return;
    }

    playCard(card).then(async (response) => {
      let newMessage;
      if (card.rankValue === 1 || card.rankValue === 8) {
        newMessage = `You played an ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`;
      } else {
        newMessage = `You played a ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`;
      }

      const pointCategories = response.data.pointsEarned;

      if (pointCategories[RUNS] > 0) {
        newMessage += `\n\nYou earned ${pointCategories[RUNS]} points for the run.`;
      }

      if (pointCategories[PAIRS] === 2) {
        newMessage += `\n\nYou earned ${pointCategories[PAIRS]} points for the pair.`;
      } else if (pointCategories[PAIRS] === 6) {
        newMessage += `\n\nYou earned ${pointCategories[PAIRS]} points for the pair royal.`;
      } else if (pointCategories[PAIRS] === 12) {
        newMessage += `\n\nYou earned ${pointCategories[PAIRS]} points for the double pair royal.`;
      }

      if (pointCategories[SPECIAL] >= 2) {
        newMessage += `\n\nYou earned 2 points for making the count ${
          count + card.value
        }.`;
      }

      // If this value is odd, the user must have earned a point from go
      // (special = 1 means the user only earned a point from go, special = 2
      // means the user earned points from getting to 15 or 31, special = 3
      // means the count is 15 AND go was called)
      if (pointCategories[SPECIAL] % 2 === 1) {
        if (hands[OPP_ID].cards.length > 0) {
          pointCategories[TOTAL_POINTS]--;
          pointCategories[SPECIAL]--;
        } else if (hands[USER_ID].cards.length === 0) {
          newMessage += `\n\nYou earned 1 point for playing the last card.`;
        }
      }

      let newGameScores = [...gameScores];
      newGameScores[USER_ID] += pointCategories[TOTAL_POINTS];
      setGameScores(newGameScores);

      setMessage(newMessage);

      const newHands = [...hands];
      newHands[USER_ID].cards = newHands[USER_ID].cards.filter(
        (cardInfo) => cardInfo.cardId !== cardId
      );
      setHands(newHands);

      const newPlayedCards = [...playedCards];
      newPlayedCards.push(card);
      setPlayedCards(newPlayedCards);

      setCount(count + card.value);

      await timeout(PROCESS_DELAY_MS);
    });
  }

  function displayDeck() {
    let deckCards = [];
    let i = 0;
    for (; i < DECK_SIZE - cardsInPlay.current; i++) {
      deckCards.push(<Card key={i} id={i} hidden />);
    }

    if (starterCard) {
      deckCards.push(
        <Card key={i} id={starterCard.cardId} cardInfo={starterCard} />
      );
    }

    return deckCards;
  }

  function stageSwitch() {
    switch (currentStage) {
      case DRAW_DEALER:
        return (
          <DrawDealer
            setMessage={setMessage}
            setDealer={setDealer}
            setStage={setCurrentStage}
            cardsInPlay={cardsInPlay}
          />
        );
      case DEAL_CRIB:
        return (
          <DealCrib
            dealer={dealer}
            hands={hands}
            crib={crib}
            starterCard={starterCard}
            getScores={getScores}
            setHands={setHands}
            setCrib={setCrib}
            setStarterCard={setStarterCard}
            setPlayerTurn={setPlayerTurn}
            setGameScores={setGameScores}
            setMessage={setMessage}
            setStage={setCurrentStage}
            displayDeck={displayDeck}
            cardsInPlay={cardsInPlay}
          />
        );
      case PLAY_ROUND:
        return (
          <>
            <div className="top-row ai-hand">
              <div className="crib-container">
                {dealer === OPP_ID && <Crib cards={crib} />}
              </div>
              {hands.length !== 0 && (
                <Hand pid={OPP_ID} cards={hands[OPP_ID].cards} />
              )}
            </div>
            <div className="middle-row">
              <div className="deck-cards">{displayDeck()}</div>
              <PlayedCards cards={playedCards} oldCards={oldPlayedCards} />
              <div className="count">{count}</div>
            </div>
            <div className="bottom-row user-hand">
              <div className="crib-container">
                {dealer === USER_ID && <Crib cards={crib} />}
              </div>
              {hands.length !== 0 && (
                <Hand
                  pid={USER_ID}
                  cards={hands[USER_ID].cards}
                  onCardClick={onHandCardClick}
                  selectedCards={selectedCards}
                />
              )}
            </div>
          </>
        );
      case COUNT_HANDS:
        return COUNT_HANDS;
      case COUNT_CRIB:
        return COUNT_CRIB;
      default:
        return null;
    }
  }

  return (
    <div className="Game">
      <div className="main-screen">{stageSwitch()}</div>
      <div className="right-bar">
        <Scoreboard gameScores={gameScores} dealer={dealer} />
        <Message message={message} />
      </div>
    </div>
  );
}

export default Game;
