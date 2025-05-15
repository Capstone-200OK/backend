package com.example.demo.repository;

import com.example.demo.entity.SortingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SortingHistoryRepository extends JpaRepository<SortingHistory, Long> {

    /**
     * 정리 기록 ID로 정리 기록 삭제
     *
     * @param sortingId 삭제할 정리 기록 ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SortingHistory f WHERE f.id = :sortingId")
    void deleteBySortingId(@Param("sortingId") Long sortingId);

    /**
     * 사용자 ID로 정리 기록 전체 조회 (최신 순 정렬)
     *
     * @param userId 사용자 ID
     * @return 정리 기록 리스트
     */
    List<SortingHistory> findAllByUserIdOrderBySortedAtDesc(Long userId);

    /**
     * 사용자 ID로 가장 최근 정리 기록 1건 조회
     *
     * @param userId 사용자 ID
     * @return Optional로 반환되는 정리 기록
     */
    Optional<SortingHistory> findTopByUserIdOrderBySortedAtDesc(Long userId);
}
