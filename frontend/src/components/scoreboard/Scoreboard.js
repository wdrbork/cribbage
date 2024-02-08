import './Scoreboard.css';
import PlayerRow from './playerRow/PlayerRow.js';
import { useState } from 'react';

function Scoreboard({ gameScores }) {
	const scores = gameScores.map((score, pid) => {
		return (
			<PlayerRow pid={pid} score={score} />
		)
	});

  return (
		<div className="border">
			<div className="header">
				<p>Scores</p>
			</div>
			<div className="scores">
				{scores}
			</div>
		</div>
  )
}

export default Scoreboard;