import "./game.css";
import api from "../../api/axiosConfig.js";
import Scoreboard from "../scoreboard";
import Message from "../message";
import Card from "../card";
import Hand from "../hand";
import Crib from "../crib";
import SendToCrib from "../sendToCrib";
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

const PROCESS_DELAY_MS = 500;

function Game({ numPlayers }) {
  const [currentStage, setCurrentStage] = useState(0);
  const [message, setMessage] = useState("");
  const [interactableDealerCards, setInteractableDealerCards] = useState(true);
  const [userDealerCard, setUserDealerCard] = useState(null);
  const [aiDealerCard, setAiDealerCard] = useState(null);
  const [gameScores, setGameScores] = useState(Array(numPlayers));
  const [dealer, setDealer] = useState(-1);
  const [hands, setHands] = useState([]);
  const [selectedCards, setSelectedCards] = useState([]);
  const [crib, setCrib] = useState([]);

  const pickedDealerCardId = useRef(-1);
  const aiDealerCardId = useRef(-1);
  const cardsInPlay = useRef(0);

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
                <SendToCrib
                  selectedCards={selectedCards}
                  onClick={onCribButtonClick}
                />
              </div>
            </div>
          </>
        );
      case PLAY_ROUND:
        return PLAY_ROUND;
      case COUNT_HANDS:
        return COUNT_HANDS;
      case COUNT_CRIB:
        return COUNT_CRIB;
      default:
        return null;
    }
  }

  const getScores = async () => {
    api
      .get("game/scores")
      .then((response) => {
        setGameScores(response.data);
      })
      .catch((err) => {
        console.error(err);
      });
  };

  useEffect(() => {
    getScores();

    if (currentStage === DRAW_DEALER) {
      api.post("game/reset_game");
      setMessage(
        "Pick a card to decide who is the dealer. The player with the lowest card deals first."
      );
    } else if (currentStage === DEAL_CRIB) {
      api
        .post("game/deal")
        .then((response) => {
          setHands(response.data);
          cardsInPlay.current =
            response.data[USER_ID].cards.length +
            response.data[OPP_ID].cards.length;
        })
        .catch((err) => {
          console.error(err);
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
      api
        .get("/game/dealer_card")
        .then((response) => {
          if (response.data.rankValue === 1 || response.data.rankValue === 8) {
            setMessage(
              "You drew an " + response.data.rank.toLowerCase() + ". "
            );
          } else {
            setMessage("You drew a " + response.data.rank.toLowerCase() + ". ");
          }
          setUserDealerCard(response.data);
        })
        .catch((err) => {
          console.error(err);
        });
    }
  }, [interactableDealerCards]);

  useEffect(() => {
    if (userDealerCard) {
      const timeout = setTimeout(() => {
        api
          .get("/game/dealer_card")
          .then((response) => {
            setAiDealerCard(response.data);

            let newMessage = "";
            if (
              response.data.rankValue === 1 ||
              response.data.rankValue === 8
            ) {
              newMessage =
                "Your opponent drew an " +
                response.data.rank.toLowerCase() +
                ". ";
            } else {
              newMessage =
                "Your opponent drew a " +
                response.data.rank.toLowerCase() +
                ". ";
            }

            if (response.data.rankValue === userDealerCard.rankValue) {
              newMessage += "There was a tie; please draw again.";
              resetDealerCards();
            } else if (response.data.rankValue < userDealerCard.rankValue) {
              newMessage += "Your opponent will deal first.";
              api
                .post("game/dealer/1")
                .then((response) => {
                  setDealer(response.data);
                })
                .catch((err) => {
                  console.error(err);
                });
            } else {
              newMessage += "You will deal first.";
              api
                .post("game/dealer/0")
                .then((response) => {
                  setDealer(response.data);
                })
                .catch((err) => {
                  console.error(err);
                });
            }

            setMessage(newMessage);
          })
          .catch((err) => {
            console.error(err);
          })
          .finally(function () {
            api.post("/game/reset_deck");
          });
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(timeout);
    }
  }, [userDealerCard]);

  useEffect(() => {
    if (aiDealerCard && userDealerCard) {
      const timeout = setTimeout(() => {
        resetDealerCards();
        setCurrentStage(currentStage + 1);
      }, PROCESS_DELAY_MS);

      return () => clearTimeout(timeout);
    }
  }, [aiDealerCard, userDealerCard]);

  function onDealerCardClick(cardId) {
    setInteractableDealerCards(false);
    pickedDealerCardId.current = cardId;
  }

  function onCribCardClick(cardId) {
    let temp = [...selectedCards];

    if (selectedCards.length == 2 && !selectedCards.includes(cardId)) {
      setMessage(
        "Only two cards can be sent to the crib. " +
          "Please unselect another card and then select this card again."
      );
      return;
    }

    // Remove the card if it was already selected. Otherwise, add it to the array
    let cardInfo = decipherCardById(cardId);
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
    for (let i = 0; i < DECK_SIZE; i++) {
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
    for (let i = 0; i < DECK_SIZE - cardsInPlay.current; i++) {
      deckCards.push(<Card key={i} id={i} hidden />);
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
