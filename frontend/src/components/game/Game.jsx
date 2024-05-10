import "./game.css";
import api from "../../api/axiosConfig.js";
import Scoreboard from "../scoreboard";
import Message from "../message";
import Card from "../card";
import Hand from "../hand";
import Crib from "../crib";
import SendToCrib from "../sendToCrib";
import PlayedCards from "../playedCards";
import { useState, useEffect, useRef } from "react";

// Game Stages
const DRAW_DEALER = 0;
const DEAL_CRIB = 1;
const PLAY_ROUND = 2;
const COUNT_HANDS = 3;
const COUNT_CRIB = 4;

const USER_ID = 0;
const OPP_ID = 1;
const DECK_SIZE = 52;
const CARDS_PER_SUIT = 13;
const MAX_COUNT = 31;
const WINNING_SCORE = 121;

const ROUND_POINT_CATEGORIES = 4;
const TOTAL_POINTS = 0;
const RUNS = 1;
const PAIRS = 2;
const SPECIAL = 3;

const PROCESS_DELAY_MS = 500;

function Game({ numPlayers }) {
  const [currentStage, setCurrentStage] = useState(DRAW_DEALER);
  const [message, setMessage] = useState("");
  const [interactableDealerCards, setInteractableDealerCards] = useState(true);
  const [userDealerCard, setUserDealerCard] = useState(null);
  const [aiDealerCard, setAiDealerCard] = useState(null);
  const [gameScores, setGameScores] = useState(Array(numPlayers));
  const [dealer, setDealer] = useState(-1);
  const [hands, setHands] = useState([]);
  const [selectedCards, setSelectedCards] = useState([]);
  const [crib, setCrib] = useState([]);
  const [starterCard, setStarterCard] = useState(null);
  const [playerTurn, setPlayerTurn] = useState(-1);
  const [count, setCount] = useState(0);
  const [playedCards, setPlayedCards] = useState([[], []]);
  const [oldPlayedCards, setOldPlayedCards] = useState([[], []]);
  const [winner, setWinner] = useState(-1);

  const pickedDealerCardId = useRef(-1);
  const aiDealerCardId = useRef(-1);
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

  const getDealerCard = async () => {
    try {
      const promise = await api.get("/game/dealer_card");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const postDealer = async (dealer) => {
    try {
      const promise = await api.post(`game/dealer/${dealer}`);
      setDealer(dealer);
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

  const moveToCrib = async (card) => {
    try {
      const promise = await api.post(
        `game/move/${USER_ID}/${card.suitValue}/${card.rankValue}`
      );
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const pickAIHand = async () => {
    try {
      const promise = await api.post("game/ai/hands");
      return promise;
    } catch (err) {
      console.error(err);
    }
  };

  const getStarterCard = async () => {
    try {
      const promise = await api.get("game/starter");
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
      const promise = await api.get(`game/ai/play/${OPP_ID}`);
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

  useEffect(() => {
    if (!interactableDealerCards) {
      getDealerCard().then((response) => {
        const card = response.data;
        if (card.rankValue === 1 || card.rankValue === 8) {
          setMessage(`You drew an ${card.rank.toLowerCase()}.`);
        } else {
          setMessage(`You drew a ${card.rank.toLowerCase()}.`);
        }
        setUserDealerCard(card);
      });
    }
  }, [interactableDealerCards]);

  useEffect(() => {
    if (userDealerCard) {
      const timeout = setTimeout(async () => {
        getDealerCard()
          .then((response) => {
            const card = response.data;
            setAiDealerCard(card);

            let newMessage = "";
            if (card.rankValue === 1 || card.rankValue === 8) {
              newMessage = `Your opponent drew an ${card.rank.toLowerCase()}. `;
            } else {
              newMessage = `Your opponent drew a ${card.rank.toLowerCase()}. `;
            }

            if (card.rankValue === userDealerCard.rankValue) {
              newMessage += "There was a tie; please draw again.";
              resetDealerCards();
            } else if (card.rankValue < userDealerCard.rankValue) {
              newMessage += "Your opponent will deal first.";
              postDealer(OPP_ID);
            } else {
              newMessage += "You will deal first.";
              postDealer(USER_ID);
            }

            setMessage(newMessage);
          })
          .finally(() => {
            resetDeck();
          });
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(timeout);
    }
  }, [userDealerCard]);

  useEffect(() => {
    if (aiDealerCard && userDealerCard) {
      const timeout = setTimeout(() => {
        resetDealerCards();
        setCurrentStage(DEAL_CRIB);
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(timeout);
    }
  }, [aiDealerCard, userDealerCard]);

  useEffect(() => {
    if (crib.length === 2) {
      crib.forEach((card) => {
        moveToCrib(card);
      });

      const timeout = setTimeout(() => {
        pickAIHand().then((response) => {
          let newHands = [...hands];
          let fullCrib = [...crib];

          response.data.forEach((card) => {
            newHands[OPP_ID].cards = newHands[OPP_ID].cards.filter(
              (cardInfo) => cardInfo.cardId !== card.cardId
            );
            fullCrib.push(card);
          });

          setHands(newHands);
          setCrib(fullCrib);
        });
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(timeout);
    }

    if (crib.length === 4) {
      const starterTimeout = setTimeout(async () => {
        getStarterCard().then((response) => {
          const card = response.data;
          cardsInPlay.current++;
          setStarterCard(card);
          if (card.rankValue === 1 || card.rankValue === 8) {
            setMessage(
              `The starter card is an ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`
            );
          } else {
            setMessage(
              `The starter card is a ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`
            );
          }

          let nibsTimeout;
          if (card.rankValue === 11) {
            nibsTimeout = setTimeout(() => {
              setMessage("Dealer gets two points from his heels.");
              getScores().then((response) => {
                setGameScores(response.data);
              });
            });
          }

          const stageTimeout = setTimeout(() => {
            setCurrentStage(PLAY_ROUND);
            setPlayerTurn(dealer === USER_ID ? OPP_ID : USER_ID);
          }, PROCESS_DELAY_MS);

          return () => {
            clearTimeout(stageTimeout);
            clearTimeout(nibsTimeout);
          };
        });
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(starterTimeout);
    }
  }, [crib]);

  useEffect(() => {
    if (playerTurn === OPP_ID) {
      setMessage("It is your opponent's turn to select a card.");
    } else if (playerTurn === USER_ID) {
      setMessage("It is your turn. Please select a card.");
    }
  }, [playerTurn, playedCards]);

  // UI FUNCTIONALITY
  function onDealerCardClick(cardId) {
    setInteractableDealerCards(false);
    pickedDealerCardId.current = cardId;
  }

  function onCribCardClick(cardId) {
    if (crib.length >= 2) return;

    let temp = [...selectedCards];

    if (selectedCards.length === 2 && !selectedCards.includes(cardId)) {
      setMessage(
        "Only two cards can be sent to the crib. " +
          "Please unselect another card and then select this card again."
      );
      return;
    }

    // Remove the card if it was already selected. Otherwise, add it to the array
    if (selectedCards.includes(cardId)) {
      temp = temp.filter((id) => id !== cardId);
      if (temp.length === 0) {
        if (dealer === USER_ID) {
          setMessage("Select two cards that will be sent to your crib.");
        } else {
          setMessage(
            "Select two cards that will be sent to your opponent's crib."
          );
        }
      } else {
        setMessage("Select one more card for the crib.");
      }
    } else {
      temp.push(cardId);
      if (temp.length === 1) {
        setMessage("Select one more card for the crib.");
      } else {
        setMessage(
          'Click the "Send to Crib" button to move the selected cards to the crib.'
        );
      }
    }

    setSelectedCards(temp);
  }

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

    playCard(card).then((response) => {
      let newMessage;
      if (card.rankValue === 1 || card.rankValue === 8) {
        newMessage = `You played an ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`;
      } else {
        newMessage = `You played a ${card.rank.toLowerCase()} of ${card.suit.toLowerCase()}s.`;
      }

      const pointCategories = response.data;
      if (pointCategories[TOTAL_POINTS] === 0) {
        setMessage(newMessage);
        return;
      }

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
      if (pointCategories[SPECIAL] % 2 === 1) {
        if (hands[OPP_ID].cards.length > 0 || hands[USER_ID].cards.length > 0) {
          playerOnGo.current = true;
          pointCategories[TOTAL_POINTS]--;
          pointCategories[SPECIAL]--;
        } else {
          newMessage += `\n\nYou earned 1 point for playing the last card.`;
        }
      }

      let newGameScores = [...gameScores];
      newGameScores[USER_ID] += pointCategories[TOTAL_POINTS];
    });

    const newHands = [...hands];
    newHands[USER_ID].cards = newHands[USER_ID].cards.filter(
      (cardInfo) => cardInfo.cardId !== cardId
    );
    setHands(newHands);

    const newPlayedCards = [...playedCards];
    newPlayedCards[USER_ID].push(card);
    setPlayedCards(newPlayedCards);

    setCount(count + card.value);
  }

  function onCribButtonClick() {
    if (selectedCards.length !== 2) {
      return;
    }

    const selectedCardObjects = hands[USER_ID].cards.filter(
      (cardInfo) =>
        cardInfo.cardId === selectedCards[0] ||
        cardInfo.cardId === selectedCards[1]
    );
    setCrib([...selectedCardObjects]);

    let newHands = [...hands];
    newHands[USER_ID].cards = newHands[USER_ID].cards.filter(
      (cardInfo) =>
        cardInfo.cardId !== selectedCards[0] &&
        cardInfo.cardId !== selectedCards[1]
    );
    setHands(newHands);

    setSelectedCards([]);
    setMessage("Opponent currently selecting cards for the crib...");
  }

  function displayDealerCards() {
    if (
      userDealerCard !== null &&
      aiDealerCard !== null &&
      aiDealerCardId.current === -1
    ) {
      aiDealerCardId.current = pickedDealerCardId.current;
      while (aiDealerCardId.current === pickedDealerCardId.current) {
        aiDealerCardId.current = Math.floor(Math.random() * DECK_SIZE);
      }
    }

    let dealerCards = [];
    let displayCard = true;
    for (let i = 0; i < DECK_SIZE - cardsInPlay.current; i++) {
      if (i === aiDealerCardId.current || i === pickedDealerCardId.current) {
        displayCard = false;
      }

      dealerCards.push(
        <Card
          key={i}
          id={i}
          interactable={interactableDealerCards}
          onClick={onDealerCardClick}
          display={displayCard}
          hidden
        />
      );
      displayCard = true;
    }

    return dealerCards;
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

  function resetDealerCards() {
    setInteractableDealerCards(true);
    setUserDealerCard(null);
    setAiDealerCard(null);
    pickedDealerCardId.current = -1;
    aiDealerCardId.current = -1;
  }

  function stageSwitch() {
    switch (currentStage) {
      case DRAW_DEALER:
        return (
          <>
            <div className="top-row ai-dealer-card">
              {aiDealerCard && <Card cardInfo={aiDealerCard} />}
            </div>
            <div className="middle-row">
              <div className="dealer-cards">{displayDealerCards()}</div>
            </div>
            <div className="bottom-row user-dealer-card">
              {userDealerCard && <Card cardInfo={userDealerCard} />}
            </div>
          </>
        );
      case DEAL_CRIB:
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
            </div>
            <div className="bottom-row user-hand">
              <div className="crib-container">
                {dealer === USER_ID && <Crib cards={crib} />}
              </div>
              {hands.length !== 0 && (
                <Hand
                  pid={USER_ID}
                  cards={hands[USER_ID].cards}
                  onCardClick={onCribCardClick}
                  selectedCards={selectedCards}
                />
              )}
              <div className="crib-button-container">
                {crib.length < 2 && (
                  <SendToCrib
                    selectedCards={selectedCards}
                    onClick={onCribButtonClick}
                  />
                )}
              </div>
            </div>
          </>
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

function decipherCardById(cardId) {
  if (cardId > DECK_SIZE) {
    throw "Invalid card ID";
  }

  let cardInfo = {};

  cardInfo["suit"] = Math.floor(cardId / CARDS_PER_SUIT);

  cardInfo["rank"] =
    cardId % CARDS_PER_SUIT === 0 ? 13 : cardId % CARDS_PER_SUIT;

  cardInfo["rankValue"] = cardInfo["rank"] > 10 ? 10 : cardInfo["rank"];

  return cardInfo;
}

export default Game;
