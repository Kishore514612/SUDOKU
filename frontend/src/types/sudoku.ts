export type GameStatus = 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'ABANDONED';

export type Difficulty = 'Easy' | 'Medium' | 'Hard' | 'Custom';

export interface CellCoordinate {
  row: number;
  col: number;
}

export interface GameState {
  id: number;
  puzzleId: string;
  difficulty: Difficulty;
  status: GameStatus;
  startedAt: string;
  pausedAt: string | null;
  completedAt: string | null;
  elapsedSeconds: number;
  mistakes: number;
  board: number[][];
  initialBoard: number[][];
  completed: boolean;
  canUndo: boolean;
  canRedo: boolean;
}

export interface MoveResponse {
  valid: boolean;
  row: number;
  column: number;
  value: number;
  reason?: string;
  completed: boolean;
  mistakes: number;
  board: number[][];
  elapsedSeconds: number;
  canUndo: boolean;
  canRedo: boolean;
}

export interface SubmitResponse {
  completed: boolean;
  valid: boolean;
  message: string;
  mistakes: number;
  elapsedSeconds: number;
  incorrectCells: { row: number; column: number }[];
}

export interface CreateGameParams {
  difficulty?: Difficulty;
  puzzleId?: string;
  initialBoard?: number[][];
  solutionBoard?: number[][];
}
