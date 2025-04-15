package com.example.demo.repository;

import com.example.demo.entity.SortingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SortingHistoryRepository extends JpaRepository<SortingHistory, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM SortingHistory f WHERE f.id = :sortingId")
    void deleteBySortingId(@Param("sortingId") Long sortingId);
}
