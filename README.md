# Personal Project: Cribbage

## Project Description
This repository will contain code for a cribbage website. I decided to code up 
cribbage because it is a game that I love to play. My parents taught me how to 
play it back in 2019, and we almost always play a game when I am visiting, so 
it only felt right to spend this summer trying to create a program that can run
it. I also plan on using it as a warmup for a chess program that I want to 
create later on.

## How Cribbage Works
Cribbage is a card game with either two or three players. A standard game 
consists of multiple rounds where 6 cards (or 5 cards if there are three 
players) are dealt to each player at the beginning of each round. Each round 
consists of three stages:

1. After cards have been dealt, players choose four cards that they would like
to retain for the next two stages. Cards that they do not want to keep are 
placed face-down in what is called the "crib" (this will come into play in the 
third stage). If there are three players, one card is placed in the crib after 
five cards have been dealt to each player. The cards that were not dealt to any
players are then cut, and the top card of the lower half is turned face-up. 
This "starter card" will be used for the third stage.

2. In the playing stage, players take turns laying down cards face-up. The 
value of each card is added to the overall count which can not go above 31. 
Players are able to earn points in this stage through various means, including 
the completion of a pair, completing a run, and causing the count to reach 15. 
Once the count reaches 31 or nobody else is able to play, the count is reset to 
0, and this process repeats until everybody has played all of their cards.

3. In this final stage, each player goes through their cards and counts up the 
total number of points present in conjunction with the starter card. Points can 
be earned through runs, pairs, flushes, and combos of cards that add up to 15. 
Once all players have counted points for their hands, the dealer for that round 
will repeat this process for the crib, with all points present going towards 
their own total for the game.

The game continues until a player reaches 121 points, signaling that they have
won. Full rules can be seen [here](https://en.wikipedia.org/wiki/Cribbage).

## Architecture
For this project, I am developing a backend using almost entirely Java. It is 
the language I am most familiar with, and I figured it would make it easier 
for me to learn about creating high-quality tests as well as a REST API since 
I wouldn't have to learn another language in the process. This decision also 
resulted in me using Spring Boot as the backend application and Maven as the 
build tool. Each of these technologies are designed for use in Java programs, 
so it made sense to use each tool for their respective task.

For the frontend, I plan on using JavaScript and CSS as the primary languages.
In particular, I want to make use of the React library. I briefly learned about
it in one of my earlier computer science classes, and I was pretty impressed 
with how much could be done using it, so it is something I want to learn more 
about and get some practice with it.