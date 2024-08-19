import "./countHands.css";
import api from "../../../api/axiosConfig.js";
import { useEffect } from "react";

import { USER_ID, OPP_ID, PROCESS_DELAY_MS } from "../../../global/vars.js";

import { COUNT_CRIB } from "../../../global/stages.js";
import timeout from "../../../global/timeout.js";

import Hand from "../../hand";
import Crib from "../../crib";

const TOTAL_POINTS = 0;
const RUNS = 1;
const PAIRS = 2;
const FIFTEEN = 3;
const FLUSH = 4;
const NOBS = 5;

let visitedScores = [false, false, false];
let scoreMessages = ["", "", ""];

function CountHands({
  dealer,
  hands,
  crib,
  shownScore,
  setGameScores,
  setMessage,
  displayDeck,
}) {
  const getScores = async () => {
    try {
      const promise = await api.post("game/getScores");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const getHandScore = async (playerId) => {
    try {
      const promise = await api.post("game/countHand", {
        pid: playerId,
      });
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const getCribScore = async () => {
    try {
      const promise = await api.post("game/countCrib");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const countHand = async (pid) => {
    const handResponse = await getHandScore(pid);
    const handScores = handResponse.data;

    const gameScores = await getScores();
    setGameScores(gameScores.data);

    let newMessage;
    if (pid === USER_ID) {
      newMessage = `You earned ${handScores[TOTAL_POINTS]} points from your hand.\n`;
    } else {
      newMessage = `Your opponent earned ${handScores[TOTAL_POINTS]} points from their hand.\n`;
    }

    if (handScores[RUNS] > 0) {
      newMessage += `\n- ${handScores[RUNS]} points earned from runs`;
    }

    if (handScores[PAIRS] > 0) {
      newMessage += `\n- ${handScores[PAIRS]} points earned from pairs`;
    }

    if (handScores[FIFTEEN] > 0) {
      newMessage += `\n- ${handScores[FIFTEEN]} points earned from cards that add up to 15`;
    }

    if (handScores[FLUSH] > 0) {
      newMessage += `\n- ${handScores[FLUSH]} points earned from the flush`;
    }

    if (handScores[NOBS] > 0) {
      newMessage += `\n- ${handScores[NOBS]} point earned from nobs`;
    }

    scoreMessages[shownScore] = newMessage;
  };

  const countCrib = async () => {
    const cribResponse = await getCribScore();
    const cribScores = cribResponse.data;

    const gameScores = await getScores();
    setGameScores(gameScores.data);

    let newMessage;
    if (dealer === USER_ID) {
      newMessage = `You earned ${cribScores[TOTAL_POINTS]} points from the crib.\n`;
    } else {
      newMessage = `Your opponent earned ${cribScores[TOTAL_POINTS]} points from the crib.\n`;
    }

    if (cribScores[RUNS] > 0) {
      newMessage += `\n- ${cribScores[RUNS]} points earned from runs`;
    }

    if (cribScores[PAIRS] > 0) {
      newMessage += `\n- ${cribScores[PAIRS]} points earned from pairs`;
    }

    if (cribScores[FIFTEEN] > 0) {
      newMessage += `\n- ${cribScores[FIFTEEN]} points earned from cards that add up to 15`;
    }

    if (cribScores[FLUSH] > 0) {
      newMessage += `\n- ${cribScores[FLUSH]} points earned from the flush`;
    }

    if (cribScores[NOBS] > 0) {
      newMessage += `\n- ${cribScores[NOBS]} points earned from nobs`;
    }

    scoreMessages[shownScore] = newMessage;
  };

  useEffect(() => {
    if (shownScore === -1) {
      scoreMessages = ["", "", ""];
      visitedScores = [false, false, false];
      return;
    }

    if (visitedScores[shownScore]) {
      setMessage(scoreMessages[shownScore]);
      return;
    }

    const addHandScore = async () => {
      if (shownScore === 2) {
        await countCrib();
      } else {
        await countHand((dealer + 1 + shownScore) % 2);
      }
      setMessage(scoreMessages[shownScore]);
    };

    addHandScore();

    visitedScores[shownScore] = true;
  }, [shownScore]);

  return (
    <>
      <div className="top-row ai-hand">
        <div className="crib-container">
          {dealer === OPP_ID && shownScore !== 2 && <Crib cards={crib} />}
        </div>
        {hands.length !== 0 && (
          <Hand
            pid={OPP_ID}
            cards={
              shownScore === 2 && dealer === OPP_ID ? crib : hands[OPP_ID].cards
            }
            hidden={false}
          />
        )}
      </div>
      <div className="middle-row">
        <div className="deck-cards">{displayDeck()}</div>
      </div>
      <div className="bottom-row user-hand">
        <div className="crib-container">
          {dealer === USER_ID && shownScore !== 2 && <Crib cards={crib} />}
        </div>
        {hands.length !== 0 && (
          <Hand
            pid={USER_ID}
            cards={
              shownScore === 2 && dealer === USER_ID
                ? crib
                : hands[USER_ID].cards
            }
            interactable={false}
          />
        )}
      </div>
    </>
  );
}

export default CountHands;
