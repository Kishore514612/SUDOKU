package com.sudoku.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class PuzzleProvider {

    public record Puzzle(String id, String difficulty, int[][] initialBoard, int[][] solutionBoard) {}

    private final Random random = new Random();

    public Puzzle getPuzzle(String puzzleId) {
        return generateDynamicPuzzle("Medium");
    }

    public Puzzle getPuzzleByDifficulty(String difficulty) {
        String diff = (difficulty != null && !difficulty.trim().isEmpty()) ? difficulty : "Medium";
        return generateDynamicPuzzle(diff);
    }

    public Puzzle getDefaultPuzzle() {
        return generateDynamicPuzzle("Medium");
    }

    /**
     * Dynamically generates a brand new, random valid Sudoku puzzle and solution board.
     */
    public Puzzle generateDynamicPuzzle(String difficulty) {
        int[][] solutionBoard = new int[9][9];

        // 1. Fill diagonal 3x3 boxes randomly
        fillDiagonalBoxes(solutionBoard);

        // 2. Solve the remaining board using randomized backtracking
        solveSudoku(solutionBoard);

        // 3. Clone solution board for initial board
        int[][] initialBoard = new int[9][9];
        for (int r = 0; r < 9; r++) {
            System.arraycopy(solutionBoard[r], 0, initialBoard[r], 0, 9);
        }

        // 4. Remove cells according to chosen difficulty
        int cellsToRemove = getCellsToRemoveCount(difficulty);
        removeCells(initialBoard, cellsToRemove);

        String puzzleId = "dyn-" + difficulty.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return new Puzzle(puzzleId, capitalize(difficulty), initialBoard, solutionBoard);
    }

    private void fillDiagonalBoxes(int[][] board) {
        for (int i = 0; i < 9; i += 3) {
            fillBox(board, i, i);
        }
    }

    private void fillBox(int[][] board, int row, int col) {
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Collections.shuffle(nums, random);
        int idx = 0;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[row + r][col + c] = nums.get(idx++);
            }
        }
    }

    private boolean solveSudoku(int[][] board) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
                    Collections.shuffle(nums, random);

                    for (int num : nums) {
                        if (isValidPlacement(board, r, c, num)) {
                            board[r][c] = num;
                            if (solveSudoku(board)) {
                                return true;
                            }
                            board[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidPlacement(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            // Check row and column
            if (board[row][i] == num || board[i][col] == num) {
                return false;
            }
        }
        // Check 3x3 box
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (board[startRow + r][startCol + c] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getCellsToRemoveCount(String difficulty) {
        return switch (difficulty.toLowerCase()) {
            case "easy" -> 36 + random.nextInt(5);      // ~36-40 empty cells
            case "hard" -> 50 + random.nextInt(5);      // ~50-54 empty cells
            default -> 43 + random.nextInt(5);          // Medium: ~43-47 empty cells
        };
    }

    private void removeCells(int[][] board, int count) {
        int removed = 0;
        while (removed < count) {
            int cellId = random.nextInt(81);
            int r = cellId / 9;
            int c = cellId % 9;
            if (board[r][c] != 0) {
                board[r][c] = 0;
                removed++;
            }
        }
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "Medium";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
