import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainMenu from "./components/layout/MainMenu";
import Login from "./components/auth/login";
import Register from "./components/auth/register";
import Game from "./components/game/game";
import GameRoom from "./components/game/GameRoom";
import Leaderboard from "./components/common/leaderboard";

const App = () => (
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<MainMenu />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/game" element={<Game />} />
      <Route path="/game-room/:matchId" element={<GameRoom />} />
      <Route path="/leaderboard" element={<Leaderboard />} />
    </Routes>
  </BrowserRouter>
);

export default App;