import "./playRound.css";
import api from "../../../api/axiosConfig.js";
import { useState, useEffect, useRef } from "react";

import { USER_ID, OPP_ID, PROCESS_DELAY_MS } from "../../../global/vars.js";

import { COUNT_HANDS } from "../../../global/stages.js";
import timeout from "../../../global/timeout.js";

import Hand from "../../hand";
import Crib from "../../crib";
import PlayedCards from "../../playedCards";

const ROUND_POINT_CATEGORIES = 4;
const TOTAL_POINTS = 0;
const RUNS = 1;
const PAIRS = 2;
const SPECIAL = 3;

const MAX_COUNT = 31;
const MAX_RETRIES = 20;

function PlayRound({
  dealer,
  hands,
  crib,
  setHands,
  setGameScores,
  setMessage,
  setStage,
  displayDeck,
}) {
  const [playerTurn, setPlayerTurn] = useState((dealer + 1) % 2);
  const [count, setCount] = useState(0);
  const [playedCards, setPlayedCards] = useState([]);
  const [oldPlayedCards, setOldPlayedCards] = useState([]);
  const playerOnGo = useRef(false);

  // API CALLS
  const playCard = async (cardInfo, attempts = 0) => {
    try {
      const suitValue = cardInfo["suitValue"];
      const rankValue = cardInfo["rankValue"];
      const promise = await api.post(
        `game/play/${USER_ID}/${suitValue}/${rankValue}`
      );
      return promise;
    } catch (err) {
      if (attempts === MAX_RETRIES) throw err;

      return playCard(cardInfo, attempts + 1);
    }
  };

  const playAICard = async (attempts = 0) => {
    try {
      const promise = await api.post(`game/ai/play/${OPP_ID}`);
      return promise;
    } catch (err) {
      if (attempts === MAX_RETRIES) throw err;

      return playAICard(attempts + 1);
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

  const getScores = async () => {
    try {
      const promise = await api.get("game/scores");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  // For after when a card has been played
  useEffect(() => {
    // We do not want to run anything if it is the start of the round and the user's turn.
    // We want all this to run if it is the AI's turn so that they are forced into playing a card
    // at the end of this function.
    const startOfRound = playedCards.length + oldPlayedCards.length === 0;

    if (startOfRound && playerTurn !== OPP_ID) return;

    Promise.all([getNextPlayer(), movePossible()]).then(async (responses) => {
      await timeout(PROCESS_DELAY_MS);
      let nextPlayer = responses[0].data;
      const movePossible = responses[1].data;
      const otherPlayer = (nextPlayer + 1) % 2;

      // Set message box if a player calls go but the other player can still play a card
      // (if the server says that the next player is the same one that played the most
      // recent card, then the other player must not be able to play a card)
      if (
        !startOfRound &&
        nextPlayer === playerTurn &&
        hands[otherPlayer].cards.length > 0 &&
        !playerOnGo.current &&
        count !== 31
      ) {
        playerOnGo.current = true;
        if (playerTurn === USER_ID) {
          setMessage("Your opponent calls go.");
        } else {
          setMessage("You cannot play a card, so you call go.");
        }
        await timeout(PROCESS_DELAY_MS);
      }

      if (!movePossible) {
        if (count !== 31) {
          playerOnGo.current = false;
          getScores().then(async (response) => {
            setGameScores(response.data);
            if (playerTurn === OPP_ID) {
              setMessage(
                "Your opponent earns 1 point for playing the last card."
              );
            } else {
              setMessage("You earn 1 point for playing the last card.");
            }
            await timeout(PROCESS_DELAY_MS);
          });
        }

        resetCount();

        let oldCards = [...oldPlayedCards];
        oldCards.push(...playedCards);
        setOldPlayedCards(oldCards);
        setPlayedCards([]);
        setCount(0);
      } else {
        setPlayerTurn(nextPlayer);
        if (nextPlayer === OPP_ID) {
          setMessage("It is your opponent's turn to select a card.");
          manageAITurn();
        } else if (nextPlayer === USER_ID) {
          setMessage("It is your turn. Please select a card.");
        }
      }

      if (hands[USER_ID].cards.length + hands[OPP_ID].cards.length === 0) {
        const endRound = async () => {
          await timeout(PROCESS_DELAY_MS);
          setMessage("The round is over.");
          await timeout(PROCESS_DELAY_MS);
          setStage(COUNT_HANDS);
        };

        endRound();
        return;
      }
    });
  }, [playedCards]);

  // For when it is the AI's turn to play a card
  function manageAITurn() {
    playAICard().then(async (response) => {
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
        getScores().then((response) => {
          setGameScores(response.data);
        });
      }

      setMessage(newMessage);

      setCount(count + playedCard.value);
    });
  }

  // INTERACTION
  function onHandCardClick(cardId) {
    if (playerTurn !== USER_ID) {
      return;
    }

    const card = hands[USER_ID].cards.find(
      (cardInfo) => cardInfo.cardId === cardId
    );

    if (count + card.value > MAX_COUNT) {
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
      // means the count is 15 or 31 AND go was called)
      if (
        pointCategories[SPECIAL] % 2 === 1 &&
        hands[USER_ID].cards.length === 0
      ) {
        newMessage += `\n\nYou earned 1 point for playing the last card.`;
      }

      getScores().then((response) => {
        setGameScores(response.data);
      });

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
    });
  }

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
          />
        )}
      </div>
    </>
  );
}

export default PlayRound;
