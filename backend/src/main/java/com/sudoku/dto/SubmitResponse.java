package com.sudoku.dto;

import java.util.ArrayList;
import java.util.List;

public class SubmitResponse {
    private boolean completed;
    private boolean valid;
    private String message;
    private int mistakes;
    private long elapsedSeconds;
    private List<CellPosition> incorrectCells = new ArrayList<>();

    public SubmitResponse() {}

    public static SubmitResponse success(long elapsedSeconds, int mistakes) {
        SubmitResponse res = new SubmitResponse();
        res.completed = true;
        res.valid = true;
        res.message = "Sudoku successfully completed!";
        res.elapsedSeconds = elapsedSeconds;
        res.mistakes = mistakes;
        return res;
    }

    public static SubmitResponse incomplete(int emptyCount, List<CellPosition> incorrectCells, int mistakes, long elapsedSeconds) {
        SubmitResponse res = new SubmitResponse();
        res.completed = false;
        res.valid = false;
        res.message = emptyCount > 0 ? "The board has " + emptyCount + " empty cell(s) remaining." : "The board contains errors.";
        res.incorrectCells = incorrectCells != null ? incorrectCells : new ArrayList<>();
        res.mistakes = mistakes;
        res.elapsedSeconds = elapsedSeconds;
        return res;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        this.mistakes = mistakes;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public List<CellPosition> getIncorrectCells() {
        return incorrectCells;
    }

    public void setIncorrectCells(List<CellPosition> incorrectCells) {
        this.incorrectCells = incorrectCells;
    }
}
