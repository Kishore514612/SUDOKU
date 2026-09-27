package com.sudoku.repository;

import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByStatusOrderByUpdatedAtDesc(GameStatus status);

    @Query("SELECT g FROM Game g WHERE g.status IN :statuses ORDER BY g.updatedAt DESC")
    List<Game> findByStatusInOrderByUpdatedAtDesc(List<GameStatus> statuses);

    default Optional<Game> findLatestResumable() {
        List<Game> resumable = findByStatusInOrderByUpdatedAtDesc(List.of(GameStatus.IN_PROGRESS, GameStatus.PAUSED));
        return resumable.isEmpty() ? Optional.empty() : Optional.of(resumable.get(0));
    }
}
