import "./countCrib.css";
import api from "../../../api/axiosConfig.js";
import { useEffect } from "react";

import { USER_ID, OPP_ID, PROCESS_DELAY_MS } from "../../../global/vars.js";

import { DEAL_HANDS } from "../../../global/stages.js";
import timeout from "../../../global/timeout.js";

import Crib from "../../crib";

const TOTAL_POINTS = 0;
const RUNS = 1;
const PAIRS = 2;
const FIFTEEN = 3;
const FLUSH = 4;
const NOBS = 5;

function CountCrib({
  dealer,
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

  const getCribScore = async () => {
    try {
      const promise = await api.post("game/countCrib");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const countCrib = async () => {
    getCribScore().then((cribResponse) => {
      const cribScores = cribResponse.data;

      getScores.then((gameScoreResponse) => {
        setGameScores(gameScoreResponse.data);

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
      });
    });
  };

  return (
    <>
      <div className="top-row ai-hand">
        <div className="crib-container"></div>
        <div className="crib-show">
          {dealer === OPP_ID && <Crib cards={crib} />}
        </div>
      </div>
      <div className="middle-row">
        <div className="deck-cards">{displayDeck()}</div>
      </div>
      <div className="bottom-row user-hand">
        <div className="crib-container"></div>
        <div className="crib-show">
          {dealer === USER_ID && <Crib cards={crib} />}
        </div>
      </div>
    </>
  );
}

export default CountCrib;
