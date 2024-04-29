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
const CLUB_ID = 0;
const DIAMOND_ID = 1;
const HEART_ID = 2;
const SPADE_ID = 3;

const PROCESS_DELAY_MS = 1000;

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
  const [playedCards, setPlayedCards] = useState([]);
  const [oldPlayedCards, setOldPlayedCards] = useState([]);

  const pickedDealerCardId = useRef(-1);
  const aiDealerCardId = useRef(-1);
  const cardsInPlay = useRef(0);

  // API CALLS
  const getScores = async () => {
    try {
      const response = await api.get("game/scores");
      setGameScores(response.data);
    } catch (err) {
      console.error(err);
    }
  };

  const getDealerCard = async () => {
    try {
      const response = await api.get("/game/dealer_card");
      return response;
    } catch (err) {
      console.error(err);
    }
  };

  const postDealer = async (dealer) => {
    try {
      const response = await api.post("game/dealer/" + dealer);
      setDealer(dealer);
      return response;
    } catch (err) {
      console.error(err);
    }
  };

  const dealCards = async () => {
    try {
      const response = await api.post("game/deal");
      return response;
    } catch (err) {
      console.error(err);
    }
  };

  const moveToCrib = async (card) => {
    try {
      const response = await api.post(
        "game/move/" + USER_ID + "/" + card.suitValue + "/" + card.rankValue
      );
      return response;
    } catch (err) {
      console.error(err);
    }
  };

  const pickAIHand = async () => {
    try {
      const response = await api.post("game/ai/hands");
      return response;
    } catch (err) {
      console.error(err);
    }
  };

  const getStarterCard = async () => {
    try {
      const response = await api.get("game/starter");
      return response;
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
    getScores();

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
          setMessage("You drew an " + card.rank.toLowerCase() + ". ");
        } else {
          setMessage("You drew a " + card.rank.toLowerCase() + ". ");
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
              newMessage =
                "Your opponent drew an " + card.rank.toLowerCase() + ". ";
            } else {
              newMessage =
                "Your opponent drew a " + card.rank.toLowerCase() + ". ";
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
              "The starter card is an " +
                card.rank.toLowerCase() +
                " of " +
                card.suit.toLowerCase() +
                "s. "
            );
          } else {
            setMessage(
              "The starter card is a " +
                card.rank.toLowerCase() +
                " of " +
                card.suit.toLowerCase() +
                "s. "
            );

            const stageTimeout = setTimeout(() => {
              setCurrentStage(PLAY_ROUND);
              setPlayerTurn(dealer === 0 ? 1 : 0);
            }, PROCESS_DELAY_MS);

            return () => clearTimeout(stageTimeout);
          }
        });
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(starterTimeout);
    }
  }, [crib]);

  useEffect(() => {
    if (playerTurn === 0) {
    }
  }, [playerTurn, playedCards]);

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
                  onCardClick={onCribCardClick}
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

  let suitId = Math.floor(cardId / CARDS_PER_SUIT);
  if (suitId === CLUB_ID) {
    cardInfo["suit"] = "CLUB";
  } else if (suitId === DIAMOND_ID) {
    cardInfo["suit"] = "DIAMOND";
  } else if (suitId === HEART_ID) {
    cardInfo["suit"] = "HEART";
  } else {
    cardInfo["suit"] = "SPADE";
  }

  cardInfo["rank"] =
    cardId % CARDS_PER_SUIT === 0 ? 13 : cardId % CARDS_PER_SUIT;
}

export default Game;
