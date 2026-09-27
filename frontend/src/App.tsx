import React, { useState } from 'react';
import type { Difficulty, GameState } from './types/sudoku';
import { GameApi } from './services/gameApi';
import { StarBackground } from './components/StarBackground';
import { LandingPage } from './pages/LandingPage';
import { GamePage } from './pages/GamePage';
import { Loader2 } from 'lucide-react';

type AppView = 'LANDING' | 'GAME';

export const App: React.FC = () => {
  const [view, setView] = useState<AppView>('LANDING');
  const [currentGame, setCurrentGame] = useState<GameState | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleStartNewGame = async (difficulty: Difficulty) => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const newGame = await GameApi.createGame({ difficulty });
      setCurrentGame(newGame);
      setView('GAME');
    } catch (err: any) {
      console.error('Error starting new game:', err);
      setErrorMessage(err.message || 'Unable to connect to game server. Ensure backend is running.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleContinueGame = (game: GameState) => {
    setCurrentGame(game);
    setView('GAME');
  };

  const handleReturnToLanding = () => {
    setCurrentGame(null);
    setView('LANDING');
  };

  return (
    <div className="relative min-h-screen bg-[#02040a] text-slate-100 font-sans flex flex-col justify-center selection:bg-cyan-500/30 selection:text-cyan-200">
      {/* Background Subtle Moving Starfield Canvas */}
      <StarBackground />

      {/* Global Error Banner */}
      {errorMessage && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 max-w-md w-full px-4">
          <div className="p-3 rounded-2xl bg-rose-950/90 border border-rose-500/50 text-rose-200 text-xs font-medium text-center shadow-lg backdrop-blur-md">
            {errorMessage}
            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="ml-2 text-rose-400 hover:text-white underline cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isLoading && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex flex-col items-center justify-center">
          <Loader2 className="w-10 h-10 text-cyan-400 animate-spin mb-3" />
          <p className="text-sm font-semibold tracking-wider text-cyan-200">Initializing Cosmic Grid...</p>
        </div>
      )}

      {/* Views */}
      {view === 'LANDING' && (
        <LandingPage
          onStartNewGame={handleStartNewGame}
          onContinueGame={handleContinueGame}
        />
      )}

      {view === 'GAME' && currentGame && (
        <GamePage
          initialGame={currentGame}
          onHome={handleReturnToLanding}
          onNewGameRequest={handleStartNewGame}
        />
      )}
    </div>
  );
};

export default App;
