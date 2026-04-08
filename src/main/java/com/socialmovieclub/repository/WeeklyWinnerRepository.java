package com.socialmovieclub.repository;

import com.socialmovieclub.entity.WeeklyWinner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyWinnerRepository extends JpaRepository<WeeklyWinner, UUID> {

    Optional<WeeklyWinner> findTopByOrderByWeekEndDateDesc();

    // List yerine Page dönüyoruz
    Page<WeeklyWinner> findAllByOrderByWeekEndDateDesc(Pageable pageable);
}