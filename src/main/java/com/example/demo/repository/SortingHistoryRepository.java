package com.example.demo.repository;

import com.example.demo.entity.SortingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SortingHistoryRepository extends JpaRepository<SortingHistory, Long> {
}
