package com.sudoku.service;

import com.sudoku.dto.*;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.Move;
import com.sudoku.model.SudokuBoard;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.MoveRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final SudokuValidationService validationService;
    private final PuzzleProvider puzzleProvider;

    public GameService(
            GameRepository gameRepository,
            MoveRepository moveRepository,
            SudokuValidationService validationService,
            PuzzleProvider puzzleProvider
    ) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.validationService = validationService;
        this.puzzleProvider = puzzleProvider;
    }

    public GameResponse createGame(CreateGameRequest request) {
        PuzzleProvider.Puzzle puzzle;
        if (request != null && request.getInitialBoard() != null && request.getInitialBoard().length == 9) {
            int[][] initial = request.getInitialBoard();
            int[][] solution = request.getSolutionBoard() != null ? request.getSolutionBoard() : new int[9][9];
            String diff = request.getDifficulty() != null ? request.getDifficulty() : "Custom";
            String pid = request.getPuzzleId() != null ? request.getPuzzleId() : "custom-" + System.currentTimeMillis();
            puzzle = new PuzzleProvider.Puzzle(pid, diff, initial, solution);
        } else if (request != null && request.getDifficulty() != null) {
            puzzle = puzzleProvider.getPuzzleByDifficulty(request.getDifficulty());
        } else if (request != null && request.getPuzzleId() != null) {
            puzzle = puzzleProvider.getPuzzle(request.getPuzzleId());
        } else {
            puzzle = puzzleProvider.getDefaultPuzzle();
        }

        SudokuBoard initialBoard = new SudokuBoard(puzzle.initialBoard());
        SudokuBoard solutionBoard = new SudokuBoard(puzzle.solutionBoard());

        Game game = new Game();
        game.setPuzzleId(puzzle.id());
        game.setDifficulty(puzzle.difficulty());
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());
        game.setElapsedSeconds(0);
        game.setMistakes(0);
        game.setInitialBoardJson(initialBoard.toJson());
        game.setCurrentBoardJson(initialBoard.toJson());
        game.setSolutionBoardJson(solutionBoard.toJson());

        Game savedGame = gameRepository.save(game);
        return GameResponse.fromGame(savedGame, false, false);
    }

    public GameResponse getGame(Long gameId) {
        Game game = findGameById(gameId);
        updateLiveElapsedTime(game);
        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
        return GameResponse.fromGame(game, canUndo, canRedo);
    }

    public Optional<GameResponse> getLatestResumableGame() {
        Optional<Game> latestOpt = gameRepository.findLatestResumable();
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }
        Game game = latestOpt.get();
        updateLiveElapsedTime(game);
        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(game.getId());
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(game.getId());
        return Optional.of(GameResponse.fromGame(game, canUndo, canRedo));
    }

    public MoveResponse makeMove(Long gameId, MoveRequest request) {
        Game game = findGameById(gameId);

        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is already completed");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused. Resume before making moves");
        }

        updateLiveElapsedTime(game);

        SudokuBoard initialBoard = SudokuBoard.fromJson(game.getInitialBoardJson());
        SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());

        int row = request.getRow();
        int col = request.getColumn();
        int value = request.getValue();

        // 1. Fixed cell check
        if (initialBoard.getCell(row, col) != 0) {
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
            return MoveResponse.invalid(
                    row, col, value,
                    "Cannot modify original puzzle cell",
                    game.getMistakes(),
                    currentBoard.getGrid(),
                    game.getElapsedSeconds(),
                    canUndo, canRedo
            );
        }

        int previousValue = currentBoard.getCell(row, col);
        if (previousValue == value) {
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
            return MoveResponse.valid(
                    row, col, value,
                    false,
                    game.getMistakes(),
                    currentBoard.getGrid(),
                    game.getElapsedSeconds(),
                    canUndo, canRedo
            );
        }

        // 2. Validate move against Sudoku rules
        // Create temporary board without the target cell to validate candidate value
        SudokuBoard tempBoard = new SudokuBoard(currentBoard.getGrid());
        tempBoard.setCell(row, col, 0);
        SudokuValidationService.ValidationResult validation = validationService.validateMove(tempBoard, row, col, value);

        if (!validation.valid() && value != 0) {
            // Count mistake
            game.setMistakes(game.getMistakes() + 1);
            gameRepository.save(game);
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
            return MoveResponse.invalid(
                    row, col, value,
                    validation.reason(),
                    game.getMistakes(),
                    currentBoard.getGrid(),
                    game.getElapsedSeconds(),
                    canUndo, canRedo
            );
        }

        // Clear redo branch on new move
        moveRepository.deleteUndoneMovesByGameId(gameId);

        // Update board
        currentBoard.setCell(row, col, value);
        game.setCurrentBoardJson(currentBoard.toJson());

        // Save move history
        int nextMoveNumber = (int) moveRepository.countByGameId(gameId) + 1;
        Move move = new Move(gameId, row, col, value, previousValue, nextMoveNumber);
        moveRepository.save(move);

        // Check if solved
        boolean solved = validationService.isBoardSolved(currentBoard);
        if (solved) {
            game.setStatus(GameStatus.COMPLETED);
            game.setCompletedAt(LocalDateTime.now());
        }

        gameRepository.save(game);

        boolean canUndo = true;
        boolean canRedo = false;

        return MoveResponse.valid(
                row, col, value,
                solved,
                game.getMistakes(),
                currentBoard.getGrid(),
                game.getElapsedSeconds(),
                canUndo, canRedo
        );
    }

    public GameResponse undoMove(Long gameId) {
        Game game = findGameById(gameId);
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot undo moves on a completed game");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused");
        }

        updateLiveElapsedTime(game);

        Optional<Move> lastActiveMoveOpt = moveRepository.findTopByGameIdAndUndoneFalseOrderByMoveNumberDesc(gameId);
        if (lastActiveMoveOpt.isPresent()) {
            Move move = lastActiveMoveOpt.get();
            SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
            currentBoard.setCell(move.getRow(), move.getColumn(), move.getPreviousValue());
            game.setCurrentBoardJson(currentBoard.toJson());

            move.setUndone(true);
            moveRepository.save(move);
            gameRepository.save(game);
        }

        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
        return GameResponse.fromGame(game, canUndo, canRedo);
    }

    public GameResponse redoMove(Long gameId) {
        Game game = findGameById(gameId);
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot redo moves on a completed game");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused");
        }

        updateLiveElapsedTime(game);

        Optional<Move> nextUndoneMoveOpt = moveRepository.findTopByGameIdAndUndoneTrueOrderByMoveNumberAsc(gameId);
        if (nextUndoneMoveOpt.isPresent()) {
            Move move = nextUndoneMoveOpt.get();
            SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
            currentBoard.setCell(move.getRow(), move.getColumn(), move.getValue());
            game.setCurrentBoardJson(currentBoard.toJson());

            move.setUndone(false);
            moveRepository.save(move);

            if (validationService.isBoardSolved(currentBoard)) {
                game.setStatus(GameStatus.COMPLETED);
                game.setCompletedAt(LocalDateTime.now());
            }

            gameRepository.save(game);
        }

        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
        return GameResponse.fromGame(game, canUndo, canRedo);
    }

    public GameResponse pauseGame(Long gameId) {
        Game game = findGameById(gameId);
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed game cannot be paused");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            return GameResponse.fromGame(game,
                    moveRepository.existsByGameIdAndUndoneFalse(gameId),
                    moveRepository.existsByGameIdAndUndoneTrue(gameId));
        }

        updateLiveElapsedTime(game);
        game.setStatus(GameStatus.PAUSED);
        game.setPausedAt(LocalDateTime.now());
        gameRepository.save(game);

        return GameResponse.fromGame(game,
                moveRepository.existsByGameIdAndUndoneFalse(gameId),
                moveRepository.existsByGameIdAndUndoneTrue(gameId));
    }

    public GameResponse resumeGame(Long gameId) {
        Game game = findGameById(gameId);
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed game cannot be resumed");
        }
        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            return GameResponse.fromGame(game,
                    moveRepository.existsByGameIdAndUndoneFalse(gameId),
                    moveRepository.existsByGameIdAndUndoneTrue(gameId));
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setPausedAt(null);
        game.setStartedAt(LocalDateTime.now()); // New baseline for elapsed calculation
        gameRepository.save(game);

        return GameResponse.fromGame(game,
                moveRepository.existsByGameIdAndUndoneFalse(gameId),
                moveRepository.existsByGameIdAndUndoneTrue(gameId));
    }

    public SubmitResponse submitGame(Long gameId) {
        Game game = findGameById(gameId);
        updateLiveElapsedTime(game);

        SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
        SudokuBoard solutionBoard = game.getSolutionBoardJson() != null ? SudokuBoard.fromJson(game.getSolutionBoardJson()) : null;

        boolean isSolved = validationService.isBoardSolved(currentBoard);

        if (isSolved) {
            game.setStatus(GameStatus.COMPLETED);
            game.setCompletedAt(LocalDateTime.now());
            gameRepository.save(game);
            return SubmitResponse.success(game.getElapsedSeconds(), game.getMistakes());
        }

        // Find empty count & incorrect cells
        int emptyCount = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (currentBoard.getCell(r, c) == 0) {
                    emptyCount++;
                }
            }
        }

        List<CellPosition> incorrectCells = validationService.findIncorrectCells(currentBoard, solutionBoard);
        return SubmitResponse.incomplete(emptyCount, incorrectCells, game.getMistakes(), game.getElapsedSeconds());
    }

    private void updateLiveElapsedTime(Game game) {
        if (game.getStatus() == GameStatus.IN_PROGRESS && game.getStartedAt() != null) {
            long currentSessionSeconds = Duration.between(game.getStartedAt(), LocalDateTime.now()).getSeconds();
            if (currentSessionSeconds > 0) {
                game.setElapsedSeconds(game.getElapsedSeconds() + currentSessionSeconds);
                game.setStartedAt(LocalDateTime.now());
                gameRepository.save(game);
            }
        }
    }

    private Game findGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with ID: " + gameId));
    }
}
