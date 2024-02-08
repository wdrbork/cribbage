import './PlayerRow.css';
import { useState } from 'react';

function PlayerRow({ pid, score }) {
    return (
        <tr>
          <td>Player {pid + 1}</td>
          <td>{score}</td>
        </tr>
    )
}

export default PlayerRow;