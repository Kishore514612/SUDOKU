package com.sudoku.controller;

import com.sudoku.dto.*;
import com.sudoku.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Create/start a new game using a puzzle supplied by the system or custom request.
     */
    @PostMapping
    public ResponseEntity<GameResponse> createGame(@RequestBody(required = false) CreateGameRequest request) {
        GameResponse response = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve the current game state by ID.
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable Long gameId) {
        GameResponse response = gameService.getGame(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the most recent active/unfinished game (IN_PROGRESS or PAUSED) for "Continue Game".
     */
    @GetMapping("/active")
    public ResponseEntity<GameResponse> getActiveGame() {
        Optional<GameResponse> activeGame = gameService.getLatestResumableGame();
        return activeGame.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Submit a player's move.
     */
    @PostMapping("/{gameId}/move")
    public ResponseEntity<MoveResponse> submitMove(
            @PathVariable Long gameId,
            @Valid @RequestBody MoveRequest request
    ) {
        MoveResponse response = gameService.makeMove(gameId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Undo the most recent user move.
     */
    @PostMapping("/{gameId}/undo")
    public ResponseEntity<GameResponse> undoMove(@PathVariable Long gameId) {
        GameResponse response = gameService.undoMove(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * Redo a previously undone move.
     */
    @PostMapping("/{gameId}/redo")
    public ResponseEntity<GameResponse> redoMove(@PathVariable Long gameId) {
        GameResponse response = gameService.redoMove(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * Pause the current game.
     */
    @PostMapping("/{gameId}/pause")
    public ResponseEntity<GameResponse> pauseGame(@PathVariable Long gameId) {
        GameResponse response = gameService.pauseGame(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * Resume the current game.
     */
    @PostMapping("/{gameId}/resume")
    public ResponseEntity<GameResponse> resumeGame(@PathVariable Long gameId) {
        GameResponse response = gameService.resumeGame(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit/check the completed Sudoku.
     */
    @PostMapping("/{gameId}/submit")
    public ResponseEntity<SubmitResponse> submitGame(@PathVariable Long gameId) {
        SubmitResponse response = gameService.submitGame(gameId);
        return ResponseEntity.ok(response);
    }
}
