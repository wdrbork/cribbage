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

function CountHands({
  dealer,
  hands,
  crib,
  setGameScores,
  setMessage,
  setStage,
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

  const countHand = async (pid) => {
    getHandScore(pid).then((handResponse) => {
      const handScores = handResponse.data;

      getScores().then((gameScoreResponse) => {
        setGameScores(gameScoreResponse.data);

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
          newMessage += `\n- ${handScores[NOBS]} points earned from nobs`;
        }

        setMessage(newMessage);
      });
    });
  };

  const countUpHands = async () => {
    if (dealer === OPP_ID) {
      countHand(USER_ID);
      await timeout(2 * PROCESS_DELAY_MS);
      countHand(OPP_ID);
    } else {
      countHand(OPP_ID);
      await timeout(2 * PROCESS_DELAY_MS);
      countHand(USER_ID);
    }

    await timeout(2 * PROCESS_DELAY_MS);
    setStage(COUNT_CRIB);
  };

  useEffect(() => {
    countUpHands();
  }, []);

  return (
    <>
      <div className="top-row ai-hand">
        <div className="crib-container">
          {dealer === OPP_ID && <Crib cards={crib} />}
        </div>
        {hands.length !== 0 && (
          <Hand pid={OPP_ID} cards={hands[OPP_ID].cards} hidden={false} />
        )}
      </div>
      <div className="middle-row">
        <div className="deck-cards">{displayDeck()}</div>
      </div>
      <div className="bottom-row user-hand">
        <div className="crib-container">
          {dealer === USER_ID && <Crib cards={crib} />}
        </div>
        {hands.length !== 0 && (
          <Hand
            pid={USER_ID}
            cards={hands[USER_ID].cards}
            interactable={false}
          />
        )}
      </div>
    </>
  );
}

export default CountHands;
